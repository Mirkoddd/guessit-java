package io.guessit.rules.extractors;

import io.guessit.api.Guessit;
import org.junit.jupiter.api.Test;

import static io.guessit.api.Guessit.parse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

class EpisodeFormatExtractorTest {
    @Test void minisode() {
        var r = parse("Show.S01E02.Minisode.mkv");
        assertThat(r.episodeFormat()).isEqualTo("Minisode");
    }
    @Test void minisodesPlural() {
        var r = parse("Show.S01.Minisodes.Pack.mkv");
        assertThat(r.episodeFormat()).isEqualTo("Minisode");
    }
    @Test void noFormat() {
        var r = Guessit.parse("Show.S01E02.mkv");
        assertNull(r.episodeFormat());
    }
}
