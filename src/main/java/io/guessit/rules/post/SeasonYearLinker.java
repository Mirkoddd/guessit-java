package io.guessit.rules.post;

import io.guessit.core.pipeline.state.Match;
import io.guessit.core.pipeline.state.MatchName;
import io.guessit.core.pipeline.state.ParseContext;
import io.guessit.core.pipeline.contracts.PostProcessor;

import java.util.Set;

/**
 * When no {@code year} match exists and a {@code season} value looks like a
 * plausible year (1900..currentYear+1), emits a {@code year} match cloned from
 * the season match.
 *
 * <p>Runs after {@link SeasonYearLinkProcessor} so that {@code SeasonYearLink}'s wider
 * 1900..2100 window already handled any prior promotion; this processor is a
 * tighter guard that uses the dynamic current-year bound.
 */
public final class SeasonYearLinker implements PostProcessor {
    private static final int MIN_YEAR = 1900;
    private static final int CUR = java.time.Year.now().getValue();

    @Override
    public String description() {
        return "resolve season vs year ambiguity";
    }

    @Override
    public void process(ParseContext ctx) {
        if (ctx.matches.named(MatchName.YEAR).findAny().isPresent()) return;

        ctx.matches.named(MatchName.SEASON)
                .filter(m -> m instanceof Match.IntegerMatch)
                .map(m -> (Match.IntegerMatch) m)
                .toList()
                .forEach(season -> {
                    int v = season.value();
                    if (v < MIN_YEAR || v > CUR + 1) return;

                    ctx.matches.add(Match.integer(MatchName.YEAR, v, season.span(),
                            season.priority(), Set.copyOf(season.tags()), false));
                });
    }
}