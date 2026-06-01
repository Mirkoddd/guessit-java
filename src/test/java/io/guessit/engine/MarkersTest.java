package io.guessit.engine;

import io.guessit.core.pipeline.state.*;
import io.guessit.core.text.Span;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.guessit.core.pipeline.state.MatchName.*;
import static org.assertj.core.api.Assertions.assertThat;

class MarkersTest {

    @Test void namedFiltersByName() {
        var markers = List.of(
                new Marker(MarkerType.PATH, new Span(0, 5, "12345")),
                new Marker(MarkerType.GROUP, new Span(1, 4, "234")),
                new Marker(MarkerType.PATH, new Span(6, 10, "6789")));
        var paths = Markers.named(markers, MarkerType.PATH).toList();
        assertThat(paths).hasSize(2);
    }

    @Test void atMatchReturnsContainingMarker() {
        var markers = List.of(new Marker(MarkerType.PATH, new Span(0, 10, "0123456789")));
        var m = Match.of(G, null, new Span(2, 5, "234"));
        assertThat(Markers.atMatch(markers, m, _ -> true).orElseThrow()).isEqualTo(markers.getFirst());
    }

    @Test void markerSortedByDescendingMatchCount() {
        var p1 = new Marker(MarkerType.PATH, new Span(0, 5, "0..4"));
        var p2 = new Marker(MarkerType.PATH, new Span(6, 12, "6..11"));
        var matches = new MatchSet();

        matches.add(Match.of(ALTERNATIVE_TITLE, null, new Span(6, 7, "a")));
        matches.add(Match.of(BONUS, null, new Span(8, 9, "b")));
        matches.add(Match.of(COUNTRY, null, new Span(10, 11, "c")));
        matches.add(Match.of(DATE, null, new Span(0, 1, "d")));

        var sorted = Markers.markerSorted(List.of(p1, p2), matches);
        assertThat(sorted.get(0)).isEqualTo(p2);
        assertThat(sorted.get(1)).isEqualTo(p1);
    }
}