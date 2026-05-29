package io.guessit.core.text.patterns;

import com.mirkoddd.sift.core.dsl.Fragment;
import com.mirkoddd.sift.core.dsl.SiftPattern;

import java.util.List;
import java.util.Map;

import static com.mirkoddd.sift.core.Sift.fromAnywhere;
import static com.mirkoddd.sift.core.Sift.oneOrMore;
import static io.guessit.core.text.patterns.CommonPatterns.anyOfStringsInList;
import static io.guessit.core.text.patterns.CommonPatterns.find;

public final class WordNumeralPatterns {
    private WordNumeralPatterns() {}

/*    public static SiftPattern<Fragment> ignored(Map<String, Integer> wordValues) {
        List<String> words = wordValues.keySet().stream().toList();
        var wordLiterals = anyOfStringsInList(words);

        return fromAnywhere()
                .mustBeFollowedBy(oneOrMore().wordCharacters())
                .followedBy(wordLiterals);
    }*/

    public static SiftPattern<Fragment> buildPattern(Map<String, Integer> wordValues) {
        List<String> words = wordValues.keySet().stream().toList();
        return anyOfStringsInList(words);
    }
}
