package eu.amadeos.apps.calculator.ui.main;

import java.util.List;

import static eu.amadeos.apps.calculator.ui.main.Key.*;

/** Which keys each mode has, row by row. */
public final class Keypads {
    /** A block of keys with a number of columns; null leaves a cell empty. */
    public record Block(int columns, List<Key> keys) {
    }

    private Keypads() {
    }

    // --8<-- [start:basic]
    public static final Block BASIC = new Block(4, List.of(
            MEMORY_CLEAR, MEMORY_RECALL, MEMORY_ADD, MEMORY_SUBTRACT,
            CLEAR, BACKSPACE, PERCENT, DIVIDE,
            D7, D8, D9, MULTIPLY,
            D4, D5, D6, SUBTRACT,
            D1, D2, D3, ADD,
            NEGATE, D0, POINT, EQUALS));
    // --8<-- [end:basic]

    public static final Block SCIENTIFIC = new Block(5, List.of(
            SECOND, ANGLE, OPEN, CLOSE, FACTORIAL,
            SQUARE, POWER, SQUARE_ROOT, ROOT, RECIPROCAL,
            SIN, COS, TAN, PI, EULER,
            LN, LOG, EXP, POW10, ABSOLUTE,
            EXPONENT, MOD, RANDOM, ANSWER, CUBE,
            SINH, COSH, TANH, POW2, LOG2));

    public static final Block PROGRAMMER_LOGIC = new Block(6, List.of(
            AND, OR, XOR, NOT, NAND, NOR,
            SHIFT_LEFT, SHIFT_RIGHT, ROTATE_LEFT, ROTATE_RIGHT, MOD, CLEAR));

    public static final Block PROGRAMMER_DIGITS = new Block(6, List.of(
            HEX_A, HEX_B, CLEAR_ENTRY, BACKSPACE, OPEN, CLOSE,
            HEX_C, HEX_D, D7, D8, D9, DIVIDE,
            HEX_E, HEX_F, D4, D5, D6, MULTIPLY,
            MEMORY_CLEAR, MEMORY_RECALL, D1, D2, D3, SUBTRACT,
            MEMORY_ADD, MEMORY_SUBTRACT, NEGATE, D0, EQUALS, ADD));
}
