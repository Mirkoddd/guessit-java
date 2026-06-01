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

    /**
     * Checks if this span completely contains another span.
     *
     * @param other The span to check.
     * @return {@code true} if the other span lies entirely within this span's boundaries.
     */
    public boolean contains(Span other) {
        return this.start <= other.start && this.end >= other.end;
    }

    /**
     * Checks if this span is completely inside another span.
     *
     * @param other The container span to check against.
     * @return {@code true} if this span lies entirely within the other span's boundaries.
     */
    public boolean isInside(Span other) {
        return other.contains(this);
    }

    /**
     * Checks if this span ends before or exactly where the other span begins.
     *
     * @param other The span to compare against.
     * @return {@code true} if this span is entirely to the left of the other span.
     */
    public boolean isBefore(Span other) {
        return this.end <= other.start;
    }

    /**
     * Checks if this span begins after or exactly where the other span ends.
     *
     * @param other The span to compare against.
     * @return {@code true} if this span is entirely to the right of the other span.
     */
    public boolean isAfter(Span other) {
        return this.start >= other.end;
    }

    /**
     * Checks if this span touches the other span directly on its left,
     * without any gap characters.
     *
     * @param other The span to compare against.
     * @return {@code true} if this span ends exactly where the other starts.
     */
    public boolean abutsBefore(Span other) {
        return this.end == other.start;
    }

    /**
     * Checks if this span touches the other span directly on its right,
     * without any gap characters.
     *
     * @param other The span to compare against.
     * @return {@code true} if this span starts exactly where the other ends.
     */
    public boolean abutsAfter(Span other) {
        return this.start == other.end;
    }

    /**
     * Calculates the distance in characters between this span and another span.
     *
     * @param other The span to measure distance to.
     * @return {@code 0} if the spans overlap or abut, otherwise the number of
     * characters between them.
     */
    public int distanceTo(Span other) {
        if (this.overlaps(other)) {
            return 0;
        }
        if (this.isBefore(other)) {
            return other.start - this.end;
        } else {
            return this.start - other.end;
        }
    }
}