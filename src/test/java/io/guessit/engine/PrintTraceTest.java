package io.guessit.engine;

import io.guessit.core.pipeline.state.Match;
import io.guessit.core.pipeline.state.Priority;
import io.guessit.core.text.Span;
import io.guessit.core.trace.PrintTrace;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static io.guessit.core.pipeline.state.Match.of;
import static io.guessit.core.pipeline.state.MatchName.*;
import static io.guessit.core.trace.PrintTrace.formatMatch;
import static org.assertj.core.api.Assertions.assertThat;

class PrintTraceTest {

    private static final Match YEAR_MATCH = of(YEAR, 2020, new Span(11, 15, "2020"));

    @Test
    void formatsBareMatchValueStartEndName() {
        Assertions.assertThat(formatMatch(YEAR_MATCH)).isEqualTo("2020:(11,15)+name=year");
    }

    @Test
    void includesPrivateBeforeName() {
        var m = new Match(WEAK, 2020, new Span(11, 15, "2020"), Priority.DEFAULT, Set.of(), true);
        Assertions.assertThat(formatMatch(m)).isEqualTo("2020:(11,15)+private+name=weak");
    }

    @Test
    void includesPriorityWhenNotDefault() {
        var m = of(SOURCE, "Blu-ray", new Span(22, 28, "Blu-ray")).withPriority(Priority.EXPECTED);
        Assertions.assertThat(formatMatch(m)).isEqualTo("Blu-ray:(22,28)+name=source+priority=2000");
    }

    @Test
    void omitsPriorityAtDefault() {
        assertThat(PrintTrace.formatMatch(YEAR_MATCH)).doesNotContain("priority=");
    }

    @Test
    void rendersTagsAlphabeticallySorted() {
        var tags = new LinkedHashSet<String>();
        tags.add("weak-episode");
        tags.add("weak-duplicate");
        var m = of(SEASON, 20, new Span(11, 13, "20")).withTags(tags);
        Assertions.assertThat(formatMatch(m)).isEqualTo("20:(11,13)+name=season+tags=[weak-duplicate,weak-episode]");
    }

    @Test
    void omitsTagsWhenEmpty() {
        assertThat(PrintTrace.formatMatch(YEAR_MATCH)).doesNotContain("tags=");
    }

    @Test
    void inputLineFollowedByBlank() {
        var sb = new StringBuilder();
        new PrintTrace(sb).input("Movie.Name.2020.mkv");
        Assertions.assertThat(sb.toString()).isEqualTo("For: Movie.Name.2020.mkv\n\n");
    }

    @Test
    void phaseLineHeader() {
        var sb = new StringBuilder();
        new PrintTrace(sb).phase("extractors");
        Assertions.assertThat(sb.toString()).isEqualTo("[phase] extractors\n");
    }

    @Test
    void stepLineIndentedTwoSpaces() {
        var sb = new StringBuilder();
        new PrintTrace(sb).step("extract", "year");
        Assertions.assertThat(sb.toString()).isEqualTo("  [extract] year\n");
    }

    @Test
    void addedLineIndentedFourSpaces() {
        var sb = new StringBuilder();
        new PrintTrace(sb).added(YEAR_MATCH);
        Assertions.assertThat(sb.toString()).isEqualTo("    + 2020:(11,15)+name=year\n");
    }

    @Test
    void removedLineIndentedFourSpaces() {
        var sb = new StringBuilder();
        new PrintTrace(sb).removed(YEAR_MATCH);
        Assertions.assertThat(sb.toString()).isEqualTo("    - 2020:(11,15)+name=year\n");
    }

    @Test
    void noChangesLineIndentedFourSpaces() {
        var sb = new StringBuilder();
        new PrintTrace(sb).noChanges();
        Assertions.assertThat(sb.toString()).isEqualTo("    (no changes)\n");
    }

    @Test
    void noteLineIndentedTwoSpaces() {
        var sb = new StringBuilder();
        new PrintTrace(sb).note("marker: path:(0,41)+name=path");
        Assertions.assertThat(sb.toString()).isEqualTo("  marker: path:(0,41)+name=path\n");
    }
}