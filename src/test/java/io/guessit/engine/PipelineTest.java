package io.guessit.engine;

import io.guessit.core.pipeline.Pipeline;
import io.guessit.core.pipeline.contracts.Extractor;
import io.guessit.core.pipeline.phases.*;
import io.guessit.core.pipeline.state.Match;
import io.guessit.core.pipeline.state.ParseContext;
import io.guessit.core.pipeline.state.Priority;
import io.guessit.core.text.Span;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Set;

import static io.guessit.api.Options.defaults;
import static io.guessit.config.OptionsConfig.empty;
import static io.guessit.core.pipeline.state.MatchName.EDITION;
import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PipelineTest {

    @Test
    void runsPhasesInOrder() {
        var trace = new ArrayList<String>();
        var pipeline = new Pipeline(of(
                new MarkerPhase(of(_ -> trace.add("marker"))),
                new ExtractorPhase(of(new Extractor() {
                    public String name() {
                        return "test";
                    }

                    public void extract(ParseContext c) {
                        trace.add("extract");
                        c.matches.add(Match.string(EDITION, "x", new Span(0, 1, "x"), Priority.DEFAULT, Set.of(), false));
                    }
                })),
                new ConflictPhase(),
                new PostPhase(of(_ -> trace.add("post"))),
                new OutputPhase(c -> {
                    trace.add("output");
                    c.result = c.resultBuilder.build();
                })
        ));
        var ctx = new ParseContext("x", defaults(), empty());
        pipeline.run(ctx);
        assertThat(trace).isEqualTo(of("marker", "extract", "post", "output"));
        assertNotNull(ctx.result);
    }
}