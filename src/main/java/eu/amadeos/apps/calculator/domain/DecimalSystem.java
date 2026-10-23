package eu.amadeos.apps.calculator.domain;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Optional;

/**
 * Decimal fractions for Basic and Scientific. Addition, subtraction, multiplication and division are exact to
 * 34 digits, so 0.1 + 0.2 is 0.3 and 1 ÷ 3 × 3 is 1. Roots, logarithms and the trigonometric functions go through
 * {@code double} and are rounded to 15 digits, which turns sin(30°) into exactly 0.5.
 */
public final class DecimalSystem implements NumberSystem {
    // --8<-- [start:precision]
    /** The working precision, that of IEEE 754 decimal128. */
    public static final MathContext CONTEXT = new MathContext(34, RoundingMode.HALF_EVEN);
    /** Digits shown in the display; the rest are kept but not shown. */
    public static final int DISPLAY_DIGITS = 16;
    /** Most digits a number can be entered with. */
    public static final int MAX_ENTRY_DIGITS = 32;
    /** Results beyond 10^±9999 are an overflow (or zero). */
    public static final int MAX_EXPONENT = 9999;
    // --8<-- [end:precision]

    private static final MathContext DOUBLE_DIGITS = new MathContext(15, RoundingMode.HALF_EVEN);
    private static final MathContext SHOWN = new MathContext(DISPLAY_DIGITS, RoundingMode.HALF_UP);
    private static final BigDecimal THREE_SIXTY = BigDecimal.valueOf(360);
    private static final BigDecimal ONE_EIGHTY = BigDecimal.valueOf(180);
    private static final BigDecimal NINETY = BigDecimal.valueOf(90);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final double LN10 = Math.log(10);
    private static final double LN2 = Math.log(2);
    /** π and e to the working precision. */
    public static final BigDecimal PI = new BigDecimal("3.141592653589793238462643383279503");
    public static final BigDecimal E = new BigDecimal("2.718281828459045235360287471352662");

    private final AngleUnit angles;
    private final char decimalSeparator;
    private final String groupingSeparator;

