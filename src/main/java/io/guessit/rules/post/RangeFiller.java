package io.guessit.rules.post;

import com.mirkoddd.sift.core.engine.SiftCompiledPattern;
import io.guessit.core.pipeline.state.*;
import io.guessit.core.pipeline.contracts.PostProcessor;
import io.guessit.core.text.Span;
import io.guessit.core.text.patterns.RangeFillerPatterns;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;

/**
 * Expand range pairs of {@code season} or {@code episode} matches into the full
 * sequence: e.g. {@code E01-04} (matches [1, 4]) becomes [1, 2, 3, 4].
 */
public final class RangeFiller implements PostProcessor {

    private static final int MAX_GAP = 6;
    private static final int MAX_JUMP = 20;

    private static final SiftCompiledPattern GAP_PATTERN = RangeFillerPatterns.buildGapPattern();

    @Override
    public String description() {
        return "fill numeric ranges between season/episode endpoints";
    }

    @Override
    public void process(ParseContext ctx) {
        ctx.trace.subStep("Stage 1: fill missing values in episode ranges");
        fillProp(ctx, MatchName.EPISODE);

        ctx.trace.subStep("Stage 2: fill missing values in season ranges");
        fillProp(ctx, MatchName.SEASON);
    }

    private static void fillProp(ParseContext ctx, MatchName prop) {
        var matches = ctx.matches.named(prop)
                .filter(m -> m instanceof Match.IntegerMatch)
                .map(m -> (Match.IntegerMatch) m)
                .sorted(Comparator.comparingInt(m -> m.span().start()))
                .toList();

        if (matches.size() < 2) return;

        var input = ctx.input;
        var fills = new ArrayList<Match>();

        for (int i = 0; i + 1 < matches.size(); i++) {
            var prev = matches.get(i);
            var next = matches.get(i + 1);

            if (!isFillablePair(ctx, prop, input, prev, next)) continue;

            int prevVal = prev.value();
            int nextVal = next.value();

            for (int v = prevVal + 1; v < nextVal; v++) {
                var span = new Span(prev.span().end(), next.span().start(), String.valueOf(v));
                fills.add(Match.integer(prop, v, span, Priority.DEFAULT, Set.of(MatchTag.RANGE_FILL.getValue()), false));
            }
        }

        fills.forEach(ctx.matches::add);
    }

    private static boolean isFillablePair(ParseContext ctx, MatchName prop, String input, Match.IntegerMatch prev, Match.IntegerMatch next) {
        int prevVal = prev.value();
        int nextVal = next.value();

        if (nextVal <= prevVal + 1) return false;
        if (nextVal - prevVal > MAX_JUMP) return false;

        if (!prev.span().isBefore(next.span())) return false;

        int gapLen = prev.span().distanceTo(next.span());
        if (gapLen <= 0 || gapLen > MAX_GAP) return false;

        var gapSpan = new Span(prev.span().end(), next.span().start(), "");
        String gap = input.substring(gapSpan.start(), gapSpan.end());
        if (!GAP_PATTERN.matchesEntire(gap)) return false;

        return ctx.matches.named(prop)
                .noneMatch(m -> m.hasTag(MatchTag.RANGE_FILL)
                        && m.span().isInside(gapSpan));
    }
}