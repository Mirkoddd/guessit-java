package io.guessit.rules.extractors;

import io.guessit.api.Guessit;
import io.guessit.api.models.Country;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CountryExtractorTest {
    @Test void usCountry() {
        var r = Guessit.parse("Show.S01.US.WEB-DL.mkv");
        assertThat(r.country()).containsOnly(new Country("US", "United States"));
    }
    @Test void ukCountry() {
        var r = Guessit.parse("Show.S01.UK.HDTV.mkv");
        assertNotNull(r.country());
    }
    @Test void disallowedCountryIgnored() {
        var r = Guessit.parse("Show.S01.JP.HDTV.mkv");
        assertNull(r.country());
    }
    @Test void englishLanguageNotMisreadAsCountry() {
        var r = Guessit.parse("Show.S01.EN.HDTV.mkv");
        assertNull(r.country());
    }
}
