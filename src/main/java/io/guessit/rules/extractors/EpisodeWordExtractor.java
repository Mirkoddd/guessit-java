package io.guessit.rules.extractors;

import io.guessit.core.pipeline.contracts.Extractor;
import io.guessit.core.pipeline.state.Match;
import io.guessit.core.pipeline.state.MatchName;
import io.guessit.core.pipeline.state.ParseContext;
import io.guessit.core.pipeline.state.Priority;
import io.guessit.core.text.Seps;
import io.guessit.core.text.Validators;
import io.guessit.core.text.patterns.EpisodeWordPatterns;
import io.guessit.rules.numerals.Numerals;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts {@code season}, {@code episode}, and {@code episode_count} from
 * word forms: "Episode 5", "Season 2", "Ep 12 of 24", "Saison 3", "Capitulo 7".
 */
public final class EpisodeWordExtractor implements Extractor {

    public static final String EPISODE = "episode";
    public static final String SEASON = "season";

    private static final String TYPE_MOVIE = "movie";
    private static final String TAG_SEASON_WORD = "season-word";
    private static final String TAG_EPISODE_WORD = "episode-word";

    private static final String GRP_COUNT = "count";
    private static final String GRP_SEASON_WORD = "seasonWord";
    private static final String GRP_SEASON_VAL = "seasonVal";
    private static final String GRP_OP = "op";
    private static final String GRP_VAL = "val";
    private static final String GRP_EP_WORD = "epWord";
    private static final String GRP_EP_VAL = "epVal";
    private static final String GRP_VERSION = "version";

    private static final int MAX_RANGE_GAP = 1;

    private static final Set<String> SHORT_EPISODE_WORDS = Set.of("e");

    private static final Set<MatchName> BLOCKED_MATCH_TYPES = EnumSet.of(
            MatchName.SCREEN_SIZE, MatchName.YEAR, MatchName.SOURCE,
            MatchName.VIDEO_CODEC, MatchName.AUDIO_CODEC,
            MatchName.VIDEO_PROFILE, MatchName.FRAME_RATE
    );

    private static final Set<String> STRONG_OPS = Set.of("&", "+", "and", "et");
    private static final Set<String> RANGE_OPS = Set.of("-", "~", "to", "a");

    private static String trimOpJunk(String s) {
        int start = 0;
        int end = s.length() - 1;

        while (start <= end && isOpJunk(s.charAt(start))) start++;
        while (end >= start && isOpJunk(s.charAt(end))) end--;

        return (start > 0 || end < s.length() - 1) ? s.substring(start, end + 1) : s;
    }

    private static boolean isOpJunk(char c) {
        return c == '.' || c == ' ' || c == '_';
    }

    private static final Pattern SEASON_RE = EpisodeWordPatterns.buildSeasonPattern(GRP_SEASON_WORD, GRP_SEASON_VAL, GRP_COUNT);
    private static final Pattern SEASON_TAIL_RE = EpisodeWordPatterns.buildSeasonTailPattern(GRP_OP, GRP_VAL);
    private static final Pattern AFTER_OF_RE = EpisodeWordPatterns.buildAfterOfPattern();
    private static final Pattern EP_RE_EPISODE_TYPE = EpisodeWordPatterns.buildEpisodePattern(true, GRP_EP_WORD, GRP_EP_VAL, GRP_VERSION, GRP_COUNT);
    private static final Pattern EP_RE_DEFAULT = EpisodeWordPatterns.buildEpisodePattern(false, GRP_EP_WORD, GRP_EP_VAL, GRP_VERSION, GRP_COUNT);
    private static final Pattern DETACHED_EP_COUNT_RE = EpisodeWordPatterns.buildDetachedEpCountPattern(GRP_EP_VAL, GRP_COUNT);

    @Override
    public String name() {
        return "episode_word";
    }

    @Override
    public String description() {
        return "weak episode word (E12, EP12, …)";
    }

    @Override
    public void extract(ParseContext ctx) {
        if (TYPE_MOVIE.equals(ctx.options.type())) return;

        extractSeasonMatches(ctx);
        extractEpisodeMatches(ctx);
        extractDetachedEpisodeCount(ctx);
    }

