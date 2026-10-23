package eu.amadeos.apps.calculator.ui.main;

import java.util.Locale;

/** The keys of the keypads. Labels are symbols and stay the same in every language; the tooltips are translated. */
public enum Key {
    D0("0", Kind.DIGIT), D1("1", Kind.DIGIT), D2("2", Kind.DIGIT), D3("3", Kind.DIGIT), D4("4", Kind.DIGIT),
    D5("5", Kind.DIGIT), D6("6", Kind.DIGIT), D7("7", Kind.DIGIT), D8("8", Kind.DIGIT), D9("9", Kind.DIGIT),
    HEX_A("A", Kind.DIGIT), HEX_B("B", Kind.DIGIT), HEX_C("C", Kind.DIGIT), HEX_D("D", Kind.DIGIT),
    HEX_E("E", Kind.DIGIT), HEX_F("F", Kind.DIGIT),
    /** The label is the language's decimal separator. */
    POINT(".", Kind.DIGIT),
    ADD("+", Kind.OPERATOR), SUBTRACT("−", Kind.OPERATOR), MULTIPLY("×", Kind.OPERATOR),
    DIVIDE("÷", Kind.OPERATOR), PERCENT("%", Kind.OPERATOR), EQUALS("=", Kind.EQUALS),
    CLEAR("C", Kind.FUNCTION), CLEAR_ENTRY("CE", Kind.FUNCTION), BACKSPACE("⌫", Kind.FUNCTION),
    NEGATE("±", Kind.FUNCTION),
    MEMORY_CLEAR("MC", Kind.MEMORY), MEMORY_RECALL("MR", Kind.MEMORY), MEMORY_ADD("M+", Kind.MEMORY),
    MEMORY_SUBTRACT("M−", Kind.MEMORY),
    SECOND("2nd", Kind.FUNCTION), ANGLE("DEG", Kind.FUNCTION), OPEN("(", Kind.FUNCTION), CLOSE(")", Kind.FUNCTION),
    FACTORIAL("n!", Kind.FUNCTION), SQUARE("x²", Kind.FUNCTION), POWER("xʸ", Kind.FUNCTION),
    SQUARE_ROOT("√x", Kind.FUNCTION), ROOT("ʸ√x", Kind.FUNCTION), RECIPROCAL("1/x", Kind.FUNCTION),
    SIN("sin", "sin⁻¹"), COS("cos", "cos⁻¹"), TAN("tan", "tan⁻¹"),
    PI("π", Kind.FUNCTION), EULER("e", Kind.FUNCTION),
    LN("ln", Kind.FUNCTION), LOG("log", Kind.FUNCTION), EXP("eˣ", Kind.FUNCTION), POW10("10ˣ", Kind.FUNCTION),
    ABSOLUTE("|x|", Kind.FUNCTION),
    EXPONENT("EE", Kind.FUNCTION), MOD("mod", Kind.FUNCTION), RANDOM("Rnd", Kind.FUNCTION), ANSWER("Ans", Kind.FUNCTION),
    CUBE("x³", Kind.FUNCTION),
    SINH("sinh", "sinh⁻¹"), COSH("cosh", "cosh⁻¹"), TANH("tanh", "tanh⁻¹"),
    POW2("2ˣ", Kind.FUNCTION), LOG2("log₂", Kind.FUNCTION),
    AND("AND", Kind.FUNCTION), OR("OR", Kind.FUNCTION), XOR("XOR", Kind.FUNCTION), NOT("NOT", Kind.FUNCTION),
    NAND("NAND", Kind.FUNCTION), NOR("NOR", Kind.FUNCTION), SHIFT_LEFT("<<", Kind.FUNCTION),
    SHIFT_RIGHT(">>", Kind.FUNCTION), ROTATE_LEFT("RoL", Kind.FUNCTION), ROTATE_RIGHT("RoR", Kind.FUNCTION);

    /** How a key looks: digits light, operators a little darker, = in the selection colour. */
    public enum Kind { DIGIT, OPERATOR, FUNCTION, MEMORY, EQUALS }

    private final String label;
    private final String secondLabel;
    private final Kind kind;

    Key(String label, Kind kind) {
        this.label = label;
        this.secondLabel = label;
        this.kind = kind;
    }

    Key(String label, String secondLabel) {
        this.label = label;
        this.secondLabel = secondLabel;
        this.kind = Kind.FUNCTION;
    }

    public String label() {
        return label;
    }

    /** The label while 2nd is on: the inverse function. */
    public String secondLabel() {
        return secondLabel;
    }

    public Kind kind() {
        return kind;
    }

    /** E.g. {@code square-root}: the component name {@code key.square-root} and the tooltip text key. */
    public String id() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    /** The digit of a digit key, '.' for the point, 0 for other keys. */
    public char digit() {
        return kind == Kind.DIGIT ? label.charAt(0) : 0;
    }

    public static Key forDigit(char digit) {
        return switch (Character.toUpperCase(digit)) {
            case '0' -> D0;
            case '1' -> D1;
            case '2' -> D2;
            case '3' -> D3;
            case '4' -> D4;
            case '5' -> D5;
            case '6' -> D6;
            case '7' -> D7;
            case '8' -> D8;
            case '9' -> D9;
            case 'A' -> HEX_A;
            case 'B' -> HEX_B;
            case 'C' -> HEX_C;
            case 'D' -> HEX_D;
            case 'E' -> HEX_E;
            case 'F' -> HEX_F;
            default -> null;
        };
    }
}
