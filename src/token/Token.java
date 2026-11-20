package token;

/** Par (tipo, lexema), reutilizado del proyecto Toy. */
public class Token {
    private final TokenType tipo;
    private final String lexema;

    public Token(TokenType tipo, String lexema) {
        this.tipo = tipo;
        this.lexema = lexema;
    }

    public TokenType getTipo() {
        return tipo;
    }

    public String getLexema() {
        return lexema;
    }

    @Override
    public String toString() {
        return "Token: " + tipo + " \"" + lexema + "\"";
    }
}
