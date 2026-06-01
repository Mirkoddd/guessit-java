package io.guessit.rules.extractors;

import io.guessit.core.pipeline.contracts.Extractor;
import io.guessit.core.pipeline.state.*;
import io.guessit.core.text.Seps;
import io.guessit.core.text.Span;
import io.guessit.core.text.Validators;
import io.guessit.core.text.patterns.SourcePatterns;
import io.guessit.core.text.patterns.SourcePatterns.SourceRule;

import java.util.Comparator;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;

public final class SourceExtractor implements Extractor {

    public static final String SOURCE = "source";

    private static final String GRP_OTHER = "other";
    private static final String GRP_ANOTHER = "another";

    private static final String CONF_RIP_PREFIX = "rip_prefix";
    private static final String CONF_RIP_SUFFIX = "rip_suffix";

    private static final String VAL_ULTRA_HD_BLURAY = "Ultra HD Blu-ray";
    private static final String VAL_ULTRA_HD = "Ultra HD";
    private static final String VAL_2160P = "2160p";
    private static final String VAL_BLU_RAY = "Blu-ray";

    @Override
    public String name() {
        return SOURCE;
    }

    @Override
    public String description() {
        return "source / medium (BluRay, WEB-DL, HDTV, DVD, …)";
    }

    @Override
    public void extract(ParseContext ctx) {
        var section = ctx.config.section(SOURCE);

        Object rawPrefix = section.getOrDefault(CONF_RIP_PREFIX, null);
        Object rawSuffix = section.getOrDefault(CONF_RIP_SUFFIX, null);

        String customPrefix = rawPrefix != null ? String.valueOf(rawPrefix) : null;
        String customSuffix = rawSuffix != null ? String.valueOf(rawSuffix) : null;

        var rules = SourcePatterns.buildRules(customPrefix, customSuffix);

        rules.forEach(rule -> apply(ctx, rule));
    }

    private static void apply(ParseContext ctx, SourceRule rule) {
        var input = ctx.input;
        var validator = Validators.sepsBefore(input).or(Validators.sepsAfter(input));
        var matcher = rule.compiledPattern().matcher(input);

        while (matcher.find()) {
            applyOneMatch(ctx, input, rule, validator, matcher);
        }
    }

    private static void applyOneMatch(ParseContext ctx, String input, SourceRule rule,
                                      Predicate<Match> validator, Matcher matcher) {
        int s = matcher.start();
        int e = matcher.end();

        var span = new Span(s, e, input.substring(s, e));

        var sourceMatch = new Match(MatchName.SOURCE, rule.source(), span,
                Priority.DEFAULT, rule.tags(), false);

        if (!validator.test(sourceMatch) || overlapsExtension(ctx, span)) return;

        boolean insideStream = ctx.matches.named(MatchName.STREAMING_SERVICE)
                .anyMatch(ss -> ss.span().contains(span) && !ss.span().equals(span));

        if (insideStream) {
            sourceMatch = new Match(MatchName.SOURCE, rule.source(), span,
                    Priority.DEFAULT, rule.tags(), true);
        }

        ctx.matches.add(sourceMatch);
        addDerivedOther(ctx, input, matcher, GRP_OTHER, rule.otherValue());
        addDerivedOther(ctx, input, matcher, GRP_ANOTHER, rule.anotherValue());
    }

    private static void addDerivedOther(ParseContext ctx, String input, Matcher matcher,
                                        String groupName, String value) {
        if (value == null) return;
        int gs = groupStart(matcher, groupName);
        int ge = groupEnd(matcher, groupName);
        if (gs >= 0 && ge > gs) {
            ctx.matches.add(new Match(MatchName.OTHER, value, new Span(gs, ge, input.substring(gs, ge)),
                    Priority.DEFAULT, Set.of(MatchTag.COEXIST.getYamlValue(), MatchTag.DERIVED_FROM_SOURCE.getYamlValue()), false));
        }
    }

    private static boolean overlapsExtension(ParseContext ctx, Span span) {
        return ctx.matches.named(MatchName.CONTAINER)
                .anyMatch(m -> m.hasTag(MatchTag.EXTENSION) && m.span().overlaps(span));
    }

    private static int groupStart(Matcher m, String name) {
        try { return m.start(name); }
        catch (IllegalArgumentException | IllegalStateException _) { return -1; }
    }

    private static int groupEnd(Matcher m, String name) {
        try { return m.end(name); }
        catch (IllegalArgumentException | IllegalStateException _) { return -1; }
    }

    @Override
    public void postProcess(ParseContext ctx) {
        validatePrefixSuffix(ctx);
        validateWeakSource(ctx);
        upgradeUltraHdBluray(ctx);
    }

    private void validatePrefixSuffix(ParseContext ctx) {
        var sepsBefore = Validators.sepsBefore(ctx.input);
        var sepsAfter = Validators.sepsAfter(ctx.input);

        ctx.matches.named(MatchName.SOURCE)
                .filter(s -> (!sepsBefore.test(s) && noNeighborTag(ctx, s.span().start() - 1, MatchTag.SOURCE_PREFIX)) ||
                        (!sepsAfter.test(s) && noNeighborTag(ctx, s.span().end(), MatchTag.SOURCE_SUFFIX)))
                .toList()
                .forEach(ctx.matches::remove);
    }

