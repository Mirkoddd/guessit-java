package io.guessit.core.pipeline.state;

import io.guessit.core.pipeline.phases.MarkerPhase;
import io.guessit.core.text.Span;

/**
 * A typed spatial boundary over the input string, produced by {@link MarkerPhase}.
 *
 * <p>Markers define structural scopes within the input, as specified by {@link MarkerType}:
 * <ul>
 * <li>{@link MarkerType#WHOLE} — the entire input. Used as a fallback scope.</li>
 * <li>{@link MarkerType#PATH} — a single directory or file segment (separated by {@code /} or {@code \\}).
 * Used to isolate rules to a specific filepart (e.g., filename vs parent directory).</li>
 * <li>{@link MarkerType#GROUP} — a balanced bracketed substring (e.g., {@code [...]} or {@code (...)}).
 * Used to scope release groups, languages, and similar isolated tags.</li>
 * </ul>
 */
public record Marker(MarkerType type, Span span) {

    /** * Checks if this marker's boundary completely encloses the given span.
     * <p>
     * Conceptually equivalent to {@code otherSpan.isInside(this.span)}.
     * * @param otherSpan the spatial span to check
     * @return {@code true} if the given span lies entirely inside this marker
     */
    public boolean covers(Span otherSpan) {
        return span.contains(otherSpan);
    }
}