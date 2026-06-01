package io.guessit.rules.extractors;

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

    static final String MARKER_PATH = "path";
    static final String MARKER_GROUP = "group";

    static final Pattern RANGE_SEP = WeakCommonPatterns.buildRangePattern();

    static void removeMatches(ParseContext ctx, Stream<Match> streamToRemove) {
        streamToRemove.toList().forEach(ctx.matches::remove);
    }

    static boolean hasScreenSizeInGroup(ParseContext ctx) {
        return ctx.markers.stream()
                .filter(mk -> MARKER_GROUP.equals(mk.name()))
                .anyMatch(mk -> ctx.matches.named(MatchName.SCREEN_SIZE).anyMatch(m -> mk.covers(m.span())));
    }
}