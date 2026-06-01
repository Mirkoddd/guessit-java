package io.guessit.core.pipeline.state;

import io.guessit.core.text.Span;
import java.util.Set;

/**
 * Represents a single extracted property (a "match") found within the input text.
 *
 * <p>A match ties a strongly-typed or semantic value to a specific spatial region
 * ({@link Span}) of the original filename or release string. Matches are produced
 * by Extractors and refined by Post-Processors before final output generation.
 *
 * @param name      The semantic property name (e.g., SEASON, EPISODE, YEAR).
 * @param value     The parsed value of the match (temporarily an Object pending generic migration).
 * @param span      The exact spatial boundaries and raw text of this match.
 * @param priority  The resolution priority used by the ConflictSolver during overlapping matches.
 * @param tags      A set of semantic tags used to carry contextual state (e.g., "range-fill", "weak").
 * @param isPrivate If {@code true}, this match is used for internal logic and will not be
 * serialized in the final output.
 */
public record Match(
        MatchName name,
        Object value,
        Span span,
        Priority priority,
        Set<String> tags,
        boolean isPrivate
) {

    /**
     * Creates a copy of this match with a new name.
     *
     * @param newName The new semantic property name.
     * @return a new Match instance with the updated name.
     */
    public Match withName(MatchName newName) {
        return new Match(newName, this.value, this.span, this.priority, this.tags, this.isPrivate);
    }

    /**
     * Creates a copy of this match with a new spatial span.
     *
     * @param newSpan The new spatial interval.
     * @return a new Match instance with the updated span.
     */
    public Match withSpan(Span newSpan) {
        return new Match(this.name, this.value, newSpan, this.priority, this.tags, this.isPrivate);
    }

    /**
     * Match with default values.
     */
    public static Match of(MatchName name, Object value, Span span) {
        return new Match(name, value, span, Priority.DEFAULT, java.util.Set.of(), false);
    }

    /**
     * Creates a copy of this match with a new priority.
     */
    public Match withPriority(Priority newPriority) {
        return new Match(this.name, this.value, this.span, newPriority, this.tags, this.isPrivate);
    }

    /**
     * Creates a copy of this match with a new set of tags.
     */
    public Match withTags(Set<String> newTags) {
        return new Match(this.name, this.value, this.span, this.priority, newTags, this.isPrivate);
    }

    public boolean hasTag(MatchTag tag) {
        if (tags == null || tags.isEmpty() || tag == null) return false;

        String yamlValue = tag.getYamlValue();

        if (tags.contains(yamlValue)) {
            return true;
        }

        for (String t : tags) {
            if (t.equalsIgnoreCase(yamlValue)) {
                return true;
            }
        }

        return false;
    }
}