package io.guessit.rules.post;

import io.guessit.core.pipeline.contracts.PostProcessor;
import io.guessit.core.pipeline.state.ParseContext;
import io.guessit.core.text.Seps;
import io.guessit.core.text.Span;

import java.util.ArrayList;

/**
 * Trims leading and trailing separator characters from each match's span,
 * preserving single-character segments (e.g., acronym dots in S.H.I.E.L.D.).
 *
 * <p>This mirrors Python guessit's {@code strip_separators} post-processor.
 */
public final class StripSeparators implements PostProcessor {
    @Override
    public String description() {
        return "trim leading/trailing separators on raw spans";
    }

    @Override
    public void process(ParseContext ctx) {
        for (var m : new ArrayList<>(ctx.matches.snapshot())) {
            int s = m.span().start();
            int e = m.span().end();

            // Single-char matches are acronym components — don't touch them
            if (e - s <= 1) continue;

            while (s < e - 1 && Seps.isSep(ctx.input.charAt(s))) s++;
            while (e > s + 1 && Seps.isSep(ctx.input.charAt(e - 1))) e--;

            if (s != m.span().start() || e != m.span().end()) {
                String newRaw = ctx.input.substring(s, e);
                Span newSpan = new Span(s, e, newRaw);
                ctx.matches.replace(m, m.withSpan(newSpan));
            }
        }
    }
}
