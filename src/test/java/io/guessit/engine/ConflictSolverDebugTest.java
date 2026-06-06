package io.guessit.engine;

import io.guessit.core.pipeline.phases.ConflictSolver;
import io.guessit.core.pipeline.state.Match;
import io.guessit.core.pipeline.state.MatchName;
import io.guessit.core.pipeline.state.MatchSet;
import io.guessit.core.pipeline.state.Priority;
import io.guessit.core.text.Span;
import io.guessit.core.trace.Trace;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ConflictSolverDebugTest {

    private static final Match YEAR_MATCH = Match.integer(MatchName.YEAR, 2020, new Span(0, 4, "2020"), Priority.DEFAULT, Set.of(), false);

    @Test
    void emitsDropDecisionForShorterSpan() {
        var fired = new ArrayList<String>();
        Trace tr = new Trace() { @Override public void subStep(String m) { fired.add(m); } };
        var ms = new MatchSet();

        ms.add(Match.integer(MatchName.YEAR, 2020, new Span(4, 8, "2020"), Priority.DEFAULT, Set.of(), false));         // length 4
        ms.add(Match.string(MatchName.SCREEN_SIZE, "x", new Span(6, 14, "y1080p"), Priority.DEFAULT, Set.of(), false));// length 8, overlaps year

        ConflictSolver.solve(ms, tr);
        assertThat(fired).anyMatch(s -> s.startsWith("Dropping ") && s.contains("overlaps") && s.contains("shorter span"));
    }

    @Test
    void emitsNothingWhenNoOverlap() {
        var fired = new ArrayList<String>();
        Trace tr = new Trace() { @Override public void subStep(String m) { fired.add(m); } };
        var ms = new MatchSet();

        ms.add(YEAR_MATCH);
        ms.add(Match.string(MatchName.SCREEN_SIZE, "1080p", new Span(5, 10, "1080p"), Priority.DEFAULT, Set.of(), false));

        ConflictSolver.solve(ms, tr);
        assertThat(fired).isEmpty();
    }

    @Test
    void backwardsCompatibleNoTraceOverloadStillWorks() {
        var ms = new MatchSet();

        ms.add(YEAR_MATCH);

        ConflictSolver.solve(ms);
        assertThat(ms.snapshot()).hasSize(1);
    }
}