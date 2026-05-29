package io.guessit.rules.extractors;

import io.guessit.core.pipeline.contracts.Extractor;
import io.guessit.core.pipeline.state.Match;
import io.guessit.core.pipeline.state.MatchName;
import io.guessit.core.pipeline.state.ParseContext;
import io.guessit.core.pipeline.state.Priority;
import io.guessit.core.text.*;
import io.guessit.core.text.patterns.ScreenSizePatterns;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

public final class ScreenSizeExtractor implements Extractor {

    public static final String SCREEN_SIZE = "screen_size";
    public static final String WEAK_SCREEN_SIZE = "weak.screen_size";
    public static final String NORMALIZED = "normalized";

    private static final String GRP_HEIGHT = "height";
    private static final String GRP_WIDTH = "width";
    private static final String GRP_SCAN = "scan";
    private static final String GRP_FRAME_RATE = "fr";
    private static final String GRP_VALUE = "value";

    private static final String CONF_INTERLACED = "interlaced";
    private static final String CONF_PROGRESSIVE = "progressive";
    private static final String CONF_FRAME_RATES = "frame_rates";
    private static final String CONF_MIN_AR = "min_ar";
    private static final String CONF_MAX_AR = "max_ar";

    private static final String CACHE_TYPE_INTERLACED = "interlaced";
    private static final String CACHE_TYPE_PROGRESSIVE = "progressive";
    private static final String CACHE_TYPE_PROGRESSIVE_HD = "progressive_hd";
    private static final String CACHE_TYPE_PROGRESSIVE_X = "progressive_x";
    private static final String CACHE_TYPE_PROGRESSIVE_WEAK = "progressive_weak";
    private static final String CACHE_TYPE_STANDALONE_FR = "standalone_fr";

    private static final String SCAN_INTERLACED = "i";
    private static final String SCAN_PROGRESSIVE = "p";
    private static final String SUFFIX_HD = "hd";
    private static final String SUFFIX_X = "x";
    private static final String VALUE_4K_LITERAL = "4k";
    private static final String VALUE_2160P_NORMALIZED = "2160p";

    private static final String TAG_DERIVED_SCREEN_SIZE = "derivedFrom:screen_size";
    private static final String TAG_COEXIST = "coexist";
    private static final String TAG_WEAK_EPISODE = "weak-episode";
    private static final String TYPE_MOVIE = "movie";

    private static final String MARKER_PATH = "path";
    private static final String MARKER_WHOLE = "whole";

    private static final Pattern WH_P = ScreenSizePatterns.buildWhPattern(GRP_WIDTH, GRP_HEIGHT);
    private static final Pattern WIDTH_HEIGHT_NORM = ScreenSizePatterns.buildWidthHeightNorm(GRP_WIDTH, GRP_HEIGHT, GRP_SCAN);
    private static final Pattern HEIGHT_SCAN_NORM = ScreenSizePatterns.buildHeightScanNorm(GRP_HEIGHT, GRP_SCAN);
    private static final Pattern FRAME_RATE_PATTERN = ScreenSizePatterns.buildFrameRatePattern(GRP_FRAME_RATE);

    private final ConcurrentMap<PatternCacheKey, Pattern> patternCache = new ConcurrentHashMap<>();

    @Override
    public String name() {
        return SCREEN_SIZE;
    }

    @Override
    public String description() {
        return "resolution (480p / 720p / 1080p / 2160p / 4K, including i variants)";
    }

    @Override
    public void extract(ParseContext ctx) {
        var section = ctx.config.section(SCREEN_SIZE);
        var validator = Validators.sepsSurround(ctx.input);
        var opts = RegexOpts.defaults().withValidator(validator);

        extractFallbackWidthHeight(ctx, opts);
        extractDynamicPatterns(ctx, section, opts, validator);
        extract4kLiteral(ctx, validator);
        extractStandaloneFrameRate(ctx, section, validator);
    }

    private void extractFallbackWidthHeight(ParseContext ctx, RegexOpts opts) {
        for (var m : PatternMatcher.regex(ctx.input, WH_P, MatchName.SCREEN_SIZE, opts, ctx.trace)) {
            ctx.matches.add(m);
        }
    }

