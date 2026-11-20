package parser;

/** Primer error sintactico encontrado, como en el reconocedor de Toy. */
public class SyntaxException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SyntaxException(String mensaje) {
        super(mensaje);
    }
}
