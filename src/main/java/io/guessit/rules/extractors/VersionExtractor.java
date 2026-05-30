package io.guessit.rules.extractors;

import io.guessit.core.pipeline.contracts.Extractor;
import io.guessit.core.pipeline.state.Match;
import io.guessit.core.pipeline.state.MatchName;
import io.guessit.core.pipeline.state.ParseContext;
import io.guessit.core.text.Seps;
import io.guessit.core.text.Span;
import io.guessit.core.text.Validators;
import io.guessit.core.text.patterns.VersionPatterns;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Extracts release {@code version} (the {@code v2}, {@code v3} suffix on
 * scene-style names).
 *
 * <p>The regex is permissive — any {@code v\d+} substring becomes a candidate.
 * The post-pass enforces the real rule: a version match must either sit
 * immediately after an episode match (forming {@code "S01E01v2"}) or be
 * separator-surrounded; bare {@code "v2"} inside a title is dropped.
 */
public final class VersionExtractor implements Extractor {

    public static final String EXTRACTOR_NAME = "version";
    private static final String GRP_VAL = "val";

    private static final Pattern PATTERN = VersionPatterns.buildPattern(GRP_VAL);

    @Override
    public String name() {
        return EXTRACTOR_NAME;
    }

    @Override
    public String description() {
        return "version (v2, v3, …)";
    }

    @Override
    public void extract(ParseContext ctx) {
        var input = ctx.input;
        var m = PATTERN.matcher(input);

        while (m.find()) {
            var span = new Span(m.start(), m.end(), m.group());
            int start = span.start();

            boolean isValidPrefix = (start == 0) || Seps.isSep(input.charAt(start - 1)) || Character.isDigit(input.charAt(start - 1));

            if (isValidPrefix) {
                int val = Integer.parseInt(m.group(GRP_VAL));
                ctx.matches.add(new Match(MatchName.VERSION, val, span, priority(), Set.of(), false));
            }
        }
    }

    /**
     * Replicates Python VersionValidator: drop version when not preceded by episode and not seps-surrounded.
     */
    @Override
    public void postProcess(ParseContext ctx) {
        var versions = ctx.matches.named(MatchName.VERSION).toList();
        if (versions.isEmpty()) return;

        var episodes = ctx.matches.named(MatchName.EPISODE).toList();
        var seps = Validators.sepsSurround(ctx.input);

        var toRemove = versions.stream()
                .filter(v -> !isValidVersionContext(v, episodes, seps, ctx.input))
                .toList();

        toRemove.forEach(ctx.matches::remove);
    }

    private boolean isValidVersionContext(Match version, List<Match> episodes, Predicate<Match> seps, String input) {
        if (seps.test(version)) {
            return true;
        }

        return episodes.stream().anyMatch(e -> isPrecededByEpisode(version, e, input));
    }

    private boolean isPrecededByEpisode(Match version, Match episode, String input) {
        if (episode.span().end() == version.span().start()) {
            return true;
        }

        return (episode.span().end() + 1 == version.span().start())
                && Character.toLowerCase(input.charAt(episode.span().end())) == 'v';
    }
}