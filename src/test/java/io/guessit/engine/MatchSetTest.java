package io.guessit.engine;

import io.guessit.core.pipeline.state.*;
import io.guessit.core.text.Span;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static io.guessit.core.pipeline.state.MatchName.*;
import static org.assertj.core.api.Assertions.assertThat;

class MatchSetTest {

    @Test
    void addAndAll() {
        var s = new MatchSet();
        s.add(Match.integer(YEAR, 2020, new Span(0, 4, "2020"), Priority.DEFAULT, Set.of(), false));
        s.add(Match.string(SOURCE, "BluRay", new Span(5, 11, "BluRay"), Priority.DEFAULT, Set.of(), false));
        assertThat(s.all().count()).isEqualTo(2);
    }

    @Test
    void namedFilter() {
        var s = new MatchSet();
        s.add(Match.integer(YEAR, 2020, new Span(0, 4, "2020"), Priority.DEFAULT, Set.of(), false));
        s.add(Match.string(SOURCE, "BluRay", new Span(5, 11, "BluRay"), Priority.DEFAULT, Set.of(), false));
        var years = s.named(YEAR).toList();
        assertThat(years).hasSize(1);

        assertThat(((Match.IntegerMatch) years.getFirst()).value()).isEqualTo(2020);
    }

    @Test
    void overlapping() {
        var s = new MatchSet();
        var a = Match.integer(YEAR, 2020, new Span(0, 4, "2020"), Priority.DEFAULT, Set.of(), false);
        var b = Match.integer(SEASON, 20, new Span(1, 3, "20"), Priority.DEFAULT, Set.of(), false);
        s.add(a);
        s.add(b);
        var overs = s.overlapping(0, 5).toList();
        assertThat(overs).hasSize(2);
        var nonOver = s.overlapping(10, 20).toList();
        assertThat(nonOver).isEmpty();
    }

    @Test
    void inMarker() {
        var s = new MatchSet();
        var marker = new Marker(MarkerType.PATH, new Span(0, 10, "abcdefghij"));
        s.add(Match.integer(YEAR, 2020, new Span(0, 4, "2020"), Priority.DEFAULT, Set.of(), false));
        s.add(Match.integer(YEAR, 1999, new Span(12, 16, "1999"), Priority.DEFAULT, Set.of(), false));
        var inside = s.inMarker(marker).toList();
        assertThat(inside).hasSize(1);

        assertThat(((Match.IntegerMatch) inside.getFirst()).value()).isEqualTo(2020);
    }

    @Test
    void removeAndReplace() {
        var s = new MatchSet();
        var a = Match.integer(YEAR, 2020, new Span(0, 4, "2020"), Priority.DEFAULT, Set.of(), false);
        var b = Match.integer(YEAR, 1999, new Span(0, 4, "1999"), Priority.DEFAULT, Set.of(), false);
        s.add(a);
        s.replace(a, b);
        assertThat(s.all().toList()).isEqualTo(List.of(b));
        s.remove(b);
        assertThat(s.all().count()).isZero();
    }

    @Test
    void rangeReturnsMatchesFullyInsideSpan() {
        var set = new MatchSet();
        set.add(Match.integer(OTHER, 1, new Span(0, 5, "00000"), Priority.DEFAULT, Set.of(), false));
        set.add(Match.integer(OTHER, 2, new Span(6, 10, "1111"), Priority.DEFAULT, Set.of(), false));
        set.add(Match.integer(OTHER, 3, new Span(11, 15, "2222"), Priority.DEFAULT, Set.of(), false));
        var inRange = set.range(0, 10, _ -> true).toList();
        assertThat(inRange).hasSize(2);
        assertThat(inRange.get(0).name()).isEqualTo(OTHER);
        assertThat(inRange.get(1).name()).isEqualTo(OTHER);
    }

    @Test
    void previousAndNextRespectPredicate() {
        var set = new MatchSet();
        var a = Match.integer(OTHER, 1, new Span(0, 3, "aaa"), Priority.DEFAULT, Set.of(), false);
        var b = Match.integer(OTHER, 2, new Span(5, 8, "bbb"), Priority.DEFAULT, Set.of(), false);
        var c = Match.integer(OTHER, 3, new Span(10, 13, "ccc"), Priority.DEFAULT, Set.of(), false);
        set.add(a);
        set.add(b);
        set.add(c);
        assertThat(set.previous(b, _ -> true).orElseThrow()).isEqualTo(a);
        assertThat(set.next(b, _ -> true).orElseThrow()).isEqualTo(c);
        assertThat(set.previous(a, _ -> true)).isEmpty();
    }

    @Test
    void chainBeforeWalksOnlyThroughSeps() {
        var input = "abc.def-ghi";
        var set = new MatchSet();
        var a = Match.integer(OTHER, 1, new Span(0, 3, "abc"), Priority.DEFAULT, Set.of(), false);
        var b = Match.integer(OTHER, 2, new Span(4, 7, "def"), Priority.DEFAULT, Set.of(), false);
        set.add(a);
        set.add(b);
        assertThat(set.chainBefore(8, input, " ._-", _ -> true).orElseThrow()).isEqualTo(b);
        assertThat(set.chainBefore(4, input, " ._-", _ -> true).orElseThrow()).isEqualTo(a);
    }

    @Test
    void chainAfterWalksOnlyThroughSeps() {
        var input = "abc.def-ghi";
        var set = new MatchSet();
        var b = Match.integer(OTHER, 2, new Span(4, 7, "def"), Priority.DEFAULT, Set.of(), false);
        var c = Match.integer(OTHER, 3, new Span(8, 11, "ghi"), Priority.DEFAULT, Set.of(), false);
        set.add(b);
        set.add(c);
        assertThat(set.chainAfter(3, input, " ._-", _ -> true).orElseThrow()).isEqualTo(b);
        assertThat(set.chainAfter(7, input, " ._-", _ -> true).orElseThrow()).isEqualTo(c);
    }

    @Test
    void taggedFiltersByTagSet() {
        var set = new MatchSet();
        set.add(Match.string(OTHER, "a", new Span(0, 1, "a"), Priority.DEFAULT, Set.of(MatchTag.INFO.getValue()), false));
        set.add(Match.string(OTHER, "b", new Span(2, 3, "b"), Priority.DEFAULT, Set.of(), false));
        var tagged = set.tagged(MatchTag.INFO).toList();
        assertThat(tagged).hasSize(1);
        assertThat(tagged.getFirst().name()).isEqualTo(OTHER);
    }
}