package io.guessit.core.pipeline.state;

import io.guessit.core.text.Span;
import io.guessit.api.models.*;
import java.time.LocalDate;
import java.util.Set;

/**
 * Represents a single extracted property found within the input text.
 * Implemented as a sealed interface to guarantee 100% Type Safety over the
 * 8 supported domain types of GuessIt.
 */
public sealed interface Match permits
        Match.StringMatch, Match.IntegerMatch, Match.DoubleMatch, Match.LanguageMatch,
        Match.CountryMatch, Match.SizeMatch, Match.BitRateMatch, Match.DateMatch {

    MatchName name();
    Span span();
    Priority priority();
    Set<String> tags();
    boolean isPrivate();

    Match withName(MatchName newName);
    Match withSpan(Span newSpan);
    Match withPriority(Priority newPriority);
    Match withTags(Set<String> newTags);

    default boolean hasTag(MatchTag tag) {
        if (tags() == null || tags().isEmpty() || tag == null) return false;

        String expectedValue = tag.getValue();

        if (tags().contains(expectedValue)) {
            return true;
        }
        for (String t : tags()) {
            if (t.equalsIgnoreCase(expectedValue)) {
                return true;
            }
        }
        return false;
    }

    static StringMatch string(MatchName name, String value, Span span) {
        return new StringMatch(name, value, span, Priority.DEFAULT, Set.of(), false);
    }
    static StringMatch string(MatchName name, String value, Span span, Priority priority, Set<String> tags, boolean isPrivate) {
        return new StringMatch(name, value, span, priority, tags, isPrivate);
    }

    static IntegerMatch integer(MatchName name, Integer value, Span span) {
        return new IntegerMatch(name, value, span, Priority.DEFAULT, Set.of(), false);
    }
    static IntegerMatch integer(MatchName name, Integer value, Span span, Priority priority, Set<String> tags, boolean isPrivate) {
        return new IntegerMatch(name, value, span, priority, tags, isPrivate);
    }

    static DoubleMatch decimal(MatchName name, Double value, Span span) {
        return new DoubleMatch(name, value, span, Priority.DEFAULT, Set.of(), false);
    }
    static DoubleMatch decimal(MatchName name, Double value, Span span, Priority priority, Set<String> tags, boolean isPrivate) {
        return new DoubleMatch(name, value, span, priority, tags, isPrivate);
    }

    static LanguageMatch language(MatchName name, Language value, Span span) {
        return new LanguageMatch(name, value, span, Priority.DEFAULT, Set.of(), false);
    }
    static LanguageMatch language(MatchName name, Language value, Span span, Priority priority, Set<String> tags, boolean isPrivate) {
        return new LanguageMatch(name, value, span, priority, tags, isPrivate);
    }

    static CountryMatch country(MatchName name, Country value, Span span) {
        return new CountryMatch(name, value, span, Priority.DEFAULT, Set.of(), false);
    }
    static CountryMatch country(MatchName name, Country value, Span span, Priority priority, Set<String> tags, boolean isPrivate) {
        return new CountryMatch(name, value, span, priority, tags, isPrivate);
    }

    static SizeMatch size(MatchName name, Size value, Span span) {
        return new SizeMatch(name, value, span, Priority.DEFAULT, Set.of(), false);
    }
    static SizeMatch size(MatchName name, Size value, Span span, Priority priority, Set<String> tags, boolean isPrivate) {
        return new SizeMatch(name, value, span, priority, tags, isPrivate);
    }

    static BitRateMatch bitRate(MatchName name, BitRate value, Span span) {
        return new BitRateMatch(name, value, span, Priority.DEFAULT, Set.of(), false);
    }
    static BitRateMatch bitRate(MatchName name, BitRate value, Span span, Priority priority, Set<String> tags, boolean isPrivate) {
        return new BitRateMatch(name, value, span, priority, tags, isPrivate);
    }

    static DateMatch date(MatchName name, LocalDate value, Span span) {
        return new DateMatch(name, value, span, Priority.DEFAULT, Set.of(), false);
    }
    static DateMatch date(MatchName name, LocalDate value, Span span, Priority priority, Set<String> tags, boolean isPrivate) {
        return new DateMatch(name, value, span, priority, tags, isPrivate);
    }

    record StringMatch(MatchName name, String value, Span span, Priority priority, Set<String> tags, boolean isPrivate) implements Match {
        @Override public Match withName(MatchName newName) { return new StringMatch(newName, value, span, priority, tags, isPrivate); }
        @Override public Match withSpan(Span newSpan) { return new StringMatch(name, value, newSpan, priority, tags, isPrivate); }
        @Override public Match withPriority(Priority newPriority) { return new StringMatch(name, value, span, newPriority, tags, isPrivate); }
        @Override public Match withTags(Set<String> newTags) { return new StringMatch(name, value, span, priority, newTags, isPrivate); }
    }

    record IntegerMatch(MatchName name, Integer value, Span span, Priority priority, Set<String> tags, boolean isPrivate) implements Match {
        @Override public Match withName(MatchName newName) { return new IntegerMatch(newName, value, span, priority, tags, isPrivate); }
        @Override public Match withSpan(Span newSpan) { return new IntegerMatch(name, value, newSpan, priority, tags, isPrivate); }
        @Override public Match withPriority(Priority newPriority) { return new IntegerMatch(name, value, span, newPriority, tags, isPrivate); }
        @Override public Match withTags(Set<String> newTags) { return new IntegerMatch(name, value, span, priority, newTags, isPrivate); }
    }

    record DoubleMatch(MatchName name, Double value, Span span, Priority priority, Set<String> tags, boolean isPrivate) implements Match {
        @Override public Match withName(MatchName newName) { return new DoubleMatch(newName, value, span, priority, tags, isPrivate); }
        @Override public Match withSpan(Span newSpan) { return new DoubleMatch(name, value, newSpan, priority, tags, isPrivate); }
        @Override public Match withPriority(Priority newPriority) { return new DoubleMatch(name, value, span, newPriority, tags, isPrivate); }
        @Override public Match withTags(Set<String> newTags) { return new DoubleMatch(name, value, span, priority, newTags, isPrivate); }
    }

    record LanguageMatch(MatchName name, Language value, Span span, Priority priority, Set<String> tags, boolean isPrivate) implements Match {
        @Override public Match withName(MatchName newName) { return new LanguageMatch(newName, value, span, priority, tags, isPrivate); }
        @Override public Match withSpan(Span newSpan) { return new LanguageMatch(name, value, newSpan, priority, tags, isPrivate); }
        @Override public Match withPriority(Priority newPriority) { return new LanguageMatch(name, value, span, newPriority, tags, isPrivate); }
        @Override public Match withTags(Set<String> newTags) { return new LanguageMatch(name, value, span, priority, newTags, isPrivate); }
    }

    record CountryMatch(MatchName name, Country value, Span span, Priority priority, Set<String> tags, boolean isPrivate) implements Match {
        @Override public Match withName(MatchName newName) { return new CountryMatch(newName, value, span, priority, tags, isPrivate); }
        @Override public Match withSpan(Span newSpan) { return new CountryMatch(name, value, newSpan, priority, tags, isPrivate); }
        @Override public Match withPriority(Priority newPriority) { return new CountryMatch(name, value, span, newPriority, tags, isPrivate); }
        @Override public Match withTags(Set<String> newTags) { return new CountryMatch(name, value, span, priority, newTags, isPrivate); }
    }

    record SizeMatch(MatchName name, Size value, Span span, Priority priority, Set<String> tags, boolean isPrivate) implements Match {
        @Override public Match withName(MatchName newName) { return new SizeMatch(newName, value, span, priority, tags, isPrivate); }
        @Override public Match withSpan(Span newSpan) { return new SizeMatch(name, value, newSpan, priority, tags, isPrivate); }
        @Override public Match withPriority(Priority newPriority) { return new SizeMatch(name, value, span, newPriority, tags, isPrivate); }
        @Override public Match withTags(Set<String> newTags) { return new SizeMatch(name, value, span, priority, newTags, isPrivate); }
    }

    record BitRateMatch(MatchName name, BitRate value, Span span, Priority priority, Set<String> tags, boolean isPrivate) implements Match {
        @Override public Match withName(MatchName newName) { return new BitRateMatch(newName, value, span, priority, tags, isPrivate); }
        @Override public Match withSpan(Span newSpan) { return new BitRateMatch(name, value, newSpan, priority, tags, isPrivate); }
        @Override public Match withPriority(Priority newPriority) { return new BitRateMatch(name, value, span, newPriority, tags, isPrivate); }
        @Override public Match withTags(Set<String> newTags) { return new BitRateMatch(name, value, span, priority, newTags, isPrivate); }
    }

    record DateMatch(MatchName name, LocalDate value, Span span, Priority priority, Set<String> tags, boolean isPrivate) implements Match {
        @Override public Match withName(MatchName newName) { return new DateMatch(newName, value, span, priority, tags, isPrivate); }
        @Override public Match withSpan(Span newSpan) { return new DateMatch(name, value, newSpan, priority, tags, isPrivate); }
        @Override public Match withPriority(Priority newPriority) { return new DateMatch(name, value, span, newPriority, tags, isPrivate); }
        @Override public Match withTags(Set<String> newTags) { return new DateMatch(name, value, span, priority, newTags, isPrivate); }
    }
}