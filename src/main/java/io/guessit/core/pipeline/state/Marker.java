package io.guessit.core.pipeline.state;

import io.guessit.core.pipeline.phases.MarkerPhase;
import io.guessit.core.text.Span;

/**
 * Named span over the input string, produced by {@link MarkerPhase}.
 *
 * <p>Three marker kinds are emitted by the default rules:
 * <ul>
 * <li>{@code whole} — the entire input. Used as a fallback title scope.</li>
 * <li>{@code path} — one per {@code /} or {@code \\}-separated segment.
 * Used to scope rules to a single filepart (filename vs parent dir).</li>
 * <li>{@code group} — one per balanced bracketed substring, e.g. {@code [...]}
 * or {@code (...)}. Used by release-group, language, and similar rules.</li>
 * </ul>
 */
public record Marker(String name, Span span) {

    /** True if the given Span lies entirely inside this marker. */
    public boolean covers(Span otherSpan) {
        return span.contains(otherSpan);
    }
}