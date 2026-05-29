package io.guessit.core.text.patterns;

import java.util.regex.Pattern;

import static com.mirkoddd.sift.core.Sift.*;
import static com.mirkoddd.sift.core.SiftPatterns.capture;

public final class BonusPatterns {
    private BonusPatterns() {
    }

    public static Pattern buildPattern(String groupName) {
        var digitsCaptureGroup = capture(groupName, oneOrMore().digits());

        var pattern = exactly(1).character('x')
                .then().namedCapture(digitsCaptureGroup);

        return CommonPatterns.compileCaseInsensitive(pattern);
    }
}
