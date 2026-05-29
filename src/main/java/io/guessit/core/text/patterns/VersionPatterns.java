package io.guessit.core.text.patterns;

import java.util.regex.Pattern;

import static com.mirkoddd.sift.core.Sift.exactly;
import static com.mirkoddd.sift.core.Sift.oneOrMore;
import static com.mirkoddd.sift.core.SiftPatterns.capture;

public final class VersionPatterns {
    private VersionPatterns() {
    }

    public static Pattern buildPattern(String groupName) {

        var digitsCapture = capture(groupName, oneOrMore().digits());

        var pattern = exactly(1).character('v')
                .then().namedCapture(digitsCapture);

        return CommonPatterns.compileCaseInsensitive(pattern);
    }
}