    private void validateWeakSource(ParseContext ctx) {
        var pathMarkers = ctx.markers.stream()
                .filter(marker -> marker.type() == MarkerType.PATH)
                .toList();

        ctx.matches.named(MatchName.SOURCE)
                .filter(m -> m.hasTag(MatchTag.WEAK_SOURCE))
                .filter(weak -> pathMarkers.stream().anyMatch(fp -> shouldRemoveWeakSource(ctx, fp, weak)))
                .toList()
                .forEach(ctx.matches::remove);
    }

    private static boolean shouldRemoveWeakSource(ParseContext ctx, Marker filePart, Match weak) {
        if (!filePart.covers(weak.span())) return false;

        boolean later = ctx.matches.named(MatchName.SOURCE)
                .anyMatch(m -> m != weak && m.span().isAfter(weak.span()) && filePart.covers(m.span()));

        if (!later) return false;
        return !ctx.input.substring(filePart.span().start(), weak.span().start()).isBlank();
    }

    private void upgradeUltraHdBluray(ParseContext ctx) {
        var pathMarkers = ctx.markers.stream()
                .filter(m -> m.type() == MarkerType.PATH)
                .toList();

        ctx.matches.named(MatchName.SOURCE)
                .filter(m -> VAL_BLU_RAY.equals(m.value()))
                .toList()
                .forEach(bd -> pathMarkers.stream()
                        .filter(fp -> fp.covers(bd.span()))
                        .findFirst()
                        .ifPresent(fp -> tryUpgradeBluray(ctx, fp, bd)));
    }

    private void tryUpgradeBluray(ParseContext ctx, Marker filePart, Match bd) {
        var beforeSpan = new Span(filePart.span().start(), bd.span().start(), "");
        var uhdOther = findUltraHd(ctx, beforeSpan, true);

        boolean ok = uhdOther != null && validRange(ctx, new Span(uhdOther.span().end(), bd.span().start(), ""));

        if (!ok) {
            var afterSpan = new Span(bd.span().end(), filePart.span().end(), "");
            uhdOther = findUltraHd(ctx, afterSpan, false);
            ok = uhdOther != null && validRange(ctx, new Span(bd.span().end(), uhdOther.span().start(), ""));
        }

        if (!ok) {
            if (!has2160p(ctx, filePart)) return;
            uhdOther = null;
        }

        if (uhdOther != null) ctx.matches.remove(uhdOther);

        ctx.matches.replace(bd, new Match(MatchName.SOURCE, VAL_ULTRA_HD_BLURAY,
                bd.span(), bd.priority(), bd.tags(), bd.isPrivate()));
    }

    private static boolean has2160p(ParseContext ctx, Marker filePart) {
        return ctx.matches.named(MatchName.SCREEN_SIZE)
                .anyMatch(m -> VAL_2160P.equals(m.value()) && filePart.covers(m.span()));
    }

    private static Match findUltraHd(ParseContext ctx, Span targetSpan, boolean preferLast) {
        var candidates = ctx.matches.named(MatchName.OTHER)
                .filter(m -> !m.isPrivate() && VAL_ULTRA_HD.equals(m.value()) && m.span().isInside(targetSpan));

        return preferLast
                ? candidates.max(Comparator.comparingInt(m -> m.span().end())).orElse(null)
                : candidates.min(Comparator.comparingInt(m -> m.span().start())).orElse(null);
    }

    private static boolean validRange(ParseContext ctx, Span gap) {
        return gap.length() <= 0 || (allMatchesAreAllowed(ctx, gap) && !hasNonSeparatorHoles(ctx, gap));
    }

    private static boolean allMatchesAreAllowed(ParseContext ctx, Span gap) {
        return ctx.matches.all()
                .filter(m -> !m.isPrivate() && m.span().isInside(gap))
                .allMatch(SourceExtractor::isAllowedMatch);
    }

    private static boolean isAllowedMatch(Match m) {
        return m.name() == MatchName.SCREEN_SIZE
                || m.name() == MatchName.COLOR_DEPTH
                || (m.name() == MatchName.OTHER && m.hasTag(MatchTag.UHD_BLURAY_NEIGHBOR));
    }

    private static boolean hasNonSeparatorHoles(ParseContext ctx, Span gap) {
        if (gap.length() <= 0) return false;

        int s = gap.start();
        int e = gap.end();
        boolean[] covered = new boolean[gap.length()];

        ctx.matches.all()
                .filter(m -> !m.isPrivate() && m.span().overlaps(gap))
                .forEach(m -> {
                    int from = Math.max(m.span().start(), s) - s;
                    int to = Math.min(m.span().end(), e) - s;
                    for (int i = from; i < to; i++) covered[i] = true;
                });

        for (int i = 0; i < covered.length; i++) {
            if (!covered[i] && !Seps.isSep(ctx.input.charAt(s + i))) return true;
        }

        return false;
    }

    private static boolean noNeighborTag(ParseContext ctx, int pos, MatchTag tag) {
        return ctx.matches.all().noneMatch(m -> m.hasTag(tag) && m.span().start() <= pos && pos <= m.span().end());
    }
}