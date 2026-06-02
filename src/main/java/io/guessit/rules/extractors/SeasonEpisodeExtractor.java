package io.guessit.rules.extractors;

import io.guessit.core.pipeline.phases.Chain;
import io.guessit.core.pipeline.contracts.Extractor;
import io.guessit.core.pipeline.state.Match;
import io.guessit.core.pipeline.state.MatchName;
import io.guessit.core.pipeline.state.MatchTag;
import io.guessit.core.pipeline.state.ParseContext;
import io.guessit.core.pipeline.state.Priority;
import io.guessit.core.text.Span;
import io.guessit.core.text.Validators;
import io.guessit.core.text.patterns.SeasonEpisodePatterns;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static io.guessit.core.pipeline.state.MatchName.*;

/**
 * Extracts {@code season} and {@code episode} from compound forms:
 * {@code S01E02}, {@code 1x02}, {@code S01E02E03}, {@code S01-S03},
 * {@code Cap0102}, and the various separator/range expansions.
 */
public final class SeasonEpisodeExtractor implements Extractor {

    private static final String SEASON_GROUP = "season";
    private static final String EPISODE_GROUP = "episode";
    private static final String EPISODE2_GROUP = "episode2";
    private static final String EXTRAS_GROUP = "extras";
    private static final String SEASON2_GROUP = "season2";
    private static final String ALL_GROUP = "all";

    private static final String EPISODE_MARKER_GROUP = "episodeMarker";
    private static final String EPISODE_SEP_GROUP = "episodeSeparator";
    private static final String SEASON_MARKER_GROUP = "seasonMarker";
    private static final String SEASON_SEP_GROUP = "seasonSeparator";

    private static final Set<String> STRONG_SEPS = Set.of("+", "&", "and", "et");
    private static final Set<String> RANGE_SEPS = Set.of("-", "~", "to", "a");
    private static final Set<String> MARKER_SEPS = Set.of("e", "ex", "xe", "ep", "x", "d", "s");
    private static final int MAX_RANGE_GAP = 1;
    private static final int MAX_EXPAND_JUMP = 50;

    private static final Pattern HEAD_S_E = SeasonEpisodePatterns.buildHeadSePattern(SEASON_GROUP, EPISODE_MARKER_GROUP, EPISODE_GROUP);
    private static final Pattern TAIL_E = SeasonEpisodePatterns.buildTailEPattern(EPISODE_SEP_GROUP, EPISODE_GROUP);
    private static final Pattern HEAD_NUM_X = SeasonEpisodePatterns.buildHeadNumXPattern(SEASON_GROUP, EPISODE_MARKER_GROUP, EPISODE_GROUP);
    private static final Pattern HEAD_E = SeasonEpisodePatterns.buildHeadEPattern(SEASON_GROUP, EPISODE_MARKER_GROUP, EPISODE_GROUP);
    private static final Pattern TAIL_E_ONLY = SeasonEpisodePatterns.buildTailEOnlyPattern(EPISODE_SEP_GROUP, EPISODE_GROUP);
    private static final Pattern HEAD_S = SeasonEpisodePatterns.buildHeadSPattern(SEASON_GROUP);
    private static final Pattern TAIL_S = SeasonEpisodePatterns.buildTailSPattern(SEASON_SEP_GROUP, SEASON_GROUP);
    private static final Pattern HEAD_CAP = SeasonEpisodePatterns.buildHeadCapPattern(SEASON_MARKER_GROUP, SEASON_GROUP, EPISODE_GROUP, SEASON2_GROUP, EPISODE2_GROUP);
    private static final Pattern S_EXTRAS = SeasonEpisodePatterns.buildSExtrasPattern(SEASON_GROUP, EXTRAS_GROUP);
    private static final Pattern SXX_ALL = SeasonEpisodePatterns.buildSxxAllPattern(SEASON_GROUP, ALL_GROUP);

    @Override
    public String name() {
        return SEASON.name().toLowerCase();
    }

    @Override
    public String description() {
        return "season + episode tokens (SxxExx, 1x01, season+episode)";
    }

