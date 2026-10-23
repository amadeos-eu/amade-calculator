package eu.amadeos.apps.calculator.domain;

import java.util.Locale;
import java.util.Objects;

/**
 * What the calculator remembers between starts: the mode, the paper tape, grouping, the angle unit of Scientific
 * and the number base, word size and sign of Programmer.
 */
public record Settings(Mode mode, boolean paperTape, boolean grouping, AngleUnit angles, Radix radix,
                       WordSize word, boolean signed) {
    public static final Settings DEFAULT = new Settings(Mode.BASIC, false, true, AngleUnit.DEGREES, Radix.DEC,
            WordSize.QWORD, true);

    public Settings {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(angles, "angles");
        Objects.requireNonNull(radix, "radix");
        Objects.requireNonNull(word, "word");
    }

    public Settings withMode(Mode newMode) {
        return new Settings(newMode, paperTape, grouping, angles, radix, word, signed);
    }

    public Settings withPaperTape(boolean shown) {
        return new Settings(mode, shown, grouping, angles, radix, word, signed);
    }

    public Settings withGrouping(boolean on) {
        return new Settings(mode, paperTape, on, angles, radix, word, signed);
    }

    public Settings withAngles(AngleUnit unit) {
        return new Settings(mode, paperTape, grouping, unit, radix, word, signed);
    }

    public Settings withRadix(Radix newRadix) {
        return new Settings(mode, paperTape, grouping, angles, newRadix, word, signed);
    }

    public Settings withWord(WordSize newWord) {
        return new Settings(mode, paperTape, grouping, angles, radix, newWord, signed);
    }

    public Settings withSigned(boolean on) {
        return new Settings(mode, paperTape, grouping, angles, radix, word, on);
    }

    /** The number system these settings call for. */
    public NumberSystem numberSystem(Locale locale) {
        return mode.decimal() ? new DecimalSystem(angles, locale, grouping)
                : new ProgrammerSystem(radix, word, signed, locale, grouping);
    }
}
