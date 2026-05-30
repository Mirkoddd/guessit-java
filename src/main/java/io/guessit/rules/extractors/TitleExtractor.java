package io.guessit.rules.extractors;

import io.guessit.core.pipeline.contracts.Extractor;
import io.guessit.core.pipeline.state.*;
import io.guessit.core.text.Formatters;
import io.guessit.core.text.Seps;
import io.guessit.core.text.Span;
import io.guessit.core.text.Validators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static io.guessit.core.pipeline.state.MatchName.EPISODE;

/**
 * Port of Python {@code rules/properties/title.py}: emits {@code title} and
 * {@code alternative_title} matches.
 *
 * <p>Three sub-rules:
 * <ul>
 * <li><b>expected_title functional</b> in {@link #extract} — emits a {@code title}
 * match for each {@code Options#expectedTitle} substring found in the input.</li>
 * <li><b>TitleFromPosition</b> in {@link #postProcess} — for the highest-scoring path
 * marker, takes the cleaned hole as a {@code title} match. Splits the hole on
 * {@link Seps#TITLE_CHARS} to produce {@code alternative_title} matches. Routes
 * inner-filepart titles to {@code episode_title} when an outer "Show/Season N"
 * filepart shape is detected.</li>
 * <li><b>PreferTitleWithYear</b> in {@link #postProcess} — prefers titles in the
 * filepart containing a year; drops the others.</li>
 * </ul>
 */
public final class TitleExtractor implements Extractor {
    static final Set<String> NON_SPECIFIC_LANGUAGES = Set.of("mul", "und");
    public static final String TITLE = "title";
    private static final String EXPECTED_TAG = "expected";

    @Override
    public String name() { return TITLE; }

    @Override
    public String description() {
        return "title (text in the leading hole between markers / matches)";
    }

    @Override
    public void extract(ParseContext ctx) {
        var expected = ctx.options.expectedTitle();
        if (expected.isEmpty()) {
            // Fall back to expected_title list from options.json so defaults
            // like "This is Us" / "OSS 117" are applied automatically.
            expected = ctx.config.topLevelList("expected_title");
        }
        if (expected.isEmpty()) return;
        var input = ctx.input;
        // Mirror python rules/common/expected.py: normalize seps in both input
        // and search to a single space before substring scanning. Spans stay
        // valid because replacement is 1:1 by char.
        var normalizedInput = normalizeSeps(input);
        var sepsSurround = Validators.sepsSurround(input);
        for (var entry : ExpectedTitleRegex.parse(expected)) {
            if (entry.literalReplacement() != null) {
                scanLiteralExpectedTitle(ctx, input, normalizedInput, sepsSurround, entry.literalReplacement());
            } else {
                scanRegexExpectedTitle(ctx, input, normalizedInput, sepsSurround, entry.pattern());
            }
        }
    }

    private static void scanLiteralExpectedTitle(ParseContext ctx, String input, String normalizedInput,
                                                 Predicate<Match> sepsSurround, String literal) {
        var search = normalizeSeps(literal);
        var lcInput = normalizedInput.toLowerCase(java.util.Locale.ROOT);
        var lcSearch = search.toLowerCase(java.util.Locale.ROOT);
        int idx = 0;
        while ((idx = lcInput.indexOf(lcSearch, idx)) >= 0) {
            var raw = input.substring(idx, idx + search.length());
            var formatted = Formatters.titleText(raw);
            var span = new Span(idx, idx + search.length(), raw);
            var m = new Match(MatchName.TITLE, formatted, span, Priority.DEFAULT, Set.of(EXPECTED_TAG, TITLE), false);
            if (sepsSurround.test(m)) ctx.matches.add(m);
            idx += search.length();
        }
    }

    private static void scanRegexExpectedTitle(ParseContext ctx, String input, String normalizedInput,
                                               Predicate<Match> sepsSurround, Pattern pattern) {
        var matcher = pattern.matcher(normalizedInput);
        while (matcher.find()) {
            var raw = input.substring(matcher.start(), matcher.end());
            var formatted = Formatters.titleText(raw);
            var span = new Span(matcher.start(), matcher.end(), raw);
            var m = new Match(MatchName.TITLE, formatted, span, Priority.DEFAULT, Set.of(EXPECTED_TAG, TITLE), false);
            if (sepsSurround.test(m)) ctx.matches.add(m);
        }
    }

