package io.guessit.core.text.patterns;

import java.util.regex.Pattern;

import static com.mirkoddd.sift.core.Sift.between;
import static com.mirkoddd.sift.core.Sift.exactly;
import static com.mirkoddd.sift.core.SiftPatterns.capture;
import static io.guessit.core.text.patterns.CommonPatterns.compileCaseInsensitive;

public final class FilmPatterns {

    private FilmPatterns() {}

    public static Pattern buildFilmPattern(String valueGroupName) {
        var filmValueGroup = capture(valueGroupName, between(1, 2).digits());

        var basePattern = exactly(1).character('f')
                .then().namedCapture(filmValueGroup);

        return compileCaseInsensitive(basePattern);
    }
}