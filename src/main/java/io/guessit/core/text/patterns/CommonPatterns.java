package io.guessit.core.text.patterns;

import com.mirkoddd.sift.core.NamedCapture;
import com.mirkoddd.sift.core.SiftPatterns;
import com.mirkoddd.sift.core.dsl.Connector;
import com.mirkoddd.sift.core.dsl.Fragment;
import com.mirkoddd.sift.core.dsl.SiftPattern;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import static com.mirkoddd.sift.core.Sift.*;
import static com.mirkoddd.sift.core.SiftPatterns.anyOf;

public final class CommonPatterns {

    private CommonPatterns() {}

    static SiftPattern<Fragment> anyOfStringsInList(List<String> list){
        var mappedToLiterals = list.stream()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .map(SiftPatterns::literal)
                .toList();

        return anyOf(mappedToLiterals);
    }

    static SiftPattern<Fragment> anyOfStrings(String... strings){
        return anyOfStringsInList(Arrays.asList(strings));
    }

    static SiftPattern<Fragment> anyOfCharacters(char... characters){
        var mappedToCharacters = new String(characters).chars()
                .mapToObj(c -> exactly(1).character((char) c))
                .toList();

        return anyOf(mappedToCharacters);
    }


    static Connector<Fragment> find(SiftPattern<Fragment> fragment) {
        return fromAnywhere().of(fragment);
    }

    static Connector<Fragment> namedCapture(NamedCapture episodeWordGroup) {
        return fromAnywhere().namedCapture(episodeWordGroup);
    }

    static Pattern compileCaseInsensitive(SiftPattern<?> siftPattern) {
        String rawRegex = siftPattern.shake();
        return Pattern.compile(rawRegex,  Pattern.CASE_INSENSITIVE);
    }
}
