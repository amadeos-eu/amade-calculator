package eu.amadeos.apps.calculator.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * The calculator without its window: the expression entered so far, the number being typed, the last result,
 * the memory and the error state. Each key of the keypad is one method. Numbers are entered, computed and written
 * by the current {@link NumberSystem}.
 *
 * <p>Like a desk calculator, a function (√, sin, x², ...) applies at once to the number in the display, while
 * operators wait for {@code =} and then follow precedence. After {@code =} a digit starts a new calculation and
 * an operator goes on with the result; a second {@code =} repeats the last operation.
 */
public final class Calculator {
    private NumberSystem system;
    private final List<Token> tokens = new ArrayList<>();
    /** The digits being typed (with '.' as point), or null if no number is being typed. */
    private StringBuilder entry;
    private boolean entryNegative;
    private BigDecimal result = BigDecimal.ZERO;
    /** The expression of the last {@code =}, shown with the result until the next key. */
    private List<Token> evaluated;
    private String error;
    private Operator repeatOperator;
    private BigDecimal repeatOperand;
    private BigDecimal memory;
    private BigDecimal answer = BigDecimal.ZERO;

    public Calculator(NumberSystem system) {
        this.system = Objects.requireNonNull(system, "system");
    }

    public NumberSystem system() {
        return system;
    }

    // ---------------------------------------------------------------- what is shown

    /** The text key of the error in the display, if the last calculation failed. */
    public Optional<String> error() {
        return Optional.ofNullable(error);
    }

    /** The upper line: the expression so far, or the last calculation with "=". */
    public String expressionText() {
        if (evaluated != null) return ExpressionText.of(evaluated, system) + " =";
        return ExpressionText.of(tokens, system);
    }

    /** The number in the display: the one being typed, the last result or what the expression gives so far. */
    public BigDecimal value() {
        if (entry != null) return entryValue();
        if (evaluated != null || tokens.isEmpty()) return result;
        if (tokens.getLast() instanceof Token.Operand operand) return operand.value();
        return preview();
    }

    /** The display text; an error shows as its text key, which the window translates. */
    public String displayText() {
        if (error != null) return error;
        if (entry != null) return system.formatEntry(entry.toString(), entryNegative);
        return system.format(value());
    }

    /** True while a number is being typed. */
    public boolean entering() {
        return entry != null;
    }

    public boolean hasMemory() {
        return memory != null;
    }

    /** How many brackets are open, for the display. */
    public int openBrackets() {
        int open = 0;
        for (Token token : tokens) {
            if (token instanceof Token.Open) open++;
            if (token instanceof Token.Close) open--;
        }
        return open;
    }

    private BigDecimal entryValue() {
        BigDecimal value = system.parse(entry.toString()).orElse(BigDecimal.ZERO);
        return entryNegative ? value.negate() : value;
    }

    /**
     * What the expression gives so far. After {@code 2 + 3 ×} that is 3, because the × still applies to the 3;
     * after {@code 2 + 3 × 4 +} it is 14. Inside an open bracket it is the value inside the bracket so far.
     */
    // --8<-- [start:preview]
    private BigDecimal preview() {
        List<Token> complete = new ArrayList<>(tokens);
        Operator pending = null;
        while (!complete.isEmpty() && !(complete.getLast() instanceof Token.Operand)
                && !(complete.getLast() instanceof Token.Close)) {
            if (complete.getLast() instanceof Token.Op(Operator op) && pending == null) pending = op;
            complete.removeLast();
        }
        int start = complete.size();
        int depth = 0;
        // Back over operands (a bracket counts as one) and the operators that bind at least as tightly as the
        // pending one; an opening bracket of the current level ends the part.
        for (int i = complete.size() - 1; i >= 0; i--) {
            Token token = complete.get(i);
            if (token instanceof Token.Close) depth++;
            if (token instanceof Token.Open) {
                if (depth == 0) break;
                depth--;
            }
            if (depth == 0 && token instanceof Token.Op(Operator op) && pending != null
                    && (op.precedence() < pending.precedence()
                    || op.precedence() == pending.precedence() && pending.rightAssociative())) {
                break;
            }
            start = i;
        }
        List<Token> part = complete.subList(start, complete.size());
        if (part.isEmpty()) return BigDecimal.ZERO;
        try {
            return Evaluator.evaluate(part, system);
        } catch (CalculationException e) {
            return BigDecimal.ZERO;
        }
    }
    // --8<-- [end:preview]

