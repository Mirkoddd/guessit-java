package io.guessit.core.text.patterns;

import com.mirkoddd.sift.core.NamedCapture;
import com.mirkoddd.sift.core.dsl.Connector;
import com.mirkoddd.sift.core.dsl.Fragment;

import java.util.List;
import java.util.regex.Pattern;

import static com.mirkoddd.sift.core.Sift.*;
import static com.mirkoddd.sift.core.SiftPatterns.*;
import static io.guessit.core.text.patterns.CommonPatterns.*;

public final class WebsitePatterns {

    private WebsitePatterns() {}

    public record WebsiteRegexes(Pattern safeSubdomainPattern, Pattern safeTldPattern, Pattern safePrefixPattern) {}

    public static WebsiteRegexes buildPatterns(String urlGroupName, List<String> tlds, List<String> safeTlds, List<String> safePrefixes) {

        var anyTldAlternation = anyOfStringsInList(tlds);
        var safeTldAlternation = anyOfStringsInList(safeTlds);
        var safePrefixAlternation = anyOfStringsInList(safePrefixes);

        var unanchoredSafePrefix = find(safePrefixAlternation);

        var singleAlphaNum = exactly(1).alphanumeric();
        var singleDash = exactly(1).character('-');
        var singleDot = exactly(1).character('.');
        var wwwLiteral = literal("www.");

        var domainSegmentChars = oneOrMore().of(anyOf(singleAlphaNum, singleDash));

        var domainSegmentAndDot = domainSegmentChars.followedBy(singleDot);

        Connector<Fragment> wwwUrlCore = oneOrMore().of(wwwLiteral)
                .followedBy(oneOrMore().of(domainSegmentAndDot))
                .followedBy(anyTldAlternation);
        var safeSubdomainPattern = compileIsolatedUrl(wwwUrlCore, urlGroupName);

        var safeTldUrlCore = zeroOrMore().of(wwwLiteral)
                .followedBy(domainSegmentAndDot)
                .followedBy(safeTldAlternation);
        var safeTldPattern = compileIsolatedUrl(safeTldUrlCore, urlGroupName);

        var safePrefixUrlCore = zeroOrMore().of(wwwLiteral)
                .followedBy(domainSegmentAndDot)
                .followedBy(oneOrMore().of(unanchoredSafePrefix.followedBy(singleDot)))
                .followedBy(anyTldAlternation);
        var safePrefixPattern = compileIsolatedUrl(safePrefixUrlCore, urlGroupName);

        return new WebsiteRegexes(safeSubdomainPattern, safeTldPattern, safePrefixPattern);
    }

    private static Pattern compileIsolatedUrl(Connector<Fragment> urlCore, String urlGroupName) {
        var boundaryAlphaNum = exactly(1).alphanumeric();

        NamedCapture capturedUrlGroup = capture(urlGroupName, urlCore);

        var isolatedUrlTree = namedCapture(capturedUrlGroup)
                .notPrecededBy(boundaryAlphaNum)
                .notFollowedBy(boundaryAlphaNum);

        return compileCaseInsensitive(isolatedUrlTree);
    }
}