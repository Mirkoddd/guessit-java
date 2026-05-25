package io.guessit.rules.post;

import io.guessit.api.Guessit;
import io.guessit.api.Options;
import io.guessit.core.trace.DebugTrace;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;

class MultiStageProcessorDebugTest {

    @Test
    void atLeastOneProcessorEmitsStageSubsteps() {
        var sw = new StringWriter();
        var trace = new DebugTrace(sw);
        // Input chosen to exercise multiple post processors.
        Guessit.withOptions(Options.defaults()).guess("Show.S01.2020.1080p.mkv", trace);
        var out = sw.toString();
        // At least one processor should emit a "Stage " subStep.
        assertThat(out).contains("Stage ");
    }
}
