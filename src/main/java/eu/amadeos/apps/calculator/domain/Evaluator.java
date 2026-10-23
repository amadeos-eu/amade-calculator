package eu.amadeos.apps.calculator.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Computes an expression with precedence: {@code 2 + 3 × 4} is 14. Brackets that are still open at the end count
 * as closed, so {@code 2 × (3 + 4} is 14 as well.
 */
public final class Evaluator {
    private final List<Token> tokens;
    private final NumberSystem system;
    private int next;

    private Evaluator(List<Token> tokens, NumberSystem system) {
        this.tokens = tokens;
        this.system = system;
    }

    public static BigDecimal evaluate(List<Token> tokens, NumberSystem system) throws CalculationException {
        Evaluator evaluator = new Evaluator(tokens, system);
        BigDecimal value = evaluator.expression(0);
        if (evaluator.next < tokens.size()) throw new CalculationException(CalculationException.INVALID_INPUT);
        return system.normalize(value);
    }

    private BigDecimal expression(int minimum) throws CalculationException {
        BigDecimal left = primary();
        while (next < tokens.size() && tokens.get(next) instanceof Token.Op(Operator operator)
                && operator.precedence() >= minimum) {
            next++;
            BigDecimal right = expression(operator.rightAssociative() ? operator.precedence() : operator.precedence() + 1);
            left = system.apply(operator, left, right);
        }
        return left;
    }

    private BigDecimal primary() throws CalculationException {
        if (next >= tokens.size()) throw new CalculationException(CalculationException.INVALID_INPUT);
        Token token = tokens.get(next++);
        return switch (token) {
            case Token.Operand operand -> operand.value();
            case Token.Open open -> {
                BigDecimal inside = expression(0);
                if (next < tokens.size()) {
                    if (!(tokens.get(next) instanceof Token.Close)) {
                        throw new CalculationException(CalculationException.INVALID_INPUT);
                    }
                    next++;
                }
                yield inside;
            }
            default -> throw new CalculationException(CalculationException.INVALID_INPUT);
        };
    }
}
