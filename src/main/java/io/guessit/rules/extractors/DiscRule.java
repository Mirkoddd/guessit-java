package io.guessit.rules.extractors;

import io.guessit.core.pipeline.contracts.Extractor;
import io.guessit.core.pipeline.state.Match;
import io.guessit.core.pipeline.state.MatchName;
import io.guessit.core.pipeline.state.MatchTag;
import io.guessit.core.pipeline.state.ParseContext;
import io.guessit.core.pipeline.state.Priority;
import io.guessit.core.text.Span;
import io.guessit.core.text.Validators;
import io.guessit.core.text.patterns.DiscPatterns;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Extracts {@code disc} (multi-disc release indices: "Disc 1", "DVD 2",
 * "BD3", …).
 *
 * <p>The post-pass also fixes up matches that {@link SeasonEpisodeExtractor}
 * recognised via the {@code D} marker (e.g. "S01D02") — those get tagged
 * {@code disc-marker} as episodes and are renamed here once the dust has
 * settled. Doing the rename in this rule keeps disc-related logic in one
 * place rather than scattered across the season/episode extractor.
 */
public final class DiscRule implements Extractor {

    public static final String EXTRACTOR_NAME = "disc";

    private static final String GRP_VAL = "val";

    private static final Pattern PATTERN = DiscPatterns.buildDiscPattern(GRP_VAL);

    @Override
    public String name() {
        return EXTRACTOR_NAME;
    }

    @Override
    public String description() {
        return "disc number (Disc 1, Disc 2, …)";
    }

    @Override
    public void extract(ParseContext ctx) {
        var input = ctx.input;
        var seps = Validators.sepsSurround(input);
        var m = PATTERN.matcher(input);

        while (m.find()) {
            var headSpan = new Span(m.start(), m.end(), m.group());

            var head = Match.string(MatchName.DISC, m.group(), headSpan, Priority.DEFAULT, Set.of(), false);

            if (seps.test(head)) {
                int v = Integer.parseInt(m.group(GRP_VAL));
                var valSpan = new Span(m.start(GRP_VAL), m.end(GRP_VAL), m.group(GRP_VAL));

                ctx.matches.add(Match.integer(MatchName.DISC, v, valSpan, Priority.DEFAULT, Set.of(), false));
            }
        }
    }

    /** Mirror Python RenameToDiscMatch: episodes from chains with `D` marker become discs. */
    @Override
    public void postProcess(ParseContext ctx) {
        var marked = ctx.matches.named(MatchName.EPISODE)
                .filter(m -> m.hasTag(MatchTag.DISC_MARKER))
                .toList();

        if (marked.isEmpty()) return;

        var renamed = marked.stream()
                .map(m -> {
                    if (m instanceof Match.IntegerMatch im) {
                        var newTags = new HashSet<>(im.tags());
                        newTags.remove(MatchTag.DISC_MARKER.getValue());
                        return Match.integer(MatchName.DISC, im.value(), im.span(), im.priority(), newTags, im.isPrivate());
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();

        marked.forEach(ctx.matches::remove);
        renamed.forEach(ctx.matches::add);
    }
}