    private void extractSeasonMatches(ParseContext ctx) {
        var input = ctx.input;
        var seasonHeadValidator = createSeasonHeadValidator(input);
        var seasonMatcher = SEASON_RE.matcher(input);

        while (seasonMatcher.find()) {
            processSeasonMatch(ctx, seasonMatcher, seasonHeadValidator);
        }
    }

    private Predicate<Match> createSeasonHeadValidator(String input) {
        var sepsBefore = Validators.sepsBefore(input);
        var sepsAfter = Validators.sepsAfter(input);
        return m -> {
            if (!sepsBefore.test(m)) return false;
            if (sepsAfter.test(m)) return true;
            int e = m.end();
            if (e >= input.length()) return true;
            char c = input.charAt(e);
            return c == '&' || c == '+' || c == '~';
        };
    }

    private void processSeasonMatch(ParseContext ctx, Matcher seasonMatcher,
                                    Predicate<Match> seasonHeadValidator) {
        var raw = seasonMatcher.group();
        var headMatch = new Match(MatchName.SEASON, null, seasonMatcher.start(), seasonMatcher.end(), raw, Priority.DEFAULT, Set.of(), true);
        if (!seasonHeadValidator.test(headMatch)) return;

        ctx.matches.add(headMatch);

        int n = addSeasonValue(ctx, seasonMatcher);
        if (n < 0) return;

        addSeasonCount(ctx, seasonMatcher);
        processSeasonTail(ctx, seasonMatcher, n);
    }

    private int addSeasonValue(ParseContext ctx, Matcher seasonMatcher) {
        int valStart = seasonMatcher.start(GRP_SEASON_VAL);
        int valEnd = seasonMatcher.end(GRP_SEASON_VAL);
        int n = parseSafe(seasonMatcher.group(GRP_SEASON_VAL));
        if (n < 0) return -1;

        ctx.matches.add(new Match(MatchName.SEASON, n, valStart, valEnd,
                ctx.input.substring(valStart, valEnd), Priority.DEFAULT, Set.of(TAG_SEASON_WORD), false));
        return n;
    }

    private void addSeasonCount(ParseContext ctx, Matcher seasonMatcher) {
        if (seasonMatcher.group(GRP_COUNT) == null) return;

        int countStart = seasonMatcher.start(GRP_COUNT);
        int countEnd = seasonMatcher.end(GRP_COUNT);
        int c = parseSafe(seasonMatcher.group(GRP_COUNT));
        if (c >= 0) {
            ctx.matches.add(new Match(MatchName.SEASON_COUNT, c, countStart, countEnd,
                    seasonMatcher.group(GRP_COUNT), Priority.DEFAULT, Set.of(), false));
        }
    }

    private void processSeasonTail(ParseContext ctx, Matcher seasonMatcher, int firstSeasonValue) {
        var input = ctx.input;
        var blockSpans = getBlockedSpans(ctx);
        var tail = SEASON_TAIL_RE.matcher(input);

        int prevVal = firstSeasonValue;

        tail.region(seasonMatcher.end(), input.length());

        while (tail.lookingAt()) {
            var tailResult = processSeasonTailMatch(ctx, tail, prevVal, blockSpans);

            if (!tailResult.isValid()) {
                return;
            }

            prevVal = tailResult.value();
            tail.region(tail.end(), input.length());
        }
    }

    private List<int[]> getBlockedSpans(ParseContext ctx) {
        return ctx.matches.all()
                .filter(m -> isBlockedMatchType(m.name()))
                .map(m -> new int[]{m.start(), m.end()})
                .toList();
    }

    private boolean isBlockedMatchType(MatchName name) {
        return BLOCKED_MATCH_TYPES.contains(name);
    }

    private record TailResult(boolean isValid, int value) {
    }