    @Override
    public void extract(ParseContext ctx) {
        var input = ctx.input;
        runChain(ctx, new Chain(HEAD_S_E).tail(TAIL_E, Chain.Repeater.STAR), input, true, false, false);
        runChain(ctx, new Chain(HEAD_NUM_X).tail(TAIL_E, Chain.Repeater.STAR), input, true, true, false);
        runChain(ctx, new Chain(HEAD_E).tail(TAIL_E_ONLY, Chain.Repeater.STAR), input, true, false, true);
        runChain(ctx, new Chain(HEAD_S).tail(TAIL_S, Chain.Repeater.STAR), input, false, false, false);
        runSExtras(ctx, input);
        runCap(ctx, input);
        runSxxAll(ctx, input);
    }

    private void runSxxAll(ParseContext ctx, String input) {
        var seps = Validators.sepsSurround(input);
        var m = SXX_ALL.matcher(input);

        while (m.find()) {
            var headSpan = new Span(m.start(), m.end(), m.group());
            var head = new Match(MatchName.SEASON, null, headSpan, Priority.DEFAULT, Set.of(MatchTag.SXX_EXX.getYamlValue()), false);

            if (seps.test(head)) {
                String cg = ctx.nextCoexistGroupTag();

                var sSpan = new Span(m.start(SEASON_GROUP), m.end(SEASON_GROUP), m.group(SEASON_GROUP));
                var aSpan = new Span(m.start(ALL_GROUP), m.end(ALL_GROUP), m.group(ALL_GROUP));

                ctx.matches.add(new Match(MatchName.SEASON, Integer.parseInt(m.group(SEASON_GROUP)),
                        sSpan, Priority.DEFAULT,
                        Set.of(MatchTag.SXX_EXX.getYamlValue(), MatchTag.COEXIST.getYamlValue(), cg), false));

                ctx.matches.add(new Match(MatchName.OTHER, "Complete",
                        aSpan, Priority.DEFAULT,
                        Set.of(MatchTag.SXX_EXX.getYamlValue(), MatchTag.COEXIST.getYamlValue(), cg), false));
            }
        }
    }

    private void runSExtras(ParseContext ctx, String input) {
        var seps = Validators.sepsSurround(input);
        var m = S_EXTRAS.matcher(input);

        while (m.find()) {
            var headSpan = new Span(m.start(), m.end(), m.group());
            var head = new Match(MatchName.SEASON, null, headSpan, Priority.DEFAULT, Set.of(MatchTag.SXX_EXX.getYamlValue()), false);

            if (seps.test(head)) {
                String cg = ctx.nextCoexistGroupTag();

                ctx.matches.add(new Match(MatchName.SEASON_HEAD, null, headSpan, Priority.DEFAULT, Set.of(MatchTag.SXX_EXX.getYamlValue()), true));

                var sSpan = new Span(m.start(SEASON_GROUP), m.end(SEASON_GROUP), m.group(SEASON_GROUP));
                ctx.matches.add(new Match(MatchName.SEASON, Integer.parseInt(m.group(SEASON_GROUP)),
                        sSpan, Priority.DEFAULT,
                        Set.of(MatchTag.SXX_EXX.getYamlValue(), MatchTag.COEXIST.getYamlValue(), cg), false));

                var eSpan = new Span(m.start(EXTRAS_GROUP), m.end(EXTRAS_GROUP), m.group(EXTRAS_GROUP));
                ctx.matches.add(new Match(MatchName.OTHER, "Extras",
                        eSpan, Priority.DEFAULT,
                        Set.of(MatchTag.SXX_EXX.getYamlValue(), MatchTag.COEXIST.getYamlValue(), cg, MatchTag.NO_RELEASE_GROUP_PREFIX.getYamlValue()), false));
            }
        }
    }

