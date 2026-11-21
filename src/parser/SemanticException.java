package parser;

/** Error detectado al evaluar una expresion sintacticamente valida. */
public class SemanticException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SemanticException(String mensaje) {
        super(mensaje);
    }
}
