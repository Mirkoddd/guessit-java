package io.guessit.core.text.patterns;

import java.util.regex.Pattern;

import static com.mirkoddd.sift.core.Sift.*;
import static com.mirkoddd.sift.core.SiftPatterns.*;
import static io.guessit.core.text.patterns.CommonPatterns.compileCaseInsensitive;
import static io.guessit.core.text.patterns.CommonPatterns.namedCapture;

public final class CrcPatterns {

    private CrcPatterns() {}

    public static Pattern buildCrcPattern(String groupName) {
        var hexFragment = exactly(8).hexDigits();
        var crcCaptureGroup = capture(groupName, hexFragment);

        var basePattern = namedCapture(crcCaptureGroup);

        return compileCaseInsensitive(basePattern);
    }

    public static Pattern buildUuidPattern(String groupName) {
        var alphaNumeric = exactly(1).alphanumeric();
        var dash = exactly(1).character('-');
        var validChars = anyOf(alphaNumeric, dash);

        var uuidFragment = atLeast(20).of(validChars);
        var uuidCaptureGroup = capture(groupName, uuidFragment);

        var basePattern = namedCapture(uuidCaptureGroup);

        return Pattern.compile(basePattern.shake());
    }

    public static Pattern buildSxxExxInsidePattern() {
        var digits = between(1, 3).digits();
        var sxx = exactly(1).character('s').followedBy(digits);
        var exx = exactly(1).character('e').followedBy(digits);

        var basePattern = sxx.followedBy(exx);

        return compileCaseInsensitive(basePattern);
    }
}