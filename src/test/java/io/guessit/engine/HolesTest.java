package io.guessit.engine;

import io.guessit.core.pipeline.state.Holes;
import io.guessit.core.pipeline.state.Marker;
import io.guessit.core.pipeline.state.Match;
import io.guessit.core.text.Formatters;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.guessit.core.pipeline.state.Holes.Hole;
import static io.guessit.core.pipeline.state.Holes.compute;
import static io.guessit.core.pipeline.state.MatchName.*;
import static io.guessit.core.text.Seps.TITLE_CHARS;
import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;

class HolesTest {
    @Test void computeReturnsGapsBetweenMatches() {
        var input = "Movie.Name.2020.1080p.x264";
        var matches = of(
                Match.of(YEAR, 2020, 11, 15, "2020"),
                Match.of(SCREEN_SIZE, "1080p", 16, 21, "1080p"),
                Match.of(VIDEO_CODEC, "H.264", 22, 26, "x264"));
        var holes = compute(input, 0, input.length(), matches, _ -> false, null, Formatters::cleanup);
        Assertions.assertThat(holes).hasSize(1);
        assertThat(holes.getFirst().value()).isEqualTo("Movie Name");
    }
    @Test void ignoredMatchesAreTransparent() {
        var input = "Hello.world.bar";
        var matches = of(Match.of(LANGUAGE, "en", 6, 11, "world"));
        var holes = compute(input, 0, input.length(), matches,
                m -> m.name() == LANGUAGE, null, Formatters::cleanup);
        Assertions.assertThat(holes).hasSize(1);
        assertThat(holes.getFirst().value()).isEqualTo("Hello world bar");
    }
    @Test void cropAroundMarker() {
        var input = "abc[def]ghi";
        var hole = new Hole(0, 11, input, Formatters::cleanup);
        var cropped = hole.crop(of(new Marker("group", 3, 8, "[def]")));
        Assertions.assertThat(cropped).hasSize(2);
        assertThat(cropped.get(0).value()).isEqualTo("abc");
        assertThat(cropped.get(1).value()).isEqualTo("ghi");
    }
    @Test void splitOnTitleSeps() {
        var input = "Foo-Bar/Baz";
        var hole = new Hole(0, 11, input, s -> s);
        var parts = hole.split(TITLE_CHARS);
        assertThat(parts.stream().map(Hole::raw).toList()).isEqualTo(of("Foo", "Bar", "Baz"));
    }
    @Test void emptyHoleSkipped() {
        var input = "ab";
        var matches = List.of(Match.of(G, null, 0, 2, "ab"));
        var holes = Holes.compute(input, 0, input.length(), matches, _ -> false, null, Formatters::cleanup);
        Assertions.assertThat(holes).isEmpty();
    }
}
