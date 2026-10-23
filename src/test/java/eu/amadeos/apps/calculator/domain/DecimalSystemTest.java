package eu.amadeos.apps.calculator.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DecimalSystemTest {
    private final DecimalSystem degrees = new DecimalSystem(AngleUnit.DEGREES, Locale.US, true);
    private final DecimalSystem radians = new DecimalSystem(AngleUnit.RADIANS, Locale.US, true);

    private String f(MathFunction function, String x) throws CalculationException {
        return degrees.format(degrees.apply(function, new BigDecimal(x)));
    }

    private String op(Operator operator, String a, String b) throws CalculationException {
        return degrees.format(degrees.apply(operator, new BigDecimal(a), new BigDecimal(b)));
    }

    @Test
    void trigonometryInDegreesHitsExactValues() throws CalculationException {
        assertEquals("0.5", f(MathFunction.SIN, "30"));
        assertEquals("0", f(MathFunction.SIN, "180"));
        assertEquals("0", f(MathFunction.COS, "90"));
        assertEquals("−1", f(MathFunction.COS, "180"));
        assertEquals("1", f(MathFunction.TAN, "45"));
        assertEquals("30", f(MathFunction.ASIN, "0.5"));
        assertEquals("45", f(MathFunction.ATAN, "1"));
        assertEquals(CalculationException.INVALID_INPUT,
                assertThrows(CalculationException.class, () -> f(MathFunction.TAN, "90")).key());
    }

    @Test
    void trigonometryInRadians() throws CalculationException {
        assertEquals("0", radians.format(radians.apply(MathFunction.SIN, DecimalSystem.PI)));
        assertEquals("1", radians.format(radians.apply(MathFunction.COS, BigDecimal.ZERO)));
    }

    @Test
    void rootsAndPowers() throws CalculationException {
        assertEquals("1.414213562373095", f(MathFunction.SQUARE_ROOT, "2"));
        assertEquals("3", op(Operator.ROOT, "27", "3"));
        assertEquals("−2", op(Operator.ROOT, "-8", "3"));
        assertEquals("1,024", op(Operator.POWER, "2", "10"));
        assertEquals("0.125", op(Operator.POWER, "2", "-3"));
        assertEquals("1.4142135623731", op(Operator.POWER, "2", "0.5"));
        assertEquals("1,500", op(Operator.EXPONENT, "1.5", "3"));
        assertEquals(CalculationException.INVALID_INPUT,
                assertThrows(CalculationException.class, () -> f(MathFunction.SQUARE_ROOT, "-1")).key());
    }

    @Test
    void logarithms() throws CalculationException {
        assertEquals("3", f(MathFunction.LOG, "1000"));
        assertEquals("−2", f(MathFunction.LOG, "0.01"));
        assertEquals("10", f(MathFunction.LOG2, "1024"));
        assertEquals("1", degrees.format(degrees.apply(MathFunction.LN, DecimalSystem.E)));
        assertEquals("1,000", f(MathFunction.POW10, "3"));
        assertEquals(CalculationException.INVALID_INPUT,
                assertThrows(CalculationException.class, () -> f(MathFunction.LN, "0")).key());
    }

    @Test
    void factorial() throws CalculationException {
        assertEquals("120", f(MathFunction.FACTORIAL, "5"));
        assertEquals("1", f(MathFunction.FACTORIAL, "0"));
        assertEquals("9.332621544394415e+157", f(MathFunction.FACTORIAL, "100"));
        assertThrows(CalculationException.class, () -> f(MathFunction.FACTORIAL, "2.5"));
    }

    @Test
    void largeAndSmallNumbersInScientificNotation() throws CalculationException {
        assertEquals("1e+20", op(Operator.POWER, "10", "20"));
        assertEquals("1.5e−12", op(Operator.MULTIPLY, "1.5", "0.000000000001"));
        assertEquals("1E-12", degrees.copyText(new BigDecimal("1e-12")));
        assertEquals("1,000,000,000,000,000", op(Operator.POWER, "10", "15"));
        assertEquals(CalculationException.OVERFLOW,
                assertThrows(CalculationException.class, () -> op(Operator.POWER, "10", "10000")).key());
        assertEquals(CalculationException.OVERFLOW,
                assertThrows(CalculationException.class, () -> op(Operator.POWER, "1e5000", "9999")).key());
    }

    @Test
    void sixteenDigitsAreShown() {
        assertEquals("0.3333333333333333", degrees.format(BigDecimal.ONE.divide(new BigDecimal(3), DecimalSystem.CONTEXT)));
        assertEquals("1234567.5", degrees.copyText(new BigDecimal("1234567.5")));
        assertEquals("1234567,5", new DecimalSystem(AngleUnit.DEGREES, Locale.GERMANY, true).copyText(new BigDecimal("1234567.5")));
    }

    @Test
    void groupingCanBeOff() {
        assertEquals("1234567", new DecimalSystem(AngleUnit.DEGREES, Locale.US, false).format(new BigDecimal(1234567)));
    }
}