    // ---------------------------------------------------------------- entering numbers

    /** A digit 0-9 or, in HEX, A-F; digits the number base does not have are ignored. */
    public void digit(char digit) {
        if (!system.radix().accepts(digit)) return;
        startTyping();
        String digits = entry.toString();
        String candidate = (digits.equals("0") ? String.valueOf(digit) : digits + digit).toUpperCase(Locale.ROOT);
        if (system.parse(candidate).isPresent()) {
            entry.setLength(0);
            entry.append(candidate);
        }
    }

    public void point() {
        if (!system.allowsPoint()) return;
        startTyping();
        if (entry.indexOf(".") < 0) entry.append('.');
    }

    /** Prepares typing a number: clears an error or a finished calculation, removes an operand it replaces. */
    private void startTyping() {
        if (entry != null) return;
        leaveError();
        if (evaluated != null) {
            evaluated = null;
            tokens.clear();
        }
        replaceTrailingOperand();
        entry = new StringBuilder("0");
        entryNegative = false;
    }

    /**
     * A new number after a function result replaces it: after √(16), typing 5 gives 5. After a closing bracket
     * it multiplies: (1 + 2) 3 is (1 + 2) × 3.
     */
    private void replaceTrailingOperand() {
        if (tokens.isEmpty()) return;
        if (tokens.getLast() instanceof Token.Operand) tokens.removeLast();
        else if (tokens.getLast() instanceof Token.Close) tokens.add(new Token.Op(Operator.MULTIPLY));
    }

    public void backspace() {
        if (error != null) {
            clear();
            return;
        }
        if (entry == null) {
            evaluated = null;
            return;
        }
        entry.setLength(entry.length() - 1);
        if (entry.isEmpty()) entry.append('0');
    }

    /** CE: the number being typed becomes 0; the expression stays. */
    public void clearEntry() {
        if (error != null) {
            clear();
            return;
        }
        if (evaluated != null) {
            clear();
            return;
        }
        replaceTrailingOperand();
        entry = new StringBuilder("0");
        entryNegative = false;
    }

    /** C: everything but the memory and Ans. */
    public void clear() {
        tokens.clear();
        entry = null;
        entryNegative = false;
        result = BigDecimal.ZERO;
        evaluated = null;
        error = null;
        repeatOperator = null;
        repeatOperand = null;
    }

    private void leaveError() {
        if (error != null) clear();
    }

    /** Puts a number in place of the display value: π, e, Ans, MR, a pasted number, a bit flipped. */
    public void insert(BigDecimal value, Term term) {
        leaveError();
        if (evaluated != null) {
            evaluated = null;
            tokens.clear();
        }
        entry = null;
        entryNegative = false;
        replaceTrailingOperand();
        try {
            BigDecimal normalized = system.normalize(value);
            tokens.add(new Token.Operand(normalized, term instanceof Term.Number ? new Term.Number(normalized) : term));
        } catch (CalculationException e) {
            fail(e);
        }
    }

    /** A number that can be typed on, e.g. after flipping a bit; falls back to {@link #insert}. */
    public void enter(BigDecimal value) {
        Optional<String> digits = system.entryDigits(value);
        if (digits.isEmpty()) {
            insert(value, new Term.Number(value));
            return;
        }
        boolean wasEntering = entry != null;
        if (!wasEntering) startTyping();
        entry.setLength(0);
        entry.append(digits.get());
        entryNegative = false;
    }

    // ---------------------------------------------------------------- operators and functions

