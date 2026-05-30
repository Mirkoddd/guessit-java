package io.guessit.rules.extractors;

import io.guessit.core.pipeline.contracts.Extractor;
import io.guessit.core.pipeline.state.MatchName;
import io.guessit.core.pipeline.state.ParseContext;
import io.guessit.core.text.PatternMatcher;
import io.guessit.core.text.RegexOpts;
import io.guessit.core.text.StringOpts;
import io.guessit.core.text.Validators;
import io.guessit.core.text.patterns.ContainerPatterns;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

/**
 * Extracts {@code container} (mkv, mp4, srt, …).
 */
public final class ContainerExtractor implements Extractor {

    public static final String CONTAINER = "container";

    private static final String TAG_EXTENSION = "extension";
    private static final String TAG_BODY = "body";
    private static final String TAG_SUBTITLE = "subtitle";
    private static final String TAG_INFO = "info";
    private static final String TAG_VIDEO = "video";
    private static final String TAG_TORRENT = "torrent";
    private static final String TAG_NZB = "nzb";

    private static final String CFG_SUBTITLES = "subtitles";
    private static final String CFG_INFO = "info";
    private static final String CFG_VIDEOS = "videos";
    private static final String CFG_TORRENT = "torrent";
    private static final String CFG_NZB = "nzb";

    private static final String EXT_SUB = "sub";
    private static final String EXT_ASS = "ass";

    private static final String GRP_EXT = "ext";

    private final ConcurrentMap<List<String>, Pattern> patternCache = new ConcurrentHashMap<>();

    @Override
    public String name() {
        return CONTAINER;
    }

    @Override
    public String description() {
        return "container / mimetype (mkv, mp4, avi, …)";
    }

    @Override
    public void extract(ParseContext ctx) {
        var section = ctx.config.section(CONTAINER);

        var subtitles = stringList(section.get(CFG_SUBTITLES));
        var info = stringList(section.get(CFG_INFO));
        var videos = stringList(section.get(CFG_VIDEOS));
        var torrent = stringList(section.get(CFG_TORRENT));
        var nzb = stringList(section.get(CFG_NZB));

        var input = ctx.input;

        extractExtensions(ctx, input, subtitles, TAG_SUBTITLE);
        extractExtensions(ctx, input, info, TAG_INFO);
        extractExtensions(ctx, input, videos, TAG_VIDEO);
        extractExtensions(ctx, input, torrent, TAG_TORRENT);
        extractExtensions(ctx, input, nzb, TAG_NZB);

        var body = new HashSet<>(subtitles);
        body.remove(EXT_SUB);
        body.remove(EXT_ASS);

        body.addAll(videos);
        body.addAll(torrent);
        body.addAll(nzb);

        var opts = StringOpts.defaults()
                .withValidator(Validators.sepsSurround(input))
                .withTags(Set.of(TAG_BODY));

        var potentialConflicts = ctx.matches.snapshot().stream()
                .filter(x -> (x.name() == MatchName.CONTAINER && x.tags().contains(TAG_EXTENSION)) ||
                        x.name() == MatchName.VIDEO_CODEC ||
                        x.name() == MatchName.AUDIO_CODEC ||
                        x.name() == MatchName.SCREEN_SIZE)
                .toList();

        for (var m : PatternMatcher.string(input, body, MatchName.CONTAINER, opts, ctx.trace)) {
            boolean hasConflict = potentialConflicts.stream()
                    .anyMatch(x -> x.span().start() < m.span().end() && x.span().end() > m.span().start());

            if (!hasConflict) {
                ctx.matches.add(m);
            }
        }
    }

    private void extractExtensions(ParseContext ctx, String input, List<String> extensions, String kindTag) {
        if (extensions.isEmpty()) return;

        Pattern p = patternCache.computeIfAbsent(extensions, extList ->
                ContainerPatterns.buildExtensionPattern(GRP_EXT, extList));

        var opts = RegexOpts.defaults()
                .withValue(s -> s.startsWith(".") ? s.substring(1).toLowerCase(Locale.ROOT) : s.toLowerCase(Locale.ROOT))
                .withTags(Set.of(TAG_EXTENSION, kindTag));

        for (var m : PatternMatcher.regex(input, p, MatchName.CONTAINER, opts, ctx.trace)) {
            ctx.matches.add(m);
        }
    }

    private static List<String> stringList(Object o) {
        if (!(o instanceof List<?> list)) {
            return List.of();
        }

        return list.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .toList();
    }
}