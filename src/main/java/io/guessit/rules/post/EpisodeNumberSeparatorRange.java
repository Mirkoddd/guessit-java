package io.guessit.rules.post;

import io.guessit.core.pipeline.contracts.PostProcessor;
import io.guessit.core.pipeline.state.Match;
import io.guessit.core.pipeline.state.MatchName;
import io.guessit.core.pipeline.state.MatchTag;
import io.guessit.core.pipeline.state.ParseContext;
import io.guessit.core.pipeline.state.Priority;
import io.guessit.core.text.Span;
import io.guessit.core.text.patterns.EpisodeRangePatterns;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expands bare episode ranges like {@code 16-20} into the full sequence
 * {@code [16, 17, 18, 19, 20]}.
 *
 * <p>This post-processor handles the case where the text following an episode
 * match contains a range separator ({@code -}, {@code ~}, or {@code to})
 * immediately followed by an integer, but that integer was <em>not</em>
 * independently parsed as an episode match (e.g. because no "Episode" keyword
 * prefixes the second number). It detects the trailing number, validates the
 * range, and inserts episode matches for the endpoint and all intermediate
 * values.
 *
 * <p>Bounded by {@link #MAX_JUMP} to avoid runaway expansion on year-like
 * differences (e.g. {@code 1990-2001} should not produce 12 episodes).
 *
 * <p>Run after {@link RangeFiller} so that ranges between two existing
 * episode matches are already filled before this rule looks for single-sided
 * ranges.
 */
public final class EpisodeNumberSeparatorRange implements PostProcessor {

    @Override
    public String description() {
        return "detect episode ranges separated by - or to";
    }

    private static final int MAX_JUMP = 20;

    private static final String GROUP_NUM = "num";
    private static final Pattern RANGE_THEN_NUM = EpisodeRangePatterns.buildRangePattern(GROUP_NUM);

    public static final MatchName EPISODE = MatchName.EPISODE;

    @Override
    public void process(ParseContext ctx) {
        var eps = ctx.matches.named(EPISODE)
                .filter(m -> !m.isPrivate() && m.value() instanceof Integer)
                .sorted(Comparator.comparingInt(m -> m.span().start()))
                .toList();
        var fills = new ArrayList<Match>();
        for (var a : eps) tryExtendRange(ctx, a, fills);
        for (var m : fills) ctx.matches.add(m);
    }

    private record RangeNumber(int value, Span span) {}

    private static void tryExtendRange(ParseContext ctx, Match a, java.util.List<Match> fills) {
        int va = (Integer) a.value();
        int scanFrom = a.span().end();
        if (scanFrom >= ctx.input.length()) return;

        var matcher = RANGE_THEN_NUM.matcher(ctx.input);
        matcher.region(scanFrom, ctx.input.length());
        if (!matcher.lookingAt()) return;

        var rn = extractRangeNumber(matcher, ctx.input);
        if (rn == null) return;
        if (rn.value() <= va || rn.value() - va > MAX_JUMP) return;
        if (vbAlreadyPresent(ctx, rn)) return;
        if (alreadyFilled(ctx, a, rn.span().end())) return;

        // Add vb itself as an episode match.
        fills.add(new Match(EPISODE, rn.value(), rn.span(), Priority.DEFAULT, Set.of(MatchTag.RANGE_FILL.getYamlValue()), false));

        for (int v = va + 1; v < rn.value(); v++) {
            var emptySpan = new Span(rn.span().start(), rn.span().start(), "");
            fills.add(new Match(EPISODE, v, emptySpan, Priority.DEFAULT, Set.of(MatchTag.RANGE_FILL.getYamlValue()), false));
        }
    }

    /** Extract the integer + span from the strictly named capture group. */
    private static RangeNumber extractRangeNumber(Matcher matcher, String input) {
        String numStr = matcher.group(GROUP_NUM);
        if (numStr == null) return null;

        int vb;
        try { vb = Integer.parseInt(numStr); }
        catch (NumberFormatException _) { return null; }

        int numStart = matcher.start(GROUP_NUM);
        int numEnd   = matcher.end(GROUP_NUM);

        var span = new Span(numStart, numEnd, input.substring(numStart, numEnd));
        return new RangeNumber(vb, span);
    }

    /** True when vb is already an episode match at that position —
     * RangeFiller already handled it (or will). */
    private static boolean vbAlreadyPresent(ParseContext ctx, RangeNumber rn) {
        return ctx.matches.named(EPISODE)
                .anyMatch(m -> m.value() instanceof Integer iv && iv == rn.value()
                        && m.span().start() >= rn.span().start() && m.span().end() <= rn.span().end());
    }

    /** True when an existing episode range-fill already covers the gap. */
    private static boolean alreadyFilled(ParseContext ctx, Match a, int numEnd) {
        return ctx.matches.named(EPISODE)
                .anyMatch(m -> m.hasTag(MatchTag.RANGE_FILL)
                        && m.span().start() >= a.span().end() && m.span().end() <= numEnd);
    }
}