    private TailResult processSeasonTailMatch(ParseContext ctx, Matcher tail,
                                              int prevVal, List<int[]> blockSpans) {

        String sepToken = tail.group(GRP_OP).strip().toLowerCase(java.util.Locale.ROOT);

        String op = trimOpJunk(sepToken);

        boolean strong = STRONG_OPS.contains(op);
        boolean range = RANGE_OPS.contains(op);

        int v = parseSafe(tail.group(GRP_VAL));

        if (v < 0 || v <= prevVal) return new TailResult(false, prevVal);

        int tStart = tail.start(GRP_VAL);
        int tEnd = tail.end(GRP_VAL);

        if (blockSpans.stream().anyMatch(sp -> sp[0] < tEnd && tStart < sp[1])) return new TailResult(false, prevVal);
        if (!strong && !range && v - prevVal > MAX_RANGE_GAP + 1) return new TailResult(false, prevVal);
        if (isFollowedByOfClause(ctx.input, tEnd)) return new TailResult(false, prevVal);

        if (range) {
            addRangeSeasons(ctx, prevVal, v, tStart);
        }

        ctx.matches.add(new Match(MatchName.SEASON, v, tStart, tEnd,
                ctx.input.substring(tStart, tEnd), Priority.DEFAULT, Set.of(TAG_SEASON_WORD), false));

        return new TailResult(true, v);
    }

    private boolean isFollowedByOfClause(String input, int position) {
        return AFTER_OF_RE.matcher(input).region(position, input.length()).lookingAt();
    }

    private void addRangeSeasons(ParseContext ctx, int prevVal, int v, int tStart) {
        for (int x = prevVal + 1; x < v; x++) {
            ctx.matches.add(new Match(MatchName.SEASON, x, tStart, tStart, "",
                    Priority.DEFAULT, Set.of(TAG_SEASON_WORD), false));
        }
    }

    private void extractEpisodeMatches(ParseContext ctx) {
        var input = ctx.input;
        var seps = Validators.sepsSurround(input);

        boolean episodeType = MatchName.EPISODE.name().equalsIgnoreCase(ctx.options.type());

        var epMatcher = (episodeType ? EP_RE_EPISODE_TYPE : EP_RE_DEFAULT).matcher(input);

        while (epMatcher.find()) {
            processEpisodeMatch(ctx, epMatcher, seps, episodeType);
        }
    }

    private void processEpisodeMatch(ParseContext ctx, Matcher epMatcher,
                                     Predicate<Match> seps, boolean episodeType) {
        var raw = epMatcher.group();
        var headMatch = new Match(MatchName.EPISODE, null, epMatcher.start(), epMatcher.end(), raw, Priority.DEFAULT, Set.of(), true);

        if (!seps.test(headMatch)) {
            handleInvalidEpisodeHead(ctx, epMatcher);
            return;
        }

        if (!validateShortEpisodeMarker(epMatcher)) return;

        Integer ep = parseEpisodeNumber(epMatcher, seps, episodeType);
        if (ep == null) return;

        addEpisodeMatches(ctx, epMatcher, headMatch, ep);
    }

    private void handleInvalidEpisodeHead(ParseContext ctx, Matcher epMatcher) {
        int markerStart = epMatcher.start(GRP_EP_WORD);
        int markerEnd = epMatcher.end(GRP_EP_WORD);
        String mw = epMatcher.group(GRP_EP_WORD);

        if (mw != null && !SHORT_EPISODE_WORDS.contains(mw.toLowerCase())
                && markerEnd < ctx.input.length()
                && Seps.isSep(ctx.input.charAt(markerEnd))) {
            ctx.matches.add(new Match(MatchName.EPISODE_WORD_MARKER, null,
                    markerStart, markerEnd, mw, Priority.DEFAULT, Set.of(), true));
        }
    }

    private boolean validateShortEpisodeMarker(Matcher epMatcher) {
        String marker = epMatcher.group(GRP_EP_WORD);
        if (marker != null && SHORT_EPISODE_WORDS.contains(marker.toLowerCase())) {
            int afterMarker = epMatcher.start(GRP_EP_WORD) + marker.length();
            return afterMarker == epMatcher.start(GRP_EP_VAL);
        }
        return true;
    }

    private Integer parseEpisodeNumber(Matcher epMatcher,
                                       Predicate<Match> seps, boolean episodeType) {
        int epStart = epMatcher.start(GRP_EP_VAL);
        int epEnd = epMatcher.end(GRP_EP_VAL);
        String epToken = epMatcher.group(GRP_EP_VAL);

        if (episodeType) {
            int ep = parseSafe(epToken);
            if (ep < 0) return null;

            if (!isPureDigits(epToken)) {
                var token = new Match(MatchName.EPISODE, ep, epStart, epEnd, epToken, Priority.DEFAULT, Set.of(), false);
                if (!seps.test(token)) return null;
            }
            return ep;
        } else {
            return Integer.parseInt(epToken);
        }
    }

