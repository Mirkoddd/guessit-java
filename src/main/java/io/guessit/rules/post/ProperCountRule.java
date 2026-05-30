package io.guessit.rules.post;

import io.guessit.core.pipeline.contracts.PostProcessor;
import io.guessit.core.pipeline.state.Match;
import io.guessit.core.pipeline.state.MatchName;
import io.guessit.core.pipeline.state.ParseContext;
import io.guessit.core.pipeline.state.Priority;
import io.guessit.core.text.Span;
import io.guessit.core.text.patterns.ProperCountPatterns;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Emits {@code proper_count} = total weight of distinct {@code other='Proper'} matches.
 * Each Proper raw counts +1, or +2 when tagged {@code real} (Real / Real-Proper /
 * Real-Repack / Real-Rerip). Mirrors python {@code other.py:ProperCountRule}.
 */
public final class ProperCountRule implements PostProcessor {

    private static final String GROUP_DIGITS = "digits";

    private static final Pattern NON_ALPHANUMERIC = ProperCountPatterns.buildNonAlphanumericPattern();
    private static final Pattern TRAILING_DIGITS = ProperCountPatterns.buildTrailingDigitsPattern(GROUP_DIGITS);

    @Override
    public String description() {
        return "count proper/repack tokens";
    }

    @Override
    public void process(ParseContext ctx) {
        var distinct = extractDistinctPropers(ctx);
        if (distinct.isEmpty()) return;

        int total = distinct.values().stream().mapToInt(ProperCountRule::calculateWeight).sum();

        int start = distinct.values().stream().mapToInt(m -> m.span().start()).min().orElse(Integer.MAX_VALUE);
        int end = distinct.values().stream().mapToInt(m -> m.span().end()).max().orElse(Integer.MIN_VALUE);

        var rawInput = ctx.input.substring(start, end);

        ctx.matches.add(new Match(
                MatchName.PROPER_COUNT,
                total,
                new Span(start, end, rawInput),
                Priority.DEFAULT,
                Set.of(),
                false
        ));
    }

    private static LinkedHashMap<String, Match> extractDistinctPropers(ParseContext ctx) {
        var distinct = new LinkedHashMap<String, Match>();

        ctx.matches.named(MatchName.OTHER)
                .filter(m -> "Proper".equals(m.value()))
                .forEach(m -> distinct.putIfAbsent(rawCleanup(m.span().raw()), m));

        return distinct;
    }

    private static int calculateWeight(Match m) {
        int trailing = trailingDigits(m.span().raw());
        if (trailing > 0) {
            return trailing;
        }
        return m.tags().contains("real") ? 2 : 1;
    }

    private static String rawCleanup(String raw) {
        if (raw == null) return "";

        String lower = raw.toLowerCase(Locale.ROOT);
        return NON_ALPHANUMERIC.matcher(lower).replaceAll("");
    }

    private static int trailingDigits(String raw) {
        if (raw == null) return 0;

        var matcher = TRAILING_DIGITS.matcher(raw);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(GROUP_DIGITS));
        }

        return 0;
    }
}