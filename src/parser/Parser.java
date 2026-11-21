package parser;

import java.util.List;
import java.util.Map;
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

    private static final double EPSILON = 1e-12;

    private boolean evaluando;
    private Map<String, Double> valores = Map.of();

    /** Reconoce exactamente una expresion seguida de EOF, sin evaluarla. */
    public void parse() {
        analizar(false, Map.of());
    }

    /** Reconoce la expresion y la evalua con los valores dados para x, y, z. */
    public double evaluar(Map<String, Double> valores) {
        return analizar(true, valores);
    }

    private double analizar(boolean evaluando, Map<String, Double> valores) {
        this.evaluando = evaluando;
        this.valores = valores;
        pos = 0;
        double resultado = expresion();
        consumir(TokenType.TOK_EOF, "el final del archivo");
        if (pos != tokens.size()) {
            throw error("se esperaba el final del archivo");
        }
        return resultado;
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

    private SemanticException semantico(String mensaje) {
        return new SemanticException("Error semantico: " + mensaje);
    }

    /** Solo en modo evaluacion: el resultado debe ser un real finito. */
    private void comprobar(double valor, String operacion) {
        if (evaluando && !Double.isFinite(valor)) {
            throw semantico(operacion + " no produce un numero real finito");
        }
    }

    /** Solo en modo evaluacion: el denominador de tan, cot, sec o csc no puede ser 0. */
    private void exigirNoCero(double denominador, String funcion, double argumento) {
        if (evaluando && Math.abs(denominador) < EPSILON) {
            throw semantico(funcion + "(" + argumento + ") no esta definida");
        }
    }

    private double valorDe(String variable) {
        if (!evaluando) {
            return 0.0;
        }
        Double valor = valores.get(variable);
        if (valor == null) {
            throw new IllegalStateException("Falta el valor de la variable " + variable);
        }
        return valor;
    }

    // 1. <expresion> -> <expSuma>
    private double expresion() {
        return expSuma();
    }

    // 2-5. <expSuma> -> <expMult> ((+ | -) <expMult>)*
    private double expSuma() {
        double acumulado = expMult();
        while (es(TokenType.TOK_PLUS, TokenType.TOK_MINUS)) {
            boolean suma = es(TokenType.TOK_PLUS);
            avanzar();
            double derecho = expMult();
            acumulado = suma ? acumulado + derecho : acumulado - derecho;
            comprobar(acumulado, suma ? "la suma" : "la resta");
        }
        return acumulado;
    }

    // 6-9. <expMult> -> <expUnaria> ((* | /) <expUnaria>)*
    private double expMult() {
        double acumulado = expUnaria();
        while (es(TokenType.TOK_MUL, TokenType.TOK_DIV)) {
            boolean multiplica = es(TokenType.TOK_MUL);
            avanzar();
            double derecho = expUnaria();
            if (multiplica) {
                acumulado = acumulado * derecho;
            } else {
                if (evaluando && derecho == 0.0) {
                    throw semantico("division por cero");
                }
                acumulado = acumulado / derecho;
            }
            comprobar(acumulado, multiplica ? "la multiplicacion" : "la division");
        }
        return acumulado;
    }

    // 10-11. <expUnaria> -> - <expUnaria> | <expPotencia>
    private double expUnaria() {
        if (es(TokenType.TOK_MINUS)) {
            avanzar();
            return -expUnaria();
        }
        return expPotencia();
    }

    // 12-14. <expPotencia> -> <primario> (^ <expUnaria>)?
    // La recursion por expUnaria conserva 2^3^2 = 512, -2^2 = -4 y 2^-3.
    private double expPotencia() {
        double base = primario();
        if (es(TokenType.TOK_EXP)) {
            avanzar();
            double exponente = expUnaria();
            double resultado = Math.pow(base, exponente);
            comprobar(resultado, "la potencia " + base + "^" + exponente);
            return resultado;
        }
        return base;
    }

    // 15-18. <primario> -> ( <expresion> ) | num | var | <funcion> ( <expresion> )
    private double primario() {
        if (es(TokenType.TOK_LPAR)) {
            avanzar();
            double valor = expresion();
            consumir(TokenType.TOK_RPAR, "')'");
            return valor;
        }
        if (es(TokenType.TOK_NUMBER)) {
            double valor = Double.parseDouble(tokens.get(pos).getLexema());
            comprobar(valor, "el numero " + tokens.get(pos).getLexema());
            avanzar();
            return valor;
        }
        if (es(TokenType.TOK_VAR)) {
            String variable = tokens.get(pos).getLexema();
            avanzar();
            return valorDe(variable);
        }
        if (es(TokenType.TOK_SIN, TokenType.TOK_COS, TokenType.TOK_TAN,
                TokenType.TOK_COT, TokenType.TOK_SEC, TokenType.TOK_CSC)) {
            TokenType funcion = funcion();
            consumir(TokenType.TOK_LPAR, "'(' despues de la funcion");
            double argumento = expresion();
            consumir(TokenType.TOK_RPAR, "')'");
            return aplicar(funcion, argumento);
        }
        throw error("se esperaba un numero, variable, funcion o '('");
    }

    // 19-24. <funcion> -> sin | cos | tan | cot | sec | csc
    private TokenType funcion() {
        if (!es(TokenType.TOK_SIN, TokenType.TOK_COS, TokenType.TOK_TAN,
                TokenType.TOK_COT, TokenType.TOK_SEC, TokenType.TOK_CSC)) {
            throw error("se esperaba una funcion trigonometrica");
        }
        TokenType tipo = tokens.get(pos).getTipo();
        avanzar();
        return tipo;
    }

    /** Argumento en radianes. cot, sec y csc se calculan a partir de sin y cos. */
    private double aplicar(TokenType funcion, double x) {
        switch (funcion) {
            case TOK_SIN:
                return Math.sin(x);
            case TOK_COS:
                return Math.cos(x);
            case TOK_TAN:
                exigirNoCero(Math.cos(x), "tan", x);
                return Math.tan(x);
            case TOK_COT:
                exigirNoCero(Math.sin(x), "cot", x);
                return Math.cos(x) / Math.sin(x);
            case TOK_SEC:
                exigirNoCero(Math.cos(x), "sec", x);
                return 1.0 / Math.cos(x);
            case TOK_CSC:
                exigirNoCero(Math.sin(x), "csc", x);
                return 1.0 / Math.sin(x);
            default:
                throw new IllegalStateException("Funcion no soportada: " + funcion);
        }
    }
}
