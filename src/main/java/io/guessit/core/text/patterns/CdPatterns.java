package io.guessit.core.text.patterns;

import com.mirkoddd.sift.core.NamedCapture;
import com.mirkoddd.sift.core.dsl.Connector;
import com.mirkoddd.sift.core.dsl.Fragment;
import com.mirkoddd.sift.core.dsl.SiftPattern;

import java.util.regex.Pattern;

import static com.mirkoddd.sift.core.Sift.*;
import static com.mirkoddd.sift.core.SiftPatterns.capture;
import static com.mirkoddd.sift.core.SiftPatterns.literal;
import static io.guessit.core.text.patterns.CommonPatterns.*;

public final class CdPatterns {

    private CdPatterns() {}

    public static Pattern buildCdOfPattern(String cdGroupName, String countGroupName) {
        var cdGroup = captureOneOrMoreDigits(cdGroupName);
        var countGroup = captureOneOrMoreDigits(countGroupName);
        var optionalSeparator = buildOptionalSeparator();

        var ofLiteral = literal("of");
        var ofBlock = find(optionalSeparator)
                .followedBy(ofLiteral, optionalSeparator)
                .then().namedCapture(countGroup);

        var basePattern = cdLiteral()
                .followedBy(optionalSeparator)
                .then().namedCapture(cdGroup)
                .then().optional().of(ofBlock);

        return compileCaseInsensitive(basePattern);
    }

    public static Pattern buildCdCountPattern(String countGroupName) {
        var countGroup = captureOneOrMoreDigits(countGroupName);
        var optionalSeparator = buildOptionalSeparator();

        var optionalTrailingS = optional().character('s');
        var cdsLiteral = cdLiteral().followedBy(optionalTrailingS);

        var basePattern = namedCapture(countGroup)
                .followedBy(optionalSeparator, cdsLiteral);

        return compileCaseInsensitive(basePattern);
    }

    private static Connector<Fragment> cdLiteral() {
        return fromAnywhere().of(literal("cd"));
    }

    private static SiftPattern<Fragment> buildOptionalSeparator() {
        return optional().of(AbbreviationsPatterns.SEPS_NO_FS_PATTERN);
    }

    private static NamedCapture captureOneOrMoreDigits(String groupName) {
        return capture(groupName, oneOrMore().digits());
    }
}