    private void extractDynamicPatterns(ParseContext ctx, Map<String, Object> section, RegexOpts opts, java.util.function.Predicate<Match> validator) {
        var interlaced = stringList(section.get(CONF_INTERLACED));
        var progressive = stringList(section.get(CONF_PROGRESSIVE));
        var frameRates = stringList(section.get(CONF_FRAME_RATES));

        if (!interlaced.isEmpty()) {
            Pattern p = patternCache.computeIfAbsent(new PatternCacheKey(CACHE_TYPE_INTERLACED, interlaced, frameRates),
                    k -> ScreenSizePatterns.buildScanPattern(SCAN_INTERLACED, k.list1(), k.list2(), GRP_WIDTH, GRP_HEIGHT, GRP_SCAN));
            addMatches(ctx, p, opts, MatchName.SCREEN_SIZE);
        }

        if (!progressive.isEmpty()) {
            Pattern pBase = patternCache.computeIfAbsent(new PatternCacheKey(CACHE_TYPE_PROGRESSIVE, progressive, frameRates),
                    k -> ScreenSizePatterns.buildScanPattern(SCAN_PROGRESSIVE, k.list1(), k.list2(), GRP_WIDTH, GRP_HEIGHT, GRP_SCAN));
            addMatches(ctx, pBase, opts, MatchName.SCREEN_SIZE);

            Pattern pHd = patternCache.computeIfAbsent(new PatternCacheKey(CACHE_TYPE_PROGRESSIVE_HD, progressive, List.of()),
                    k -> ScreenSizePatterns.buildProgressiveSuffixPattern(SUFFIX_HD, k.list1(), GRP_WIDTH, GRP_HEIGHT, GRP_SCAN));
            addMatches(ctx, pHd, opts, MatchName.SCREEN_SIZE);

            Pattern pX = patternCache.computeIfAbsent(new PatternCacheKey(CACHE_TYPE_PROGRESSIVE_X, progressive, List.of()),
                    k -> ScreenSizePatterns.buildProgressiveSuffixPattern(SUFFIX_X, k.list1(), GRP_WIDTH, GRP_HEIGHT, GRP_SCAN));
            addMatches(ctx, pX, opts, MatchName.SCREEN_SIZE);

            var weakOpts = RegexOpts.defaults()
                    .withValidator(validator)
                    .withTags(Set.of(WEAK_SCREEN_SIZE));

            Pattern pWeak = patternCache.computeIfAbsent(new PatternCacheKey(CACHE_TYPE_PROGRESSIVE_WEAK, progressive, List.of()),
                    k -> ScreenSizePatterns.buildProgressiveWeakPattern(k.list1(), GRP_WIDTH, GRP_HEIGHT));
            addMatches(ctx, pWeak, weakOpts, MatchName.SCREEN_SIZE);
        }
    }

    private void extract4kLiteral(ParseContext ctx, java.util.function.Predicate<Match> validator) {
        var fourK = StringOpts.defaults().withValidator(validator);
        for (var m : PatternMatcher.string(ctx.input, Set.of(VALUE_4K_LITERAL), MatchName.SCREEN_SIZE, fourK, ctx.trace)) {
            ctx.matches.add(new Match(MatchName.SCREEN_SIZE, VALUE_2160P_NORMALIZED, m.start(), m.end(), m.raw(),
                    m.priority(), Set.of(NORMALIZED), false));
        }
    }

    private void extractStandaloneFrameRate(ParseContext ctx, Map<String, Object> section, java.util.function.Predicate<Match> validator) {
        var frameRates = stringList(section.get(CONF_FRAME_RATES));
        if (frameRates.isEmpty()) return;

        var frOpts = RegexOpts.defaults()
                .withValue(s -> {
                    int dotIdx = s.indexOf('.');
                    return Integer.valueOf(dotIdx == -1 ? s : s.substring(0, dotIdx));
                })
                .withTags(Set.of(TAG_COEXIST))
                .withValidator(validator);

        Pattern p = patternCache.computeIfAbsent(new PatternCacheKey(CACHE_TYPE_STANDALONE_FR, frameRates, List.of()),
                k -> ScreenSizePatterns.buildStandaloneFrameRatePattern(k.list1(), GRP_VALUE));

        addMatches(ctx, p, frOpts, MatchName.FRAME_RATE);
    }

