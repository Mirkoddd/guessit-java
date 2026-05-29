package io.guessit.rules.extractors;

import io.guessit.core.pipeline.contracts.Extractor;
import io.guessit.core.pipeline.state.Match;
import io.guessit.core.pipeline.state.MatchName;
import io.guessit.core.pipeline.state.ParseContext;
import io.guessit.core.text.Validators;
import io.guessit.api.models.BitRate;
import io.guessit.core.text.patterns.BitRatePatterns;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts bit rate values from filenames.
 *
 * <p>All matches are initially tagged as {@code audio_bit_rate}. A subsequent
 * {@link io.guessit.rules.post.BitRateTypeRule} promotes matches
 * to {@code video_bit_rate} when they are preceded by a video-context match.
 */
public final class BitRateExtractor implements Extractor {

    private static final String GRP_RAW = "raw";
    private static final String TAG_WEAK_AUDIO_CHANNELS = "weak-audio_channels";
    private static final String TAG_RELEASE_GROUP_PREFIX = "release-group-prefix";

    private static final List<Pattern> PATTERNS = BitRatePatterns.buildPatterns(GRP_RAW);

    @Override
    public String name() {
        return "audio_bit_rate";
    }

    @Override
    public String description() {
        return "video / audio bit rate (kbps, Mbps)";
    }

    @Override
    public void extract(ParseContext ctx) {
        var input = ctx.input;
        var seps = Validators.sepsSurround(input);

        var channels = ctx.matches.named(MatchName.AUDIO_CHANNELS)
                .filter(m -> !m.tags().contains(TAG_WEAK_AUDIO_CHANNELS))
                .toList();

        for (var pattern : PATTERNS) {
            var matcher = pattern.matcher(input);
            while (matcher.find()) {
                tryAddBitRate(ctx, matcher, seps, channels);
            }
        }
    }

    private void tryAddBitRate(ParseContext ctx, Matcher matcher, Predicate<Match> seps, List<Match> channels) {
        int start = matcher.start(GRP_RAW);
        int end = matcher.end(GRP_RAW);
        String raw = matcher.group(GRP_RAW);

        var head = new Match(MatchName.AUDIO_BIT_RATE, null, start, end, raw, priority(), Set.of(), false);
        if (!seps.test(head)) return;

        if (overlapsAny(start, end, channels)) return;

        ctx.matches.add(new Match(MatchName.AUDIO_BIT_RATE, BitRate.fromString(raw), start, end, raw,
                priority(), Set.of(TAG_RELEASE_GROUP_PREFIX), false));
    }

    private static boolean overlapsAny(int start, int end, List<Match> spans) {
        return spans.stream().anyMatch(sp -> start < sp.end() && sp.start() < end);
    }
}