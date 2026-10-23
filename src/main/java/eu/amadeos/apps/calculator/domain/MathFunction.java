package eu.amadeos.apps.calculator.domain;

/**
 * The functions of one number. They apply at once to the number in the display, and the expression line shows
 * them with their argument, e.g. {@code sin(30)} or {@code 4²}.
 */
public enum MathFunction {
    NEGATE("−(%s)"),
    PERCENT("%s%%"),
    SQUARE("%s²"),
    CUBE("%s³"),
    SQUARE_ROOT("√(%s)"),
    RECIPROCAL("1/(%s)"),
    FACTORIAL("%s!"),
    ABSOLUTE("|%s|"),
    SIN("sin(%s)"),
    COS("cos(%s)"),
    TAN("tan(%s)"),
    ASIN("sin⁻¹(%s)"),
    ACOS("cos⁻¹(%s)"),
    ATAN("tan⁻¹(%s)"),
    SINH("sinh(%s)"),
    COSH("cosh(%s)"),
    TANH("tanh(%s)"),
    ASINH("sinh⁻¹(%s)"),
    ACOSH("cosh⁻¹(%s)"),
    ATANH("tanh⁻¹(%s)"),
    LN("ln(%s)"),
    LOG("log(%s)"),
    LOG2("log₂(%s)"),
    EXP("e^(%s)"),
    POW10("10^(%s)"),
    POW2("2^(%s)"),
    NOT("NOT(%s)"),
    ROTATE_LEFT("RoL(%s)"),
    ROTATE_RIGHT("RoR(%s)");

    private final String pattern;

    MathFunction(String pattern) {
        this.pattern = pattern;
    }

    /** The text in the expression line with the argument for {@code %s}. */
    public String pattern() {
        return pattern;
    }

    /** True if the argument comes first, as in {@code 4²}; a negative argument then needs brackets. */
    public boolean postfix() {
        return pattern.startsWith("%s");
    }

    /** The function that undoes this one, for the 2nd key: sin becomes sin⁻¹. Itself if there is none. */
    public MathFunction inverse() {
        return switch (this) {
            case SIN -> ASIN;
            case COS -> ACOS;
            case TAN -> ATAN;
            case SINH -> ASINH;
            case COSH -> ACOSH;
            case TANH -> ATANH;
            case ASIN -> SIN;
            case ACOS -> COS;
            case ATAN -> TAN;
            case ASINH -> SINH;
            case ACOSH -> COSH;
            case ATANH -> TANH;
            default -> this;
        };
    }
}
