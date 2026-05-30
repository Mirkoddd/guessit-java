package io.guessit.engine;

import io.guessit.core.pipeline.phases.ConflictSolver;
import io.guessit.core.pipeline.state.Match;
import io.guessit.core.pipeline.state.MatchSet;
import io.guessit.core.pipeline.state.Priority;
import io.guessit.core.text.Span;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static io.guessit.core.pipeline.phases.ConflictSolver.solve;
import static io.guessit.core.pipeline.state.Match.of;
import static io.guessit.core.pipeline.state.MatchName.*;
import static org.assertj.core.api.Assertions.assertThat;

class ConflictSolverTest {

    @Test
    void higherPriorityWinsOverlap() {
        var s = new MatchSet();
        s.add(new Match(YEAR, 2020, new Span(0, 4, "2020"), Priority.EXPECTED, Set.of(), false));
        s.add(of(SEASON, 20, new Span(0, 2, "20")));
        solve(s);
        var names = s.all().map(Match::name).toList();
        assertThat(names).isEqualTo(List.of(YEAR));
    }

    @Test
    void longerWinsOnTiePriority() {
        var s = new MatchSet();
        s.add(of(OTHER, 1, new Span(0, 4, "abcd")));
        s.add(of(OTHER, 2, new Span(0, 2, "ab")));
        solve(s);
        assertThat(s.all().map(Match::name).toList()).isEqualTo(List.of(OTHER));
    }

    @Test
    void earlierStartWinsOnTiePriorityAndLength() {
        var s = new MatchSet();
        s.add(of(OTHER, 1, new Span(2, 4, "ab")));
        s.add(of(OTHER, 2, new Span(0, 2, "cd")));
        ConflictSolver.solve(s);
        assertThat(s.all().count()).isEqualTo(2);
    }

    @Test
    void coexistTagSurvives() {
        var s = new MatchSet();
        s.add(of(COUNTRY, "FR", new Span(0, 2, "FR")));

        s.add(new Match(LANGUAGE, "fr", new Span(0, 2, "fr"), Priority.DEFAULT, Set.of("coexist"), false));

        ConflictSolver.solve(s);
        assertThat(s.all().count()).isEqualTo(2);
    }

    @Test
    void noOverlapKeepsAll() {
        var s = new MatchSet();
        s.add(of(OTHER, 1, new Span(0, 2, "ab")));
        s.add(of(OTHER, 2, new Span(5, 7, "cd")));
        ConflictSolver.solve(s);
        assertThat(s.all().count()).isEqualTo(2);
    }
}