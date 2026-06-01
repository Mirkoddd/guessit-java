package io.guessit.rules.extractors;

import io.guessit.core.pipeline.contracts.Extractor;
import io.guessit.core.pipeline.state.*;
import io.guessit.core.text.Formatters;
import io.guessit.core.text.Span;
import io.guessit.core.text.Validators;
import io.guessit.core.text.patterns.FilmPatterns;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Detects film numbers from {@code f\d{1,2}} patterns, e.g. {@code f01}.
 *
 * <p>Pattern must be surrounded by separators (seps_surround) and the numeric
 * part is limited to 1–2 digits to avoid false positives on longer tokens.
 *
 * <p>postProcess builds a {@code film_title} from the leading hole in the same
 * filepart marker before the film match.
 */
public final class FilmExtractor implements Extractor {

    public static final String EXTRACTOR_NAME = "film";
    private static final String GRP_N = "n";

    private static final Pattern PATTERN = FilmPatterns.buildFilmPattern(GRP_N);

    @Override
    public String name() {
        return EXTRACTOR_NAME;
    }

    @Override
    public String description() {
        return "film number (Film 1, Movie 2, …)";
    }

    @Override
    public void extract(ParseContext ctx) {
        var input = ctx.input;
        var seps = Validators.sepsSurround(input);
        var m = PATTERN.matcher(input);

        while (m.find()) {
            var span = new Span(m.start(), m.end(), m.group());
            var head = new Match(MatchName.FILM, null, span, priority(), Set.of(), false);

            if (seps.test(head)) {
                int v = Integer.parseInt(m.group(GRP_N));
                ctx.matches.add(new Match(MatchName.FILM, v, span, priority(), Set.of(), false));
            }
        }
    }

    @Override
    public void postProcess(ParseContext ctx) {
        if (ctx.matches.named(MatchName.FILM_TITLE).findAny().isPresent()) {
            return;
        }

        ctx.matches.named(MatchName.FILM)
                .filter(x -> !x.isPrivate())
                .findFirst()
                .flatMap(film -> extractFilmTitle(ctx, film))
                .ifPresent(ctx.matches::add);
    }

    private Optional<Match> extractFilmTitle(ParseContext ctx, Match film) {
        return ctx.markers.stream()
                .filter(mk -> mk.type() == MarkerType.PATH && mk.covers(film.span()))
                .findFirst()
                .flatMap(fp -> {
                    var holes = Holes.compute(
                            ctx.input,
                            fp.span().start(),
                            film.span().start(),
                            ctx.matches.snapshot(),
                            Match::isPrivate,
                            null,
                            Formatters::cleanup
                    );

                    if (holes.isEmpty()) {
                        return Optional.empty();
                    }

                    var hole = holes.getFirst();
                    var title = hole.value();

                    if (title == null || title.isBlank()) {
                        return Optional.empty();
                    }

                    return Optional.of(new Match(MatchName.FILM_TITLE, title,
                            hole.span(), priority(), Set.of(), false));
                });
    }
}