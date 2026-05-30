package io.guessit.rules.post;

import io.guessit.api.Options;
import io.guessit.config.OptionsConfig;
import io.guessit.core.pipeline.state.ParseContext;
import io.guessit.core.text.Span;
import org.junit.jupiter.api.Test;

import static io.guessit.api.Guessit.parse;
import static io.guessit.core.pipeline.state.Match.of;
import static io.guessit.core.pipeline.state.MatchName.TITLE;
import static org.assertj.core.api.Assertions.assertThat;

class StripSeparatorsTest {

    private static ParseContext ctx(String input) {
        return new ParseContext(input, Options.defaults(), OptionsConfig.empty());
    }

    @Test void stripsLeadingDot() {
        // ".Show" — leading dot at position 0, text at 1..5
        var ctx = ctx(".Show.mkv");

        ctx.matches.add(of(TITLE, "Show", new Span(0, 5, ".Show")));
        new StripSeparators().process(ctx);
        var m = ctx.matches.named(TITLE).findFirst().orElseThrow();

        assertThat(m.span().start()).isEqualTo(1);
        assertThat(m.span().end()).isEqualTo(5);
        assertThat(m.value()).isEqualTo("Show");
    }

    @Test void stripsTrailingDot() {
        // "Show." — trailing dot at position 4
        var ctx = ctx("Show.mkv");
        ctx.matches.add(of(TITLE, "Show", new Span(0, 5, "Show.")));
        new StripSeparators().process(ctx);
        var m = ctx.matches.named(TITLE).findFirst().orElseThrow();
        assertThat(m.span().start()).isZero();
        assertThat(m.span().end()).isEqualTo(4);
    }

    @Test void preservesSingleCharAcronymComponent() {
        // Single-char spans (e.g., 'S' in S.H.I.E.L.D.) must not be stripped
        var ctx = ctx("S.H.I.E.L.D.");
        ctx.matches.add(of(TITLE, "S", new Span(0, 1, "S")));
        new StripSeparators().process(ctx);
        var m = ctx.matches.named(TITLE).findFirst().orElseThrow();
        assertThat(m.span().start()).isZero();
        assertThat(m.span().end()).isEqualTo(1);
    }

    @Test void noOpWhenNoSurroundingSeps() {
        var ctx = ctx("ShowName");
        ctx.matches.add(of(TITLE, "ShowName", new Span(0, 8, "ShowName")));
        new StripSeparators().process(ctx);
        var m = ctx.matches.named(TITLE).findFirst().orElseThrow();
        assertThat(m.span().start()).isZero();
        assertThat(m.span().end()).isEqualTo(8);
    }

    /** Integration smoke test: basic parse must not throw and must find a title. */
    @Test void integrationSmokeShowTitle() {
        var r = parse("Show.S01E01.mkv");
        assertThat(r.title()).isEqualTo("Show");
    }
}