package io.guessit.core.text.patterns;

import com.mirkoddd.sift.core.dsl.Fragment;
import com.mirkoddd.sift.core.dsl.SiftPattern;

import java.util.List;
import java.util.regex.Pattern;

import static com.mirkoddd.sift.core.Sift.exactly;
import static com.mirkoddd.sift.core.SiftPatterns.capture;
import static com.mirkoddd.sift.core.SiftPatterns.literal;
import static io.guessit.core.text.patterns.CommonPatterns.anyOfStringsInList;
import static io.guessit.core.text.patterns.CommonPatterns.compileCaseInsensitive;

public final class ContainerPatterns {

    private ContainerPatterns() {}

    public static Pattern buildExtensionPattern(String groupName, List<String> extensions) {
        var extensionGroup = capture(groupName, anyOfExtensions(extensions));

        var pattern = exactly(1).character('.')
                .then().namedCapture(extensionGroup)
                .andNothingElse();

        return compileCaseInsensitive(pattern);
    }

    private static SiftPattern<Fragment> anyOfExtensions(List<String> extensions) {
        if (extensions.size() == 1) {
            return literal(extensions.getFirst());
        }

        return anyOfStringsInList(extensions);
    }
}