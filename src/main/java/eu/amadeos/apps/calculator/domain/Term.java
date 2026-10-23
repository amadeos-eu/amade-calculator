package eu.amadeos.apps.calculator.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * How an operand of the expression came about, so that the expression line can show {@code sin(30)} rather
 * than 0.5. Numbers are kept as values and written in the current number base and language.
 */
public sealed interface Term {
    record Number(BigDecimal value) implements Term {
    }

    /** A constant or a remembered value under its name: π, e, Ans. */
    record Named(String name) implements Term {
    }

    record Call(MathFunction function, Term argument) implements Term {
    }

    /** A bracketed part of the expression, as a function's argument. */
    record Group(List<Token> tokens) implements Term {
        public Group {
            tokens = List.copyOf(tokens);
        }
    }
}