    public void operator(Operator operator) {
        if (!system.allows(operator) || error != null) return;
        if (evaluated != null) {
            tokens.clear();
            tokens.add(new Token.Operand(result, new Term.Number(result)));
            evaluated = null;
        }
        commitEntry();
        if (!tokens.isEmpty() && tokens.getLast() instanceof Token.Op) {
            tokens.set(tokens.size() - 1, new Token.Op(operator));
            return;
        }
        if (tokens.isEmpty() || tokens.getLast() instanceof Token.Open) {
            BigDecimal start = tokens.isEmpty() ? result : BigDecimal.ZERO;
            tokens.add(Token.Operand.of(start));
        }
        tokens.add(new Token.Op(operator));
    }

    private void commitEntry() {
        if (entry == null) return;
        BigDecimal value = entryValue();
        entry = null;
        entryNegative = false;
        tokens.add(Token.Operand.of(value));
    }

    public void open() {
        leaveError();
        if (evaluated != null) {
            evaluated = null;
            tokens.clear();
        }
        commitEntry();
        if (!tokens.isEmpty() && (tokens.getLast() instanceof Token.Operand || tokens.getLast() instanceof Token.Close)) {
            tokens.add(new Token.Op(Operator.MULTIPLY));
        }
        tokens.add(Token.OPEN);
    }

    public void close() {
        if (error != null || openBrackets() <= 0) return;
        commitEntry();
        Token last = tokens.getLast();
        if (last instanceof Token.Open) return;
        if (last instanceof Token.Op) tokens.removeLast();
        tokens.add(Token.CLOSE);
    }

    /** Applies a function to the number in the display, or to the bracket just closed. */
    // --8<-- [start:function]
    public void function(MathFunction function) {
        if (!system.allows(function) || error != null) return;
        try {
            Token.Operand operand = takeOperand();
            BigDecimal value = function == MathFunction.PERCENT ? percent(operand.value())
                    : system.apply(function, operand.value());
            tokens.add(new Token.Operand(value, new Term.Call(function, operand.term())));
        } catch (CalculationException e) {
            fail(e);
        }
    }
    // --8<-- [end:function]

    /**
     * The percent key as on a desk calculator: {@code 200 + 10 %} adds 10 % of 200, so it is 220; after × and ÷
     * the percentage is a fraction, {@code 100 × 19 %} is 19.
     */
    // --8<-- [start:percent]
    private BigDecimal percent(BigDecimal x) throws CalculationException {
        BigDecimal fraction = system.apply(MathFunction.PERCENT, x);
        if (tokens.size() >= 2 && tokens.getLast() instanceof Token.Op(Operator op)
                && (op == Operator.ADD || op == Operator.SUBTRACT)) {
            BigDecimal base = Evaluator.evaluate(tokens.subList(0, tokens.size() - 1), system);
            return system.apply(Operator.MULTIPLY, base, fraction);
        }
        return fraction;
    }
    // --8<-- [end:percent]

    /** Takes the number in the display out of the expression, for a function to replace it. */
    private Token.Operand takeOperand() throws CalculationException {
        if (entry != null) {
            BigDecimal value = entryValue();
            entry = null;
            entryNegative = false;
            return Token.Operand.of(value);
        }
        if (evaluated != null) {
            evaluated = null;
            tokens.clear();
            return Token.Operand.of(result);
        }
        if (!tokens.isEmpty() && tokens.getLast() instanceof Token.Operand operand) {
            tokens.removeLast();
            return operand;
        }
        if (!tokens.isEmpty() && tokens.getLast() instanceof Token.Close) {
            int depth = 0;
            for (int i = tokens.size() - 1; i >= 0; i--) {
                if (tokens.get(i) instanceof Token.Close) depth++;
                if (tokens.get(i) instanceof Token.Open && --depth == 0) {
                    List<Token> inside = List.copyOf(tokens.subList(i + 1, tokens.size() - 1));
                    BigDecimal value = Evaluator.evaluate(inside, system);
                    tokens.subList(i, tokens.size()).clear();
                    return new Token.Operand(value, new Term.Group(inside));
                }
            }
        }
        return Token.Operand.of(value());
    }

    /** ±: changes the sign of the number being typed, or negates the number in the display. */
    public void negate() {
        if (error != null) return;
        if (entry != null && system.signedEntry()) {
            entryNegative = !entryNegative;
            return;
        }
        function(MathFunction.NEGATE);
    }

