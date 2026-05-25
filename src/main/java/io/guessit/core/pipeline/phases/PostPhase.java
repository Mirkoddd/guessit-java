package io.guessit.core.pipeline.phases;

import io.guessit.core.pipeline.contracts.Described;
import io.guessit.core.pipeline.contracts.PostProcessor;
import io.guessit.core.pipeline.state.ParseContext;
import io.guessit.core.trace.TraceDiff;

import java.util.List;

/**
 * Phase 5 — cross-cutting cleanup that doesn't belong to any single extractor.
 *
 * <p>Default processors include {@code PreferLastPath} (drop matches in
 * earlier path segments when the last segment already produced one of the
 * same name), {@code PrivateRemover} (drop scaffolding matches), and
 * {@code TitleMarkerSelector} (pick the path segment from which the title
 * will be derived).
 */
public record PostPhase(List<PostProcessor> processors) implements Phase {

    public PostPhase { processors = List.copyOf(processors); }

    @Override
    public void apply(ParseContext ctx) {
        ctx.trace.phase("post", "running heuristic rules");
        for (var p : processors) {
            var before = ctx.matches.snapshot();
            ctx.trace.step("rule", p.getClass().getSimpleName(), p.description());
            p.process(ctx);
            TraceDiff.emit(before, ctx.matches.snapshot(), ctx);
        }
    }
}
