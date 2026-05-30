package io.guessit.rules.post;

import io.guessit.api.Options;
import io.guessit.config.OptionsConfig;
import io.guessit.core.pipeline.state.Match;
import io.guessit.core.pipeline.state.MatchName;
import io.guessit.core.pipeline.state.ParseContext;
import io.guessit.core.pipeline.state.Priority;
import io.guessit.core.text.Span;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.guessit.api.OptionsBuilder.options;
import static io.guessit.config.OptionsConfig.empty;
import static io.guessit.core.pipeline.state.Match.of;
import static io.guessit.core.pipeline.state.MatchName.*;
import static org.assertj.core.api.Assertions.assertThat;

class TypeProcessorTest {
    private static ParseContext ctx(String input) {
        return new ParseContext(input, Options.defaults(), OptionsConfig.empty());
    }

    @Test void seasonOrEpisodeYieldsEpisode() {
        var ctx = ctx("anything");
        ctx.matches.add(of(SEASON, 1, new Span(0, 2, "S1")));
        new TypeProcessor().process(ctx);
        Assertions.assertThat(ctx.matches.named(TYPE).findFirst().orElseThrow().value()).isEqualTo("episode");
    }

    @Test void noEpisodeOrYearYieldsMovie() {
        var ctx = ctx("anything");
        ctx.matches.add(of(YEAR, 2020, new Span(0, 4, "2020")));
        new TypeProcessor().process(ctx);
        Assertions.assertThat(ctx.matches.named(TYPE).findFirst().orElseThrow().value()).isEqualTo("movie");
    }

    @Test void dateWithoutYearYieldsEpisode() {
        var ctx = ctx("anything");
        ctx.matches.add(of(DATE, null, new Span(0, 10, "2020-01-01")));
        new TypeProcessor().process(ctx);
        Assertions.assertThat(ctx.matches.named(TYPE).findFirst().orElseThrow().value()).isEqualTo("episode");
    }

    @Test void optionsTypeOverridesEverything() {
        var opts = options().type("movie").build();
        var ctx = new ParseContext("x", opts, empty());
        ctx.matches.add(of(EPISODE, 1, new Span(0, 1, "1")));
        new TypeProcessor().process(ctx);
        Assertions.assertThat(ctx.matches.named(TYPE).findFirst().orElseThrow().value()).isEqualTo("movie");
    }

    @Test void movieTypeRenamesEpisodeTitleToAlternativeTitle() {
        var ctx = ctx("Some Movie");
        ctx.matches.add(of(YEAR, 2020, new Span(0, 4, "2020")));
        ctx.matches.add(of(EPISODE_TITLE, "Bonus", new Span(5, 10, "Bonus")));
        new TypeProcessor().process(ctx);
        assertThat(ctx.matches.named(EPISODE_TITLE).count()).isZero();
        Assertions.assertThat(ctx.matches.named(ALTERNATIVE_TITLE).findFirst().orElseThrow().value()).isEqualTo("Bonus");
    }

    @Test void episodeTitleWithAlternativeReplacedTagSurvivesMovieDemotion() {
        var ctx = ctx("Some Movie");
        ctx.matches.add(Match.of(MatchName.YEAR, 2020, new Span(0, 4, "2020")));

        ctx.matches.add(new Match(MatchName.EPISODE_TITLE, "X", new Span(5, 6, "X"), Priority.DEFAULT,
                java.util.Set.of("alternative-replaced"), false));

        new TypeProcessor().process(ctx);
        assertThat(ctx.matches.named(MatchName.EPISODE_TITLE).count()).isOne();
    }
}