package io.guessit.rules.numerals;

import com.mirkoddd.sift.core.dsl.Fragment;
import com.mirkoddd.sift.core.dsl.SiftPattern;
import io.guessit.core.text.patterns.WordNumeralPatterns;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Handles validation and parsing of localized number words.
 */
final class WordNumerals implements TokenNumeralParser {

    WordNumerals() {
    }

    private static final Map<String, Integer> WORD_VALUES = buildWordMap();

    static final SiftPattern<Fragment> PATTERN = WordNumeralPatterns.buildPattern(WORD_VALUES);

    private static Map<String, Integer> buildWordMap() {
        Map<String, Integer> map = new HashMap<>();
        for (List<String> dict : DictionaryRegistry.getAllWords()) {
            for (int i = 0; i < dict.size(); i++) {
                map.putIfAbsent(dict.get(i).toLowerCase(Locale.ROOT), i);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    @Override
    public Integer tryParse(List<String> words) {
        return words.stream()
                .map(word -> word.toLowerCase(Locale.ROOT))
                .map(WORD_VALUES::get)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}