    private void runChain(ParseContext ctx, Chain chain, String input, boolean withEpisode, boolean skipScreenSize, boolean isWeakEChain) {
        var seps = Validators.sepsSurround(input);
        var screenSizeSpans = getScreenSizeSpans(ctx, skipScreenSize);
        var existingSxxExx = getExistingSxxExxSpans(ctx, isWeakEChain);

        for (var run : chain.scan(input, this::effectiveRunEnd)) {
            if (!shouldSkipRun(run, skipScreenSize, screenSizeSpans, isWeakEChain, existingSxxExx)) {
                var trimmedRun = trimRunToValidTails(run);
                int runEnd = calculateRunEnd(run, trimmedRun.episodeSpans, trimmedRun.seasonSpans);

                var headSpan = new Span(run.start(), runEnd, input.substring(run.start(), runEnd));
                var headMatch = new Match(MatchName.SEASON_HEAD, null, headSpan, Priority.DEFAULT, Set.of(MatchTag.SXX_EXX.getYamlValue()), true);

                if (seps.test(headMatch)) {
                    ctx.matches.add(headMatch);
                    emitSeasonAndEpisodeMatches(ctx, input, withEpisode, trimmedRun);
                }
            }
        }
    }

    private List<Span> getScreenSizeSpans(ParseContext ctx, boolean skipScreenSize) {
        return skipScreenSize
                ? ctx.matches.named(MatchName.SCREEN_SIZE).map(Match::span).toList()
                : List.of();
    }

    private List<Span> getExistingSxxExxSpans(ParseContext ctx, boolean isWeakEChain) {
        return isWeakEChain
                ? ctx.matches.all()
                  .filter(m -> m.hasTag(MatchTag.SXX_EXX))
                  .map(Match::span)
                  .toList()
                : List.of();
    }

    private boolean shouldSkipRun(Chain.Run run, boolean skipScreenSize, List<Span> screenSizeSpans,
                                  boolean isWeakEChain, List<Span> existingSxxExx) {
        var runSpan = new Span(run.start(), run.end(), "");
        if (skipScreenSize && screenSizeSpans.stream().anyMatch(runSpan::overlaps)) return true;
        return isWeakEChain && existingSxxExx.stream().anyMatch(runSpan::overlaps);
    }

    private record TrimmedRunData(List<String> seasonValues, List<String> episodeValues, List<int[]> seasonSpans,
                                  List<int[]> episodeSpans, List<String> episodeSeparators,
                                  List<String> episodeMarkers) {
    }

    private TrimmedRunData trimRunToValidTails(Chain.Run run) {
        var seasonValues = run.captures(SEASON_GROUP);
        var episodeValues = run.captures(EPISODE_GROUP);
        var seasonSpans = run.spans(SEASON_GROUP);
        var episodeSpans = run.spans(EPISODE_GROUP);
        var episodeSeparators = run.captures(EPISODE_SEP_GROUP);
        var episodeMarkers = run.captures(EPISODE_MARKER_GROUP);
        var seasonSeparators = run.captures(SEASON_SEP_GROUP);

        int validTails = longestValidTail(episodeValues, episodeSeparators);
        if (!episodeValues.isEmpty() && validTails < episodeValues.size() - 1) {
            int keep = validTails + 1;
            episodeValues = episodeValues.subList(0, keep);
            episodeSpans = episodeSpans.subList(0, keep);
            episodeSeparators = episodeSeparators.subList(0, Math.max(0, keep - 1));
        }

        int validSeasonTails = longestValidTail(seasonValues, seasonSeparators);
        if (!seasonValues.isEmpty() && validSeasonTails < seasonValues.size() - 1) {
            int keep = validSeasonTails + 1;
            seasonValues = seasonValues.subList(0, keep);
            seasonSpans = seasonSpans.subList(0, keep);
        }

        return new TrimmedRunData(seasonValues, episodeValues, seasonSpans,
                episodeSpans, episodeSeparators, episodeMarkers);
    }

    private int calculateRunEnd(Chain.Run run, List<int[]> episodeSpans, List<int[]> seasonSpans) {
        int runEnd = run.end();
        if (!episodeSpans.isEmpty()) {
            runEnd = Math.min(runEnd, episodeSpans.getLast()[1]);
        } else if (!seasonSpans.isEmpty()) {
            runEnd = Math.min(runEnd, seasonSpans.getLast()[1]);
        }
        return runEnd;
    }

