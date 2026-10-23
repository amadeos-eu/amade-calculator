package eu.amadeos.apps.calculator.domain;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * What numbers are and how they are written: decimal fractions for Basic and Scientific ({@link DecimalSystem}),
 * integers of a word size in a number base for Programmer ({@link ProgrammerSystem}). The {@link Calculator}
 * keeps the expression; the number system enters, computes and writes the numbers.
 */
public interface NumberSystem {
    /** The base digits are entered in: {@link Radix#DEC} for decimal fractions. */
    Radix radix();

    /** True if a decimal point can be entered. */
    boolean allowsPoint();

    /** True if the ± key changes the sign of the number being entered rather than negating a result. */
    boolean signedEntry();

    boolean allows(Operator operator);

    boolean allows(MathFunction function);

    /**
     * The value of entered digits (without sign, possibly with a point), or empty if they are too many or no
     * longer fit the word size.
     */
    Optional<BigDecimal> parse(String digits);

    /** The digits that enter the value again, if it can be entered (not a negative programmer number in DEC). */
    Optional<String> entryDigits(BigDecimal value);

    BigDecimal apply(Operator operator, BigDecimal left, BigDecimal right) throws CalculationException;

    BigDecimal apply(MathFunction function, BigDecimal argument) throws CalculationException;

    /** The value as this system keeps it: rounded, or wrapped to the word size; too large values fail. */
    BigDecimal normalize(BigDecimal value) throws CalculationException;

    /** The value for the display: grouped if grouping is on, in the language's decimal separator. */
    String format(BigDecimal value);

    /** The number being entered, as typed (trailing zeros and point stay), grouped like {@link #format}. */
    String formatEntry(String digits, boolean negative);

    /** The value for the clipboard: no grouping, so that other programs read it back. */
    String copyText(BigDecimal value);
}
