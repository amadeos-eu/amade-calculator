package eu.amadeos.apps.calculator.domain;

import java.util.Arrays;
import java.util.Optional;

/** How many bits the Programmer mode computes with; results wrap around like in a CPU register. */
public enum WordSize {
    QWORD(64),
    DWORD(32),
    WORD(16),
    BYTE(8);

    private final int bits;

    WordSize(int bits) {
        this.bits = bits;
    }

    public int bits() {
        return bits;
    }

    public static Optional<WordSize> forBits(int bits) {
        return Arrays.stream(values()).filter(w -> w.bits == bits).findFirst();
    }
}
