package eu.amadeos.apps.calculator.domain;

import java.util.Arrays;
import java.util.Optional;

/** The three faces of the calculator, switched in the View menu. */
public enum Mode {
    BASIC("basic"),
    SCIENTIFIC("scientific"),
    PROGRAMMER("programmer");

    private final String key;

    Mode(String key) {
        this.key = key;
    }

    /** The name in the settings file and in the text keys, e.g. {@code mode.scientific}. */
    public String key() {
        return key;
    }

    /** True for Basic and Scientific, which compute with decimal fractions; Programmer computes with integers. */
    public boolean decimal() {
        return this != PROGRAMMER;
    }

    public static Optional<Mode> forKey(String key) {
        return Arrays.stream(values()).filter(m -> m.key.equals(key)).findFirst();
    }
}
