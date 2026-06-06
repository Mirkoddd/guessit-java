package io.guessit.rules.post;

import io.guessit.core.pipeline.state.Match;
import io.guessit.core.pipeline.state.MatchName;
import io.guessit.core.pipeline.state.ParseContext;
import io.guessit.core.pipeline.contracts.PostProcessor;

import java.util.Set;

/**
 * Promotes a season number that lies in a plausible year range to also be a
 * {@code year} match. Mirrors python guessit's {@code season_to_year} rule:
 * when {@code SxxExx} parses with a four-digit season (e.g. {@code S2014E18}
 * or {@code Looney Tunes 1940x01}) and no other year is present, emit a
 * {@code year=value} match alongside the season.
 *
 * <p>Skips emission when an explicit year already exists or the season value
 * is outside the {@link #MIN_YEAR}..{@link #MAX_YEAR} window — small season
 * numbers ("S2" → 2) shouldn't masquerade as years.
 */
public final class SeasonYearLinkProcessor implements PostProcessor {
    private static final int MIN_YEAR = 1900;
    private static final int MAX_YEAR = 2100;
    private static final String TAG_SEASON_DERIVED = "season-derived";

    @Override
    public String description() {
        return "link season+year tokens that belong together";
    }

    @Override
    public void process(ParseContext ctx) {
        if (ctx.matches.named(MatchName.YEAR).findAny().isPresent()) {
            return;
        }

        ctx.matches.named(MatchName.SEASON)
                .filter(m -> m instanceof Match.IntegerMatch)
                .map(m -> (Match.IntegerMatch) m)
                .filter(SeasonYearLinkProcessor::isValidYearSeason)
                .findFirst()
                .ifPresent(s -> promoteToYear(ctx, s));
    }

    private static boolean isValidYearSeason(Match.IntegerMatch s) {
        int year = s.value();
        return year >= MIN_YEAR && year <= MAX_YEAR;
    }

    private static void promoteToYear(ParseContext ctx, Match.IntegerMatch s) {
        ctx.matches.add(Match.integer(
                MatchName.YEAR,
                s.value(),
                s.span(),
                s.priority(),
                Set.of(TAG_SEASON_DERIVED),
                false
        ));
    }
}