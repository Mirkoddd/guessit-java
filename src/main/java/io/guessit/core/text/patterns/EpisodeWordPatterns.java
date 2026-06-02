package io.guessit.core.text.patterns;

import com.mirkoddd.sift.core.dsl.Fragment;
import com.mirkoddd.sift.core.dsl.SiftPattern;
import io.guessit.rules.numerals.Numerals;

import java.util.List;
import java.util.regex.Pattern;

import static com.mirkoddd.sift.core.Sift.*;
import static com.mirkoddd.sift.core.SiftPatterns.anyOf;
import static com.mirkoddd.sift.core.SiftPatterns.capture;
import static io.guessit.core.text.patterns.CommonPatterns.*;

public final class EpisodeWordPatterns {

    private static final List<String> EPISODE_WORDS = List.of("episode", "episodes", "ep", "eps", "episodio", "episodios", "capitulo", "capitulos", "part", "parts", "ch", "chapter", "chapters", "e");
    private static final List<String> SEASON_WORDS = List.of("season", "seasons", "saison", "saisons", "seizoen", "temp", "temporada", "temporadas", "staffel", "staffeln", "stagione", "stagioni");
    private static final List<String> OF_WORDS = List.of("of", "sur", "de");

    private EpisodeWordPatterns() {
    }

    public static Pattern buildRedundantSeasonWordPattern(String groupName) {

        var digitsGroup = capture(groupName, oneOrMore().digits());
        var additional = anyOfStrings("serie", "series");
        var allWords = anyOf(anyOfStringsInList(SEASON_WORDS), additional);

        var basePattern = fromStart()
                .of(allWords)
                .followedBy(optionalSeparator())
                .then().namedCapture(digitsGroup)
                .andNothingElse();

        return compileCaseInsensitive(basePattern);
    }

    public static Pattern buildSeasonPattern(String wordGroupName, String valueGroupName, String countGroupName) {
        var seasonWordGroup = capture(wordGroupName, anyOfStringsInList(SEASON_WORDS));
        var seasonValueGroup = capture(valueGroupName, Numerals.NUMERAL_PATTERN);

        var basePattern = fromWordBoundary()
                .then().namedCapture(seasonWordGroup)
                .followedBy(optionalSeparator())
                .then().namedCapture(seasonValueGroup)
                .then().optional().of(buildOfCountClause(countGroupName));

        return compileCaseInsensitive(basePattern);
    }

    public static Pattern buildSeasonTailPattern(String operatorGroupName, String valueGroupName) {
        var tailSeparatorChar = anyOfCharacters(' ', '.', '_');
        var optionalTailSeparator = zeroOrMore().of(tailSeparatorChar);
        var tailOperators = anyOfStrings("and", "et", "to", "a", "-", "~", "&", "+");

        var tailOperatorBlock = find(optionalTailSeparator)
                .followedBy(tailOperators, optionalTailSeparator);

        var tailOperatorGroup = capture(operatorGroupName, anyOf(tailOperatorBlock, requiredSeparator()));
        var tailValueGroup = capture(valueGroupName, oneOrMore().digits());

        var basePattern = namedCapture(tailOperatorGroup)
                .then().namedCapture(tailValueGroup);

        return compileCaseInsensitive(basePattern);
    }

    public static Pattern buildAfterOfPattern() {
        var basePattern = fromStart()
                .of(optionalSeparator())
                .followedBy(anyOfStringsInList(OF_WORDS), optionalSeparator())
                .then().oneOrMore().digits();

        return compileCaseInsensitive(basePattern);
    }

    public static Pattern buildEpisodePattern(boolean isEpisodeType, String wordGroupName, String valueGroupName, String versionGroupName, String countGroupName) {
        var episodeWordGroup = capture(wordGroupName, anyOfStringsInList(EPISODE_WORDS));

        var episodePrefixBlock = namedCapture(episodeWordGroup)
                .notPrecededBy(exactly(1).alphanumeric())
                .followedBy(optionalSeparator());

        var episodeValueGroup = isEpisodeType
                ? capture(valueGroupName, Numerals.NUMERAL_PATTERN)
                : capture(valueGroupName, oneOrMore().digits());

        var versionGroup = capture(versionGroupName, oneOrMore().digits());
        var versionClause = exactly(1).character('v')
                .then().namedCapture(versionGroup);

        var basePattern = find(episodePrefixBlock)
                .then().namedCapture(episodeValueGroup)
                .then().optional().of(versionClause)
                .then().optional().of(buildOfCountClause(countGroupName));

        return compileCaseInsensitive(basePattern);
    }

    public static Pattern buildDetachedEpCountPattern(String episodeValueGroupName, String countGroupName) {
        var detachedEpisodeGroup = capture(episodeValueGroupName, oneOrMore().digits());

        var optionalEpisodeWordClause = find(optionalSeparator())
                .followedBy(anyOfStringsInList(EPISODE_WORDS));

        var basePattern = namedCapture(detachedEpisodeGroup)
                .followedBy(buildOfCountClause(countGroupName))
                .then().optional().of(optionalEpisodeWordClause);

        return compileCaseInsensitive(basePattern);
    }

    private static SiftPattern<Fragment> buildOfCountClause(String countGroupName) {
        var countGroup = capture(countGroupName, oneOrMore().digits());
        return find(optionalSeparator())
                .followedBy(anyOfStringsInList(OF_WORDS), optionalSeparator())
                .then().namedCapture(countGroup);
    }

    private static SiftPattern<Fragment> separatorCharacter() {
        return anyOfCharacters(' ', '.', '_', '-');
    }

    private static SiftPattern<Fragment> optionalSeparator() {
        return zeroOrMore().of(separatorCharacter());
    }

    private static SiftPattern<Fragment> requiredSeparator() {
        return oneOrMore().of(separatorCharacter());
    }
}