package io.guessit.core.text.patterns;

import com.mirkoddd.sift.core.dsl.*;

import java.util.List;
import java.util.regex.Pattern;

import static com.mirkoddd.sift.core.Sift.*;
import static com.mirkoddd.sift.core.SiftPatterns.anyOf;
import static com.mirkoddd.sift.core.SiftPatterns.capture;
import static com.mirkoddd.sift.core.SiftPatterns.literal;
import static io.guessit.core.text.patterns.CommonPatterns.*;

public final class ScreenSizePatterns {

    private static final String LEGACY_OPTIONAL_ZEROS_SUFFIX = "(?:\\.0{1,3})?";

    private ScreenSizePatterns() {}

    public static Pattern buildWhPattern(String widthGroupName, String heightGroupName) {
        var spaces = zeroOrMore().whitespace();
        var separatorChars = anyOfCharacters('x', '*');

        var widthCaptureGroup = capture(widthGroupName, threeOrFourDigits());
        var heightCaptureGroup = capture(heightGroupName, threeOrFourDigits());

        var basePattern = namedCapture(widthCaptureGroup)
                .followedBy(optionalDash(), spaces)
                .followedBy(separatorChars, spaces)
                .followedBy(optionalDash(), namedCapture(heightCaptureGroup));

        return compileCaseInsensitive(basePattern);
    }

    public static Pattern buildWidthHeightNorm(String widthGroupName, String heightGroupName, String scanGroupName) {
        var spaces = zeroOrMore().whitespace();
        var separatorChars = anyOfCharacters('x', '*', '-');

        var widthCaptureGroup = capture(widthGroupName, threeOrFourDigits());

        var heightCaptureGroup = capture(heightGroupName, threeOrFourDigits());

        var scanCaptureGroup = capture(scanGroupName, anyOfCharacters('i', 'p'));
        var optionalScanClause = optional().of(namedCapture(scanCaptureGroup));

        var basePattern = namedCapture(widthCaptureGroup)
                .followedBy(spaces, separatorChars)
                .followedBy(spaces, namedCapture(heightCaptureGroup))
                .followedBy(optionalScanClause);

        return compileCaseInsensitive(basePattern);
    }

    public static Pattern buildHeightScanNorm(String heightGroupName, String scanGroupName) {
        var heightCaptureGroup = capture(heightGroupName, threeOrFourDigits());

        var scanCaptureGroup = capture(scanGroupName, anyOfCharacters('i', 'p'));
        var optionalScanClause = optional().of(namedCapture(scanCaptureGroup));

        var basePattern = namedCapture(heightCaptureGroup)
                .followedBy(optionalScanClause);

        return compileCaseInsensitive(basePattern);
    }

    public static Pattern buildFrameRatePattern(String frameRateGroupName) {
        var frameRateDecimals = dot().then().between(1, 3).digits();
        var frameRateNumber = exactly(2).digits().followedBy(optional().of(frameRateDecimals));

        var frameRateCaptureGroup = capture(frameRateGroupName, frameRateNumber);
        var frameRatePureNode = namedCapture(frameRateCaptureGroup);

        var scanCharacters = anyOfCharacters('i', 'p');

        var basePattern = threeOrFourDigits()
                .followedBy(scanCharacters, frameRatePureNode)
                .andNothingElse();

        return compileCaseInsensitive(basePattern);
    }

    private static VariableCharacterConnector<Fragment> threeOrFourDigits() {
        return between(3, 4).digits();
    }

    public static Pattern buildScanPattern(String scanType, List<String> heights, List<String> frameRates, String widthGroupName, String heightGroupName, String scanGroupName) {
        var resolutionPrefixClause = buildResolutionPrefixClause(widthGroupName);
        var heightCaptureGroup = capture(heightGroupName, anyOfStringsInList(heights));
        var scanCaptureGroup = capture(scanGroupName, literal(scanType));

        var basePattern = find(resolutionPrefixClause)
                .then().namedCapture(heightCaptureGroup)
                .then().namedCapture(scanCaptureGroup);

        if (frameRates != null && !frameRates.isEmpty()) {
            var frameRatesClause = anyOfFrameRatesClause(frameRates);
            basePattern = basePattern.then().optional().of(frameRatesClause);
        }

        return compileCaseInsensitive(basePattern);
    }

    public static Pattern buildProgressiveSuffixPattern(String suffix, List<String> progressive, String widthGroupName, String heightGroupName, String scanGroupName) {
        var resolutionPrefixClause = buildResolutionPrefixClause(widthGroupName);

        var progressiveHeightGroup = capture(heightGroupName, anyOfStringsInList(progressive));
        var pureHeightNode = namedCapture(progressiveHeightGroup);

        var scanCaptureGroup = capture(scanGroupName, literal("p"));
        var optionalScanClause = optional().of(namedCapture(scanCaptureGroup));

        var suffixLiteral = literal(suffix);

        var basePattern = find(resolutionPrefixClause)
                .then().of(pureHeightNode)
                .then().of(optionalScanClause)
                .followedBy(suffixLiteral);

        return compileCaseInsensitive(basePattern);
    }

    public static Pattern buildProgressiveWeakPattern(List<String> progressive, String widthGroupName, String heightGroupName) {
        var resolutionPrefixClause = buildResolutionPrefixClause(widthGroupName);

        var progressiveHeightGroup = capture(heightGroupName, anyOfStringsInList(progressive));
        var pureHeightNode = namedCapture(progressiveHeightGroup);

        var basePattern = find(resolutionPrefixClause)
                .then().of(pureHeightNode);

        return compileCaseInsensitive(basePattern);
    }

    public static Pattern buildStandaloneFrameRatePattern(List<String> frameRates, String valueGroupName) {
        var frameRatesClause = anyOfFrameRatesClause(frameRates);
        var valueCaptureGroup = capture(valueGroupName, frameRatesClause);
        var rootValueNode = namedCapture(valueCaptureGroup);

        var suffixWords = anyOfStrings("p", "fps");

        var basePattern = rootValueNode
                .followedBy(optionalDash(), suffixWords);

        return compileCaseInsensitive(basePattern);
    }

    private static VariableConnector<Fragment> optionalDash() {
        return optional().character('-');
    }

    private static SiftPattern<Fragment> buildResolutionPrefixClause(String widthGroupName) {
        var widthCaptureGroup = capture(widthGroupName, threeOrFourDigits());
        var pureWidthNode = namedCapture(widthCaptureGroup);
        var dimensionsSeparator = anyOfCharacters('x', '*');

        var innerPrefixBlock = pureWidthNode.followedBy(dimensionsSeparator);
        return optional().of(innerPrefixBlock);
    }

    private static SiftPattern<Fragment> anyOfFrameRatesClause(List<String> items) {
        var frameRatesList = items.stream()
                .map(ScreenSizePatterns::parseFrameRateConfig)
                .toList();

        if (frameRatesList.size() == 1) {
            return frameRatesList.getFirst();
        }

        return anyOf(frameRatesList);
    }


    private static SiftPattern<Fragment> parseFrameRateConfig(String frConfig) {
        if (frConfig.endsWith(LEGACY_OPTIONAL_ZEROS_SUFFIX)) {
            String baseValue = frConfig.replace(LEGACY_OPTIONAL_ZEROS_SUFFIX, "");
            var optionalZerosBlock = dot()
                    .then().between(1, 3).character('0');

            return find(literal(baseValue)).followedBy(optional().of(optionalZerosBlock));
        }

        String cleanValue = frConfig.replace("\\.", ".");
        return literal(cleanValue);
    }

    private static Connector<Fragment> dot() {
        return exactly(1).character('.');
    }
}