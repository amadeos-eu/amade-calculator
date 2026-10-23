package eu.amadeos.apps.calculator.domain;

import java.math.BigDecimal;
import java.util.Objects;

/** One part of the expression being entered: a number, an operator or a bracket. */
public sealed interface Token {
    Token OPEN = new Open();
    Token CLOSE = new Close();

    /** A number with how it came about, for the expression line. */
    record Operand(BigDecimal value, Term term) implements Token {
        public Operand {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(term, "term");
        }

        public static Operand of(BigDecimal value) {
            return new Operand(value, new Term.Number(value));
        }
    }

    record Op(Operator operator) implements Token {
        public Op {
            Objects.requireNonNull(operator, "operator");
        }
    }

    record Open() implements Token {
    }

    record Close() implements Token {
    }
}
