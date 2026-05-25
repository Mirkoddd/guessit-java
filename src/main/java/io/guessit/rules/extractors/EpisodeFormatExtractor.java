package io.guessit.rules.extractors;

import com.mirkoddd.sift.core.SiftGlobalFlag;
import io.guessit.core.pipeline.contracts.Extractor;
import io.guessit.core.pipeline.state.MatchName;
import io.guessit.core.pipeline.state.ParseContext;
import io.guessit.core.text.PatternMatcher;
import io.guessit.core.text.RegexOpts;
import io.guessit.core.text.Validators;

import java.util.regex.Pattern;

import static com.mirkoddd.sift.core.Sift.filteringWith;
import static com.mirkoddd.sift.core.Sift.optional;
import static com.mirkoddd.sift.core.SiftPatterns.literal;

/**
 * Extracts {@code episode_format}. Currently only "Minisode(s)" is recognised
 * — guessit's catalogue here is small and stable; new formats can be added
 * by widening the pattern alternation.
 */
public final class EpisodeFormatExtractor implements Extractor {
    private static final Pattern PATTERN = buildMinisodesPattern();

    private static Pattern buildMinisodesPattern() {
        var minisode = literal("minisode");
        var optionalS = optional().character('s');
        var ignoreCasePattern = filteringWith(SiftGlobalFlag.CASE_INSENSITIVE)
                .fromAnywhere().of(minisode).followedBy(optionalS);
        return Pattern.compile(ignoreCasePattern.shake());
    }

    @Override public String name() { return MatchName.EPISODE_FORMAT.toString().toLowerCase(); }

    @Override
    public String description() {
        return "episode format keyword (Episode, Chapter, …)";
    }

    @Override
    public void extract(ParseContext ctx) {
        var input = ctx.input;
        var opts = RegexOpts.defaults()
            .withValue(_ -> "Minisode")
            .withValidator(m -> Validators.sepsSurround(input).test(m));
        for (var m : PatternMatcher.regex(input, PATTERN, MatchName.EPISODE_FORMAT, opts, ctx.trace)) {
            ctx.matches.add(m);
        }
    }
}
