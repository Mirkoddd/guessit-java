package io.guessit.rules.post;

import com.mirkoddd.sift.core.engine.SiftCompiledPattern;
import io.guessit.core.pipeline.state.Match;
import io.guessit.core.pipeline.state.MatchName;
import io.guessit.core.pipeline.state.ParseContext;
import io.guessit.core.pipeline.contracts.PostProcessor;
import io.guessit.core.pipeline.state.Priority;
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

    private static final String TAG_RANGE_FILL = "range-fill";

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
                .filter(m -> m.value() instanceof Integer)
                .sorted(Comparator.comparingInt(m -> m.span().start()))
                .toList();

        if (matches.size() < 2) return;

        var input = ctx.input;
        var fills = new ArrayList<Match>();

        for (int i = 0; i + 1 < matches.size(); i++) {
            var prev = matches.get(i);
            var next = matches.get(i + 1);

            if (!isFillablePair(ctx, prop, input, prev, next)) continue;

            int prevVal = (Integer) prev.value();
            int nextVal = (Integer) next.value();

            for (int v = prevVal + 1; v < nextVal; v++) {
                var span = new Span(prev.span().end(), next.span().start(), String.valueOf(v));
                fills.add(new Match(prop, v, span, Priority.DEFAULT, Set.of(TAG_RANGE_FILL), false));
            }
        }

        fills.forEach(ctx.matches::add);
    }

    private static boolean isFillablePair(ParseContext ctx, MatchName prop, String input, Match prev, Match next) {
        int prevVal = (Integer) prev.value();
        int nextVal = (Integer) next.value();

        if (nextVal <= prevVal + 1) return false;
        if (nextVal - prevVal > MAX_JUMP) return false;
        if (next.span().start() < prev.span().end()) return false;

        int gapLen = next.span().start() - prev.span().end();
        if (gapLen <= 0 || gapLen > MAX_GAP) return false;

        String gap = input.substring(prev.span().end(), next.span().start());
        if (!GAP_PATTERN.matchesEntire(gap)) return false;

        int prevEnd = prev.span().end();
        int nextStart = next.span().start();

        return ctx.matches.named(prop)
                .noneMatch(m -> m.tags().contains(TAG_RANGE_FILL)
                        && m.span().start() >= prevEnd && m.span().end() <= nextStart);
    }
}