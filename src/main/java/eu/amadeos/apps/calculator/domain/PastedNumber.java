package eu.amadeos.apps.calculator.domain;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Reads a number from the clipboard, as people copy them: {@code 1,234.5}, {@code 1.234,5}, {@code 1 234,5},
 * {@code −3}, {@code 1.5E-3}; in Programmer {@code FF2A}, {@code 0xff2a}, {@code 0b1010} or {@code 1111 0000}.
 */
public final class PastedNumber {
    private static final Pattern THOUSANDS = Pattern.compile("-?\\d{1,3}([.,]\\d{3})+");

    private PastedNumber() {
    }

    public static Optional<BigDecimal> parse(String text, NumberSystem system, Locale locale) {
        String cleaned = text.strip().replace('−', '-').replaceAll("[\\s'  _]", "");
        if (cleaned.isEmpty()) return Optional.empty();
        return system instanceof ProgrammerSystem programmer ? integer(cleaned, programmer)
                : decimal(cleaned, DecimalFormatSymbols.getInstance(locale).getDecimalSeparator());
    }

    private static Optional<BigDecimal> decimal(String text, char decimalSeparator) {
        String number = text;
        int dot = number.lastIndexOf('.');
        int comma = number.lastIndexOf(',');
        if (dot >= 0 && comma >= 0) {
            // Both: the one that comes last separates the fraction.
            char point = dot > comma ? '.' : ',';
            char group = point == '.' ? ',' : '.';
            number = number.replace(String.valueOf(group), "").replace(point, '.');
        } else if (dot >= 0 || comma >= 0) {
            char only = dot >= 0 ? '.' : ',';
            boolean grouping = only != decimalSeparator && THOUSANDS.matcher(number).matches();
            number = grouping ? number.replace(String.valueOf(only), "") : number.replace(only, '.');
        }
        try {
            return Optional.of(new BigDecimal(number));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static Optional<BigDecimal> integer(String text, ProgrammerSystem system) {
        String digits = text;
        boolean negative = digits.startsWith("-");
        if (negative) digits = digits.substring(1);
        int base = system.radix().base();
        String lower = digits.toLowerCase(Locale.ROOT);
        if (lower.length() > 2 && lower.charAt(0) == '0' && "xbo".indexOf(lower.charAt(1)) >= 0) {
            base = switch (lower.charAt(1)) {
                case 'x' -> 16;
                case 'b' -> 2;
                default -> 8;
            };
            digits = digits.substring(2);
        }
        if (base == 10) digits = digits.replace(",", "").replace(".", "");
        try {
            BigInteger value = new BigInteger(digits, base);
            if (value.bitLength() > system.word().bits()) return Optional.empty();
            BigDecimal result = negative ? new BigDecimal(value.negate()) : system.fromPattern(value);
            return Optional.of(system.normalize(result));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