    private void emitSeasonAndEpisodeMatches(ParseContext ctx, String input, boolean withEpisode, TrimmedRunData data) {
        boolean discRun = data.episodeMarkers.stream().anyMatch(s -> s.equalsIgnoreCase("d"))
                || data.episodeSeparators.stream().anyMatch(s -> s.equalsIgnoreCase("d"));
        String cg = withEpisode && !data.seasonValues.isEmpty() && !data.episodeValues.isEmpty()
                ? ctx.nextCoexistGroupTag() : null;

        emitSeasonMatches(ctx, input, data.seasonValues, data.seasonSpans, cg);

        if (withEpisode) {
            emitEpisodeMatches(ctx, input, data.episodeValues, data.episodeSpans, cg, discRun);
        }
    }

    private void emitSeasonMatches(ParseContext ctx, String input, List<String> seasonValues,
                                   List<int[]> seasonSpans, String cg) {
        for (int i = 0; i < seasonValues.size(); i++) {
            int[] sp = seasonSpans.get(i);
            var stags = cg != null ? Set.of(MatchTag.SXX_EXX.getYamlValue(), MatchTag.COEXIST.getYamlValue(), cg) : Set.of(MatchTag.SXX_EXX.getYamlValue(), MatchTag.COEXIST.getYamlValue());

            var span = new Span(sp[0], sp[1], input.substring(sp[0], sp[1]));
            ctx.matches.add(new Match(MatchName.SEASON, Integer.valueOf(seasonValues.get(i)),
                    span, Priority.DEFAULT, stags, false));
        }
    }

    private void emitEpisodeMatches(ParseContext ctx, String input, List<String> episodeValues,
                                    List<int[]> episodeSpans, String cg, boolean discRun) {
        Set<String> baseTags = cg != null ? Set.of(MatchTag.SXX_EXX.getYamlValue(), MatchTag.COEXIST.getYamlValue(), cg) : Set.of(MatchTag.SXX_EXX.getYamlValue(), MatchTag.COEXIST.getYamlValue());
        Set<String> tags = discRun
                ? java.util.stream.Stream.concat(baseTags.stream(), java.util.stream.Stream.of(MatchTag.DISC_MARKER.getYamlValue()))
                  .collect(java.util.stream.Collectors.toUnmodifiableSet())
                : baseTags;

        for (int i = 0; i < episodeValues.size(); i++) {
            int[] ep = episodeSpans.get(i);

            var span = new Span(ep[0], ep[1], input.substring(ep[0], ep[1]));
            ctx.matches.add(new Match(MatchName.EPISODE, Integer.valueOf(episodeValues.get(i)),
                    span, Priority.DEFAULT, tags, false));
        }
    }

    private int effectiveRunEnd(Chain.Run run) {
        var episodeValues = run.captures(EPISODE_GROUP);
        var episodeSpans = run.spans(EPISODE_GROUP);
        var episodeSeparators = run.captures(EPISODE_SEP_GROUP);
        var seasonValues = run.captures(SEASON_GROUP);
        var seasonSpans = run.spans(SEASON_GROUP);
        var seasonSeparators = run.captures(SEASON_SEP_GROUP);

        int epKeep = episodeValues.isEmpty() ? 0
                : Math.min(longestValidTail(episodeValues, episodeSeparators) + 1, episodeValues.size());
        int seasonKeep = seasonValues.isEmpty() ? 0
                : Math.min(longestValidTail(seasonValues, seasonSeparators) + 1, seasonValues.size());

        int end;
        if (epKeep > 0) {
            end = episodeSpans.get(epKeep - 1)[1];
        } else if (seasonKeep > 0) {
            end = seasonSpans.get(seasonKeep - 1)[1];
        } else {
            return -1;
        }

        if (end >= run.end()) return -1;
        return end;
    }

