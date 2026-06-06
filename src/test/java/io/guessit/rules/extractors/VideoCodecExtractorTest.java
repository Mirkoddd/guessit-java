package io.guessit.rules.extractors;

import io.guessit.api.Options;
import io.guessit.config.OptionsConfig;
import io.guessit.core.pipeline.phases.ConflictSolver;
import io.guessit.core.pipeline.state.Match;
import io.guessit.core.pipeline.state.MatchName;
import io.guessit.core.pipeline.state.ParseContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.guessit.core.pipeline.state.MatchName.*;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("OptionalGetWithoutIsPresent")
class VideoCodecExtractorTest {

    private static ParseContext run(String input) {
        var ctx = new ParseContext(input, Options.defaults(), OptionsConfig.empty());
        var x = new VideoCodecExtractor();
        x.extract(ctx);
        ConflictSolver.solve(ctx.matches);
        x.postProcess(ctx);
        return ctx;
    }

    @Test void h264() {
        assertThat(((Match.StringMatch) run("Movie.2015.1080p.x264.mkv").matches.named(VIDEO_CODEC).findFirst().get()).value()).isEqualTo("H.264");
    }

    @Test void h265() {
        assertThat(((Match.StringMatch) run("Movie.2015.1080p.x265.mkv").matches.named(VIDEO_CODEC).findFirst().get()).value()).isEqualTo("H.265");
    }

    @Test void hevc() {
        assertThat(((Match.StringMatch) run("Movie.2015.1080p.HEVC.mkv").matches.named(VIDEO_CODEC).findFirst().get()).value()).isEqualTo("H.265");
    }

    @Test void hevc10ColorDepth() {
        var ctx = run("Movie.2015.1080p.HEVC10.mkv");
        assertThat(((Match.StringMatch) ctx.matches.named(VIDEO_CODEC).findFirst().get()).value()).isEqualTo("H.265");
        assertThat(((Match.StringMatch) ctx.matches.named(COLOR_DEPTH).findFirst().get()).value()).isEqualTo("10-bit");
    }

    @Test void xvid() {
        assertThat(((Match.StringMatch) run("Movie.2015.XviD.avi").matches.named(VIDEO_CODEC).findFirst().get()).value()).isEqualTo("Xvid");
    }

    @Test void divx() {
        assertThat(((Match.StringMatch) run("Movie.2015.DivX.avi").matches.named(VIDEO_CODEC).findFirst().get()).value()).isEqualTo("DivX");
    }

    @Test void mpeg2() {
        assertThat(((Match.StringMatch) run("Movie.2015.MPEG-2.mkv").matches.named(VIDEO_CODEC).findFirst().get()).value()).isEqualTo("MPEG-2");
    }

    @Test void dxvaApi() {
        assertThat(((Match.StringMatch) run("Movie.2015.DXVA.mkv").matches.named(VIDEO_API).findFirst().get()).value()).isEqualTo("DXVA");
    }

    @Test void rejectsBareDigits() {
        Assertions.assertThat(run("Random.text.264.no.codec").matches.named(MatchName.VIDEO_CODEC).findAny()).isEmpty();
    }
}