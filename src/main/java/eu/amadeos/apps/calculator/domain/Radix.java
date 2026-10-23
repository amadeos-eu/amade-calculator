package eu.amadeos.apps.calculator.domain;

import java.util.Arrays;
import java.util.Optional;

/** The number bases of the Programmer mode. */
public enum Radix {
    HEX(16, 4),
    DEC(10, 3),
    OCT(8, 3),
    BIN(2, 4);

    private final int base;
    private final int group;

    Radix(int base, int group) {
        this.base = base;
        this.group = group;
    }

    public int base() {
        return base;
    }

    /** Digits per group when the number is shown grouped: 4 for HEX and BIN, 3 for DEC and OCT. */
    public int group() {
        return group;
    }

    /** True if the character (0-9, A-F in either case) is a digit of this base. */
    public boolean accepts(char digit) {
        return Character.digit(digit, base) >= 0;
    }

    public static Optional<Radix> forName(String name) {
        return Arrays.stream(values()).filter(r -> r.name().equalsIgnoreCase(name)).findFirst();
    }
}
