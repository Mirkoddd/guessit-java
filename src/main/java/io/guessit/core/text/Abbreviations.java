package io.guessit.core.text;

import io.guessit.core.text.patterns.AbbreviationsPatterns;

/**
 * Mirrors Python rebulk's pattern-source rewriting helpers.
 *
 * <p>guessit defines many regexes using a shorthand where a literal {@code -}
 * means "any single non-FS separator". {@link #dash} expands that shorthand
 * into a proper character class, so the same pattern matches {@code "WEB-DL"},
 * {@code "WEB.DL"}, {@code "WEB DL"}, etc. The rewrite is character-aware —
 * dashes inside {@code [...]} character classes or after an escape are left
 * alone.
 */
public final class Abbreviations {

    private Abbreviations() {}

    /** Replace every unescaped, non-class `-` in the source with `[<seps_no_fs>]`.
     * Mirrors Python rebulk's dash abbreviation: a single separator character (not zero-or-more). */
    public static String dash(String src) {
        return rewriteLiteral(src);
    }

    private static String rewriteLiteral(String src) {
        var sb = new StringBuilder(src.length() + AbbreviationsPatterns.SEPS_REPLACEMENT.length() * 2);
        boolean escaped = false;
        int classDepth = 0;

        for (int i = 0; i < src.length(); i++) {
            char c = src.charAt(i);

            if (escaped) {
                sb.append(c);
                escaped = false;
            } else if (c == '\\') {
                sb.append(c);
                escaped = true;
            } else if (c == '[') {
                sb.append(c);
                classDepth++;
            } else if (c == ']' && classDepth > 0) {
                sb.append(c);
                classDepth--;
            } else if (c == '-' && classDepth == 0) {
                sb.append(AbbreviationsPatterns.SEPS_REPLACEMENT);
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }
}