    /**
     * @param angles   how sin, cos and tan read their argument
     * @param locale   gives the decimal and grouping separators
     * @param grouping false shows 1234567 instead of 1,234,567
     */
    public DecimalSystem(AngleUnit angles, Locale locale, boolean grouping) {
        this.angles = angles;
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);
        this.decimalSeparator = symbols.getDecimalSeparator();
        this.groupingSeparator = grouping ? String.valueOf(symbols.getGroupingSeparator()) : "";
    }

    public AngleUnit angles() {
        return angles;
    }

    public char decimalSeparator() {
        return decimalSeparator;
    }

    @Override
    public Radix radix() {
        return Radix.DEC;
    }

    @Override
    public boolean allowsPoint() {
        return true;
    }

    @Override
    public boolean signedEntry() {
        return true;
    }

    @Override
    public boolean allows(Operator operator) {
        return switch (operator) {
            case ADD, SUBTRACT, MULTIPLY, DIVIDE, MOD, POWER, ROOT, EXPONENT -> true;
            default -> false;
        };
    }

    @Override
    public boolean allows(MathFunction function) {
        return switch (function) {
            case NOT, ROTATE_LEFT, ROTATE_RIGHT -> false;
            default -> true;
        };
    }

    @Override
    public Optional<BigDecimal> parse(String digits) {
        String text = digits.isEmpty() ? "0" : digits;
        if (text.endsWith(".")) text += "0";
        if (text.chars().filter(Character::isDigit).count() > MAX_ENTRY_DIGITS) return Optional.empty();
        try {
            return Optional.of(new BigDecimal(text));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> entryDigits(BigDecimal value) {
        if (value.signum() < 0) return Optional.empty();
        String plain = value.stripTrailingZeros().toPlainString();
        return plain.length() > MAX_ENTRY_DIGITS + 1 ? Optional.empty() : Optional.of(plain);
    }

    // ---------------------------------------------------------------- operators

    @Override
    public BigDecimal apply(Operator operator, BigDecimal a, BigDecimal b) throws CalculationException {
        BigDecimal result = switch (operator) {
            case ADD -> a.add(b, CONTEXT);
            case SUBTRACT -> a.subtract(b, CONTEXT);
            case MULTIPLY -> a.multiply(b, CONTEXT);
            case DIVIDE -> {
                if (b.signum() == 0) throw new CalculationException(CalculationException.DIVISION_BY_ZERO);
                yield a.divide(b, CONTEXT);
            }
            case MOD -> {
                if (b.signum() == 0) throw new CalculationException(CalculationException.DIVISION_BY_ZERO);
                yield a.remainder(b, CONTEXT);
            }
            case POWER -> power(a, b);
            case ROOT -> root(a, b);
            case EXPONENT -> {
                if (!isInteger(b) || b.abs().compareTo(BigDecimal.valueOf(2L * MAX_EXPONENT)) > 0) {
                    throw new CalculationException(CalculationException.INVALID_INPUT);
                }
                yield a.scaleByPowerOfTen(b.intValueExact());
            }
            default -> throw new IllegalArgumentException(operator + " is not a decimal operator");
        };
        return normalize(result);
    }

    static BigDecimal power(BigDecimal base, BigDecimal exponent) throws CalculationException {
        if (isInteger(exponent) && exponent.abs().compareTo(BigDecimal.valueOf(MAX_EXPONENT)) <= 0) {
            int n = exponent.intValueExact();
            if (base.signum() == 0 && n < 0) throw new CalculationException(CalculationException.DIVISION_BY_ZERO);
            if (base.signum() != 0) {
                // Checked first, so that (10^5000)^9999 does not compute a number with 50 million digits.
                double decimalExponent = n * ln(base.abs()) / LN10;
                if (decimalExponent > MAX_EXPONENT + 1) throw new CalculationException(CalculationException.OVERFLOW);
                if (decimalExponent < -MAX_EXPONENT - 1) return BigDecimal.ZERO;
            }
            return base.pow(n, CONTEXT);
        }
        if (base.signum() < 0) throw new CalculationException(CalculationException.INVALID_INPUT);
        if (base.signum() == 0) {
            if (exponent.signum() < 0) throw new CalculationException(CalculationException.DIVISION_BY_ZERO);
            return BigDecimal.ZERO;
        }
        double direct = Math.pow(base.doubleValue(), exponent.doubleValue());
        if (Double.isFinite(direct) && direct != 0 && Double.isFinite(base.doubleValue())) return fromDouble(direct);
        return exp(ln(base) * exponent.doubleValue());
    }

    static BigDecimal root(BigDecimal radicand, BigDecimal degree) throws CalculationException {
        if (degree.signum() == 0) throw new CalculationException(CalculationException.INVALID_INPUT);
        if (isInteger(degree) && degree.abs().compareTo(BigDecimal.valueOf(MAX_EXPONENT)) <= 0) {
            int n = degree.intValueExact();
            if (radicand.signum() < 0) {
                if (n % 2 == 0) throw new CalculationException(CalculationException.INVALID_INPUT);
                return root(radicand.negate(), degree).negate();
            }
            if (n < 0) {
                BigDecimal positive = root(radicand, degree.negate());
                if (positive.signum() == 0) throw new CalculationException(CalculationException.DIVISION_BY_ZERO);
                return BigDecimal.ONE.divide(positive, CONTEXT);
            }
            if (n == 1) return radicand;
            if (n == 2) return radicand.sqrt(CONTEXT);
            if (radicand.signum() == 0) return BigDecimal.ZERO;
            BigDecimal approximate = exp(ln(radicand) / n);
            // Exact roots stay exact: the cube root of 27 is 3, not 3.00000000000000.
            for (MathContext digits : new MathContext[]{new MathContext(12), new MathContext(15)}) {
                BigDecimal candidate = approximate.round(digits).stripTrailingZeros();
                if (candidate.pow(n, CONTEXT).compareTo(radicand) == 0) return candidate;
            }
            return approximate;
        }
        if (radicand.signum() < 0) throw new CalculationException(CalculationException.INVALID_INPUT);
        if (radicand.signum() == 0) return BigDecimal.ZERO;
        return exp(ln(radicand) / degree.doubleValue());
    }

    // ---------------------------------------------------------------- functions

    @Override
    public BigDecimal apply(MathFunction function, BigDecimal x) throws CalculationException {
        BigDecimal result = switch (function) {
            case NEGATE -> x.negate();
            case PERCENT -> x.divide(HUNDRED, CONTEXT);
            case SQUARE -> x.multiply(x, CONTEXT);
            case CUBE -> power(x, BigDecimal.valueOf(3));
            case SQUARE_ROOT -> {
                if (x.signum() < 0) throw new CalculationException(CalculationException.INVALID_INPUT);
                yield x.sqrt(CONTEXT);
            }
            case RECIPROCAL -> {
                if (x.signum() == 0) throw new CalculationException(CalculationException.DIVISION_BY_ZERO);
                yield BigDecimal.ONE.divide(x, CONTEXT);
            }
            case FACTORIAL -> factorial(x);
            case ABSOLUTE -> x.abs();
            case SIN, COS, TAN -> trigonometric(function, x);
            case ASIN, ACOS -> {
                if (x.abs().compareTo(BigDecimal.ONE) > 0) {
                    throw new CalculationException(CalculationException.INVALID_INPUT);
                }
                double radians = function == MathFunction.ASIN ? Math.asin(x.doubleValue()) : Math.acos(x.doubleValue());
                yield angle(radians);
            }
            case ATAN -> angle(Math.atan(x.doubleValue()));
            case SINH -> fromDouble(Math.sinh(x.doubleValue()));
            case COSH -> fromDouble(Math.cosh(x.doubleValue()));
            case TANH -> fromDouble(Math.tanh(x.doubleValue()));
            case ASINH -> {
                double v = x.doubleValue();
                yield fromDouble(Math.copySign(Math.log(Math.abs(v) + Math.sqrt(v * v + 1)), v));
            }
            case ACOSH -> {
                if (x.compareTo(BigDecimal.ONE) < 0) throw new CalculationException(CalculationException.INVALID_INPUT);
                double v = x.doubleValue();
                yield fromDouble(Math.log(v + Math.sqrt(v * v - 1)));
            }
            case ATANH -> {
                if (x.abs().compareTo(BigDecimal.ONE) >= 0) {
                    throw new CalculationException(CalculationException.INVALID_INPUT);
                }
                double v = x.doubleValue();
                yield fromDouble(0.5 * Math.log((1 + v) / (1 - v)));
            }
            case LN -> {
                requirePositive(x);
                yield fromDouble(ln(x));
            }
            case LOG -> {
                requirePositive(x);
                BigDecimal stripped = x.stripTrailingZeros();
                if (stripped.unscaledValue().equals(BigInteger.ONE)) yield BigDecimal.valueOf(-stripped.scale());
                yield fromDouble(ln(x) / LN10);
            }
            case LOG2 -> {
                requirePositive(x);
                if (isInteger(x) && x.toBigInteger().bitCount() == 1) {
                    yield BigDecimal.valueOf(x.toBigInteger().bitLength() - 1L);
                }
                yield fromDouble(ln(x) / LN2);
            }
            case EXP -> exp(x.doubleValue());
            case POW10 -> power(BigDecimal.TEN, x);
            case POW2 -> power(BigDecimal.TWO, x);
            case NOT, ROTATE_LEFT, ROTATE_RIGHT ->
                    throw new IllegalArgumentException(function + " is not a decimal function");
        };
        return normalize(result);
    }

    // --8<-- [start:trigonometric]
    private BigDecimal trigonometric(MathFunction function, BigDecimal x) throws CalculationException {
        double radians;
        if (angles == AngleUnit.DEGREES) {
            // Whole multiples of 90° are exact, and tan(90°) has no value.
            BigDecimal degrees = x.remainder(THREE_SIXTY);
            boolean straight = degrees.remainder(ONE_EIGHTY).signum() == 0;
            boolean right = degrees.subtract(NINETY).remainder(ONE_EIGHTY).signum() == 0;
            if ((function == MathFunction.SIN || function == MathFunction.TAN) && straight) return BigDecimal.ZERO;
            if (function == MathFunction.COS && right) return BigDecimal.ZERO;
            if (function == MathFunction.TAN && right) throw new CalculationException(CalculationException.INVALID_INPUT);
            radians = Math.toRadians(degrees.doubleValue());
        } else {
            radians = x.doubleValue();
        }
        double value = switch (function) {
            case SIN -> Math.sin(radians);
            case COS -> Math.cos(radians);
            default -> Math.tan(radians);
        };
        // sin(π) is 1.2E-16 in double; values that small are rounding noise of a zero.
        if (function != MathFunction.TAN && Math.abs(value) < 1e-15) return BigDecimal.ZERO;
        return fromDouble(value);
    }
    // --8<-- [end:trigonometric]

    private BigDecimal angle(double radians) throws CalculationException {
        return fromDouble(angles == AngleUnit.DEGREES ? Math.toDegrees(radians) : radians);
    }

    private static BigDecimal factorial(BigDecimal x) throws CalculationException {
        if (!isInteger(x) || x.signum() < 0) throw new CalculationException(CalculationException.INVALID_INPUT);
        if (x.compareTo(BigDecimal.valueOf(3000)) > 0) throw new CalculationException(CalculationException.OVERFLOW);
        int n = x.intValueExact();
        BigInteger product = BigInteger.ONE;
        for (int i = 2; i <= n; i++) product = product.multiply(BigInteger.valueOf(i));
        return new BigDecimal(product).round(CONTEXT);
    }

    private static void requirePositive(BigDecimal x) throws CalculationException {
        if (x.signum() <= 0) throw new CalculationException(CalculationException.INVALID_INPUT);
    }

    /** The natural logarithm, also of numbers beyond the range of double. */
    private static double ln(BigDecimal positive) {
        int exponent = exponent(positive);
        double mantissa = positive.movePointLeft(exponent).doubleValue();
        return Math.log(mantissa) + exponent * LN10;
    }

    /** e to the power of x, also for results beyond the range of double. */
    private static BigDecimal exp(double x) throws CalculationException {
        if (Double.isNaN(x)) throw new CalculationException(CalculationException.INVALID_INPUT);
        double decimalExponent = x / LN10;
        if (decimalExponent > MAX_EXPONENT + 1) throw new CalculationException(CalculationException.OVERFLOW);
        if (decimalExponent < -MAX_EXPONENT - 1) return BigDecimal.ZERO;
        if (Math.abs(decimalExponent) < 300) return fromDouble(Math.exp(x));
        double whole = Math.floor(decimalExponent);
        return fromDouble(Math.pow(10, decimalExponent - whole)).scaleByPowerOfTen((int) whole);
    }

    /** A double result, rounded to the 15 digits a double holds reliably. */
    // --8<-- [start:from-double]
    static BigDecimal fromDouble(double value) throws CalculationException {
        if (Double.isNaN(value)) throw new CalculationException(CalculationException.INVALID_INPUT);
        if (Double.isInfinite(value)) throw new CalculationException(CalculationException.OVERFLOW);
        return new BigDecimal(value).round(DOUBLE_DIGITS).stripTrailingZeros();
    }
    // --8<-- [end:from-double]

    static boolean isInteger(BigDecimal value) {
        return value.signum() == 0 || value.stripTrailingZeros().scale() <= 0;
    }

    /** The decimal exponent: 2 for 345, -3 for 0.00345. */
    static int exponent(BigDecimal nonZero) {
        return nonZero.precision() - nonZero.scale() - 1;
    }

    @Override
    public BigDecimal normalize(BigDecimal value) throws CalculationException {
        if (value.signum() == 0) return BigDecimal.ZERO;
        BigDecimal rounded = value.round(CONTEXT);
        int exponent = exponent(rounded);
        if (exponent > MAX_EXPONENT) throw new CalculationException(CalculationException.OVERFLOW);
        if (exponent < -MAX_EXPONENT) return BigDecimal.ZERO;
        return rounded;
    }

    // ---------------------------------------------------------------- text

    @Override
    public String format(BigDecimal value) {
        return write(value, groupingSeparator, "−", "e");
    }

    @Override
    public String copyText(BigDecimal value) {
        return write(value, "", "-", "E");
    }

    private String write(BigDecimal value, String grouping, String minus, String exponentMark) {
        BigDecimal shown = value.round(SHOWN).stripTrailingZeros();
        if (shown.signum() == 0) return "0";
        String sign = shown.signum() < 0 ? minus : "";
        shown = shown.abs();
        int exponent = exponent(shown);
        if (exponent >= DISPLAY_DIGITS || exponent < -9) {
            String mantissa = plain(shown.movePointLeft(exponent), "");
            return sign + mantissa + exponentMark + (exponent > 0 ? "+" + exponent : minus + (-exponent));
        }
        return sign + plain(shown, grouping);
    }

    private String plain(BigDecimal nonNegative, String grouping) {
        String text = nonNegative.toPlainString();
        int point = text.indexOf('.');
        String whole = point < 0 ? text : text.substring(0, point);
        String fraction = point < 0 ? "" : text.substring(point + 1);
        String grouped = Grouping.group(whole, 3, grouping);
        return fraction.isEmpty() ? grouped : grouped + decimalSeparator + fraction;
    }

    @Override
    public String formatEntry(String digits, boolean negative) {
        String text = digits.isEmpty() ? "0" : digits;
        int point = text.indexOf('.');
        String whole = point < 0 ? text : text.substring(0, point);
        String rest = point < 0 ? "" : decimalSeparator + text.substring(point + 1);
        return (negative ? "−" : "") + Grouping.group(whole, 3, groupingSeparator) + rest;
    }
}