    private static int longestValidTail(List<String> values, List<String> seps) {
        int validTails = 0;

        for (int i = 1; i < values.size() && i - 1 < seps.size(); i++) {
            String sep = seps.get(i - 1).toLowerCase(java.util.Locale.ROOT);
            int prev = Integer.parseInt(values.get(i - 1));
            int cur = Integer.parseInt(values.get(i));

            if (STRONG_SEPS.contains(sep)) {
                return values.size() - 1;
            }

            if (!isValidTailStep(sep, prev, cur)) {
                return validTails;
            }

            validTails = i;
        }

        return validTails;
    }

    private static boolean isValidTailStep(String sep, int prev, int cur) {
        boolean isWeak = !RANGE_SEPS.contains(sep) && !MARKER_SEPS.contains(sep);
        int gap = cur - prev;

        if (isWeak) {
            return gap > 0 && gap <= MAX_RANGE_GAP + 1;
        } else if (RANGE_SEPS.contains(sep)) {
            return gap > 0 && gap <= MAX_EXPAND_JUMP;
        }

        return true;
    }

    private void runCap(ParseContext ctx, String input) {
        var seps = Validators.sepsSurround(input);
        var matcher = HEAD_CAP.matcher(input);

        while (matcher.find()) {
            var headSpan = new Span(matcher.start(), matcher.end(), matcher.group());
            var head = new Match(MatchName.SEASON, null, headSpan, Priority.DEFAULT, Set.of(MatchTag.SXX_EXX.getYamlValue(), MatchTag.SEE_PATTERN.getYamlValue()), false);

            if (seps.test(head)) {
                extractCapMatchAndExtensions(ctx, matcher);
            }
        }
    }

    private void extractCapMatchAndExtensions(ParseContext ctx, java.util.regex.Matcher matcher) {
        String cg = ctx.nextCoexistGroupTag();

        var sSpan = new Span(matcher.start(SEASON_GROUP), matcher.end(SEASON_GROUP), matcher.group(SEASON_GROUP));
        ctx.matches.add(new Match(MatchName.SEASON, Integer.parseInt(matcher.group(SEASON_GROUP)),
                sSpan,
                Priority.DEFAULT, Set.of(MatchTag.SXX_EXX.getYamlValue(), MatchTag.COEXIST.getYamlValue(), MatchTag.SEE_PATTERN.getYamlValue(), cg), false));

        var eSpan = new Span(matcher.start(EPISODE_GROUP), matcher.end(EPISODE_GROUP), matcher.group(EPISODE_GROUP));
        ctx.matches.add(new Match(MatchName.EPISODE, Integer.parseInt(matcher.group(EPISODE_GROUP)),
                eSpan,
                Priority.DEFAULT, Set.of(MatchTag.SXX_EXX.getYamlValue(), MatchTag.COEXIST.getYamlValue(), MatchTag.SEE_PATTERN.getYamlValue(), cg), false));

        if (matcher.group(SEASON2_GROUP) != null) {
            extractCapSecondaryEpisode(ctx, matcher, cg);
        }
    }

    private void extractCapSecondaryEpisode(ParseContext ctx, java.util.regex.Matcher matcher, String cg) {
        int s1 = Integer.parseInt(matcher.group(SEASON_GROUP));
        int s2 = Integer.parseInt(matcher.group(SEASON2_GROUP));
        int e1 = Integer.parseInt(matcher.group(EPISODE_GROUP));
        int e2 = Integer.parseInt(matcher.group(EPISODE2_GROUP));

        var e2Span = new Span(matcher.start(EPISODE2_GROUP), matcher.end(EPISODE2_GROUP), matcher.group(EPISODE2_GROUP));
        ctx.matches.add(new Match(MatchName.EPISODE, e2, e2Span, Priority.DEFAULT, Set.of(MatchTag.SXX_EXX.getYamlValue(), MatchTag.COEXIST.getYamlValue(), MatchTag.SEE_PATTERN.getYamlValue(), cg), false));

        if (s2 == s1 && e2 > e1) {
            for (int v = e1 + 1; v < e2; v++) {
                var fillSpan = new Span(matcher.end(EPISODE_GROUP), matcher.start(EPISODE2_GROUP), String.valueOf(v));
                ctx.matches.add(new Match(MatchName.EPISODE, v, fillSpan, Priority.DEFAULT,
                        Set.of(MatchTag.SXX_EXX.getYamlValue(), MatchTag.COEXIST.getYamlValue(), MatchTag.SEE_PATTERN.getYamlValue(), MatchTag.RANGE_FILL.getYamlValue(), cg), false));
            }
        }
    }

