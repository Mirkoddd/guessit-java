package io.guessit.core.pipeline.contracts;

import io.guessit.core.pipeline.state.ParseContext;

/** Stateless callback over the final, deconflicted match set. */
@FunctionalInterface
public interface PostProcessor extends Described {
    void process(ParseContext ctx);

    @Override
    default String description() { return getClass().getSimpleName(); }
}
