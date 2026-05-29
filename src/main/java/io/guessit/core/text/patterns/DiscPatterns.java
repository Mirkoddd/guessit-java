package io.guessit.core.text.patterns;

import java.util.regex.Pattern;

import static com.mirkoddd.sift.core.Sift.*;
import static com.mirkoddd.sift.core.SiftPatterns.capture;
import static io.guessit.core.text.patterns.CommonPatterns.*;

public final class DiscPatterns {

    private DiscPatterns() {}

    public static Pattern buildDiscPattern(String groupName) {
        var prefixes = anyOfStrings("disc", "dvd", "vcd", "bd", "brd", "bluray");
        var separatorChars = anyOfCharacters(' ', '.', '_', '-');
        var separators = zeroOrMore().of(separatorChars);

        var digitCaptureGroup = capture(groupName, oneOrMore().digits());

        var basePattern = fromWordBoundary()
                .followedBy(prefixes, separators)
                .then().namedCapture(digitCaptureGroup)
                .wordBoundary();

        return compileCaseInsensitive(basePattern);
    }
}