    @Override
    public void postProcess(ParseContext ctx) {
        dropTailOverCodec(ctx);
        expandRanges(ctx);
        removeInvalidSecondaryChain(ctx, MatchName.SEASON);
        removeInvalidSecondaryChain(ctx, MatchName.EPISODE);
    }

    private void dropTailOverCodec(ParseContext ctx) {
        var blockingNames = Set.of(MatchName.VIDEO_CODEC, MatchName.AUDIO_CODEC, MatchName.SOURCE,
                MatchName.SCREEN_SIZE, MatchName.AUDIO_CHANNELS, MatchName.AUDIO_PROFILE, MatchName.VIDEO_PROFILE);
        var blocking = ctx.matches.all()
                .filter(m -> blockingNames.contains(m.name()))
                .toList();

        if (blocking.isEmpty()) return;

        for (var name : new MatchName[]{MatchName.SEASON, MatchName.EPISODE}) {
            var sxxExxList = ctx.matches.named(name)
                    .filter(m -> m.hasTag(MatchTag.SXX_EXX))
                    .sorted(java.util.Comparator.comparingInt(m -> m.span().start()))
                    .toList();

            if (sxxExxList.size() <= 1) continue;

            var toRemove = new ArrayList<Match>();
            for (int i = 1; i < sxxExxList.size(); i++) {
                var m = sxxExxList.get(i);
                for (var b : blocking) {
                    if (b.span().overlaps(m.span())) {
                        toRemove.add(m);
                        break;
                    }
                }
            }
            toRemove.forEach(ctx.matches::remove);
        }
    }

    private void expandRanges(ParseContext ctx) {
        var input = ctx.input;
        var episodes = ctx.matches.named(MatchName.EPISODE)
                .filter(m -> m.hasTag(MatchTag.SXX_EXX))
                .sorted(java.util.Comparator.comparingInt(m -> m.span().start()))
                .toList();

        for (int i = 0; i + 1 < episodes.size(); i++) {
            processRangeExpansion(ctx, input, episodes.get(i), episodes.get(i + 1));
        }
    }

    private void processRangeExpansion(ParseContext ctx, String input, Match prev, Match next) {
        if (!prev.span().isBefore(next.span()) || prev.span().distanceTo(next.span()) > 3) return;

        var gap = input.substring(prev.span().end(), next.span().start());
        if (!containsRange(gap)) return;

        int prevVal = (Integer) prev.value();
        int nextVal = (Integer) next.value();

        if (nextVal - prevVal > MAX_EXPAND_JUMP) return;

        boolean disc = prev.hasTag(MatchTag.DISC_MARKER) && next.hasTag(MatchTag.DISC_MARKER);

        var fillTags = disc
                ? Set.of(MatchTag.SXX_EXX.getYamlValue(), MatchTag.COEXIST.getYamlValue(), MatchTag.RANGE_FILL.getYamlValue(), MatchTag.DISC_MARKER.getYamlValue())
                : Set.of(MatchTag.SXX_EXX.getYamlValue(), MatchTag.COEXIST.getYamlValue(), MatchTag.RANGE_FILL.getYamlValue());

        for (int v = prevVal + 1; v < nextVal; v++) {
            var span = new Span(prev.span().end(), next.span().start(), String.valueOf(v));
            ctx.matches.add(new Match(MatchName.EPISODE, v, span, Priority.DEFAULT, fillTags, false));
        }
    }

