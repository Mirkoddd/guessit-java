package io.guessit.engine;

import io.guessit.core.pipeline.state.Match;
import io.guessit.core.pipeline.state.Priority;
import io.guessit.core.text.Span;
import io.guessit.core.trace.PrintTrace;
import io.guessit.core.trace.Trace;
import io.guessit.core.trace.TraceDiff;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static io.guessit.core.pipeline.state.MatchName.SCREEN_SIZE;
import static io.guessit.core.pipeline.state.MatchName.YEAR;
import static org.assertj.core.api.Assertions.assertThat;

class TraceDiffTest {

    private static final Match YEAR_MATCH = Match.integer(YEAR, 2020, new Span(11, 15, "2020"), Priority.DEFAULT, Set.of(), false);

    private static final Match SCREEN_MATCH = Match.string(SCREEN_SIZE, "1080p", new Span(16, 21, "1080p"), Priority.DEFAULT, Set.of(), false);

    static class CapturingTrace implements Trace {
        final List<String> events = new ArrayList<>();
        @Override public void added(Match m)   { events.add("+ " + PrintTrace.formatMatch(m)); }
        @Override public void removed(Match m) { events.add("- " + PrintTrace.formatMatch(m)); }
        @Override public void noChanges() { events.add("(no changes)"); }
    }

    @Test
    void emitsAddedForMatchPresentOnlyInAfter() {
        var trace = new CapturingTrace();
        TraceDiff.emit(List.of(), List.of(YEAR_MATCH), trace);
        assertThat(trace.events).containsExactly("+ 2020:(11,15)+name=year");
    }

    @Test
    void emitsRemovedForMatchPresentOnlyInBefore() {
        var trace = new CapturingTrace();
        TraceDiff.emit(List.of(YEAR_MATCH), List.of(), trace);
        assertThat(trace.events).containsExactly("- 2020:(11,15)+name=year");
    }

    @Test
    void emitsNoChangesWhenIdentical() {
        var trace = new CapturingTrace();
        TraceDiff.emit(List.of(YEAR_MATCH), List.of(YEAR_MATCH), trace);
        assertThat(trace.events).containsExactly("(no changes)");
    }

    @Test
    void emitsRemovesBeforeAdds() {
        var trace = new CapturingTrace();
        TraceDiff.emit(List.of(YEAR_MATCH), List.of(SCREEN_MATCH), trace);
        assertThat(trace.events).containsExactly(
                "- 2020:(11,15)+name=year",
                "+ 1080p:(16,21)+name=screen_size"
        );
    }

    @Test
    void preservesAfterOrderForAdds() {
        var trace = new CapturingTrace();
        TraceDiff.emit(List.of(), List.of(YEAR_MATCH, SCREEN_MATCH), trace);
        assertThat(trace.events).containsExactly(
                "+ 2020:(11,15)+name=year",
                "+ 1080p:(16,21)+name=screen_size"
        );
    }
}