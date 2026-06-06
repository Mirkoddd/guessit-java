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
import static io.guessit.core.pipeline.state.MatchName.*;
import static org.assertj.core.api.Assertions.assertThat;

class ConflictSolverTest {

    @Test
    void higherPriorityWinsOverlap() {
        var s = new MatchSet();
        s.add(Match.integer(YEAR, 2020, new Span(0, 4, "2020"), Priority.EXPECTED, Set.of(), false));
        s.add(Match.integer(SEASON, 20, new Span(0, 2, "20"), Priority.DEFAULT, Set.of(), false));
        solve(s);
        var names = s.all().map(Match::name).toList();
        assertThat(names).isEqualTo(List.of(YEAR));
    }

    @Test
    void longerWinsOnTiePriority() {
        var s = new MatchSet();
        s.add(Match.integer(OTHER, 1, new Span(0, 4, "abcd"), Priority.DEFAULT, Set.of(), false));
        s.add(Match.integer(OTHER, 2, new Span(0, 2, "ab"), Priority.DEFAULT, Set.of(), false));
        solve(s);
        assertThat(s.all().map(Match::name).toList()).isEqualTo(List.of(OTHER));
    }

    @Test
    void earlierStartWinsOnTiePriorityAndLength() {
        var s = new MatchSet();
        s.add(Match.integer(OTHER, 1, new Span(2, 4, "ab"), Priority.DEFAULT, Set.of(), false));
        s.add(Match.integer(OTHER, 2, new Span(0, 2, "cd"), Priority.DEFAULT, Set.of(), false));
        ConflictSolver.solve(s);
        assertThat(s.all().count()).isEqualTo(2);
    }

    @Test
    void coexistTagSurvives() {
        var s = new MatchSet();
        s.add(Match.string(COUNTRY, "FR", new Span(0, 2, "FR"), Priority.DEFAULT, Set.of(), false));
        s.add(Match.string(LANGUAGE, "fr", new Span(0, 2, "fr"), Priority.DEFAULT, Set.of("coexist"), false));

        ConflictSolver.solve(s);
        assertThat(s.all().count()).isEqualTo(2);
    }

    @Test
    void noOverlapKeepsAll() {
        var s = new MatchSet();
        s.add(Match.integer(OTHER, 1, new Span(0, 2, "ab"), Priority.DEFAULT, Set.of(), false));
        s.add(Match.integer(OTHER, 2, new Span(5, 7, "cd"), Priority.DEFAULT, Set.of(), false));
        ConflictSolver.solve(s);
        assertThat(s.all().count()).isEqualTo(2);
    }
}