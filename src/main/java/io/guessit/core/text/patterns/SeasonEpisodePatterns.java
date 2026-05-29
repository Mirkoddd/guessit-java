package io.guessit.core.text.patterns;

import com.mirkoddd.sift.core.dsl.Connector;
import com.mirkoddd.sift.core.dsl.Fragment;
import com.mirkoddd.sift.core.dsl.VariableConnector;

import java.util.regex.Pattern;

import static com.mirkoddd.sift.core.Sift.*;
import static com.mirkoddd.sift.core.SiftPatterns.capture;
import static com.mirkoddd.sift.core.SiftPatterns.literal;
import static io.guessit.core.text.patterns.CommonPatterns.*;

public final class SeasonEpisodePatterns {

    private SeasonEpisodePatterns() {
    }

    public static Pattern buildHeadSePattern(String seasonGroupName, String episodeMarkerGroupName, String episodeGroupName) {
        var optionalSeparator = optional().of(
                anyOfCharacters(' ', '[', ']', '(', ')', '{', '}', '+', '*', '|', '=', '_', '~', '#', '.', ',', ';', ':', '-')
        );

        var markers = anyOfStrings("e", "ex", "xe", "ep", "x", "d");

        var seasonGroup = capture(seasonGroupName, oneOrMore().digits());
        var episodeMarkerGroup = capture(episodeMarkerGroupName, markers);
        var episodeGroup = capture(episodeGroupName, oneOrMore().digits());

        var basePattern = getS()
                .followedBy(namedCapture(seasonGroup), optionalSeparator)
                .followedBy(namedCapture(episodeMarkerGroup), optionalSeparator)
                .followedBy(namedCapture(episodeGroup));

        return compileCaseInsensitive(basePattern);
    }

    public static Pattern buildTailEPattern(String episodeSeparatorGroupName, String episodeGroupName) {
        var separators = anyOfStrings("ex", "xe", "ep", "and", "et", "to", "e", "x", "d", ".", "_", " ", "-", "+", "&", "a", "~");

        var episodeSeparatorGroup = capture(episodeSeparatorGroupName, separators);
        var episodeGroup = capture(episodeGroupName, between(1, 4).digits());

        var optionalAt = optional().character('@');

        var basePattern = namedCapture(episodeSeparatorGroup)
                .followedBy(optionalAt, namedCapture(episodeGroup));

        return compileCaseInsensitive(basePattern);
    }

    public static Pattern buildHeadNumXPattern(String seasonGroupName, String episodeMarkerGroupName, String episodeGroupName) {
        var seasonGroup = capture(seasonGroupName, oneOrMore().digits());
        var episodeMarkerGroup = capture(episodeMarkerGroupName, literal("x"));
        var episodeGroup = capture(episodeGroupName, oneOrMore().digits());

        var optionalSpace = optional().character(' ');

        var basePattern = namedCapture(seasonGroup)
                .followedBy(optionalSpace, namedCapture(episodeMarkerGroup))
                .followedBy(optionalSpace, namedCapture(episodeGroup));

        return compileCaseInsensitive(basePattern);
    }

    public static Pattern buildHeadEPattern(String seasonGroupName, String episodeMarkerGroupName, String episodeGroupName) {
        var seasonGroup = namedCapture(capture(seasonGroupName, between(1, 2).digits()));
        var optionalSeasonClause = optional().of(seasonGroup);

        var episodeMarkerGroup = capture(episodeMarkerGroupName, literal("e"));
        var episodeGroup = capture(episodeGroupName, between(1, 4).digits());

        var basePattern = find(optionalSeasonClause)
                .followedBy(namedCapture(episodeMarkerGroup), namedCapture(episodeGroup));

        return compileCaseInsensitive(basePattern);
    }

    public static Pattern buildTailEOnlyPattern(String episodeSeparatorGroupName, String episodeGroupName) {
        var separators = anyOfStrings("e", "x", "-");

        var episodeSeparatorGroup = capture(episodeSeparatorGroupName, separators);
        var episodeGroup = capture(episodeGroupName, between(1, 4).digits());

        var basePattern = namedCapture(episodeSeparatorGroup)
                .followedBy(namedCapture(episodeGroup));

        return compileCaseInsensitive(basePattern);
    }

    public static Pattern buildHeadSPattern(String seasonGroupName) {
        var seasonGroup = capture(seasonGroupName, oneOrMore().digits());

        var basePattern = getS().followedBy(namedCapture(seasonGroup));

        return compileCaseInsensitive(basePattern);
    }

    public static Pattern buildTailSPattern(String seasonSeparatorGroupName, String seasonGroupName) {
        var separators = anyOfStrings("and", "et", "to", "s", "a", "-", "+", "&", "~", ".", " ", "_");

        var seasonSeparatorGroup = capture(seasonSeparatorGroupName, separators);
        var seasonGroup = capture(seasonGroupName, oneOrMore().digits());

        var basePattern = namedCapture(seasonSeparatorGroup)
                .followedBy(namedCapture(seasonGroup));

        return compileCaseInsensitive(basePattern);
    }

    public static Pattern buildHeadCapPattern(String seasonMarkerGroupName, String seasonGroupName, String episodeGroupName, String secondarySeasonGroupName, String secondaryEpisodeGroupName) {
        var gap = anyOfCharacters(' ', '.', '_', '-');
        var optionalGap = optional().of(gap);
        var dashOrUnderscore = anyOfCharacters('_', '-');

        var secondarySeasonGroup = capture(secondarySeasonGroupName, between(1, 2).digits());
        var secondaryEpisodeGroup = capture(secondaryEpisodeGroupName, exactly(2).digits());

        var secondaryRangeClause = find(dashOrUnderscore)
                .followedBy(namedCapture(secondarySeasonGroup), namedCapture(secondaryEpisodeGroup));

        var optionalSecondaryRangeClause = optional().of(secondaryRangeClause);

        var seasonMarkerGroup = capture(seasonMarkerGroupName, literal("cap"));
        var seasonGroup = capture(seasonGroupName, between(1, 2).digits());
        var episodeGroup = capture(episodeGroupName, exactly(2).digits());

        var basePattern = namedCapture(seasonMarkerGroup)
                .followedBy(optionalGap)
                .followedBy(namedCapture(seasonGroup), namedCapture(episodeGroup))
                .followedBy(optionalSecondaryRangeClause);

        return compileCaseInsensitive(basePattern);
    }

    public static Pattern buildSExtrasPattern(String seasonGroupName, String extrasGroupName) {
        var extrasWordClause = find(literal("Extra"))
                .followedBy(optionalS());

        var seasonGroup = capture(seasonGroupName, oneOrMore().digits());
        var extrasGroup = capture(extrasGroupName, extrasWordClause);

        var basePattern = getS()
                .followedBy(namedCapture(seasonGroup), namedCapture(extrasGroup));

        return compileCaseInsensitive(basePattern);
    }

    public static Pattern buildSxxAllPattern(String seasonGroupName, String allGroupName) {
        var markers = anyOfStrings("xE", "Ex", "E", "x");

        var seasonGroup = capture(seasonGroupName, oneOrMore().digits());
        var allGroup = capture(allGroupName, literal("All"));

        var optionalDash = optional().character('-');

        var basePattern = optionalS()
                .followedBy(namedCapture(seasonGroup), optionalDash)
                .followedBy(markers)
                .followedBy(optionalDash, namedCapture(allGroup));

        return compileCaseInsensitive(basePattern);
    }

    private static Connector<Fragment> getS() {
        return exactly(1).character('s');
    }

    private static VariableConnector<Fragment> optionalS() {
        return optional().character('s');
    }
}