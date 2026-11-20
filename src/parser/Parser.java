package parser;

import java.util.List;
import token.Token;
import token.TokenType;

/**
 * Descendente recursivo de Toy adaptado a las 24 producciones de la tabla 6.
 * Las colas de suma y multiplicacion se expresan como ciclos equivalentes.
 * Solo reconoce sintaxis: no evalua ni solicita valores de variables.
 */
public class Parser {
    private final List<Token> tokens;
    private int pos;

    public Parser(List<Token> tokens) {
        this.tokens = List.copyOf(tokens);
        if (this.tokens.isEmpty()
                || this.tokens.get(this.tokens.size() - 1).getTipo() != TokenType.TOK_EOF) {
            throw new IllegalArgumentException("La lista debe terminar con TOK_EOF.");
        }
    }

    /** Reconoce exactamente una expresion seguida de EOF. */
    public void parse() {
        pos = 0;
        expresion();
        consumir(TokenType.TOK_EOF, "el final del archivo");
        if (pos != tokens.size()) {
            throw error("se esperaba el final del archivo");
        }
    }

    private boolean es(TokenType... tipos) {
        TokenType actual = tokens.get(pos).getTipo();
        for (TokenType tipo : tipos) {
            if (actual == tipo) {
                return true;
            }
        }
        return false;
    }

    private void avanzar() {
        pos++;
    }

    private void consumir(TokenType esperado, String descripcion) {
        if (!es(esperado)) {
            throw error("se esperaba " + descripcion);
        }
        avanzar();
    }

    private SyntaxException error(String mensaje) {
        Token token = tokens.get(pos);
        String encontrado = token.getTipo() == TokenType.TOK_EOF
                ? "el final del archivo"
                : "'" + token.getLexema() + "' (token " + (pos + 1) + ")";
        return new SyntaxException("Error sintactico: " + mensaje
                + ", pero se encontro " + encontrado);
    }

    // 1. <expresion> -> <expSuma>
    private void expresion() {
        expSuma();
    }

    // 2-5. <expSuma> -> <expMult> ((+ | -) <expMult>)*
    private void expSuma() {
        expMult();
        while (es(TokenType.TOK_PLUS, TokenType.TOK_MINUS)) {
            avanzar();
            expMult();
        }
    }

    // 6-9. <expMult> -> <expUnaria> ((* | /) <expUnaria>)*
    private void expMult() {
        expUnaria();
        while (es(TokenType.TOK_MUL, TokenType.TOK_DIV)) {
            avanzar();
            expUnaria();
        }
    }

    // 10-11. <expUnaria> -> - <expUnaria> | <expPotencia>
    private void expUnaria() {
        if (es(TokenType.TOK_MINUS)) {
            avanzar();
            expUnaria();
        } else {
            expPotencia();
        }
    }

    // 12-14. <expPotencia> -> <primario> (^ <expUnaria>)?
    // La recursion por expUnaria conserva 2^3^2, -2^2 y 2^-3.
    private void expPotencia() {
        primario();
        if (es(TokenType.TOK_EXP)) {
            avanzar();
            expUnaria();
        }
    }

    // 15-18. <primario> -> ( <expresion> ) | num | var | <funcion> ( <expresion> )
    private void primario() {
        if (es(TokenType.TOK_LPAR)) {
            avanzar();
            expresion();
            consumir(TokenType.TOK_RPAR, "')'");
        } else if (es(TokenType.TOK_NUMBER, TokenType.TOK_VAR)) {
            avanzar();
        } else if (es(TokenType.TOK_SIN, TokenType.TOK_COS, TokenType.TOK_TAN,
                TokenType.TOK_COT, TokenType.TOK_SEC, TokenType.TOK_CSC)) {
            funcion();
            consumir(TokenType.TOK_LPAR, "'(' despues de la funcion");
            expresion();
            consumir(TokenType.TOK_RPAR, "')'");
        } else {
            throw error("se esperaba un numero, variable, funcion o '('");
        }
    }

    // 19-24. <funcion> -> sin | cos | tan | cot | sec | csc
    private void funcion() {
        if (!es(TokenType.TOK_SIN, TokenType.TOK_COS, TokenType.TOK_TAN,
                TokenType.TOK_COT, TokenType.TOK_SEC, TokenType.TOK_CSC)) {
            throw error("se esperaba una funcion trigonometrica");
        }
        avanzar();
    }
}