    private void addEpisodeMatches(ParseContext ctx, Matcher epMatcher, Match headMatch, int ep) {
        ctx.matches.add(headMatch);

        int epStart = epMatcher.start(GRP_EP_VAL);
        int epEnd = epMatcher.end(GRP_EP_VAL);
        ctx.matches.add(new Match(MatchName.EPISODE, ep, epStart, epEnd,
                ctx.input.substring(epStart, epEnd), Priority.DEFAULT, Set.of(TAG_EPISODE_WORD), false));

        addEpisodeVersion(ctx, epMatcher);
        addEpisodeCountFromMatch(ctx, epMatcher);
    }

    private void addEpisodeVersion(ParseContext ctx, Matcher epMatcher) {
        if (epMatcher.group(GRP_VERSION) != null) {
            int v = Integer.parseInt(epMatcher.group(GRP_VERSION));
            ctx.matches.add(new Match(MatchName.VERSION, v, epMatcher.start(GRP_VERSION), epMatcher.end(GRP_VERSION),
                    epMatcher.group(GRP_VERSION), Priority.DEFAULT, Set.of(), false));
        }
    }

    private void addEpisodeCountFromMatch(ParseContext ctx, Matcher epMatcher) {
        if (epMatcher.group(GRP_COUNT) != null) {
            int c = Integer.parseInt(epMatcher.group(GRP_COUNT));
            ctx.matches.add(new Match(MatchName.EPISODE_COUNT, c, epMatcher.start(GRP_COUNT), epMatcher.end(GRP_COUNT),
                    epMatcher.group(GRP_COUNT), Priority.DEFAULT, Set.of(), false));
        }
    }

    private void extractDetachedEpisodeCount(ParseContext ctx) {
        var input = ctx.input;
        var seps = Validators.sepsSurround(input);
        var dm = DETACHED_EP_COUNT_RE.matcher(input);

        while (dm.find()) {
            processDetachedMatch(ctx, dm, seps);
        }
    }

    private void processDetachedMatch(ParseContext ctx, Matcher dm,
                                      Predicate<Match> seps) {
        var raw = dm.group();
        var headMatch = new Match(MatchName.EPISODE, null, dm.start(), dm.end(), raw, Priority.DEFAULT, Set.of(), false);
        if (!seps.test(headMatch)) return;

        int dStart = dm.start(GRP_EP_VAL);
        int dEnd = dm.end(GRP_EP_VAL);
        boolean overlapsSeason = ctx.matches.range(dStart, dEnd,
                m -> m.name() == MatchName.SEASON && m.value() != null).findAny().isPresent();
        if (overlapsSeason) return;

        int e = Integer.parseInt(dm.group(GRP_EP_VAL));
        int c = Integer.parseInt(dm.group(GRP_COUNT));

        ctx.matches.add(new Match(MatchName.EPISODE, e, dm.start(GRP_EP_VAL), dm.end(GRP_EP_VAL),
                dm.group(GRP_EP_VAL), Priority.DEFAULT, Set.of(TAG_EPISODE_WORD), false));
        ctx.matches.add(new Match(MatchName.EPISODE_COUNT, c, dm.start(GRP_COUNT), dm.end(GRP_COUNT),
                dm.group(GRP_COUNT), Priority.DEFAULT, Set.of(), false));
        ctx.matches.add(new Match(MatchName.EP_COUNT_SPAN, null, dm.start(), dm.end(),
                raw, Priority.DEFAULT, Set.of(), true));
    }

    @Override
    public void postProcess(ParseContext ctx) {
        var heads = ctx.matches.all()
                .filter(m -> (m.name() == MatchName.SEASON || m.name() == MatchName.EPISODE)
                        && m.value() == null && m.isPrivate())
                .toList();
        for (var head : heads) {
            boolean hasValue = ctx.matches.range(head.start(), head.end(),
                    m -> m.name() == head.name() && m.value() != null).findAny().isPresent();
            if (!hasValue) ctx.matches.remove(head);
        }
    }

    private static int parseSafe(String token) {
        try {
            return Numerals.parse(token);
        } catch (IllegalArgumentException _) {
            return -1;
        }
    }

    private static boolean isPureDigits(String s) {
        if (s == null || s.isEmpty()) return false;
        return s.chars().allMatch(Character::isDigit);
    }

}