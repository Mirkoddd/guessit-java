package io.guessit.core.pipeline.state;

/**
 * Defines priority levels for conflict resolution.
 * If two matches overlap, the one with the higher priority survives.
 */
public enum Priority {
    /** Explicit matches derived from user options (e.g., expected_title, expected_group). */
    EXPECTED(2000),

    /** Highly reliable rules and strict patterns (e.g., Scene Release Groups). */
    SCENE(1500),

    /** Absolute override (e.g., DateExtractor: must swallow any digits matching inside its span). */
    OVERRIDE(1100),

    /** Base level for ALL standard extractors (e.g., Title, Year, Codecs, Language, etc.). */
    DEFAULT(1000),

    /** Highly plausible inferences, but yields to explicit matches. */
    PROBABLE(800),

    /** Weaker inferences, easily overwritten by stronger context. */
    POSSIBLE(700),

    /** Very speculative matches based on raw patterns (e.g., CRC32 hashes). */
    SPECULATIVE(500),

    /** Last resort matches. */
    FALLBACK(100),

    /** None. */
    NONE(0);

    private final int score;

    Priority(int score) {
        this.score = score;
    }

    /**
     * Retrieves the numeric score of this priority for conflict resolution.
     * * @return the integer value representing the priority level.
     */
    public int getScore() {
        return score;
    }
}