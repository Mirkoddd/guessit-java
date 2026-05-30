package io.guessit.core.text;

/**
 * Represents an exact spatial interval within the original input text.
 *
 * <p>This record encapsulates the start index, end index, and the exact
 * raw substring that was matched. It replaces the disparate spatial
 * parameters previously scattered across extractors, ensuring higher
 * cohesion and enabling zero-allocation sharing between related matches.
 *
 * @param start The inclusive start index of the match in the original input.
 * @param end   The exclusive end index of the match in the original input.
 * @param raw   The exact original substring from the input that was matched.
 */
public record Span(int start, int end, String raw) {

    /**
     * Computes the length of this span.
     *
     * @return the total number of characters in this span.
     */
    public int length() {
        return end - start;
    }

    /**
     * Checks if this span overlaps with another span.
     *
     * @param other The span to check for intersection.
     * @return {@code true} if the two spans share at least one character position,
     * {@code false} otherwise.
     */
    public boolean overlaps(Span other) {
        return this.start < other.end && this.end > other.start;
    }
}