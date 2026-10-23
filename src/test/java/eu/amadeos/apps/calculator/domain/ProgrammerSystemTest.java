package eu.amadeos.apps.calculator.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProgrammerSystemTest {
    private static ProgrammerSystem system(Radix radix, WordSize word, boolean signed) {
        return new ProgrammerSystem(radix, word, signed, Locale.US, true);
    }

    private static BigDecimal n(long value) {
        return BigDecimal.valueOf(value);
    }

    @Test
    void resultsWrapAroundTheWordSize() throws CalculationException {
        ProgrammerSystem signedByte = system(Radix.DEC, WordSize.BYTE, true);
        assertEquals(n(-128), signedByte.apply(Operator.ADD, n(127), n(1)));
        ProgrammerSystem unsignedByte = system(Radix.DEC, WordSize.BYTE, false);
        assertEquals(n(0), unsignedByte.apply(Operator.ADD, n(255), n(1)));
        assertEquals(n(255), unsignedByte.apply(Operator.SUBTRACT, n(0), n(1)));
    }

    @Test
    void divisionDropsTheFraction() throws CalculationException {
        ProgrammerSystem s = system(Radix.DEC, WordSize.QWORD, true);
        assertEquals(n(3), s.apply(Operator.DIVIDE, n(7), n(2)));
        assertEquals(n(-3), s.apply(Operator.DIVIDE, n(-7), n(2)));
        assertEquals(n(-1), s.apply(Operator.MOD, n(-7), n(2)));
        assertEquals(CalculationException.DIVISION_BY_ZERO,
                assertThrows(CalculationException.class, () -> s.apply(Operator.DIVIDE, n(1), n(0))).key());
    }

    @Test
    void bitOperations() throws CalculationException {
        ProgrammerSystem s = system(Radix.HEX, WordSize.BYTE, false);
        assertEquals(n(0x0F), s.apply(Operator.AND, n(0x3F), n(0xCF)));
        assertEquals(n(0xFF), s.apply(Operator.OR, n(0xF0), n(0x0F)));
        assertEquals(n(0xF0), s.apply(Operator.XOR, n(0xFF), n(0x0F)));
        assertEquals(n(0xF0), s.apply(Operator.NAND, n(0xFF), n(0x0F)));
        assertEquals(n(0xF0), s.apply(MathFunction.NOT, n(0x0F)));
        assertEquals(n(0x03), s.apply(MathFunction.ROTATE_LEFT, n(0x81)));
        assertEquals(n(0xC0), s.apply(MathFunction.ROTATE_RIGHT, n(0x81)));
        assertEquals(n(0xF0), s.apply(Operator.SHIFT_LEFT, n(0x0F), n(4)));
        assertEquals(n(0x00), s.apply(Operator.SHIFT_LEFT, n(0x0F), n(8)));
    }

    @Test
    void shiftRightKeepsTheSignOnlyWhenSigned() throws CalculationException {
        assertEquals(n(-64), system(Radix.DEC, WordSize.BYTE, true).apply(Operator.SHIFT_RIGHT, n(-128), n(1)));
        assertEquals(n(64), system(Radix.DEC, WordSize.BYTE, false).apply(Operator.SHIFT_RIGHT, n(128), n(1)));
    }

    @Test
    void numbersInEveryBase() {
        ProgrammerSystem s = system(Radix.HEX, WordSize.WORD, true);
        BigDecimal value = n(0xFF2A);
        assertEquals(n(-214), s.normalize(value));
        assertEquals("FF2A", s.format(value, Radix.HEX));
        assertEquals("−214", s.format(value, Radix.DEC));
        assertEquals("177 452", s.format(value, Radix.OCT));
        assertEquals("1111 1111 0010 1010", s.format(value, Radix.BIN));
        assertEquals("FF2A", s.copyText(value));
        ProgrammerSystem unsigned = system(Radix.DEC, WordSize.QWORD, false);
        assertEquals("18,446,744,073,709,551,615", unsigned.format(n(-1)));
    }

    @Test
    void enteringDigits() {
        ProgrammerSystem signedDec = system(Radix.DEC, WordSize.BYTE, true);
        assertEquals(Optional.of(n(127)), signedDec.parse("127"));
        assertEquals(Optional.empty(), signedDec.parse("128"));
        ProgrammerSystem hex = system(Radix.HEX, WordSize.BYTE, true);
        assertEquals(Optional.of(n(-1)), hex.parse("FF"));
        assertEquals(Optional.empty(), hex.parse("100"));
        assertEquals(Optional.of("FF"), hex.entryDigits(n(-1)));
        assertEquals(Optional.empty(), signedDec.entryDigits(n(-1)));
        assertEquals("1 0000", system(Radix.BIN, WordSize.BYTE, false).formatEntry("10000", false));
    }

    @Test
    void flippingBits() {
        ProgrammerSystem s = system(Radix.BIN, WordSize.BYTE, true);
        assertEquals(n(-128), s.flipBit(n(0), 7));
        assertEquals(n(5), s.flipBit(n(4), 0));
        assertEquals(n(4), s.flipBit(n(4), 12));
    }
}
