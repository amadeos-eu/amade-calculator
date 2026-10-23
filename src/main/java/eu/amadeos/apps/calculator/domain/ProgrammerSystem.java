package eu.amadeos.apps.calculator.domain;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Optional;

/**
 * Integers of a word size for the Programmer mode. Every result wraps around like in a CPU register: in a signed
 * byte 127 + 1 is −128. Division drops the fraction, as in C. HEX, OCT and BIN show the bit pattern, so −1 in a
 * signed byte is FF; DEC shows the value.
 */
public final class ProgrammerSystem implements NumberSystem {
    private final Radix radix;
    private final WordSize word;
    private final boolean signed;
    private final boolean grouping;
    private final String decimalGrouping;
    private final BigInteger mask;
    private final BigInteger modulus;

    public ProgrammerSystem(Radix radix, WordSize word, boolean signed, Locale locale, boolean grouping) {
        this.radix = radix;
        this.word = word;
        this.signed = signed;
        this.grouping = grouping;
        this.decimalGrouping = grouping
                ? String.valueOf(DecimalFormatSymbols.getInstance(locale).getGroupingSeparator()) : "";
        this.modulus = BigInteger.ONE.shiftLeft(word.bits());
        this.mask = modulus.subtract(BigInteger.ONE);
    }

    public WordSize word() {
        return word;
    }

    public boolean signed() {
        return signed;
    }

    @Override
    public Radix radix() {
        return radix;
    }

    @Override
    public boolean allowsPoint() {
        return false;
    }

    @Override
    public boolean signedEntry() {
        return false;
    }

    @Override
    public boolean allows(Operator operator) {
        return switch (operator) {
            case POWER, ROOT, EXPONENT -> false;
            default -> true;
        };
    }

    @Override
    public boolean allows(MathFunction function) {
        return switch (function) {
            case NEGATE, NOT, ROTATE_LEFT, ROTATE_RIGHT -> true;
            default -> false;
        };
    }

    /** The largest value that can be entered: 127 in a signed byte, 255 in an unsigned one. */
    private BigInteger largest() {
        return signed && radix == Radix.DEC ? mask.shiftRight(1) : mask;
    }