    private static boolean containsRange(String gap) {
        var lc = gap.toLowerCase(java.util.Locale.ROOT).strip();
        return lc.equals("-") || lc.equals("~") || lc.equals("to") || lc.equals("a");
    }

    private void removeInvalidSecondaryChain(ParseContext ctx, MatchName prop) {
        var matches = ctx.matches.named(prop)
                .sorted(java.util.Comparator.comparingInt(m -> m.span().start())).toList();
        if (matches.size() <= 1) return;

        boolean strongSeen = matches.stream().anyMatch(m -> m.hasTag(MatchTag.SXX_EXX));
        if (!strongSeen) return;

        int strongMaxEnd = calculateStrongMaxEnd(matches);
        var mediaSpans = collectMediaSpans(ctx);

        var toRemove = matches.stream()
                .filter(m -> shouldRemoveMatch(m, matches, strongMaxEnd, mediaSpans, ctx.input))
                .toList();

        toRemove.forEach(ctx.matches::remove);
    }

    private int calculateStrongMaxEnd(List<Match> matches) {
        return matches.stream()
                .filter(m -> m.hasTag(MatchTag.SXX_EXX))
                .mapToInt(m -> m.span().end())
                .max()
                .orElse(Integer.MAX_VALUE);
    }

    private List<Match> collectMediaSpans(ParseContext ctx) {
        var mediaNames = Set.of(MatchName.SCREEN_SIZE, MatchName.SOURCE, MatchName.VIDEO_CODEC,
                MatchName.AUDIO_CODEC, MatchName.AUDIO_CHANNELS, MatchName.AUDIO_PROFILE, MatchName.VIDEO_PROFILE,
                MatchName.STREAMING_SERVICE);
        return ctx.matches.all()
                .filter(m -> mediaNames.contains(m.name()))
                .toList();
    }

    private boolean shouldRemoveMatch(Match m, List<Match> allMatches,
                                      int strongMaxEnd, List<Match> mediaSpans, String input) {
        if (m.hasTag(MatchTag.SXX_EXX) || m.hasTag(MatchTag.WEAK_DUPLICATE)) return false;

        boolean isWeak = m.hasTag(MatchTag.WEAK_EPISODE);

        if (isWeak && m.span().start() >= strongMaxEnd && !hasMediaAfter(m, mediaSpans)) return false;

        return !isHighValueRangePaired(m, allMatches, input);
    }

    private boolean hasMediaAfter(Match weak, List<Match> mediaSpans) {
        return mediaSpans.stream().anyMatch(media -> media.span().isAfter(weak.span()));
    }

    private boolean isHighValueRangePaired(Match m, List<Match> matches, String input) {
        boolean isWeak = m.hasTag(MatchTag.WEAK_EPISODE) || m.hasTag(MatchTag.WEAK_DUPLICATE);
        if (!isWeak || !(m.value() instanceof Integer iv) || iv < 100) return false;

        return matches.stream().anyMatch(other -> isValidRangePair(m, other, input));
    }

    private boolean isValidRangePair(Match cur, Match other, String input) {
        if (other == cur || !other.hasTag(MatchTag.WEAK_EPISODE) || other.hasTag(MatchTag.WEAK_DUPLICATE))
            return false;
        if (!(other.value() instanceof Integer ov) || ov < 100) return false;

        if (cur.span().overlaps(other.span()) || cur.span().distanceTo(other.span()) > 5) return false;

        Match first = cur.span().isBefore(other.span()) ? cur : other;
        Match second = first == cur ? other : cur;

        String gap = input.substring(first.span().end(), second.span().start());
        return isValidGap(gap);
    }

    private static boolean isValidGap(String gap) {
        boolean foundRangeSeparator = false;

        for (int i = 0; i < gap.length(); i++) {
            char c = gap.charAt(i);

            if (c == '-' || c == '~') {
                if (foundRangeSeparator) return false;
                foundRangeSeparator = true;
            } else if (c != ' ' && c != '.' && c != '_') {
                return false;
            }
        }

        return foundRangeSeparator;
    }
}