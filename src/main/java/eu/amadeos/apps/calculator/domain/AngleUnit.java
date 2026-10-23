package eu.amadeos.apps.calculator.domain;

/** How the trigonometric functions of the Scientific mode read and give angles. */
public enum AngleUnit {
    DEGREES("DEG"),
    RADIANS("RAD");

    private final String label;

    AngleUnit(String label) {
        this.label = label;
    }

    /** DEG or RAD, on the key and in the display. */
    public String label() {
        return label;
    }

    public AngleUnit other() {
        return this == DEGREES ? RADIANS : DEGREES;
    }
}
