package io.guessit.rules.extractors;

import io.guessit.core.pipeline.contracts.Extractor;
import io.guessit.core.pipeline.state.*;
import io.guessit.core.text.PatternMatcher;
import io.guessit.core.text.RegexOpts;
import io.guessit.core.text.Validators;
import io.guessit.core.text.patterns.YearPatterns;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Extracts release {@code year} as a 4-digit integer in [1920, 2030).
 *
 * <p>The extractor is intentionally permissive — any 4-digit run that is
 * separator-surrounded and falls in the year range becomes a candidate.
 * Resolution between multiple candidates in the same filepart happens in
 * {@link #postProcess}, not at extract time, because year resolution
 * depends on which other markers (groups) survived.
 */
public final class YearExtractor implements Extractor {

    private static final Pattern PATTERN = YearPatterns.buildYearPattern();

    @Override
    public String name() {
        return "year";
    }

    @Override
    public String description() {
        return "4-digit year (1920–2029)";
    }

    @Override
    public void extract(ParseContext ctx) {
        var input = ctx.input;
        var opts = RegexOpts.defaults()
                .withValue(Integer::valueOf)
                .withValidator(m -> {
                    if (!Validators.sepsSurround(input).test(m)) return false;

                    if (m instanceof Match.IntegerMatch im) {
                        int v = im.value();
                        return 1920 <= v && v < 2030;
                    }
                    return false;
                });

        PatternMatcher.regex(input, PATTERN, MatchName.YEAR, opts, ctx.trace)
                .forEach(ctx.matches::add);
    }

    @Override
    public void postProcess(ParseContext ctx) {
        var years = ctx.matches.named(MatchName.YEAR).toList();
        if (years.size() <= 1) return;

        var fileParts = Markers.named(ctx.markers, MarkerType.PATH).toList();

        var toRemove = fileParts.stream()
                .flatMap(fp -> {
                    var inPart = years.stream().filter(y -> fp.covers(y.span())).toList();
                    return inPart.size() <= 1 ? Stream.empty() : determineRemovals(ctx, inPart).stream();
                })
                .toList();

        WeakExtractorCommon.removeMatches(ctx, toRemove.stream());
        dropWeakDuplicatesInsideRemoved(ctx, toRemove);
    }

    private static List<Match> determineRemovals(ParseContext ctx, List<Match> inPart) {
        var partitions = inPart.stream().collect(Collectors.partitioningBy(y ->
                ctx.markers.stream().anyMatch(mk ->
                        mk.type() == MarkerType.GROUP && mk.covers(y.span()))
        ));

        var grouped = partitions.get(true);
        var ungrouped = partitions.get(false);

        if (!grouped.isEmpty() && !ungrouped.isEmpty()) {
            return Stream.concat(
                    ungrouped.stream(),
                    grouped.stream().skip(1)
            ).toList();
        } else if (grouped.isEmpty() && !ungrouped.isEmpty()) {
            return Stream.concat(
                    Stream.of(ungrouped.getFirst()),
                    ungrouped.stream().skip(2)
            ).toList();
        }

        return List.of();
    }

    private static void dropWeakDuplicatesInsideRemoved(ParseContext ctx, List<Match> toRemove) {
        var weakDuplicates = ctx.matches.all()
                .filter(m -> m.hasTag(MatchTag.WEAK_DUPLICATE))
                .filter(m -> m.name() == MatchName.SEASON || m.name() == MatchName.EPISODE)
                .filter(m -> toRemove.stream().anyMatch(dropped -> dropped.span().overlaps(m.span())));

        WeakExtractorCommon.removeMatches(ctx, weakDuplicates);
    }
}