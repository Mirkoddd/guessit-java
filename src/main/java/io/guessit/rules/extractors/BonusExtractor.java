package io.guessit.rules.extractors;

import com.mirkoddd.sift.core.SiftGlobalFlag;
import io.guessit.core.pipeline.contracts.Extractor;
import io.guessit.core.pipeline.state.Holes;
import io.guessit.core.pipeline.state.Match;
import io.guessit.core.pipeline.state.MatchName;
import io.guessit.core.pipeline.state.ParseContext;
import io.guessit.core.text.Formatters;
import io.guessit.core.text.Validators;
import io.guessit.core.text.patterns.BonusPatterns;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import static com.mirkoddd.sift.core.Sift.*;
import static com.mirkoddd.sift.core.SiftPatterns.*;

/**
 * Detects bonus feature numbers from {@code x\d+} patterns, e.g. {@code x05}.
 *
 * <p>Conflict guard: if a {@code video_codec} match (e.g. x264, x265) or a
 * strong (non-weak) {@code episode} match overlaps the same span, the bonus
 * candidate is silently dropped so the stronger extractor wins.
 *
 * <p>postProcess builds a {@code bonus_title} from the trailing hole in the
 * same filepart marker after the bonus match.
 */
public final class BonusExtractor implements Extractor {

    public static final String WEAK_EPISODE = "weak-episode";
    private static final String GRP_NAME = "name";
    private static final String MARKER_PATH = "path";
    private static final Pattern P = BonusPatterns.buildPattern(GRP_NAME);

    @Override
    public String name() {
        return "bonus";
    }

    @Override
    public String description() {
        return "bonus content (Bonus, Featurette, …)";
    }

    @Override
    public void extract(ParseContext ctx) {
        var input = ctx.input;
        var seps = Validators.sepsSurround(input);
        var m = P.matcher(input);

        var potentialConflicts = ctx.matches.snapshot().stream()
                .filter(x -> x.name() == MatchName.VIDEO_CODEC ||
                        (x.name() == MatchName.EPISODE && !x.tags().contains(WEAK_EPISODE)))
                .toList();

        while (m.find()) {
            var head = new Match(MatchName.BONUS, null, m.start(), m.end(), m.group(), priority(), Set.of(), false);

            boolean hasConflict = potentialConflicts.stream()
                    .anyMatch(x -> x.start() < m.end() && x.end() > m.start());

            if (seps.test(head) && !hasConflict) {
                ctx.matches.add(new Match(MatchName.BONUS, Integer.parseInt(m.group(GRP_NAME)),
                        m.start(), m.end(), m.group(), priority(), Set.of(), false));
            }
        }
    }

    @Override
    public void postProcess(ParseContext ctx) {
        var bonusMatches = ctx.matches.named(MatchName.BONUS).toList();

        if (bonusMatches.isEmpty() || ctx.matches.named(MatchName.BONUS_TITLE).findAny().isPresent()) {
            return;
        }

        bonusMatches.stream()
                .map(bonus -> extractBonusTitle(ctx, bonus))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .ifPresent(ctx.matches::add);
    }

    private Optional<Match> extractBonusTitle(ParseContext ctx, Match bonus) {
        return ctx.markers.stream()
                .filter(mk -> mk.name().equals(MARKER_PATH) && mk.covers(bonus.start(), bonus.end()))
                .findFirst()
                .flatMap(fp -> {
                    var holes = Holes.compute(
                            ctx.input,
                            bonus.end(),
                            fp.end(),
                            ctx.matches.snapshot(),
                            m -> m.isPrivate() || m.tags().contains(WEAK_EPISODE),
                            null,
                            Formatters::cleanup
                    );

                    if (holes.isEmpty()) {
                        return Optional.empty();
                    }

                    var hole = holes.getFirst();
                    var title = hole.value();

                    if (title == null || title.isBlank()) {
                        return Optional.empty();
                    }

                    return Optional.of(new Match(MatchName.BONUS_TITLE, title,
                            hole.start, hole.end, hole.raw(), priority(), Set.of(), false));
                });
    }
}