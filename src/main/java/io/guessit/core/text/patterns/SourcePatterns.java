package io.guessit.core.text.patterns;

import io.guessit.core.text.Abbreviations;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

// the regex logic is too fragile to use sift, I'll check later after refactoring
public final class SourcePatterns {

    public static final String BLU_RAY = "Blu-ray";
    private static final Set<String> COMMON_TAGS = Set.of("video-codec-prefix", "streaming_service.suffix");

    public static final String GRP_OTHER = "other";
    public static final String GRP_ANOTHER = "another";

    private static final ConcurrentMap<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<RuleCacheKey, List<SourceRule>> RULE_LIST_CACHE = new ConcurrentHashMap<>();

    private static final String DEFAULT_RIP_PREFIX = "(?<" + GRP_OTHER + ">Rip)-?";
    private static final String DEFAULT_RIP_SUFFIX = "-?(?<" + GRP_OTHER + ">Rip)";

    public record SourceRule(String source, Pattern compiledPattern, String otherValue,
                             String anotherValue, Set<String> tags, boolean weak) {}

    private record RuleCacheKey(String prefix, String suffix) {}

    private SourcePatterns() {}

    public static List<SourceRule> buildRules(String customPrefix, String customSuffix) {
        String ripPrefix = customPrefix != null ? customPrefix : DEFAULT_RIP_PREFIX;
        String ripSuffix = customSuffix != null ? customSuffix : DEFAULT_RIP_SUFFIX;

        return RULE_LIST_CACHE.computeIfAbsent(
                new RuleCacheKey(ripPrefix, ripSuffix),
                k -> buildRulesInternal(k.prefix(), k.suffix())
        );
    }

    private static List<SourceRule> buildRulesInternal(String ripPrefix, String ripSuffix) {
        String optRipSuffix = "(?:" + ripSuffix + ")?";

        return Stream.of(
                        rule("VHS").regex("(VHS)" + optRipSuffix).other("Rip"),
                        rule("Camera").regex("(CAM)" + optRipSuffix).other("Rip"),
                        rule("HD Camera").regex("(HD-?CAM)" + optRipSuffix).other("Rip"),
                        rule("Telesync").regex("(TELESYNC|TS)" + optRipSuffix).other("Rip"),
                        rule("HD Telesync").regex("(HD-?TELESYNC|HD-?TS)" + optRipSuffix).other("Rip"),

                        rule("Workprint").regex("WORKPRINT|WP"),

                        rule("Telecine").regex("(TELECINE|TC)" + optRipSuffix).other("Rip"),
                        rule("HD Telecine").regex("(HD-?TELECINE|HD-?TC)" + optRipSuffix).other("Rip"),

                        rule("Pay-per-view").regex("(PPV)" + optRipSuffix).other("Rip"),
                        rule("TV").regex("(SD-?TV)" + optRipSuffix).other("Rip"),
                        rule("TV").regex("(TV)" + ripSuffix).other("Rip"),

                        rule("TV").regex(ripPrefix + "(TV|SD-?TV)").other("Rip"),
                        rule("TV").regex("TV(?=-?Dub\\b)"),

                        rule("Digital TV").regex("(DVB|PD-?TV)" + optRipSuffix).other("Rip"),
                        rule("DVD").regex("(DVD)" + optRipSuffix).other("Rip"),
                        rule("Digital Master").regex("(DM)" + optRipSuffix).other("Rip"),

                        rule("DVD").regex("VIDEO-?TS|DVD-?R(?:$|(?!E))|DVD-?9|DVD-?5"),

                        rule("HDTV").regex("(HD-?TV)" + optRipSuffix).other("Rip"),
                        rule("HDTV").regex("(TV-?HD)" + ripSuffix).other("Rip"),
                        rule("HDTV").regex("TV-?(?<" + GRP_OTHER + ">Rip-?HD)").other("Rip"),

                        rule("Video on Demand").regex("(VOD)" + optRipSuffix).other("Rip"),
                        rule("Web").regex("(WEB|WEB-?DL)" + ripSuffix).other("Rip"),
                        rule("Web").regex("WEB-?(?<" + GRP_ANOTHER + ">Cap)" + optRipSuffix).other("Rip").another("Rip"),
                        rule("Web").regex("WEB-?DL|WEB-?U?HD|DL-?WEB|DL(?=-?Mux)"),
                        rule("Web").regex("WEB").tags(Set.of("weak.source")).weak(),

                        rule("HD-DVD").regex("(HD-?DVD)" + optRipSuffix).other("Rip"),
                        rule(BLU_RAY).regex("(Blu-?ray|BD25|BD50|BD[59]|BD)" + optRipSuffix).other("Rip"),

                        rule(BLU_RAY).regex("(?<" + GRP_ANOTHER + ">BR)-?(?=Scr(?:eener)?|Mux)").another("Reencoded"),
                        rule(BLU_RAY).regex("(?<" + GRP_ANOTHER + ">BR)" + ripSuffix).other("Rip").another("Reencoded"),

                        rule("Ultra HD Blu-ray").regex("Ultra-?Blu-?ray|Blu-?ray-?Ultra"),
                        rule("Analog HDTV").regex("AHDTV"),

                        rule("Ultra HDTV").regex("(UHD-?TV)" + optRipSuffix).other("Rip"),
                        rule("Ultra HDTV").regex("(UHD)" + ripSuffix).other("Rip"),

                        rule("Satellite").regex("(DSR|DTH)" + optRipSuffix).other("Rip"),
                        rule("Satellite").regex("(DSR?|SAT)" + ripSuffix).other("Rip")
                )
                .map(RuleBuilder::build)
                .filter(Objects::nonNull)
                .toList();
    }

    private static RuleBuilder rule(String source) {
        return new RuleBuilder(source);
    }

    private static class RuleBuilder {
        private final String source;
        private String regex;
        private String otherValue = null;
        private String anotherValue = null;
        private boolean weak = false;
        private Set<String> tags = COMMON_TAGS;

        RuleBuilder(String source) { this.source = source; }

        RuleBuilder regex(String r) { this.regex = r; return this; }
        RuleBuilder other(String o) { this.otherValue = o; return this; }
        RuleBuilder another(String a) { this.anotherValue = a; return this; }
        RuleBuilder weak() { this.weak = true; return this; }
        RuleBuilder tags(Set<String> t) { this.tags = t; return this; }

        SourceRule build() {
            var pattern = PATTERN_CACHE.computeIfAbsent(regex, s -> {
                try { return Pattern.compile(Abbreviations.dash(s), Pattern.CASE_INSENSITIVE); }
                catch (PatternSyntaxException _) { return null; }
            });
            if (pattern == null) return null;
            return new SourceRule(source, pattern, otherValue, anotherValue, tags, weak);
        }
    }
}