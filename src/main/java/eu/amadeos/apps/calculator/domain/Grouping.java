package eu.amadeos.apps.calculator.domain;

/** Puts a separator between groups of digits, counted from the right. */
final class Grouping {
    private Grouping() {
    }

    static String group(String digits, int size, String separator) {
        if (separator.isEmpty() || digits.length() <= size) return digits;
        StringBuilder out = new StringBuilder(digits.length() + digits.length() / size);
        int first = digits.length() % size;
        if (first > 0) out.append(digits, 0, first);
        for (int i = first; i < digits.length(); i += size) {
            if (!out.isEmpty()) out.append(separator);
            out.append(digits, i, i + size);
        }
        return out.toString();
    }
}
