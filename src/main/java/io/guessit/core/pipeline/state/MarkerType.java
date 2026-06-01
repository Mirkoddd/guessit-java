package io.guessit.core.pipeline.state;

public enum MarkerType {
    PATH("path"),
    GROUP("group"),
    WHOLE("whole");

    private final String value;

    MarkerType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    public static MarkerType fromString(String text) {
        for (MarkerType type : MarkerType.values()) {
            if (type.value.equalsIgnoreCase(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown Marker: " + text);
    }
}