    private void addMatches(ParseContext ctx, Pattern pattern, RegexOpts opts, MatchName matchName) {
        for (var m : PatternMatcher.regex(ctx.input, pattern, matchName, opts, ctx.trace)) {
            ctx.matches.add(m);
        }
    }

    @Override
    public void postProcess(ParseContext ctx) {
        var section = ctx.config.section(SCREEN_SIZE);
        var standardHeights = new HashSet<>(stringList(section.get(CONF_PROGRESSIVE)));
        double minAr = ((Number) section.getOrDefault(CONF_MIN_AR, 1.333)).doubleValue();
        double maxAr = ((Number) section.getOrDefault(CONF_MAX_AR, 1.898)).doubleValue();

        normalizeScreenSizeMatches(ctx, standardHeights, minAr, maxAr);
        resolveWeakScreenSizeConflicts(ctx);
        extractFrameRatesFromScreenSize(ctx);
        keepOnlyLastDistinctScreenSize(ctx);
    }

    private void normalizeScreenSizeMatches(ParseContext ctx, Set<String> standardHeights, double minAr, double maxAr) {
        for (var m : ctx.matches.named(MatchName.SCREEN_SIZE).toList()) {
            if (m.tags().contains(NORMALIZED)) continue;

            var wh = WIDTH_HEIGHT_NORM.matcher(m.raw());
            if (wh.find()) {
                normalizeWidthHeightMatch(ctx, m, wh, standardHeights, minAr, maxAr);
            } else {
                var hs = HEIGHT_SCAN_NORM.matcher(m.raw());
                if (hs.find()) {
                    normalizeHeightScanMatch(ctx, m, hs);
                }
            }
        }
    }

    private void normalizeWidthHeightMatch(ParseContext ctx, Match m, java.util.regex.Matcher wh,
                                           Set<String> standardHeights, double minAr, double maxAr) {
        int w = Integer.parseInt(wh.group(GRP_WIDTH));
        int h = Integer.parseInt(wh.group(GRP_HEIGHT));
        String scan = wh.group(GRP_SCAN) == null ? SCAN_PROGRESSIVE : wh.group(GRP_SCAN).toLowerCase(Locale.ROOT);
        double ar = (double) w / h;

        ctx.matches.add(new Match(MatchName.ASPECT_RATIO, Math.round(ar * 1000.0) / 1000.0,
                m.start(), m.end(), m.raw(), m.priority(), Set.of(TAG_DERIVED_SCREEN_SIZE), false));

        String value = (standardHeights.contains(String.valueOf(h)) && minAr < ar && ar < maxAr)
                ? h + scan : w + "x" + h;
        Set<String> tags = m.tags().contains(WEAK_SCREEN_SIZE)
                ? Set.of(NORMALIZED, WEAK_SCREEN_SIZE) : Set.of(NORMALIZED);

        ctx.matches.replace(m, new Match(MatchName.SCREEN_SIZE, value, m.start(), m.end(), m.raw(),
                m.priority(), tags, false));
    }

    private void normalizeHeightScanMatch(ParseContext ctx, Match m, java.util.regex.Matcher hs) {
        String h = hs.group(GRP_HEIGHT);
        String scan = hs.group(GRP_SCAN) == null ? SCAN_PROGRESSIVE : hs.group(GRP_SCAN).toLowerCase(Locale.ROOT);
        Set<String> tags = m.tags().contains(WEAK_SCREEN_SIZE)
                ? Set.of(NORMALIZED, WEAK_SCREEN_SIZE) : Set.of(NORMALIZED);

        ctx.matches.replace(m, new Match(MatchName.SCREEN_SIZE, h + scan, m.start(), m.end(), m.raw(),
                m.priority(), tags, false));
    }