    /** = : computes the expression; pressed again, repeats the last operation on the result. */
    // --8<-- [start:equals]
    public Optional<TapeEntry> equals() {
        if (error != null) return Optional.empty();
        if (evaluated != null) {
            if (repeatOperator == null) return Optional.empty();
            List<Token> repeat = List.of(Token.Operand.of(result), new Token.Op(repeatOperator),
                    Token.Operand.of(repeatOperand));
            return finish(repeat);
        }
        commitEntry();
        while (!tokens.isEmpty() && (tokens.getLast() instanceof Token.Op || tokens.getLast() instanceof Token.Open)) {
            tokens.removeLast();
        }
        if (tokens.isEmpty()) return Optional.empty();
        for (int i = openBrackets(); i > 0; i--) tokens.add(Token.CLOSE);
        int size = tokens.size();
        if (size >= 3 && tokens.get(size - 2) instanceof Token.Op(Operator op)
                && tokens.get(size - 1) instanceof Token.Operand operand) {
            repeatOperator = op;
            repeatOperand = operand.value();
        } else {
            repeatOperator = null;
            repeatOperand = null;
        }
        return finish(List.copyOf(tokens));
    }
    // --8<-- [end:equals]

    private Optional<TapeEntry> finish(List<Token> expression) {
        try {
            BigDecimal value = Evaluator.evaluate(expression, system);
            tokens.clear();
            evaluated = expression;
            result = value;
            answer = value;
            return Optional.of(new TapeEntry(ExpressionText.of(expression, system), system.format(value)));
        } catch (CalculationException e) {
            fail(e);
            return Optional.empty();
        }
    }

    private void fail(CalculationException e) {
        error = e.key();
        entry = null;
        repeatOperator = null;
    }

    // ---------------------------------------------------------------- memory and Ans

    public void memoryClear() {
        memory = null;
    }

    public void memoryRecall() {
        if (memory != null) insert(memory, new Term.Number(memory));
    }

    public void memoryAdd() {
        memoryUpdate(Operator.ADD);
    }

    public void memorySubtract() {
        memoryUpdate(Operator.SUBTRACT);
    }

    private void memoryUpdate(Operator operator) {
        if (error != null) return;
        try {
            memory = system.apply(operator, memory == null ? BigDecimal.ZERO : memory, value());
        } catch (CalculationException e) {
            fail(e);
        }
        // The number is done: the next digit starts a new one.
        commitEntry();
    }

    /** The result of the last {@code =}. */
    public BigDecimal answer() {
        return answer;
    }

    // ---------------------------------------------------------------- number systems

    /**
     * Another way of writing the same numbers: another angle unit, number base or grouping. The expression stays;
     * the number being typed is written in the new base.
     */
    public void changeNotation(NumberSystem newSystem) {
        if (entry != null && newSystem.radix() != system.radix()) {
            BigDecimal value = entryValue();
            Optional<String> digits = newSystem.entryDigits(value);
            if (digits.isPresent()) {
                entry.setLength(0);
                entry.append(digits.get());
            } else {
                entry = null;
                entryNegative = false;
                tokens.add(Token.Operand.of(value));
            }
        }
        system = Objects.requireNonNull(newSystem, "newSystem");
    }

    /**
     * Other numbers, e.g. from Scientific to Programmer or another word size: only the number in the display
     * stays, converted (the fraction dropped, wrapped to the word size).
     */
    public void changeSystem(NumberSystem newSystem) {
        BigDecimal value = error != null ? BigDecimal.ZERO : value();
        BigDecimal memoryValue = memory;
        clear();
        system = Objects.requireNonNull(newSystem, "newSystem");
        result = convert(value);
        answer = convert(answer);
        memory = memoryValue == null ? null : convert(memoryValue);
    }

    private BigDecimal convert(BigDecimal value) {
        try {
            BigDecimal converted = system.allowsPoint() ? value : new BigDecimal(value.toBigInteger());
            return system.normalize(converted);
        } catch (CalculationException e) {
            return BigDecimal.ZERO;
        }
    }
}
