package eu.amadeos.apps.calculator.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PastedNumberTest {
    private static Optional<BigDecimal> decimal(String text, Locale locale) {
        return PastedNumber.parse(text, new DecimalSystem(AngleUnit.DEGREES, locale, true), locale);
    }

    private static Optional<BigDecimal> integer(String text, Radix radix) {
        return PastedNumber.parse(text, new ProgrammerSystem(radix, WordSize.WORD, true, Locale.US, true), Locale.US);
    }

    @Test
    void decimalNumbersAsPeopleWriteThem() {
        assertEquals(Optional.of(new BigDecimal("1234.5")), decimal("1,234.5", Locale.US));
        assertEquals(Optional.of(new BigDecimal("1234.5")), decimal("1.234,5", Locale.US));
        assertEquals(Optional.of(new BigDecimal("1234.5")), decimal("1 234,5", Locale.FRANCE));
        assertEquals(Optional.of(new BigDecimal("1234")), decimal("1.234", Locale.GERMANY));
        assertEquals(Optional.of(new BigDecimal("1.5")), decimal("1,5", Locale.GERMANY));
        assertEquals(Optional.of(new BigDecimal("1.5")), decimal("1,5", Locale.US));
        assertEquals(Optional.of(new BigDecimal("-3")), decimal(" −3 ", Locale.US));
        assertEquals(Optional.of(new BigDecimal("1.5E-3")), decimal("1.5E-3", Locale.US));
        assertEquals(Optional.empty(), decimal("twelve", Locale.US));
        assertEquals(Optional.empty(), decimal("", Locale.US));
    }

    @Test
    void integersInTheBaseOrWithAPrefix() {
        assertEquals(Optional.of(BigDecimal.valueOf(-214)), integer("FF2A", Radix.HEX));
        assertEquals(Optional.of(BigDecimal.valueOf(-214)), integer("0xff2a", Radix.DEC));
        assertEquals(Optional.of(BigDecimal.valueOf(10)), integer("0b1010", Radix.HEX));
        assertEquals(Optional.of(BigDecimal.valueOf(240)), integer("1111 0000", Radix.BIN));
        assertEquals(Optional.of(BigDecimal.valueOf(1234)), integer("1,234", Radix.DEC));
        assertEquals(Optional.of(BigDecimal.valueOf(-5)), integer("-5", Radix.DEC));
        assertEquals(Optional.empty(), integer("10000", Radix.HEX));
        assertEquals(Optional.empty(), integer("G", Radix.HEX));
    }
}
