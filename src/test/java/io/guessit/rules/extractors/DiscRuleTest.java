package io.guessit.rules.extractors;

import io.guessit.api.Guessit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class DiscRuleTest {
    @Test void disc() {
        var r = Guessit.parse("Show.S01.Disc.2.mkv");
        Assertions.assertThat(r.extras()).containsEntry("disc", 2);
    }
    @Test void dvd() {
        var r = Guessit.parse("Show.S01.DVD3.mkv");
        Assertions.assertThat(r.extras()).containsEntry("disc", 3);
    }
}
