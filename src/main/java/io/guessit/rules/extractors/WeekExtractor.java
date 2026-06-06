package io.guessit.rules.extractors;

import io.guessit.core.pipeline.contracts.Extractor;
import io.guessit.core.pipeline.state.Match;
import io.guessit.core.pipeline.state.MatchName;
import io.guessit.core.pipeline.state.ParseContext;
import io.guessit.core.pipeline.state.Priority;
import io.guessit.core.text.Span;
import io.guessit.core.text.Validators;
import io.guessit.core.text.patterns.WeekPatterns;
import io.guessit.rules.date.DateOrchestrator;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Extracts {@code week} from "Week 5"-style tokens. Validated against the
 * 1–52 ISO week range via {@link DateOrchestrator#validWeek}; out-of-range
 * candidates are dropped to avoid swallowing things like "Week 99".
 */
public final class WeekExtractor implements Extractor {

    private static final String GRP_WEEK = "weekNum";

    private static final Pattern PATTERN = WeekPatterns.buildPattern(GRP_WEEK);

    @Override
    public String name() {
        return "week";
    }

    @Override
    public String description() {
        return "week tokens (W12, Week 12, …)";
    }

    @Override
    public void extract(ParseContext ctx) {
        var input = ctx.input;
        var seps = Validators.sepsSurround(input);
        var m = PATTERN.matcher(input);

        while (m.find()) {
            var headSpan = new Span(m.start(), m.end(), m.group());
            var head = Match.string(MatchName.WEEK, m.group(), headSpan, Priority.DEFAULT, Set.of(), false);

            if (seps.test(head)) {
                int v = Integer.parseInt(m.group(GRP_WEEK));

                if (DateOrchestrator.validWeek(v)) {
                    var weekSpan = new Span(m.start(GRP_WEEK), m.end(GRP_WEEK), m.group(GRP_WEEK));
                    ctx.matches.add(Match.integer(MatchName.WEEK, v, weekSpan, Priority.DEFAULT, Set.of(), false));
                }
            }
        }
    }
}