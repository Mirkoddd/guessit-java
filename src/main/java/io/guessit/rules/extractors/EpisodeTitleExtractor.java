package io.guessit.rules.extractors;

import io.guessit.core.pipeline.contracts.Extractor;
import io.guessit.core.pipeline.state.*;
import io.guessit.core.text.Formatters;
import io.guessit.core.text.Seps;
import io.guessit.core.text.Span;
import io.guessit.rules.post.TypeProcessor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public final class EpisodeTitleExtractor implements Extractor {
    public static final String SEASON = "season";

    private static final Set<MatchName> PREVIOUS_NAMES = Set.of(
            MatchName.EPISODE, MatchName.EPISODE_COUNT, MatchName.SEASON, MatchName.SEASON_COUNT, MatchName.DATE, MatchName.TITLE, MatchName.YEAR);

    private static final Set<MatchName> CONFLICT_PREVIOUS_NAMES = Set.of(
            MatchName.EPISODE, MatchName.EPISODE_COUNT, MatchName.SEASON, MatchName.SEASON_COUNT, MatchName.DATE, MatchName.YEAR);

    private static final Set<MatchName> NEXT_NAMES = Set.of(
            MatchName.STREAMING_SERVICE, MatchName.SCREEN_SIZE, MatchName.SOURCE, MatchName.VIDEO_CODEC,
            MatchName.AUDIO_CODEC, MatchName.OTHER, MatchName.CONTAINER);

    private static final Set<MatchName> AFFECTED_NAMES = Set.of(MatchName.PART, MatchName.YEAR);
    private static final Set<MatchName> AFFECTED_IF_HOLES_AFTER = Set.of(MatchName.PART);
    public static final String EPISODE_TITLE = "episode_title";
    private static final String MOVIE_TYPE = "movie";

    @Override
    public String name() {
        return EPISODE_TITLE;
    }

    @Override
    public String description() {
        return "episode title (text after season/episode tokens)";
    }

    @Override
    public void extract(ParseContext ctx) { /* no extraction phase */ }

    @Override
    public void postProcess(ParseContext ctx) {
        removeConflictsWithEpisodeTitle(ctx);
        filePart3EpisodeTitle(ctx);
        filePart2EpisodeTitle(ctx);
        titleToEpisodeTitle(ctx);
        episodeTitleFromPosition(ctx);
        alternativeTitleReplace(ctx);
        dropLanguagesInsideTitleHoles(ctx);
    }

    private static void dropLanguagesInsideTitleHoles(ParseContext ctx) {
        var titleSpans = ctx.matches.all()
                .filter(EpisodeTitleExtractor::isTitleRelated)
                .map(Match::span)
                .toList();

        if (titleSpans.isEmpty()) return;

        var toRemove = ctx.matches.all()
                .filter(m -> isShortLanguageMatch(m) && isStrictlyInsideAnySpan(m, titleSpans))
                .toList();

        toRemove.forEach(ctx.matches::remove);
    }

    private static boolean isTitleRelated(Match m) {
        return m.name() == MatchName.TITLE
                || m.name() == MatchName.ALTERNATIVE_TITLE
                || m.name() == MatchName.EPISODE_TITLE;
    }

    private static boolean isShortLanguageMatch(Match m) {
        return (m.name() == MatchName.LANGUAGE || m.name() == MatchName.SUBTITLE_LANGUAGE) && m.span().length() <= 3;
    }

    private static boolean isStrictlyInsideAnySpan(Match m, List<Span> spans) {
        return spans.stream().anyMatch(sp -> m.span().isInside(sp) && !m.span().equals(sp));
    }

    private void removeConflictsWithEpisodeTitle(ParseContext ctx) {
        var toRemove = new ArrayList<Match>();

        for (var fp : Markers.named(ctx.markers, MarkerType.PATH).toList()) {
            ctx.matches.inMarker(fp)
                    .filter(m -> AFFECTED_NAMES.contains(m.name()))
                    .filter(m -> conflictsWithEpisodeTitle(ctx, fp, m))
                    .forEach(toRemove::add);
        }

        toRemove.forEach(ctx.matches::remove);
    }

    private static boolean conflictsWithEpisodeTitle(ParseContext ctx, Marker fp, Match m) {
        var before = findAdjacentConflict(ctx, fp.span().start(), m.span().start(), true);
        if (before == null || !CONFLICT_PREVIOUS_NAMES.contains(before.name())) return false;

        var after = findAdjacentConflict(ctx, m.span().end(), fp.span().end(), false);
        if (after == null || !NEXT_NAMES.contains(after.name())) return false;

        var holesBefore = Holes.compute(ctx.input, before.span().end(), m.span().start(), ctx.matches.snapshot(), _ -> false, null, Formatters::cleanup);
        var holesAfter = Holes.compute(ctx.input, m.span().end(), after.span().start(), ctx.matches.snapshot(), _ -> false, null, Formatters::cleanup);

        if (holesBefore.isEmpty() && holesAfter.isEmpty()) return false;
        return !AFFECTED_IF_HOLES_AFTER.contains(m.name()) || !holesAfter.isEmpty();
    }

    private static Match findAdjacentConflict(ParseContext ctx, int start, int end, boolean isBefore) {
        var stream = ctx.matches.range(start, end, x -> !x.isPrivate());
        return isBefore
                ? stream.max(Comparator.comparingInt((Match m) -> m.span().end())).orElse(null)
                : stream.min(Comparator.comparingInt((Match m) -> m.span().start())).orElse(null);
    }

    private void titleToEpisodeTitle(ParseContext ctx) {
        var titles = ctx.matches.named(MatchName.TITLE).toList();

        long distinctValues = titles.stream()
                .filter(m -> m instanceof Match.StringMatch)
                .map(m -> ((Match.StringMatch) m).value())
                .distinct()
                .count();

        if (distinctValues < 2) return;

        for (var t : titles) {
            processTitleDemotion(ctx, t);
        }
    }

    private void processTitleDemotion(ParseContext ctx, Match t) {
        int prevEnd = ctx.matches.snapshot().stream()
                .filter(m -> !m.isPrivate() && m.span().end() <= t.span().start())
                .mapToInt(m -> m.span().end())
                .max().orElse(-1);

        if (prevEnd >= 0 && hasEpisodeEndingAt(ctx, prevEnd)) {
            ctx.matches.replace(t, t.withName(MatchName.EPISODE_TITLE));
        }
    }

    private boolean hasEpisodeEndingAt(ParseContext ctx, int targetEnd) {
        return ctx.matches.snapshot().stream()
                .filter(m -> !m.isPrivate())
                .anyMatch(m -> m.span().end() == targetEnd && m.name() == MatchName.EPISODE);
    }

    private void episodeTitleFromPosition(ParseContext ctx) {
        if (ctx.matches.named(MatchName.EPISODE_TITLE).findAny().isPresent()) return;

        var paths = ctx.markers.stream().filter(m -> m.type() == MarkerType.PATH).toList();
        var titleExtractor = new TitleExtractor();
        boolean hasCrc = ctx.matches.named(MatchName.CRC32).findAny().isPresent();
        boolean isMovie = MOVIE_TYPE.equals(TypeProcessor.predictType(ctx));

        for (var fp : Markers.markerSorted(paths, ctx.matches)) {
            if (extractEpisodeTitlesInFilePart(ctx, fp, titleExtractor, hasCrc, isMovie)) {
                break;
            }
        }
    }

    private boolean extractEpisodeTitlesInFilePart(ParseContext ctx, Marker fp, TitleExtractor titleExtractor,
                                                   boolean hasCrc, boolean isMovie) {
        if (ctx.matches.inMarker(fp).noneMatch(m -> m.name() == MatchName.TITLE)) {
            return false;
        }

        var titles = titleExtractor.checkTitlesInFilepart(ctx, fp, TitleExtractor::isIgnored,
                MatchName.EPISODE_TITLE, List.of(MatchTag.TITLE.getValue()), null, true);
        if (titles == null) return false;

        var titlesToAdd = titles.titles().stream()
                .filter(t -> shouldKeepEpisodeTitleCandidate(ctx, fp, t, hasCrc, isMovie))
                .toList();

        titlesToAdd.forEach(ctx.matches::add);
        titles.toRemove().forEach(ctx.matches::remove);

        return !titlesToAdd.isEmpty();
    }

    private boolean shouldKeepEpisodeTitleCandidate(ParseContext ctx, Marker fp, Match t, boolean hasCrc, boolean isMovie) {
        var prev = ctx.matches.previous(t, m -> PREVIOUS_NAMES.contains(m.name()));
        if (prev.isEmpty() && !hasCrc) return false;
        return !(isMovie && wedgedBetweenProperties(ctx, fp, t));
    }

    private static boolean wedgedBetweenProperties(ParseContext ctx, Marker filePart, Match candidate) {
        Set<MatchName> trailingNames = Set.of(MatchName.SOURCE, MatchName.VIDEO_CODEC, MatchName.AUDIO_CODEC,
                MatchName.SCREEN_SIZE, MatchName.AUDIO_CHANNELS, MatchName.AUDIO_PROFILE, MatchName.VIDEO_PROFILE,
                MatchName.STREAMING_SERVICE, MatchName.CONTAINER, MatchName.PART, MatchName.RELEASE_GROUP, MatchName.WEBSITE);

        boolean propBefore = hasTrailingPropertyInRange(ctx, trailingNames, filePart.span().start(), candidate.span().start());
        boolean propAfter = hasTrailingPropertyInRange(ctx, trailingNames, candidate.span().end(), filePart.span().end());

        return propBefore && propAfter;
    }

    private static boolean hasTrailingPropertyInRange(ParseContext ctx, Set<MatchName> names, int start, int end) {
        var targetSpan = new Span(start, end, "");
        return ctx.matches.all().anyMatch(m -> names.contains(m.name()) && m.span().isInside(targetSpan));
    }

    private static Optional<Match> previousAdjacent(ParseContext ctx, int startPos, Predicate<Match> predicate) {
        for (int p = startPos; p >= 0; p--) {
            final int endPos = p;

            var endingMatches = ctx.matches.all().filter(m -> m.span().end() == endPos).toList();
            if (endingMatches.isEmpty()) continue;

            return endingMatches.stream().filter(predicate).findFirst();
        }
        return Optional.empty();
    }

    private void alternativeTitleReplace(ParseContext ctx) {
        if (ctx.matches.named(MatchName.EPISODE_TITLE).findAny().isPresent()) return;

        ctx.matches.named(MatchName.ALTERNATIVE_TITLE).findFirst().ifPresent(alt ->
                ctx.matches.chainBefore(alt.span().start(), ctx.input, Seps.CHARS,
                                m -> m.hasTag(MatchTag.TITLE))
                        .ifPresent(mainTitle -> processAltTitleReplace(ctx, alt, mainTitle)));
    }

    private void processAltTitleReplace(ParseContext ctx, Match alt, Match mainTitle) {
        var prev = previousAdjacent(ctx, mainTitle.span().start(), m -> PREVIOUS_NAMES.contains(m.name()));
        boolean hasCrc = ctx.matches.named(MatchName.CRC32).findAny().isPresent();

        if (prev.isPresent() || hasCrc) {
            var newTags = new HashSet<>(alt.tags());
            newTags.add(MatchTag.ALTERNATIVE_REPLACED.getValue());

            if (alt instanceof Match.StringMatch sm) {
                ctx.matches.replace(alt, Match.string(MatchName.EPISODE_TITLE, sm.value(), sm.span(),
                        sm.priority(), Set.copyOf(newTags), sm.isPrivate()));
            }
        }
    }

    static void filePart3EpisodeTitleStatic(ParseContext ctx) {
        new EpisodeTitleExtractor().filePart3EpisodeTitle(ctx);
    }

    static void filePart2EpisodeTitleStatic(ParseContext ctx) {
        new EpisodeTitleExtractor().filePart2EpisodeTitle(ctx);
    }

    private void filePart3EpisodeTitle(ParseContext ctx) {
        if (ctx.matches.all().anyMatch(m -> m.hasTag(MatchTag.FILE_PART_TITLE))) return;

        var paths = Markers.named(ctx.markers, MarkerType.PATH).toList();
        if (paths.size() < 3) return;

        var filename = paths.getLast();
        var directory = paths.get(paths.size() - 2);
        var subdirectory = paths.get(paths.size() - 3);

        if (missingRequiredMatchInRange(ctx, MatchName.EPISODE, filename) ||
                missingRequiredMatchInRange(ctx, MatchName.SEASON, directory)) {
            return;
        }

        if (hasTitleInFilename(ctx, filename)) return;

        var h = findEpisodeTitleHoles(ctx, subdirectory);
        if (h != null) {
            ctx.matches.add(Match.string(MatchName.TITLE, h.value(), h.span(), Priority.DEFAULT, Set.of(), false));
        }
    }

    private boolean missingRequiredMatchInRange(ParseContext ctx, MatchName name, Marker marker) {
        return ctx.matches.inMarker(marker).noneMatch(m -> m.name() == name);
    }

    private boolean hasTitleInFilename(ParseContext ctx, Marker filename) {
        return ctx.matches.named(MatchName.TITLE)
                .anyMatch(m -> filename.covers(m.span()));
    }

    private static Holes.Hole findEpisodeTitleHoles(ParseContext ctx, Marker subdirectory) {
        Predicate<Match> ignore = m -> {
            if (m.hasTag(MatchTag.WEAK_EPISODE)) return true;
            if (!TitleExtractor.isIgnored(m)) return false;
            return (m.name() != MatchName.COUNTRY && m.name() != MatchName.LANGUAGE)
                    || !isBracketWrapped(ctx.input, m);
        };

        var holes = Holes.compute(ctx.input, subdirectory.span().start(), subdirectory.span().end(),
                ctx.matches.snapshot(), ignore, Seps.TITLE_CHARS, Formatters::cleanup);

        return holes.isEmpty() ? null : holes.getFirst();
    }

    private static boolean isBracketWrapped(String input, Match m) {
        int s = m.span().start();
        int e = m.span().end();
        if (s <= 0 || e >= input.length()) return false;
        char before = input.charAt(s - 1);
        char after = input.charAt(e);
        return (before == '(' && after == ')') || (before == '[' && after == ']');
    }

    private void filePart2EpisodeTitle(ParseContext ctx) {
        if (ctx.matches.all().anyMatch(m -> m.hasTag(MatchTag.FILE_PART_TITLE))) return;

        var paths = Markers.named(ctx.markers, MarkerType.PATH).toList();
        if (paths.size() < 2) return;

        var filename = paths.getLast();
        var directory = paths.get(paths.size() - 2);

        if (missingRequiredMatchInRange(ctx, MatchName.EPISODE, filename)) return;

        boolean hasSeason = ctx.matches.inMarker(directory).anyMatch(m -> m.name() == MatchName.SEASON)
                || ctx.matches.inMarker(filename).anyMatch(m -> m.name() == MatchName.SEASON);

        if (!hasSeason) return;

        var h = findEpisodeTitleHoles(ctx, directory);
        if (h != null) {
            ctx.matches.add(Match.string(MatchName.TITLE, h.value(), h.span(), Priority.DEFAULT, Set.of(MatchTag.FILE_PART_TITLE.getValue()), false));
        }
    }
}