    private void resolveWeakScreenSizeConflicts(ParseContext ctx) {
        var weakSizes = ctx.matches.named(MatchName.SCREEN_SIZE)
                .filter(m -> m.tags().contains(WEAK_SCREEN_SIZE))
                .toList();

        if (weakSizes.isEmpty()) return;

        var strongNames = Set.of(MatchName.DATE, MatchName.SOURCE, MatchName.OTHER,
                MatchName.STREAMING_SERVICE, MatchName.VIDEO_PROFILE);
        var allMatches = ctx.matches.all().toList();

        for (var ws : weakSizes) {
            if (!hasStrongNeighbor(ws, allMatches, strongNames, ctx.input)) {
                ctx.matches.remove(ws);
                restoreWeakEpisodeIfNeeded(ctx, ws);
            }
        }
    }

    private boolean hasStrongNeighbor(Match ws, List<Match> allMatches, Set<MatchName> strongNames, String input) {
        for (var n : allMatches) {
            if (n == ws || !strongNames.contains(n.name())) continue;

            if (n.end() <= ws.start() && isGapOnlySeparators(input, n.end(), ws.start())) return true;
            if (n.start() >= ws.end() && isGapOnlySeparators(input, ws.end(), n.start())) return true;
        }
        return false;
    }

    private boolean isGapOnlySeparators(String input, int start, int end) {
        return input.substring(start, end).chars().allMatch(c -> Seps.isSep((char) c));
    }

    private void restoreWeakEpisodeIfNeeded(ParseContext ctx, Match ws) {
        boolean hasEpHere = ctx.matches.named(MatchName.EPISODE)
                .anyMatch(e -> e.start() == ws.start() && e.end() == ws.end());

        if (!hasEpHere && !TYPE_MOVIE.equals(ctx.options.type())) {
            String raw = ws.raw();

            if (!raw.isEmpty() && raw.chars().allMatch(Character::isDigit)) {
                int v = Integer.parseInt(raw);
                if (v >= 100 || io.guessit.rules.extractors.WeakEpisodeExtractor.EPISODE.equals(ctx.options.type())
                        || ctx.options.episodePreferNumber() != null) {
                    ctx.matches.add(new Match(MatchName.EPISODE, v, ws.start(), ws.end(),
                            raw, Priority.PROBABLE, Set.of(TAG_WEAK_EPISODE), false));
                }
            }
        }
    }

    private void extractFrameRatesFromScreenSize(ParseContext ctx) {
        boolean hasFrameRate = ctx.matches.all().anyMatch(m -> m.name() == MatchName.FRAME_RATE);
        if (hasFrameRate) return;

        for (var m : ctx.matches.named(MatchName.SCREEN_SIZE).toList()) {
            var fr = FRAME_RATE_PATTERN.matcher(m.raw());
            if (fr.find()) {
                var rawFr = fr.group(GRP_FRAME_RATE);

                int dotIdx = rawFr.indexOf('.');
                int val = Integer.parseInt(dotIdx == -1 ? rawFr : rawFr.substring(0, dotIdx));

                ctx.matches.add(new Match(MatchName.FRAME_RATE, val,
                        m.start() + fr.start(GRP_FRAME_RATE), m.start() + fr.end(GRP_FRAME_RATE),
                        rawFr, m.priority(), Set.of(TAG_COEXIST, TAG_DERIVED_SCREEN_SIZE), false));
            }
        }
    }

    private void keepOnlyLastDistinctScreenSize(ParseContext ctx) {
        for (var filePart : ctx.markers) {
            if (!MARKER_PATH.equals(filePart.name()) && !MARKER_WHOLE.equals(filePart.name())) continue;

            var inPart = ctx.matches.named(MatchName.SCREEN_SIZE)
                    .filter(m -> filePart.covers(m.start(), m.end()))
                    .sorted((a, b) -> a.start() != b.start()
                            ? Integer.compare(b.start(), a.start())
                            : Integer.compare(b.end(), a.end()))
                    .toList();

            if (inPart.size() > 1) {
                long distinct = inPart.stream().map(m -> String.valueOf(m.value())).distinct().count();
                if (distinct > 1) {
                    inPart.subList(1, inPart.size()).forEach(ctx.matches::remove);
                }
            }
        }
    }

    private static List<String> stringList(Object o) {
        if (o instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .toList();
        }
        return List.of();
    }

    private record PatternCacheKey(String type, List<String> list1, List<String> list2) {}
}