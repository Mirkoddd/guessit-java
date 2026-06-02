package io.guessit.rules.post;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.guessit.api.Guessit.parse;

class YearSeasonLinkerTest {
    @Test
    void yearWithEpisodeNoSeasonAddsSeason() {
        var r = parse("Show.2014.E03.mkv");
        Assertions.assertThat(r.season()).isEqualTo(2014);
    }
}
