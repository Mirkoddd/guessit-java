package io.guessit.rules.post;

import io.guessit.api.Options;
import io.guessit.config.OptionsConfig;
import io.guessit.core.pipeline.state.Marker;
import io.guessit.core.pipeline.state.MarkerType;
import io.guessit.core.pipeline.state.ParseContext;
import io.guessit.core.text.Span;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.guessit.core.pipeline.state.Match.of;
import static io.guessit.core.pipeline.state.MatchName.TITLE;
import static org.assertj.core.api.Assertions.assertThat;

class EnlargeGroupMatchesTest {

    private static ParseContext ctx(String input) {
        return new ParseContext(input, Options.defaults(), OptionsConfig.empty());
    }

    @Test
    void matchTouchingGroupStartIsExtendedLeft() {
        // input: "[abc value]"  positions: 0=[, 1=a..c, 5=v..e, 10=]
        // group marker: start=0, end=11
        // match: start=1, end=4  — touches left boundary only (end=4 != group.end-1=10)
        // expected after processing: start=0, end=4
        var ctx = ctx("[abc value]");
        ctx.markers.add(new Marker(MarkerType.GROUP, new Span(0, 11, "[abc value]")));
        ctx.matches.add(of(TITLE, "abc", new Span(1, 4, "abc")));

        new EnlargeGroupMatches().process(ctx);

        var result = ctx.matches.named(TITLE).findFirst().orElseThrow();
        assertThat(result.span().start()).isZero();
        Assertions.assertThat(result.span().end()).isEqualTo(4);
    }

    @Test
    void matchTouchingGroupEndIsExtendedRight() {
        // input: "[abc value]"  positions: 0=[, 5=v..u..e, 10=]
        // group marker: start=0, end=11
        // match: start=5, end=10 — touches right boundary only (start=5 != group.start+1=1)
        // expected after processing: start=5, end=11
        var ctx = ctx("[abc value]");
        ctx.markers.add(new Marker(MarkerType.GROUP, new Span(0, 11, "[abc value]")));
        ctx.matches.add(of(TITLE, "value", new Span(5, 10, "value")));

        new EnlargeGroupMatches().process(ctx);

        var result = ctx.matches.named(TITLE).findFirst().orElseThrow();
        Assertions.assertThat(result.span().start()).isEqualTo(5);
        Assertions.assertThat(result.span().end()).isEqualTo(11);
    }

    @Test
    void matchTouchingBothBoundariesIsExtendedBothWays() {
        // input: "[value]"  positions: 0=[, 1..5=value, 6=]
        // group marker: start=0, end=7
        // match: start=1, end=6 — touches both boundaries
        // expected after processing: start=0, end=7
        var ctx = ctx("[value]");
        ctx.markers.add(new Marker(MarkerType.GROUP, new Span(0, 7, "[value]")));
        ctx.matches.add(of(TITLE, "value", new Span(1, 6, "value")));

        new EnlargeGroupMatches().process(ctx);

        var result = ctx.matches.named(TITLE).findFirst().orElseThrow();
        assertThat(result.span().start()).isZero();
        Assertions.assertThat(result.span().end()).isEqualTo(7);
    }

    @Test
    void nonGroupMarkerIsIgnored() {
        // "path" markers should not trigger enlargement
        var ctx = ctx("[value]");
        ctx.markers.add(new Marker(MarkerType.PATH, new Span(0, 7, "[value]")));
        ctx.matches.add(of(TITLE, "value", new Span(1, 6, "value")));

        new EnlargeGroupMatches().process(ctx);

        var result = ctx.matches.named(TITLE).findFirst().orElseThrow();
        Assertions.assertThat(result.span().start()).isEqualTo(1);
        Assertions.assertThat(result.span().end()).isEqualTo(6);
    }

    @Test
    void matchNotTouchingBoundariesIsUnchanged() {
        // match entirely inside the group but not at boundaries
        var ctx = ctx("[abc value xyz]");
        ctx.markers.add(new Marker(MarkerType.GROUP, new Span(0, 15, "[abc value xyz]")));
        ctx.matches.add(of(TITLE, "value", new Span(5, 10, "value")));

        new EnlargeGroupMatches().process(ctx);

        var result = ctx.matches.named(TITLE).findFirst().orElseThrow();
        Assertions.assertThat(result.span().start()).isEqualTo(5);
        Assertions.assertThat(result.span().end()).isEqualTo(10);
    }
}