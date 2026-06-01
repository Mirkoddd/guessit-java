package io.guessit.rules.post;

import io.guessit.core.pipeline.contracts.PostProcessor;
import io.guessit.core.pipeline.state.Marker;
import io.guessit.core.pipeline.state.MarkerType;
import io.guessit.core.pipeline.state.Match;
import io.guessit.core.pipeline.state.ParseContext;
import io.guessit.core.text.Span;

/**
 * Port of python {@code processors.EnlargeGroupMatches}: for each {@code group}
 * marker, any match that starts at {@code group.start + 1} is extended leftward
 * to {@code group.start}, and any match that ends at {@code group.end - 1} is
 * extended rightward to {@code group.end}.  This ensures the surrounding bracket
 * characters are included in the matched span.
 */
public final class EnlargeGroupMatches implements PostProcessor {
    @Override
    public String description() {
        return "enlarge match span to cover its containing bracket group";
    }

    @Override
    public void process(ParseContext ctx) {
        for (var g : ctx.markers) {
            if (g.type() == MarkerType.GROUP) enlargeForGroup(ctx, g);
        }
    }

    private static void enlargeForGroup(ParseContext ctx, Marker g) {
        for (var m : ctx.matches.inMarker(g).toList()) {
            var next = enlargedMatch(m, g, ctx.input);
            if (next != null) ctx.matches.replace(m, next);
        }
    }

    private static Match enlargedMatch(Match m, Marker g, String input) {
        int newStart = m.span().start();
        int newEnd = m.span().end();
        boolean changed = false;

        if (newStart == g.span().start() + 1 && newEnd <= g.span().end()) {
            newStart = g.span().start();
            changed = true;
        }

        if (newEnd == g.span().end() - 1 && newStart >= g.span().start()) {
            newEnd = g.span().end();
            changed = true;
        }

        if (changed) {
            String newRaw = input.substring(newStart, newEnd);
            Span newSpan = new Span(newStart, newEnd, newRaw);
            return m.withSpan(newSpan);
        }

        return null;
    }
}