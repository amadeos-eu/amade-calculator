package eu.amadeos.apps.calculator.domain;

import java.util.List;

/** Writes an expression for the expression line and the paper tape: {@code sin(30) + 2 × √(16)}. */
public final class ExpressionText {
    private ExpressionText() {
    }

    public static String of(List<Token> tokens, NumberSystem system) {
        StringBuilder out = new StringBuilder();
        Token previous = null;
        for (Token token : tokens) {
            // Spaces around operators, none inside brackets.
            boolean space = previous != null && !(previous instanceof Token.Open) && !(token instanceof Token.Close);
            if (space) out.append(' ');
            switch (token) {
                case Token.Operand operand -> out.append(term(operand.term(), system));
                case Token.Op op -> out.append(op.operator().symbol());
                case Token.Open open -> out.append('(');
                case Token.Close close -> out.append(')');
            }
            previous = token;
        }
        return out.toString();
    }

    public static String term(Term term, NumberSystem system) {
        return switch (term) {
            case Term.Number number -> system.format(number.value());
            case Term.Named named -> named.name();
            case Term.Group group -> "(" + of(group.tokens(), system) + ")";
            case Term.Call call -> {
                String argument = term(call.argument(), system);
                boolean negative = call.argument() instanceof Term.Number n && n.value().signum() < 0;
                if (call.function().postfix() && (negative || call.argument() instanceof Term.Call inner
                        && !inner.function().postfix() && inner.function().pattern().startsWith("−"))) {
                    argument = "(" + argument + ")";
                }
                yield call.function().pattern().formatted(argument);
            }
        };
    }
}