    private static String normalizeSeps(String s) {
        var sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            var c = s.charAt(i);
            sb.append(Seps.isSep(c) ? ' ' : c);
        }
        return sb.toString();
    }

    @Override
    public void postProcess(ParseContext ctx) {
        var hasExpected = ctx.matches.named(MatchName.TITLE).anyMatch(m -> m.tags().contains(EXPECTED_TAG));
        if (!hasExpected) {
            // Mirror python: Filepart3/2EpisodeTitle seed a title at the
            // outer/subdir hole BEFORE TitleFromPosition runs. Without this,
            // titleFromPosition would pick the directory's first hole as
            // title and leave the filename without one — preventing
            // EpisodeTitleExtractor.episodeTitleFromPosition from finding
            // the post-episode hole as episode_title (e.g. "Psy Vs Psy" in
            // "Psych.S02E03.Psy.Vs.Psy.Français.srt").
            EpisodeTitleExtractor.filePart3EpisodeTitleStatic(ctx);
            EpisodeTitleExtractor.filePart2EpisodeTitleStatic(ctx);
            titleFromPosition(ctx);
        }
        preferTitleWithYear(ctx);
    }

    private void titleFromPosition(ParseContext ctx) {
        var paths = ctx.markers.stream().filter(m -> "path".equals(m.name())).toList();
        if (paths.isEmpty()) return;
        var sorted = Markers.markerSorted(paths, ctx.matches);
        var serieNameFilepart = serieNameFilepart(ctx, paths);
        var toAppend = new ArrayList<Match>();
        var toRemove = new ArrayList<Match>();

        boolean filenameProvidesTitle = serieNameFilepart != null
                && processSerieNameFilepart(ctx, serieNameFilepart, toAppend, toRemove);

        var consumedYearFileparts = new HashSet<Marker>();
        if (!filenameProvidesTitle) {
            selectFirstNonSerieFilepart(ctx, sorted, serieNameFilepart, consumedYearFileparts,
                    toAppend, toRemove);
        }
        appendYearFilepartTitles(ctx, paths, consumedYearFileparts, toAppend, toRemove);

        for (var r : toRemove) ctx.matches.remove(r);
        for (var t : toAppend) ctx.matches.add(t);
    }

    /** Returns true when the filename filepart provided the show title. */
    private boolean processSerieNameFilepart(ParseContext ctx, Marker serieNameFilepart,
                                             List<Match> toAppend, List<Match> toRemove) {
        int holeCount = countUsableHoles(ctx, serieNameFilepart, this::serieNameIgnored);
        var titles = checkTitlesInFilepart(ctx, serieNameFilepart, this::serieNameIgnored);
        if (titles == null) return false;
        if (holeCount >= 2) {
            return appendMultiHoleTitles(titles, toAppend, toRemove);
        }
        return appendSingleHoleTitle(ctx, serieNameFilepart, titles, toAppend, toRemove);
    }

    /**
     * Filename has 2+ title-eligible holes around episode: title comes from
     * the filename, not the outer dir (mirrors python rebulk behaviour).
     */
    private boolean appendMultiHoleTitles(TitlesInFilepart titles, List<Match> toAppend, List<Match> toRemove) {
        if (titles.titles.isEmpty()) return false;
        var first = titles.titles.getFirst();
        toAppend.add(new Match(MatchName.TITLE, first.value(), first.span(),
                first.priority(), Set.of(TITLE, "filepart-title"), false));
        for (int i = 1; i < titles.titles.size(); i++) {
            var t = titles.titles.get(i);
            toAppend.add(new Match(MatchName.EPISODE_TITLE, t.value(), t.span(),
                    t.priority(), Set.of(TITLE), false));
        }
        toRemove.addAll(titles.toRemove);
        return true;
    }

    /**
     * Mirror python: a filename hole BEFORE the episode marker is the show
     * title (e.g. "Show-E01.mkv"); a hole AFTER is the episode title (e.g.
     * "E01-episode title.mkv"). Without this split, both shapes emit
     * episode_title, leaving the show title from an outer generic dir.
     */
    private boolean appendSingleHoleTitle(ParseContext ctx, Marker serieNameFilepart,
                                          TitlesInFilepart titles,
                                          List<Match> toAppend, List<Match> toRemove) {
        if (titles.titles.size() != 1) return false;
        var ep = ctx.matches.inMarker(serieNameFilepart).filter(m -> m.name() == EPISODE).findFirst().orElse(null);
        var t = titles.titles.getFirst();
        var holeBeforeEpisode = ep != null && t.span().end() <= ep.span().start();
        toRemove.addAll(titles.toRemove);
        if (holeBeforeEpisode) {
            toAppend.add(new Match(MatchName.TITLE, t.value(), t.span(),
                    t.priority(), Set.of(TITLE, "filepart-title"), false));
            return true;
        }
        toAppend.add(new Match(MatchName.EPISODE_TITLE, t.value(), t.span(),
                t.priority(), Set.of(TITLE), false));
        return false;
    }

    private void selectFirstNonSerieFilepart(ParseContext ctx, List<Marker> sorted, Marker serieNameFilepart,
                                             Set<Marker> consumedYearFileparts,
                                             List<Match> toAppend, List<Match> toRemove) {
        for (var fp : sorted) {
            if (fp == serieNameFilepart) continue;
            consumedYearFileparts.add(fp);
            var titles = checkTitlesInFilepart(ctx, fp, _ -> false);
            if (titles == null) continue;
            toAppend.addAll(titles.titles);
            toRemove.addAll(titles.toRemove);
            break;
        }
    }

    private void appendYearFilepartTitles(ParseContext ctx, List<Marker> paths,
                                          Set<Marker> consumedYearFileparts,
                                          List<Match> toAppend, List<Match> toRemove) {
        var yearFileparts = paths.stream()
                .filter(fp -> ctx.matches.inMarker(fp).anyMatch(m -> m.name() == MatchName.YEAR))
                .toList();
        for (var fp : yearFileparts) {
            if (consumedYearFileparts.contains(fp)) continue;
            var titles = checkTitlesInFilepart(ctx, fp, _ -> false);
            if (titles == null) continue;
            toAppend.addAll(titles.titles);
            toRemove.addAll(titles.toRemove);
        }
    }

    private void preferTitleWithYear(ParseContext ctx) {
        var titles = ctx.matches.named(MatchName.TITLE).toList();
        if (titles.isEmpty()) return;
        var withYearInGroup = new ArrayList<Match>();
        var withYear = new ArrayList<Match>();
        for (var t : titles) {
            var fp = Markers.atMatch(ctx.markers, t, m -> "path".equals(m.name())).orElse(null);
            if (fp == null) continue;
            var year = ctx.matches.inMarker(fp).filter(m -> m.name() == MatchName.YEAR).findFirst().orElse(null);
            if (year == null) continue;
            var inGroup = Markers.atMatch(ctx.markers, year, m -> "group".equals(m.name())).isPresent();
            (inGroup ? withYearInGroup : withYear).add(t);
        }
        Set<Object> keepValues;
        if (!withYearInGroup.isEmpty()) keepValues = withYearInGroup.stream().map(Match::value).collect(java.util.stream.Collectors.toSet());
        else if (!withYear.isEmpty()) keepValues = withYear.stream().map(Match::value).collect(java.util.stream.Collectors.toSet());
        else return;
        for (var t : titles) {
            if (!keepValues.contains(t.value())) {
                ctx.matches.remove(t);
            } else if (!t.tags().contains("equivalent-ignore")) {
                // Mirror python PreferTitleWithYear AppendTags: surviving
                // titles get "equivalent-ignore" so EquivalentHoles doesn't
                // overwrite their better-cased outer-folder value with a
                // titlecased filename hole (e.g. "Comme une Image" must not
                // be replaced by "Comme Une Image" from inner "Comme.Une.Image").
                var withTag = new HashSet<>(t.tags());
                withTag.add("equivalent-ignore");
                ctx.matches.replace(t, new Match(t.name(), t.value(), t.span(),
                        t.priority(), withTag, t.isPrivate()));
            }
        }
    }

    private int countUsableHoles(ParseContext ctx, Marker filepart,
                                 java.util.function.Predicate<Match> additionalIgnore) {
        java.util.function.Predicate<Match> ignore = m ->
                isIgnored(m) || (additionalIgnore != null && additionalIgnore.test(m));
        var holes = Holes.compute(ctx.input, filepart.span().start(), filepart.span().end(),
                ctx.matches.snapshot(), ignore, null, Formatters::titleText);
        holes = holesProcess(ctx, holes);
        int n = 0;
        for (var h : holes) {
            if (h != null && h.isEmpty() && !h.value().isEmpty()) n++;
        }
        return n;
    }

    private boolean serieNameIgnored(Match m) {
        for (var tag : m.tags()) {
            if ("weak".equals(tag) || tag.startsWith("weak-")) return true;
        }
        return false;
    }

    private Marker serieNameFilepart(ParseContext ctx, List<Marker> fileparts) {
        for (var index = 1; index < fileparts.size() - 1; index++) {
            var fp = fileparts.get(index);
            var inFp = ctx.matches.inMarker(fp).filter(m -> !m.isPrivate()).toList();
            if (inFp.size() == 1 && inFp.getFirst().name() == MatchName.SEASON
                    && spansFilepartIgnoringSeps(inFp.getFirst(), fp, ctx.input)) {
                return fileparts.get(index + 1);
            }
            // The season head match is now private; check ALL matches (including private)
            // for a full-span season head.
            var allInFp = ctx.matches.inMarker(fp).toList();
            var seasonHeads = allInFp.stream().filter(m -> m.name() == MatchName.SEASON && m.value() == null
                    && spansFilepartIgnoringSeps(m, fp, ctx.input)).toList();
            if (seasonHeads.size() == 1) {
                return fileparts.get(index + 1);
            }
        }
        return null;
    }

    /** True when {@code m} occupies {@code fp} except for separator padding —
     * mirrors python's parent.span match-or-equals tolerance. */
    private static boolean spansFilepartIgnoringSeps(Match m, Marker fp, String input) {
        if (m.span().start() < fp.span().start() || m.span().end() > fp.span().end()) return false;
        for (int i = fp.span().start(); i < m.span().start(); i++) if (!Seps.isSep(input.charAt(i))) return false;
        for (int i = m.span().end(); i < fp.span().end(); i++) if (!Seps.isSep(input.charAt(i))) return false;
        return true;
    }

    record TitlesInFilepart(List<Match> titles, List<Match> toRemove) {}

    /** Returns null when no usable hole was found. */
    TitlesInFilepart checkTitlesInFilepart(ParseContext ctx, Marker filepart,
                                           java.util.function.Predicate<Match> additionalIgnore) {
        var ignore = (java.util.function.Predicate<Match>) m ->
                isIgnored(m) || (additionalIgnore != null && additionalIgnore.test(m));
        return checkTitlesInFilepart(ctx, filepart, ignore, MatchName.TITLE,
                List.of(TITLE),
                MatchName.ALTERNATIVE_TITLE, false);
    }

    /**
     * Shared implementation used by EpisodeTitleExtractor too; emits {@code matchName}-named
     * matches and (when {@code alternativeMatchName != null}) splits the hole on title_seps
     * to spawn alternative-title matches.
     */
    TitlesInFilepart checkTitlesInFilepart(ParseContext ctx, Marker filepart,
                                           java.util.function.Predicate<Match> ignore,
                                           MatchName matchName, List<String> matchTags,
                                           MatchName alternativeMatchName,
                                           boolean episodeTitleContext) {
        var holes = computeProcessedHoles(ctx, filepart, ignore);

        for (var hole : holes) {
            if (hole == null) continue;

            var adjustedHole = adjustHoleAndCollectMatches(ctx, filepart, hole, episodeTitleContext);
            if (adjustedHole.hole().span().length() <= 0 || adjustedHole.hole().value().isEmpty()) continue;

            var titles = createTitleMatches(ctx, adjustedHole.hole(), matchName, matchTags, alternativeMatchName);
            if (titles.isEmpty()) continue;

            return new TitlesInFilepart(titles, adjustedHole.toRemove());
        }
        return null;
    }

    private List<Holes.Hole> computeProcessedHoles(ParseContext ctx, Marker filepart,
                                                   java.util.function.Predicate<Match> ignore) {
        var allMatches = ctx.matches.snapshot();
        var holes = Holes.compute(ctx.input, filepart.span().start(), filepart.span().end(),
                allMatches, ignore, null, Formatters::titleText);
        return holesProcess(ctx, holes);
    }

    private record AdjustedHoleResult(Holes.Hole hole, List<Match> toRemove) {
    }

    private AdjustedHoleResult adjustHoleAndCollectMatches(ParseContext ctx, Marker filepart,
                                                           Holes.Hole hole, boolean episodeTitleContext) {
        var toRemove = new ArrayList<Match>();
        var toKeep = new ArrayList<Match>();
        var ignoredInHole = ctx.matches.range(hole.span().start(), hole.span().end(), TitleExtractor::isIgnored).toList();

        var currentHole = hole;
        if (!ignoredInHole.isEmpty()) {
            currentHole = adjustHoleBoundaries(ctx, filepart, currentHole, ignoredInHole, toKeep);
            collectMatchesToRemove(ctx, currentHole, ignoredInHole, toKeep, toRemove, episodeTitleContext);
        }

        return new AdjustedHoleResult(currentHole, toRemove);
    }

    private Holes.Hole adjustHoleBoundaries(ParseContext ctx, Marker filepart, Holes.Hole hole,
                                            List<Match> ignoredInHole, List<Match> toKeep) {
        var currentHole = hole;

        // Process trailing matches (reversed)
        var reversed = new ArrayList<>(ignoredInHole).reversed();
        for (var m : reversed) {
            var trailing = ctx.matches.chainBefore(currentHole.span().end(), ctx.input, Seps.CHARS, x -> x == m).orElse(null);
            if (trailing != null && shouldKeep(m, toKeep, ctx, filepart, currentHole, false)) {
                toKeep.add(m);
                currentHole = currentHole.withBounds(currentHole.span().start(), m.span().start());
            }
        }

        // Process starting matches
        for (var m : ignoredInHole) {
            if (toKeep.contains(m)) continue;
            var starting = ctx.matches.chainAfter(currentHole.span().start(), ctx.input, Seps.CHARS, x -> x == m).orElse(null);
            if (starting != null && shouldKeep(m, toKeep, ctx, filepart, currentHole, true)) {
                toKeep.add(m);
                currentHole = currentHole.withBounds(m.span().end(), currentHole.span().end());
            }
        }

        return currentHole;
    }

    private void collectMatchesToRemove(ParseContext ctx, Holes.Hole hole, List<Match> ignoredInHole,
                                        List<Match> toKeep, List<Match> toRemove, boolean episodeTitleContext) {
        for (var m : ignoredInHole) {
            if (shouldRemove(m, ctx, hole, episodeTitleContext)) {
                toRemove.add(m);
            }
        }
        toRemove.removeAll(toKeep);
    }

    private List<Match> createTitleMatches(ParseContext ctx, Holes.Hole hole, MatchName matchName,
                                           List<String> matchTags, MatchName alternativeMatchName) {
        if (isRedundantSeasonWord(hole.value(), ctx)) return List.of();

        var titles = new ArrayList<Match>();
        titles.add(new Match(matchName, hole.value(), hole.span(),
                Priority.DEFAULT, Set.copyOf(matchTags), false));

        if (alternativeMatchName != null) {
            var split = splitAndMergeHyphenatedWords(hole, ctx.input);
            return processSplitTitles(split, hole, matchName, matchTags, alternativeMatchName, ctx, titles);
        }

        return titles;
    }

    private List<Holes.Hole> splitAndMergeHyphenatedWords(Holes.Hole hole, String input) {
        var split = hole.split(Seps.TITLE_CHARS);
        if (split.size() <= 1) return split;

        var merged = new ArrayList<Holes.Hole>();
        merged.add(split.getFirst());

        for (int i = 1; i < split.size(); i++) {
            var prev = merged.getLast();
            var cur = split.get(i);

            if (isHyphenatedCompound(prev, cur, input)) {
                merged.set(merged.size() - 1, new Holes.Hole(
                        new Span(prev.span().start(), cur.span().end(), input.substring(prev.span().start(), cur.span().end())),
                        prev.formatter()
                ));
            } else {
                merged.add(cur);
            }
        }
        return merged;
    }

    private boolean isHyphenatedCompound(Holes.Hole prev, Holes.Hole cur, String input) {
        var sep = input.substring(prev.span().end(), cur.span().start());
        var prevRaw = prev.raw();
        var curRaw = cur.raw();

        return sep.length() == 1 && sep.charAt(0) == '-'
                && !prevRaw.isEmpty() && !Seps.isSep(prevRaw.charAt(prevRaw.length() - 1))
                && !curRaw.isEmpty() && !Seps.isSep(curRaw.charAt(0));
    }

    private List<Match> processSplitTitles(List<Holes.Hole> split, Holes.Hole originalHole,
                                           MatchName matchName, List<String> matchTags,
                                           MatchName alternativeMatchName, ParseContext ctx,
                                           List<Match> titles) {
        if (split.size() > 1) {
            return createMultipleTitleMatches(split, matchName, matchTags, alternativeMatchName, ctx);
        } else if (split.size() == 1 && !split.getFirst().equals(originalHole)) {
            return createSingleAdjustedMatch(split.getFirst(), matchName, matchTags);
        }
        return titles;
    }

    private List<Match> createMultipleTitleMatches(List<Holes.Hole> split, MatchName matchName,
                                                   List<String> matchTags, MatchName alternativeMatchName,
                                                   ParseContext ctx) {
        var titles = new ArrayList<Match>();
        var first = split.getFirst();
        titles.add(new Match(matchName, first.value(), first.span(),
                Priority.DEFAULT, Set.copyOf(matchTags), false));

        for (var i = 1; i < split.size(); i++) {
            var s = split.get(i);
            if (isRedundantSeasonWord(s.value(), ctx)) continue;
            titles.add(new Match(alternativeMatchName, s.value(), s.span(),
                    Priority.DEFAULT, Set.of(TITLE), false));
        }
        return titles;
    }

    private List<Match> createSingleAdjustedMatch(Holes.Hole hole, MatchName matchName, List<String> matchTags) {
        var titles = new ArrayList<Match>();
        titles.add(new Match(matchName, hole.value(), hole.span(),
                Priority.DEFAULT, Set.copyOf(matchTags), false));
        return titles;
    }

    private List<Holes.Hole> holesProcess(ParseContext ctx, List<Holes.Hole> holes) {
        var groupMarkers = new ArrayList<>(Markers.named(ctx.markers, "group").toList());
        var iter = groupMarkers.iterator();
        while (iter.hasNext()) {
            var g = iter.next();
            var groupMatch = new Match(MatchName.G, null, g.span(), Priority.DEFAULT, Set.of(), false);
            var path = Markers.atMatch(ctx.markers, groupMatch, m -> "path".equals(m.name())).orElse(null);

            if (path != null
                    && ((path.span().start() == g.span().start() && path.span().end() == g.span().end())
                    || (path.span().start() == g.span().start() - 1 && path.span().end() == g.span().end() + 1))) {
                iter.remove();
            }
        }
        var ret = new ArrayList<Holes.Hole>();
        for (var h : holes) ret.addAll(h.crop(groupMarkers));
        return ret;
    }

    private static final java.util.regex.Pattern SEASON_WORD_PATTERN = java.util.regex.Pattern.compile(
            "(?i)^(?:season|seasons|saison|saisons|seizoen|serie|series|temp|temporada|temporadas|"
                    + "staffel|staffeln|stagione|stagioni)[ ._-]*(\\d+)$");

    private static boolean isRedundantSeasonWord(String value, ParseContext ctx) {
        if (value == null || value.isEmpty()) return false;
        var m = SEASON_WORD_PATTERN.matcher(value.trim());
        if (!m.matches()) return false;
        int n;
        try { n = Integer.parseInt(m.group(1)); } catch (NumberFormatException _) { return false; }
        return ctx.matches.named(MatchName.SEASON)
                .anyMatch(s -> Integer.valueOf(n).equals(s.value()));
    }

    static boolean isIgnored(Match m) {
        if (!Set.of(MatchName.LANGUAGE, MatchName.COUNTRY, MatchName.EPISODE_DETAILS).contains(m.name())) return false;
        var raw = m.span().raw();
        if (raw == null) return true;
        var upper = raw.equals(raw.toUpperCase(java.util.Locale.ROOT))
                && raw.chars().anyMatch(Character::isLetter);
        return !(raw.length() > 3 && upper);
    }

    private boolean shouldKeep(Match m, List<Match> toKeep, ParseContext ctx, Marker filepart,
                               Holes.Hole hole, boolean starting) {
        if (Set.of(MatchName.LANGUAGE, MatchName.COUNTRY).contains(m.name())) {
            if (hole.value().length() == m.span().raw().length()) return true;
            var others = ctx.matches.inMarker(filepart).filter(
                    x -> x.name() == m.name() && !toKeep.contains(x)
                            && !NON_SPECIFIC_LANGUAGES.contains(String.valueOf(x.value()))
                            && (x.span().end() <= hole.span().start() || x.span().start() >= hole.span().end()));
            return others.findAny().isEmpty() && (!starting || m.span().raw().length() <= 3);
        }
        return false;
    }

    private boolean shouldRemove(Match m, ParseContext ctx, Holes.Hole hole, boolean episodeTitleContext) {
        if (m.name() == MatchName.EPISODE_DETAILS) {
            if (episodeTitleContext) return false;
            if ("episode".equals(ctx.options.type())) {
                return m.span().start() >= hole.span().start() && m.span().end() <= hole.span().end();
            }
        }
        return !episodeTitleContext || !Set.of(MatchName.LANGUAGE, MatchName.COUNTRY).contains(m.name());
    }
}