package eu.amadeos.apps.calculator.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculatorTest {
    private final Calculator calc = new Calculator(new DecimalSystem(AngleUnit.DEGREES, Locale.US, true));

    /** Types keys: digits, '.', + - * / ^, ( ), = and 'n' for ±, '%' for percent. */
    private Calculator type(String keys) {
        for (char c : keys.toCharArray()) {
            switch (c) {
                case '+' -> calc.operator(Operator.ADD);
                case '-' -> calc.operator(Operator.SUBTRACT);
                case '*' -> calc.operator(Operator.MULTIPLY);
                case '/' -> calc.operator(Operator.DIVIDE);
                case '^' -> calc.operator(Operator.POWER);
                case '(' -> calc.open();
                case ')' -> calc.close();
                case '=' -> calc.equals();
                case '.' -> calc.point();
                case 'n' -> calc.negate();
                case '%' -> calc.function(MathFunction.PERCENT);
                case ' ' -> {
                }
                default -> calc.digit(c);
            }
        }
        return calc;
    }

    @Test
    void decimalFractionsAreExact() {
        type("0.1 + 0.2 =");
        assertEquals("0.3", calc.displayText());
        calc.clear();
        type("1 / 3 * 3 =");
        assertEquals("1", calc.displayText());
    }

    @Test
    void multiplicationBeforeAddition() {
        type("2 + 3 * 4 =");
        assertEquals("14", calc.displayText());
        assertEquals("2 + 3 × 4 =", calc.expressionText());
    }

    @Test
    void bracketsAndPowers() {
        type("(2 + 3) * 4 =");
        assertEquals("20", calc.displayText());
        calc.clear();
        type("2 ^ 3 ^ 2 =");
        assertEquals("512", calc.displayText());
        calc.clear();
        type("2 * (3 + 4 =");
        assertEquals("14", calc.displayText());
        assertEquals("2 × (3 + 4) =", calc.expressionText());
    }

    @Test
    void displayShowsTheValueSoFar() {
        type("2 + 3 *");
        assertEquals("3", calc.displayText());
        assertEquals("2 + 3 ×", calc.expressionText());
        type("4 +");
        assertEquals("14", calc.displayText());
    }

    @Test
    void anOperatorReplacesTheLastOne() {
        type("5 + - 2 =");
        assertEquals("3", calc.displayText());
    }

    @Test
    void equalsAgainRepeatsTheLastOperation() {
        type("2 + 3 = = =");
        assertEquals("11", calc.displayText());
        assertEquals("8 + 3 =", calc.expressionText());
    }

    @Test
    void afterEqualsADigitStartsAnewAndAnOperatorGoesOn() {
        type("2 + 3 = 7");
        assertEquals("7", calc.displayText());
        assertEquals("", calc.expressionText());
        calc.clear();
        type("2 + 3 = * 2 =");
        assertEquals("10", calc.displayText());
    }

    @Test
    void functionsApplyAtOnce() {
        type("16");
        calc.function(MathFunction.SQUARE_ROOT);
        assertEquals("4", calc.displayText());
        assertEquals("√(16)", calc.expressionText());
        type("+ 30");
        calc.function(MathFunction.SIN);
        assertEquals("0.5", calc.displayText());
        type("=");
        assertEquals("4.5", calc.displayText());
        assertEquals("√(16) + sin(30) =", calc.expressionText());
    }

    @Test
    void aFunctionOfABracket() {
        type("(1 + 2)");
        calc.function(MathFunction.SQUARE);
        assertEquals("9", calc.displayText());
        assertEquals("(1 + 2)²", calc.expressionText());
    }

    @Test
    void aDigitAfterAFunctionReplacesIt() {
        type("2 + 9");
        calc.function(MathFunction.SQUARE_ROOT);
        type("5 =");
        assertEquals("7", calc.displayText());
    }

    @Test
    void percentAsOnADeskCalculator() {
        type("200 + 10 % =");
        assertEquals("220", calc.displayText());
        calc.clear();
        type("100 * 19 % =");
        assertEquals("19", calc.displayText());
        calc.clear();
        type("50 %");
        assertEquals("0.5", calc.displayText());
    }

    @Test
    void negateTypingAndResults() {
        type("5 n");
        assertEquals("−5", calc.displayText());
        type("+ 2 =");
        assertEquals("−3", calc.displayText());
        type("n");
        assertEquals("3", calc.displayText());
        assertEquals("−(−3)", calc.expressionText());
    }

    @Test
    void groupingAndEntryAsTyped() {
        type("1234567.50");
        assertEquals("1,234,567.50", calc.displayText());
        calc.backspace();
        calc.backspace();
        calc.backspace();
        assertEquals("1,234,567", calc.displayText());
    }

    @Test
    void divisionByZeroIsAnErrorUntilTheNextKey() {
        type("5 / 0 =");
        assertEquals(Optional.of(CalculationException.DIVISION_BY_ZERO), calc.error());
        assertEquals(CalculationException.DIVISION_BY_ZERO, calc.displayText());
        type("7");
        assertTrue(calc.error().isEmpty());
        assertEquals("7", calc.displayText());
    }

    @Test
    void clearEntryKeepsTheExpression() {
        type("12 + 34");
        calc.clearEntry();
        type("5 =");
        assertEquals("17", calc.displayText());
    }

    @Test
    void memory() {
        type("5");
        calc.memoryAdd();
        type("3");
        calc.memoryAdd();
        assertTrue(calc.hasMemory());
        calc.clear();
        type("2 *");
        calc.memoryRecall();
        type("=");
        assertEquals("16", calc.displayText());
        calc.memoryClear();
        assertFalse(calc.hasMemory());
    }

    @Test
    void piAndAnswer() {
        calc.insert(DecimalSystem.PI, new Term.Named("π"));
        assertEquals("3.141592653589793", calc.displayText());
        type("* 2 =");
        assertEquals("π × 2 =", calc.expressionText());
        calc.clear();
        type("6 =");
        calc.insert(calc.answer(), new Term.Named("Ans"));
        assertEquals("6", calc.displayText());
    }

    @Test
    void tapeEntryForEachResult() {
        type("1234.5 * 3");
        Optional<TapeEntry> entry = calc.equals();
        assertEquals(Optional.of(new TapeEntry("1,234.5 × 3", "3,703.5")), entry);
    }

    @Test
    void germanSeparators() {
        Calculator german = new Calculator(new DecimalSystem(AngleUnit.DEGREES, Locale.GERMANY, true));
        for (char c : "12345".toCharArray()) german.digit(c);
        german.point();
        german.digit('5');
        assertEquals("12.345,5", german.displayText());
    }

    @Test
    void switchingToProgrammerKeepsTheWholeNumber() {
        type("255.7");
        calc.changeSystem(new ProgrammerSystem(Radix.HEX, WordSize.BYTE, false, Locale.US, true));
        assertEquals("FF", calc.displayText());
        calc.changeNotation(new ProgrammerSystem(Radix.BIN, WordSize.BYTE, false, Locale.US, true));
        assertEquals("1111 1111", calc.displayText());
        assertEquals(new BigDecimal(255), calc.value());
    }

    @Test
    void theEntryFollowsTheNumberBase() {
        Calculator programmer = new Calculator(new ProgrammerSystem(Radix.HEX, WordSize.WORD, false, Locale.US, false));
        programmer.digit('f');
        programmer.digit('F');
        assertEquals("FF", programmer.displayText());
        programmer.changeNotation(new ProgrammerSystem(Radix.DEC, WordSize.WORD, false, Locale.US, false));
        assertEquals("255", programmer.displayText());
        programmer.digit('1');
        assertEquals("2551", programmer.displayText());
    }

    @Test
    void programmerDigitsBeyondTheWordSizeAreIgnored() {
        Calculator programmer = new Calculator(new ProgrammerSystem(Radix.HEX, WordSize.BYTE, true, Locale.US, false));
        programmer.digit('F');
        programmer.digit('F');
        programmer.digit('F');
        assertEquals("FF", programmer.displayText());
        assertEquals(new BigDecimal(-1), programmer.value());
        programmer.digit('9');
        assertEquals("FF", programmer.displayText());
    }

    @Test
    void programmerOperatorsWithPrecedence() {
        Calculator programmer = new Calculator(new ProgrammerSystem(Radix.HEX, WordSize.QWORD, true, Locale.US, false));
        for (char c : "FF00".toCharArray()) programmer.digit(c);
        programmer.operator(Operator.OR);
        programmer.digit('2');
        programmer.digit('A');
        programmer.equals();
        assertEquals("FF2A", programmer.displayText());
        assertEquals("FF00 OR 2A =", programmer.expressionText());
    }
}
