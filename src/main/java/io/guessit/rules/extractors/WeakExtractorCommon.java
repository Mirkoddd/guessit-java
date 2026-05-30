package io.guessit.rules.extractors;

import io.guessit.core.pipeline.state.Marker;
import io.guessit.core.pipeline.state.Match;
import io.guessit.core.pipeline.state.MatchName;
import io.guessit.core.pipeline.state.ParseContext;
import io.guessit.core.text.patterns.WeakCommonPatterns;

import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Shared utilities, constants, and Sift patterns for weak extractors
 * (WeakDuplicateExtractor and WeakEpisodeExtractor).
 */
final class WeakExtractorCommon {

    private WeakExtractorCommon() {
    }

    static final String TYPE_MOVIE = "movie";
    static final String TYPE_EPISODE = "episode";

    static final String WEAK_EPISODE = "weak-episode";
    static final String WEAK_DUPLICATE = "weak-duplicate";
    static final String SXXEXX = "SxxExx";

    static final String MARKER_PATH = "path";
    static final String MARKER_GROUP = "group";

    static final Pattern RANGE_SEP = WeakCommonPatterns.buildRangePattern();

    static boolean isInside(Match target, int start, int end) {
        return target.span().start() >= start && target.span().end() <= end;
    }

    static boolean isInside(Match target, Match container) {
        return target.span().start() >= container.span().start() && target.span().end() <= container.span().end();
    }

    static boolean isInside(Match target, Marker container) {
        return container.covers(target.span());
    }

    static void removeMatches(ParseContext ctx, Stream<Match> streamToRemove) {
        streamToRemove.toList().forEach(ctx.matches::remove);
    }

    static boolean hasScreenSizeInGroup(ParseContext ctx) {
        return ctx.markers.stream()
                .filter(mk -> MARKER_GROUP.equals(mk.name()))
                .anyMatch(mk -> ctx.matches.named(MatchName.SCREEN_SIZE).anyMatch(m -> mk.covers(m.span())));
    }
}