package io.guessit.core.text;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class Formatters {
    private Formatters() {}

    private static final String EXCLUDED_CLEAN_CHARS = ",:;-/\\";
    private static final Pattern MULTI_SPACE = Pattern.compile(" +");

    private static final List<String> ARTICLES = List.of("the", "a", "an");
    private static final List<String> TITLE_SEPARATORS = List.of(", ", ",");

    private static final String CLEAN_CHARS;
    static {
        var sb = new StringBuilder();
        for (var c : Seps.CHARS.toCharArray()) {
            if (EXCLUDED_CLEAN_CHARS.indexOf(c) < 0) sb.append(c);
        }
        CLEAN_CHARS = sb.toString();
    }

    public static String strip(String s) {
        if (s == null || s.isEmpty()) return s;
        var start = 0;
        var end = s.length();
        while (start < end && Seps.isSep(s.charAt(start))) start++;
        while (end > start && Seps.isSep(s.charAt(end - 1))) end--;
        return s.substring(start, end);
    }

    public static String strip(String s, String chars) {
        if (s == null || s.isEmpty()) return s;
        var start = 0;
        var end = s.length();
        while (start < end && chars.indexOf(s.charAt(start)) >= 0) start++;
        while (end > start && chars.indexOf(s.charAt(end - 1)) >= 0) end--;
        return s.substring(start, end);
    }

    public static String cleanup(String input) {
        if (input == null || input.isEmpty()) return input;

        var cleanString = initialClean(input);
        var indices = findSepIndices(cleanString);
        var dots = new java.util.HashSet<Character>();

        if (!indices.isEmpty()) {
            var potential = findPotentialDots(indices, input);
            var replace = findReplaceablePositions(potential);
            if (!replace.isEmpty()) {
                cleanString = applyReplacements(cleanString, replace, input, dots);
            }
        }
        cleanString = strip(cleanString, stripCharsExcluding(dots));
        return MULTI_SPACE.matcher(cleanString).replaceAll(" ");
    }

    private static String initialClean(String input) {
        var clean = new StringBuilder(input.length());
        for (var c : input.toCharArray()) {
            clean.append(CLEAN_CHARS.indexOf(c) >= 0 ? ' ' : c);
        }
        return clean.toString();
    }

    private static java.util.List<Integer> findSepIndices(String s) {
        var idx = new java.util.ArrayList<Integer>();
        for (int i = 0; i < s.length(); i++) {
            if (Seps.isSep(s.charAt(i))) idx.add(i);
        }
        return idx;
    }

    private static LinkedHashSet<Integer> findPotentialDots(List<Integer> indices, String input) {
        var potential = new LinkedHashSet<Integer>();
        for (var i : indices) {
            if (potentialBefore(i, input) && potentialAfter(i, input)) potential.add(i);
        }
        return potential;
    }

    private static List<Integer> findReplaceablePositions(LinkedHashSet<Integer> potential) {
        var replace = new java.util.ArrayList<Integer>();
        for (var p : potential) {
            if (potential.contains(p - 2) || potential.contains(p + 2)) replace.add(p);
        }
        return replace;
    }

    private static String applyReplacements(String cleanString, List<Integer> replace,
                                            String input, java.util.HashSet<Character> dots) {
        var chars = cleanString.toCharArray();
        for (var r : replace) {
            dots.add(input.charAt(r));
            chars[r] = input.charAt(r);
        }
        return new String(chars);
    }

    private static String stripCharsExcluding(java.util.HashSet<Character> dots) {
        var sb = new StringBuilder();
        for (var c : Seps.CHARS.toCharArray()) {
            if (!dots.contains(c)) sb.append(c);
        }
        return sb.toString();
    }

    private static boolean potentialBefore(int i, String input) {
        if (i - 1 < 0 || i >= input.length()) return false;
        if (!Seps.isSep(input.charAt(i))) return false;
        if (Seps.isSep(input.charAt(i - 1))) return false;

        int back = i - 2;

        if (back < 0) return true;

        return Seps.isSep(input.charAt(back));
    }

    private static boolean potentialAfter(int i, String input) {
        if (i + 2 >= input.length()) return true;
        return input.charAt(i + 2) == input.charAt(i) && !Seps.isSep(input.charAt(i + 1));
    }

    public static String reorderTitle(String title) {
        if (title == null) return null;
        var ltitle = title.toLowerCase(Locale.ROOT);

        for (var article : ARTICLES) {
            for (var separator : TITLE_SEPARATORS) {
                var suffix = separator + article;
                if (ltitle.endsWith(suffix)) {
                    String originalArticle = title.substring(title.length() - article.length());
                    String coreTitle = title.substring(0, title.length() - suffix.length());

                    return originalArticle + " " + coreTitle;
                }
            }
        }
        return title;
    }

    public static String titleText(String input) {
        if (input == null) return null;
        return reorderTitle(cleanup(input));
    }
}