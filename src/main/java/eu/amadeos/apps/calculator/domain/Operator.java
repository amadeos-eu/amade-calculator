package eu.amadeos.apps.calculator.domain;

/**
 * The binary operators. Precedence follows mathematics and C: powers before multiplication before addition,
 * then the shifts, then AND, XOR and OR.
 */
public enum Operator {
    ADD("+", 4),
    SUBTRACT("−", 4),
    MULTIPLY("×", 5),
    DIVIDE("÷", 5),
    MOD("mod", 5),
    /** x to the power of y. */
    POWER("^", 7, true),
    /** The y-th root of x: {@code 27 yroot 3} is 3. */
    ROOT("yroot", 7, true),
    /** x times ten to the power of y, the EE key: {@code 1.5 E 3} is 1500. */
    EXPONENT("E", 8, true),
    SHIFT_LEFT("<<", 3),
    SHIFT_RIGHT(">>", 3),
    AND("AND", 2),
    NAND("NAND", 2),
    XOR("XOR", 1),
    OR("OR", 0),
    NOR("NOR", 0);

    private final String symbol;
    private final int precedence;
    private final boolean rightAssociative;

    Operator(String symbol, int precedence) {
        this(symbol, precedence, false);
    }

    Operator(String symbol, int precedence, boolean rightAssociative) {
        this.symbol = symbol;
        this.precedence = precedence;
        this.rightAssociative = rightAssociative;
    }

    /** How the operator is written in the expression line. */
    public String symbol() {
        return symbol;
    }

    public int precedence() {
        return precedence;
    }

    /** True for powers: {@code 2^3^2} is {@code 2^(3^2)}. */
    public boolean rightAssociative() {
        return rightAssociative;
    }
}
