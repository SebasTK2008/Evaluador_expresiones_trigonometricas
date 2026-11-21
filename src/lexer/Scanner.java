package lexer;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Pattern;
import token.Token;
import token.TokenType;

/**
 * Escaner de Toy adaptado: primero obtiene lexemas mediante el AFD,
 * luego los clasifica. No admite comentarios ni identificadores generales.
 */
public class Scanner {
    private static final Pattern NUMBER_PATTERN =
            Pattern.compile("[0-9]+(\\.[0-9]+)?([eE][+-]?[0-9]+)?");
    private static final Map<String, TokenType> RESERVED_WORDS = Map.ofEntries(
            Map.entry("sin", TokenType.TOK_SIN),
            Map.entry("sen", TokenType.TOK_SIN),
            Map.entry("cos", TokenType.TOK_COS),
            Map.entry("tan", TokenType.TOK_TAN),
            Map.entry("cot", TokenType.TOK_COT),
            Map.entry("sec", TokenType.TOK_SEC),
            Map.entry("csc", TokenType.TOK_CSC),
            Map.entry("x", TokenType.TOK_VAR),
            Map.entry("y", TokenType.TOK_VAR),
            Map.entry("z", TokenType.TOK_VAR));
    private static final Map<String, TokenType> SPECIAL_SYMBOLS = Map.of(
            "+", TokenType.TOK_PLUS, "-", TokenType.TOK_MINUS,
            "*", TokenType.TOK_MUL, "/", TokenType.TOK_DIV,
            "^", TokenType.TOK_EXP, "(", TokenType.TOK_LPAR,
            ")", TokenType.TOK_RPAR);

    private final char[] buffer;
    private ArrayList<String> lastLexemas = new ArrayList<>();

    public Scanner(char[] buffer) {
        this.buffer = buffer.clone();
    }

    /** Clasifica los lexemas y agrega la marca interna de fin de entrada. */
    public ArrayList<Token> classifyAllTokens(ArrayList<String> lexemas) {
        ArrayList<Token> tokens = new ArrayList<>();
        for (String lexema : lexemas) {
            tokens.add(classifyToken(lexema));
        }
        tokens.add(new Token(TokenType.TOK_EOF, "\u22a3"));
        return tokens;
    }

    /** Recorre el AFD aplicando la regla del lexema mas largo. */
    public ArrayList<String> getLexemas() {
        Status status = Status.START;
        Automaton automaton = new Automaton();
        StringBuilder lexema = new StringBuilder();
        ArrayList<String> lexemas = new ArrayList<>();
        lastLexemas = lexemas;
        int i = 0;
        int lexemaStart = 0;

        while (i < buffer.length) {
            char c = buffer[i];
            Alphabet alphabet = automaton.classify(c);
            if (status == Status.START && alphabet == Alphabet.WHITE_SPACE) {
                i++;
                continue;
            }
            Status nextStatus = automaton.table[status.ordinal()][alphabet.ordinal()];
            if (nextStatus == Status.STOP) {
                if (!automaton.isAcceptance(status)) {
                    throw new IllegalArgumentException(describeError(status, c, i, lexemaStart));
                }
                lexemas.add(lexema.toString());
                lexema.setLength(0);
                status = Status.START;
                // El caracter actual se reprocesa desde START.
                continue;
            }
            if (lexema.length() == 0) {
                lexemaStart = i;
            }
            lexema.append(c);
            status = nextStatus;
            i++;
        }
        if (status != Status.START) {
            if (!automaton.isAcceptance(status)) {
                throw new IllegalArgumentException(
                        describeError(status, '\0', buffer.length, lexemaStart));
            }
            lexemas.add(lexema.toString());
        }
        return lexemas;
    }

    public ArrayList<String> getLastLexemas() {
        return new ArrayList<>(lastLexemas);
    }

    /** Reconoce solamente las funciones, variables y simbolos del documento. */
    public Token classifyToken(String lexema) {
        if (RESERVED_WORDS.containsKey(lexema)) {
            return new Token(RESERVED_WORDS.get(lexema), lexema);
        }
        if (SPECIAL_SYMBOLS.containsKey(lexema)) {
            return new Token(SPECIAL_SYMBOLS.get(lexema), lexema);
        }
        if (NUMBER_PATTERN.matcher(lexema).matches()) {
            return new Token(TokenType.TOK_NUMBER, lexema);
        }
        throw new IllegalArgumentException("Identificador no reconocido: \"" + lexema + "\"");
    }

    private String describeError(Status status, char c, int position, int lexemaStart) {
        switch (status) {
            case START:
                if (c == '.') {
                    return "El numero debe comenzar con un digito en la posicion " + position;
                }
                return "Caracter no reconocido '" + c + "' en la posicion " + position;
            case IN_DECIMAL_POINT:
                return "Se esperaban digitos despues del punto decimal del numero en la posicion "
                        + lexemaStart;
            case IN_EXPONENT:
                return "Se esperaba un digito o un signo en el exponente del numero en la posicion "
                        + lexemaStart;
            case IN_EXPONENT_SIGN:
                return "Se esperaban digitos despues del signo del exponente del numero en la posicion "
                        + lexemaStart;
            default:
                return "Lexema incompleto en la posicion " + lexemaStart;
        }
    }
}
