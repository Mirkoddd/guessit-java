package io.guessit.engine;

import io.guessit.core.pipeline.contracts.Extractor;
import io.guessit.core.pipeline.contracts.PostProcessor;
import io.guessit.core.pipeline.phases.MarkerPhase;
import io.guessit.core.pipeline.phases.PostPhase;
import io.guessit.core.pipeline.state.ParseContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DescribedTest {

    @Test
    void extractorDescriptionFallsBackToName() {
        Extractor anon = new Extractor() {
            @Override public String name() { return "x"; }
            @Override public void extract(ParseContext ctx) {}
        };
        assertThat(anon.description()).isEqualTo("x");
    }

    @Test
    void extractorDescriptionOverridable() {
        Extractor anon = new Extractor() {
            @Override public String name() { return "year"; }
            @Override public String description() { return "4-digit year"; }
            @Override public void extract(ParseContext ctx) {}
        };
        assertThat(anon.description()).isEqualTo("4-digit year");
    }

    @Test
    void postProcessorDescriptionFallsBackToSimpleClassName() {
        PostProcessor proc = ctx -> {};
        assertThat(proc.description()).isNotNull().isNotEmpty();
    }

    @Test
    void markerProducerDescriptionFallsBackToSimpleClassName() {
        MarkerPhase.MarkerProducer prod = ctx -> {};
        assertThat(prod.description()).isNotNull().isNotEmpty();
    }
}
