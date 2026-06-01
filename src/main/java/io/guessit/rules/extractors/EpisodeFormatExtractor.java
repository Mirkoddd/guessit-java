package io.guessit.rules.extractors;

import io.guessit.core.pipeline.contracts.Extractor;
import io.guessit.core.pipeline.state.MatchName;
import io.guessit.core.pipeline.state.ParseContext;
import io.guessit.core.text.PatternMatcher;
import io.guessit.core.text.RegexOpts;
import io.guessit.core.text.Validators;
import io.guessit.core.text.patterns.EpisodeFormatPatterns;

import java.util.regex.Pattern;

/**
 * Extracts {@code episode_format}. Currently only "Minisode(s)" is recognized
 * — guessit's catalogue here is small and stable; new formats can be added
 * by widening the pattern alternation.
 */
public final class EpisodeFormatExtractor implements Extractor {
    private static final Pattern PATTERN = EpisodeFormatPatterns.buildMinisodesPattern();

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
