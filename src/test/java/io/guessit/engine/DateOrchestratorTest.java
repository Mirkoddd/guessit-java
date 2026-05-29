package io.guessit.engine;

import io.guessit.rules.date.DateOrchestrator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateOrchestratorTest {
    @Test void validYearRange() {
        assertTrue(DateOrchestrator.validYear(1920));
        assertTrue(DateOrchestrator.validYear(2029));
        assertFalse(DateOrchestrator.validYear(1919));
        assertFalse(DateOrchestrator.validYear(2030));
    }
    @Test void validWeekRange() {
        assertTrue(DateOrchestrator.validWeek(1));
        assertTrue(DateOrchestrator.validWeek(52));
        assertFalse(DateOrchestrator.validWeek(0));
        assertFalse(DateOrchestrator.validWeek(53));
    }
    @Test void searchYmd() {
        var r = DateOrchestrator.search(" Show 2002-04-22 1080p ", null, null).orElseThrow();
        assertThat(r.date()).isEqualTo(LocalDate.of(2002, 4, 22));
    }
    @Test void searchDmy() {
        var r = DateOrchestrator.search("And this on 17-06-1998.", null, null).orElseThrow();
        assertThat(r.date()).isEqualTo(LocalDate.of(1998, 6, 17));
    }
    @Test void searchTwoDigitYearGuessesDayFirst() {
        var r = DateOrchestrator.search(" e 22-04-02 e", null, null).orElseThrow();
        assertThat(r.date()).isEqualTo(LocalDate.of(2002, 4, 22));
    }
    @Test void searchYearFirstHonoured() {
        var r = DateOrchestrator.search(" e 02.04.22 e", true, null).orElseThrow();
        assertThat(r.date()).isEqualTo(LocalDate.of(2002, 4, 22));
    }
    @Test void noDate() {
        assertThat(DateOrchestrator.search(" no date ", null, null)).isEmpty();
    }
    @Test void searchYmdWithX() {
        var r = DateOrchestrator.search("Something.2008x12.13-FlexGet", null, null).orElseThrow();
        assertThat(r.date()).isEqualTo(LocalDate.of(2008, 12, 13));
    }
}
