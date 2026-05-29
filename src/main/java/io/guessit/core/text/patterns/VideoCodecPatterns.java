package io.guessit.core.text.patterns;

import com.mirkoddd.sift.core.dsl.Connector;
import com.mirkoddd.sift.core.dsl.Fragment;
import com.mirkoddd.sift.core.dsl.SiftPattern;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static com.mirkoddd.sift.core.Sift.optional;
import static com.mirkoddd.sift.core.SiftPatterns.anyOf;
import static com.mirkoddd.sift.core.SiftPatterns.capture;
import static com.mirkoddd.sift.core.SiftPatterns.literal;
import static io.guessit.core.text.patterns.CommonPatterns.*;

/**
 * Dictionary holding all the data-driven rules and pre-compiled Sift patterns
 * for the VideoCodecExtractor.
 */
public final class VideoCodecPatterns {

    private VideoCodecPatterns() {}

    private static Connector<Fragment> optionalLiteral(String text) {
        return optionallyFind(literal(text));
    }

    private static Connector<Fragment> optionallyFind(SiftPattern<Fragment> fragment) {
        return optional().of(fragment);
    }

    private static RegexRule rule(Pattern p, String val) { return new RegexRule(p, val, false); }
    private static RegexRule tagged(Pattern p, String val) { return new RegexRule(p, val, true); }
    private static StrRule aliases(String val, String... keys) { return new StrRule(Set.of(keys), val); }

    private static final SiftPattern<Fragment> OPT_DASH = optionallyFind(anyOfCharacters(' ', '.', '_', '-'));
    private static final SiftPattern<Fragment> HX = anyOfCharacters('h', 'x');

    private static final SiftPattern<Fragment> MPEG = find(literal("Mp"))
            .followedBy(optionalLiteral("e"))
            .followedBy(literal("g"));

    private static final SiftPattern<Fragment> BITS = find(OPT_DASH)
            .followedBy(literal("bit"))
            .followedBy(optionalLiteral("s"));

    private static final SiftPattern<Fragment> OPT_MPEG4 = optionallyFind(
            find(literal("MPEG")).followedBy(OPT_DASH).followedBy(literal("4"))
    );

    private static final Pattern P_RV = compileCaseInsensitive(find(literal("Rv")).then().exactly(2).digits());
    private static final Pattern P_DIVX = compileCaseInsensitive(anyOfStrings("DVDivX", "DivX"));
    private static final Pattern P_XVID = compileCaseInsensitive(literal("XviD"));
    private static final Pattern P_VP7 = compileCaseInsensitive(literal("VP7"));
    private static final Pattern P_VP9 = compileCaseInsensitive(literal("VP9"));
    private static final Pattern P_VC1 = compileCaseInsensitive(find(literal("VC")).followedBy(OPT_DASH, literal("1")));
    private static final Pattern P_VP8 = compileCaseInsensitive(find(literal("VP8")).followedBy(optionalLiteral("0")));
    private static final Pattern P_H263 = compileCaseInsensitive(find(HX).followedBy(OPT_DASH, literal("263")));

    private static final Pattern P_H265 = compileCaseInsensitive(anyOf(
            find(HX).followedBy(OPT_DASH).followedBy(literal("265")),
            find(literal("HEVC"))
    ));

    private static final Pattern P_MPEG2 = compileCaseInsensitive(anyOf(
            find(MPEG).followedBy(OPT_DASH).followedBy(literal("2")),
            find(HX).followedBy(OPT_DASH).followedBy(literal("262"))
    ));

    private static final Pattern P_H264 = compileCaseInsensitive(anyOf(
            find(HX).followedBy(OPT_DASH).followedBy(literal("264")),
            find(OPT_MPEG4).followedBy(literal("AVC")).followedBy(optionalLiteral("HD"))
    ));

    private static final Pattern P_HI422P = compileCaseInsensitive(find(literal("Hi422P")));
    private static final Pattern P_HI444PP = compileCaseInsensitive(find(literal("Hi444PP")));
    private static final Pattern P_HI10P = compileCaseInsensitive(find(literal("Hi10")).followedBy(optionalLiteral("P")));
    private static final Pattern P_AVCHD = compileCaseInsensitive(find(literal("AVC")).followedBy(optionalLiteral("HD")));

    private static final Pattern P_12BIT = compileCaseInsensitive(find(literal("12")).followedBy(BITS));
    private static final Pattern P_8BIT = compileCaseInsensitive(find(literal("8")).followedBy(BITS));
    private static final Pattern P_10BIT = compileCaseInsensitive(anyOf(
            find(literal("10")).followedBy(BITS),
            find(literal("YUV420P10")),
            find(literal("Hi10")).followedBy(optionalLiteral("P"))
    ));

    public static final String GRP_C = "c";
    public static final String GRP_D = "d";

    public static final Pattern P_HEVC10 = compileCaseInsensitive(
            namedCapture(capture(GRP_C, literal("hevc")))
                    .then().namedCapture(capture(GRP_D, literal("10")))
    );

    public record RegexRule(Pattern pattern, String value, boolean isTagged) {}

    public record StrRule(Set<String> aliases, String value) {}

    public static final List<RegexRule> CODEC_RULES = List.of(
            rule(P_RV, "RealVideo"),
            rule(P_MPEG2, "MPEG-2"),
            rule(P_DIVX, "DivX"),
            rule(P_XVID, "Xvid"),
            rule(P_VC1, "VC-1"),
            rule(P_VP7, "VP7"),
            rule(P_VP8, "VP8"),
            rule(P_VP9, "VP9"),
            rule(P_H263, "H.263"),
            rule(P_H264, "H.264"),
            rule(P_H265, "H.265")
    );

    public static final List<StrRule> PROFILE_STR_RULES = List.of(
            aliases("Baseline", "BP"),
            aliases("Extended", "XP", "EP"),
            aliases("Main", "MP"),
            aliases("High", "HP", "HiP"),
            aliases("Scalable Video Coding", "SC", "SVC"),
            aliases("High Efficiency Video Coding", "HEVC")
    );

    public static final List<RegexRule> PROFILE_REGEX_RULES = List.of(
            tagged(P_AVCHD, "Advanced Video Codec High Definition"),
            rule(P_HI422P, "High 4:2:2"),
            rule(P_HI444PP, "High 4:4:4 Predictive"),
            rule(P_HI10P, "High 10")
    );

    public static final List<RegexRule> COLOR_DEPTH_RULES = List.of(
            rule(P_12BIT, "12-bit"),
            rule(P_10BIT, "10-bit"),
            rule(P_8BIT, "8-bit")
    );
}