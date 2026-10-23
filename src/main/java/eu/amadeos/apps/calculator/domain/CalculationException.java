package eu.amadeos.apps.calculator.domain;

/** A calculation without a result, e.g. a division by zero; the key names the text shown instead. */
public final class CalculationException extends Exception {
    public static final String DIVISION_BY_ZERO = "error.division-by-zero";
    public static final String INVALID_INPUT = "error.invalid-input";
    public static final String OVERFLOW = "error.overflow";

    private final String key;

    public CalculationException(String key) {
        super(key);
        this.key = key;
    }

    /** One of {@link #DIVISION_BY_ZERO}, {@link #INVALID_INPUT} and {@link #OVERFLOW}. */
    public String key() {
        return key;
    }
}
