package io.guessit.core.pipeline.state;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Strongly-typed tags used internally by extractors and post-processors.
 * These map directly to the tag values defined in the configuration YAML files.
 */
public enum MatchTag {
    SXX_EXX("SxxExx"),
    COEXIST("coexist"),
    SEE_PATTERN("see-pattern"),
    RANGE_FILL("range-fill"),
    DISC_MARKER("disc-marker"),
    AUDIO_PROFILE_RULE("audio_profile.rule"),
    WEAK_DUPLICATE("weak-duplicate"),
    WEAK_EPISODE("weak-episode"),

    HAS_NEIGHBOR("has-neighbor"),
    HAS_NEIGHBOR_BEFORE("has-neighbor-before"),
    HAS_NEIGHBOR_AFTER("has-neighbor-after"),

    SOURCE_PREFIX("source-prefix"),
    SOURCE_SUFFIX("source-suffix"),
    VIDEO_CODEC_PREFIX("video-codec-prefix"),
    VIDEO_CODEC_SUFFIX("video-codec-suffix"),
    DERIVED_VIDEO_CODEC("derivedFrom:video_codec"),
    VIDEO_PROFILE_RULE("video_profile.rule"),
    STREAMING_SERVICE_PREFIX("streaming_service.prefix"),
    STREAMING_SERVICE_SUFFIX("streaming_service.suffix"),
    RELEASE_GROUP_PREFIX("release-group-prefix"),
    NO_RELEASE_GROUP_PREFIX("no-release-group-prefix"),
    WEAK_AUDIO_CHANNELS("weak-audio_channels"),

    BODY("body"),
    INFO("info"),
    VIDEO("video"),
    TORRENT("torrent"),
    NZB("nzb"),

    EXPECTED("expected"),
    SEASON_WORD("season-word"),
    SEASON_DERIVED("season-derived"),
    EPISODE_WORD("episode-word"),

    ATTACHED_AFFIX("attached-affix"),
    SUBTITLE("subtitle"),
    EXTENSION("extension"),

    OTHER_VALIDATE_SCREENER("other.validate.screener"),
    OTHER_VALIDATE_MUX("other.validate.mux"),
    AT_END("at-end"),

    SCENE("scene"),
    NOT_A_RELEASE_GROUP("not-a-release-group"),
    ANIME("anime"),

    DERIVED_FROM_VIDEO_CODEC("derivedFrom:video_codec"),
    DERIVED_FROM_SOURCE("derivedFrom:source"),
    WEAK_SOURCE("weak.source"),
    UHD_BLURAY_NEIGHBOR("uhdbluray-neighbor"),

    TITLE("title"),
    FILE_PART_TITLE("file-part-title"),
    ALTERNATIVE_REPLACED("alternative-replaced"),
    WEBSITE_PREFIX("website.prefix"),
    EQUIVALENT_IGNORE("equivalent-ignore"),
    WEAK_SCREEN_SIZE("weak.screen_size"),
    NORMALIZED("normalized"),
    DERIVED_SCREEN_SIZE("derivedFrom:screen_size"),

    // --- Coexist Groups (Dynamic substitution) ---
    CG_1("cg:1"),
    CG_2("cg:2"),
    CG_3("cg:3"),
    CG_4("cg:4"),
    CG_5("cg:5"),
    CG_6("cg:6"),
    CG_7("cg:7"),
    CG_8("cg:8"),
    CG_9("cg:9"),
    CG_10("cg:10"),

    // YAML
    REAL("real"),
    DOLBY("Dolby Digital"),
    AAC("AAC"),
    DTS_HD("DTS-HD"),
    DTS("DTS");

    private final String yamlValue;
    private static final Map<String, MatchTag> LOOKUP = new HashMap<>();

    static {
        for (MatchTag tag : MatchTag.values()) {
            LOOKUP.put(tag.yamlValue.toLowerCase(Locale.ROOT), tag);
        }
    }

    MatchTag(String yamlValue) {
        this.yamlValue = yamlValue;
    }

    /**
     * @return The raw string value as defined in the YAML configuration or old extractors.
     */
    public String getValue() {
        return yamlValue;
    }

    /**
     * Converts a string from a YAML file (or legacy code) into the strongly-typed Enum.
     *
     * @param text The string value of the tag.
     * @return The corresponding MatchTag, or null if not found.
     */
    public static MatchTag fromString(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return LOOKUP.get(text.toLowerCase(Locale.ROOT));
    }
}