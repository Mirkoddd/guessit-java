package io.guessit.rules.extractors;

import io.guessit.core.pipeline.contracts.Extractor;
import io.guessit.core.pipeline.state.Match;
import io.guessit.core.pipeline.state.MatchName;
import io.guessit.core.pipeline.state.ParseContext;
import io.guessit.core.text.Seps;
import io.guessit.core.text.Span;
import io.guessit.core.text.Validators;
import io.guessit.core.text.patterns.CdPatterns;

import java.util.Set;
import java.util.regex.Pattern;

public final class CdExtractor implements Extractor {
    public static final String COUNT = "count";
    private static final String CD = "cd";

    private static final Pattern CD_OF = CdPatterns.buildCdOfPattern(CD, COUNT);

    private static final Pattern CD_COUNT = CdPatterns.buildCdCountPattern(COUNT);

    @Override
    public String name() {
        return "cd";
    }

    @Override
    public String description() {
        return "CD / disc number tokens (CD1, CD2, …)";
    }

    @Override
    public void extract(ParseContext ctx) {
        var input = ctx.input;
        var seps = Validators.sepsSurround(input);

        scanCdOf(ctx, input, seps);
        scanCdCount(ctx, input, seps);
    }

    private void scanCdOf(ParseContext ctx, String input, java.util.function.Predicate<Match> seps) {
        var m = CD_OF.matcher(input);
        while (m.find()) {
            var headSpan = new Span(m.start(), m.end(), m.group());
            var head = new Match(MatchName.CD, null, headSpan, priority(), Set.of(), false);

            if (seps.test(head)) {
                int cd = Integer.parseInt(m.group(CD));

                if (cd > 0 && cd < 100) {
                    var cdSpan = new Span(m.start(CD), m.end(CD), m.group(CD));
                    ctx.matches.add(new Match(MatchName.CD, cd, cdSpan, priority(), Set.of(), false));
                    addCdCountIfPresent(ctx, m);
                }
            }
        }
    }

    private void addCdCountIfPresent(ParseContext ctx, java.util.regex.Matcher m) {
        String countGroup = m.group(COUNT);

        if (countGroup != null) {
            int c = Integer.parseInt(countGroup);

            if (c > 0 && c < 100) {
                var countSpan = new Span(m.start(COUNT), m.end(COUNT), countGroup);
                ctx.matches.add(new Match(MatchName.CD_COUNT, c, countSpan,
                        priority(), Set.of(), false));
            }
        }
    }

    private void scanCdCount(ParseContext ctx, String input, java.util.function.Predicate<Match> seps) {
        var m = CD_COUNT.matcher(input);
        while (m.find()) {
            var headSpan = new Span(m.start(), m.end(), m.group());
            var head = new Match(MatchName.CD_COUNT, null, headSpan, priority(), Set.of(), false);

            if (seps.test(head)) {
                int c = Integer.parseInt(m.group(COUNT));

                if (c > 0 && c < 100) {
                    var countSpan = new Span(m.start(COUNT), m.end(COUNT), m.group(COUNT));
                    ctx.matches.add(new Match(MatchName.CD_COUNT, c, countSpan,
                            priority(), Set.of(), false));
                    addCdLiteralMarker(ctx, input, m);
                }
            }
        }
    }

    /** Cover the trailing "cd"/"cds" literal with a private marker so
     * it doesn't leak into a title/alt-title hole. */
    private void addCdLiteralMarker(ParseContext ctx, String input, java.util.regex.Matcher m) {
        int litStart = m.end(COUNT);
        int litEnd = m.end();

        while (litStart < litEnd && Seps.isSep(input.charAt(litStart))) {
            litStart++;
        }

        if (litEnd > litStart) {
            var markerSpan = new Span(litStart, litEnd, input.substring(litStart, litEnd));
            ctx.matches.add(new Match(MatchName.CD_MARKER, null, markerSpan, priority(), Set.of(), true));
        }
    }
}