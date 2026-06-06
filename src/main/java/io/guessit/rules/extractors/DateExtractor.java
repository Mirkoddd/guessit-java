package io.guessit.rules.extractors;

import io.guessit.core.pipeline.state.Priority;
import io.guessit.rules.date.DateOrchestrator;
import io.guessit.core.pipeline.contracts.Extractor;
import io.guessit.core.pipeline.state.Match;
import io.guessit.core.pipeline.state.MatchName;
import io.guessit.core.pipeline.state.ParseContext;
import io.guessit.core.text.Span;

import java.util.Set;

/**
 * Extracts {@code date} via {@link DateOrchestrator#search}.
 *
 * <p>Priority 1100 (above the default 1000) so the date wins overlap against
 * the year/season/episode digits embedded inside it. The post-pass
 * additionally removes any year/season/episode/crc32 match whose span sits
 * fully inside the date — those are now redundant components of the date,
 * not standalone properties.
 */
public final class DateExtractor implements Extractor {

    @Override
    public String name() {
        return "date";
    }

    @Override
    public String description() {
        return "date (YYYY-MM-DD, DD-MM-YYYY, …)";
    }

    @Override
    public Priority priority() {
        return Priority.OVERRIDE;
    }

    @Override
    public void extract(ParseContext ctx) {
        if (isDateFiltered(ctx)) {
            return;
        }

        var input = ctx.input;

        DateOrchestrator.search(input, ctx.options.dateYearFirst(), ctx.options.dateDayFirst())
                .ifPresent(r -> {
                    var span = new Span(r.start(), r.end(), input.substring(r.start(), r.end()));
                    ctx.matches.add(Match.date(MatchName.DATE, r.date(), span, priority(), Set.of(), false));
                });
    }

    /**
     * Removes year/season/episode/crc32 matches that land inside the date span.
     */
    @Override
    public void postProcess(ParseContext ctx) {
        if (isDateFiltered(ctx)) {
            return;
        }

        ctx.matches.named(MatchName.DATE)
                .findFirst()
                .ifPresent(dateMatch -> removeRedundantInnerMatches(ctx, dateMatch));
    }

    private boolean isDateFiltered(ParseContext ctx) {
        var excludes = ctx.options.excludes();
        var includes = ctx.options.includes();
        return (!excludes.isEmpty() && excludes.contains("date"))
                || (!includes.isEmpty() && !includes.contains("date"));
    }

    private void removeRedundantInnerMatches(ParseContext ctx, Match dateMatch) {
        var toRemove = ctx.matches.all()
                .filter(m -> isRedundantInnerMatch(m, dateMatch))
                .toList();

        toRemove.forEach(ctx.matches::remove);
    }

    private boolean isRedundantInnerMatch(Match m, Match dateMatch) {
        if (m.equals(dateMatch) || m.name() == MatchName.DATE) {
            return false;
        }

        boolean isTargetType = m.name() == MatchName.YEAR
                || m.name() == MatchName.SEASON
                || m.name() == MatchName.EPISODE
                || m.name() == MatchName.CRC32;

        return isTargetType && m.span().isInside(dateMatch.span());
    }
}