package eu.amadeos.apps.calculator.domain;

/** One finished calculation on the paper tape: the expression and its result, both as shown. */
public record TapeEntry(String expression, String result) {
}
