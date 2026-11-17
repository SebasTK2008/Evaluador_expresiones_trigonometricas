package lexer;

import java.util.EnumSet;
import java.util.Set;

/**
 * AFD tabular de Toy adaptado a la tabla 4 del documento.
 * STOP termina el lexema sin consumir el caracter que causo la transicion.
 */
public class Automaton {
    // Columnas: LETTER, EXPONENT, DIGIT, DOT, PLUS, MINUS, SYMBOL, BLANK, OTHER.
    public final Status[][] table = {
        {Status.IN_IDENTIFIER, Status.IN_IDENTIFIER, Status.IN_INTEGER, Status.STOP, Status.IN_SIMPLE_SYMBOL, Status.IN_SIMPLE_SYMBOL, Status.IN_SIMPLE_SYMBOL, Status.START, Status.STOP},
        {Status.IN_IDENTIFIER, Status.IN_IDENTIFIER, Status.IN_IDENTIFIER, Status.STOP, Status.STOP, Status.STOP, Status.STOP, Status.STOP, Status.STOP},
        {Status.STOP, Status.IN_EXPONENT, Status.IN_INTEGER, Status.IN_DECIMAL_POINT, Status.STOP, Status.STOP, Status.STOP, Status.STOP, Status.STOP},
        {Status.STOP, Status.STOP, Status.IN_DECIMAL, Status.STOP, Status.STOP, Status.STOP, Status.STOP, Status.STOP, Status.STOP},
        {Status.STOP, Status.IN_EXPONENT, Status.IN_DECIMAL, Status.STOP, Status.STOP, Status.STOP, Status.STOP, Status.STOP, Status.STOP},
        {Status.STOP, Status.STOP, Status.IN_EXPONENT_DIGITS, Status.STOP, Status.IN_EXPONENT_SIGN, Status.IN_EXPONENT_SIGN, Status.STOP, Status.STOP, Status.STOP},
        {Status.STOP, Status.STOP, Status.IN_EXPONENT_DIGITS, Status.STOP, Status.STOP, Status.STOP, Status.STOP, Status.STOP, Status.STOP},
        {Status.STOP, Status.STOP, Status.IN_EXPONENT_DIGITS, Status.STOP, Status.STOP, Status.STOP, Status.STOP, Status.STOP, Status.STOP},
        {Status.STOP, Status.STOP, Status.STOP, Status.STOP, Status.STOP, Status.STOP, Status.STOP, Status.STOP, Status.STOP},
        {Status.STOP, Status.STOP, Status.STOP, Status.STOP, Status.STOP, Status.STOP, Status.STOP, Status.STOP, Status.STOP}
    };

    private final Set<Status> acceptance = EnumSet.of(
            Status.IN_IDENTIFIER, Status.IN_INTEGER, Status.IN_DECIMAL,
            Status.IN_EXPONENT_DIGITS, Status.IN_SIMPLE_SYMBOL);

    public Automaton() {
        if (table.length != Status.values().length) {
            throw new IllegalStateException("La tabla debe tener una fila por cada estado.");
        }
        for (Status[] row : table) {
            if (row.length != Alphabet.values().length) {
                throw new IllegalStateException("La tabla debe tener una columna por cada categoria.");
            }
        }
    }

    /** Clasifica caracteres ASCII; e/E tambien pueden formar identificadores. */
    public Alphabet classify(char c) {
        if (c == 'e' || c == 'E') {
            return Alphabet.EXPONENT;
        }
        if (c == '_' || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
            return Alphabet.LETTER;
        }
        if (c >= '0' && c <= '9') {
            return Alphabet.DIGIT;
        }
        if (c == '.') {
            return Alphabet.DOT;
        }
        if (c == '+') {
            return Alphabet.PLUS;
        }
        if (c == '-') {
            return Alphabet.MINUS;
        }
        if (c == '*' || c == '/' || c == '^' || c == '(' || c == ')') {
            return Alphabet.SIMPLE_SYMBOL;
        }
        if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
            return Alphabet.WHITE_SPACE;
        }
        return Alphabet.OTHER;
    }

    public boolean isAcceptance(Status status) {
        return acceptance.contains(status);
    }
}