    @Override
    public Optional<BigDecimal> parse(String digits) {
        if (digits.isEmpty()) return Optional.of(BigDecimal.ZERO);
        BigInteger entered;
        try {
            entered = new BigInteger(digits, radix.base());
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        if (entered.signum() < 0 || entered.compareTo(largest()) > 0) return Optional.empty();
        return Optional.of(fromPattern(entered));
    }

    @Override
    public Optional<String> entryDigits(BigDecimal value) {
        if (radix == Radix.DEC) {
            return value.signum() < 0 ? Optional.empty() : Optional.of(value.toBigInteger().toString());
        }
        return Optional.of(pattern(value).toString(radix.base()).toUpperCase(Locale.ROOT));
    }

    /** The bits of the value as an unsigned number below 2^bits: −1 in a signed byte is 255. */
    public BigInteger pattern(BigDecimal value) {
        return value.toBigInteger().and(mask);
    }

    /** The value of a bit pattern: in signed mode the top bit makes it negative. */
    public BigDecimal fromPattern(BigInteger pattern) {
        BigInteger bits = pattern.and(mask);
        if (signed && bits.testBit(word.bits() - 1)) bits = bits.subtract(modulus);
        return new BigDecimal(bits);
    }

    /** The value with one bit switched, for the bit field. */
    public BigDecimal flipBit(BigDecimal value, int bit) {
        if (bit < 0 || bit >= word.bits()) return normalizeQuietly(value);
        return fromPattern(pattern(value).flipBit(bit));
    }

    private BigDecimal normalizeQuietly(BigDecimal value) {
        return fromPattern(pattern(value));
    }

    @Override
    public BigDecimal apply(Operator operator, BigDecimal left, BigDecimal right) throws CalculationException {
        BigInteger a = normalize(left).toBigInteger();
        BigInteger b = normalize(right).toBigInteger();
        BigInteger pa = pattern(left);
        BigInteger pb = pattern(right);
        BigInteger result = switch (operator) {
            case ADD -> a.add(b);
            case SUBTRACT -> a.subtract(b);
            case MULTIPLY -> a.multiply(b);
            case DIVIDE -> {
                if (b.signum() == 0) throw new CalculationException(CalculationException.DIVISION_BY_ZERO);
                yield a.divide(b);
            }
            case MOD -> {
                if (b.signum() == 0) throw new CalculationException(CalculationException.DIVISION_BY_ZERO);
                yield a.remainder(b);
            }
            case AND -> pa.and(pb);
            case OR -> pa.or(pb);
            case XOR -> pa.xor(pb);
            case NAND -> pa.and(pb).xor(mask);
            case NOR -> pa.or(pb).xor(mask);
            case SHIFT_LEFT -> {
                int n = shiftCount(b);
                yield n >= word.bits() ? BigInteger.ZERO : pa.shiftLeft(n);
            }
            case SHIFT_RIGHT -> {
                // Signed numbers shift arithmetically (the sign stays), unsigned ones logically.
                int n = Math.min(shiftCount(b), word.bits());
                yield signed ? a.shiftRight(n) : pa.shiftRight(n);
            }
            case POWER, ROOT, EXPONENT ->
                    throw new IllegalArgumentException(operator + " is not a programmer operator");
        };
        return fromPattern(result);
    }

    private static int shiftCount(BigInteger count) throws CalculationException {
        if (count.signum() < 0) throw new CalculationException(CalculationException.INVALID_INPUT);
        return count.bitLength() > 16 ? Integer.MAX_VALUE : count.intValue();
    }

    @Override
    public BigDecimal apply(MathFunction function, BigDecimal argument) {
        BigInteger p = pattern(argument);
        int bits = word.bits();
        BigInteger result = switch (function) {
            case NEGATE -> p.negate();
            case NOT -> p.xor(mask);
            case ROTATE_LEFT -> p.shiftLeft(1).or(p.shiftRight(bits - 1));
            case ROTATE_RIGHT -> p.shiftRight(1).or(p.testBit(0) ? BigInteger.ONE.shiftLeft(bits - 1) : BigInteger.ZERO);
            default -> throw new IllegalArgumentException(function + " is not a programmer function");
        };
        return fromPattern(result);
    }

    @Override
    public BigDecimal normalize(BigDecimal value) {
        return normalizeQuietly(value);
    }

    @Override
    public String format(BigDecimal value) {
        return format(value, radix);
    }

    /** The value in any base, for the HEX, DEC, OCT and BIN lines. */
    public String format(BigDecimal value, Radix base) {
        BigDecimal v = normalizeQuietly(value);
        if (base == Radix.DEC) {
            String digits = v.toBigInteger().abs().toString();
            return (v.signum() < 0 ? "−" : "") + Grouping.group(digits, 3, decimalGrouping);
        }
        String digits = pattern(v).toString(base.base()).toUpperCase(Locale.ROOT);
        return Grouping.group(digits, base.group(), grouping ? " " : "");
    }

    @Override
    public String formatEntry(String digits, boolean negative) {
        String text = digits.isEmpty() ? "0" : digits.toUpperCase(Locale.ROOT);
        String separator = radix == Radix.DEC ? decimalGrouping : grouping ? " " : "";
        return (negative ? "−" : "") + Grouping.group(text, radix.group(), separator);
    }

    @Override
    public String copyText(BigDecimal value) {
        BigDecimal v = normalizeQuietly(value);
        if (radix == Radix.DEC) return v.toBigInteger().toString();
        return pattern(v).toString(radix.base()).toUpperCase(Locale.ROOT);
    }
}
