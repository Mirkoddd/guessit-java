package io.guessit.rules.extractors;

import io.guessit.core.pipeline.contracts.Extractor;
import io.guessit.core.pipeline.state.Match;
import io.guessit.core.pipeline.state.MatchName;
import io.guessit.core.pipeline.state.ParseContext;
import io.guessit.core.pipeline.state.Priority;
import io.guessit.core.text.Span;
import io.guessit.core.text.Validators;
import io.guessit.core.text.patterns.CrcPatterns;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Detects 8-hex-digit CRC32 values, e.g. {@code [ABCD1234]} or {@code .ABCD1234.},
 * plus UUID/hash-like id numbers via the same heuristic used by python guessit
 * ({@code guess_idnumber}).
 *
 * <p>Priority is 500 (lower than season/episode at 1000) so ConflictPhase will
 * favor season/episode over crc32 when they overlap. uuid uses the default
 * priority but its conflict_solver in python keeps the uuid; here we lower it
 * just enough to lose to strong matches but win against bare digit weak ones.
 */
public final class CrcExtractor implements Extractor {

    public static final String EXTRACTOR_NAME = "crc32";
    private static final String GRP_VALUE = "val";

    private static final Pattern CRC = CrcPatterns.buildCrcPattern(GRP_VALUE);
    private static final Pattern UUID = CrcPatterns.buildUuidPattern(GRP_VALUE);
    private static final Pattern SXX_EXX_INSIDE = CrcPatterns.buildSxxExxInsidePattern();

    @Override
    public String name() {
        return EXTRACTOR_NAME;
    }

    @Override
    public Priority priority() {
        return Priority.SPECULATIVE;
    }

    @Override
    public String description() {
        return "CRC32 checksum (8 hex chars)";
    }

    @Override
    public void extract(ParseContext ctx) {
        extractCrc32(ctx);
        extractUuid(ctx);
        dropSeasonEpisodeInsideCrc(ctx);
    }

    private void dropSeasonEpisodeInsideCrc(ParseContext ctx) {
        var crcSpans = ctx.matches.named(MatchName.CRC32)
                .map(Match::span)
                .toList();

        if (crcSpans.isEmpty()) return;

        var toRemove = ctx.matches.all()
                .filter(m -> {
                    var n = m.name();
                    return n == MatchName.SEASON || n == MatchName.EPISODE || n == MatchName.SEASON_HEAD;
                })
                .filter(m -> crcSpans.stream().anyMatch(s -> m.span().isInside(s)))
                .toList();

        toRemove.forEach(ctx.matches::remove);
    }

    private void extractCrc32(ParseContext ctx) {
        var input = ctx.input;
        var seps = Validators.sepsSurround(input);
        var m = CRC.matcher(input);

        while (m.find()) {
            var val = m.group(GRP_VALUE);
            var span = new Span(m.start(GRP_VALUE), m.end(GRP_VALUE), val);

            var head = Match.string(MatchName.CRC32, val, span, priority(), Set.of(), false);

            if (seps.test(head)) {
                ctx.matches.add(Match.string(MatchName.CRC32, val, span, priority(), Set.of(), false));
            }
        }
    }

    private void extractUuid(ParseContext ctx) {
        var input = ctx.input;
        var seps = Validators.sepsSurround(input);
        var m = UUID.matcher(input);

        while (m.find()) {
            var raw = m.group(GRP_VALUE);

            if (isLikelyIdNumber(raw) && !SXX_EXX_INSIDE.matcher(raw).find()) {
                var span = new Span(m.start(GRP_VALUE), m.end(GRP_VALUE), raw);

                var head = Match.string(MatchName.UUID, raw, span, priority(), Set.of(), false);

                if (seps.test(head)) {
                    ctx.matches.add(Match.string(MatchName.UUID, raw, span, priority(), Set.of(), false));
                }
            }
        }
    }

    private enum CharType { DIGIT, LETTER, OTHER }

    private static boolean isLikelyIdNumber(String s) {
        CharType lastType = CharType.LETTER;
        int switchCount = 0;
        int switchLetterCount = 0;
        int letterCount = 0;
        char lastLetter = 0;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            CharType currentType = classifyChar(c);

            if (currentType == CharType.LETTER) {
                if (c != lastLetter) switchLetterCount++;
                lastLetter = c;
                letterCount++;
            }

            if (currentType != lastType) switchCount++;
            lastType = currentType;
        }

        double switchRatio = (double) switchCount / s.length();
        double lettersRatio = letterCount == 0 ? 1.0 : (double) switchLetterCount / letterCount;
        return switchRatio > 0.4 && lettersRatio > 0.4;
    }

    private static CharType classifyChar(char c) {
        if (Character.isDigit(c)) return CharType.DIGIT;
        if (Character.isLetter(c)) return CharType.LETTER;
        return CharType.OTHER;
    }
}