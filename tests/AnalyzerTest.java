import input.ExpressionFileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lexer.Scanner;
import parser.Parser;
import parser.SemanticException;
import parser.SyntaxException;
import token.Token;
import token.TokenType;

/** Pruebas sin dependencias externas: java -cp bin AnalyzerTest. */
public class AnalyzerTest {
    private static int checks;

    public static void main(String[] args) {
        testTokens();
        testValidExpressions();
        testLexicalErrors();
        testSyntaxErrors();
        testReader();
        testEvaluation();
        testIdentities();
        testSemanticErrors();
        System.out.println("OK: " + checks + " comprobaciones.");
    }

    private static final double TOLERANCE = 1e-12;

    private static double eval(String input, Map<String, Double> values) {
        return new Parser(scan(input)).evaluar(values);
    }

    private static void checkValue(String input, Map<String, Double> values, double expected) {
        double actual = eval(input, values);
        check(Math.abs(actual - expected) <= TOLERANCE,
                input + " " + values + " -> " + actual + ", esperado " + expected);
    }

    /** Valores esperados calculados a mano o con java.lang.Math, de forma independiente del parser. */
    private static void testEvaluation() {
        Map<String, Double> none = Map.of();
        // Aritmetica y precedencia.
        checkValue("2+3*4", none, 14);
        checkValue("(2+3)*4", none, 20);
        checkValue("8-3-2", none, 3);
        checkValue("8/4/2", none, 1);
        checkValue("2^3^2", none, 512);
        checkValue("-2^2", none, -4);
        checkValue("2^-3", none, 0.125);
        checkValue("2^-3^2", none, 1.0 / 512);
        checkValue("--5", none, 5);
        checkValue("1--2", none, 3);
        checkValue("1.5e2", none, 150);
        checkValue("1.5e-3", none, 0.0015);
        checkValue("7", none, 7);
        // Funciones en puntos conocidos.
        checkValue("sin(0)", none, 0);
        checkValue("cos(0)", none, 1);
        checkValue("tan(0)", none, 0);
        checkValue("cos(0)*2^2", none, 4);
        checkValue("sin(3.14159265358979/2)", none, 1);
        checkValue("csc(3.14159265358979/2)", none, 1);
        checkValue("sec(0)", none, 1);
        checkValue("tan(3.14159265358979/4)", none, 1);
        // Ejemplos del enunciado.
        checkValue("sen(3.14 / 2) + cos(0) * 2^2", none, Math.sin(1.57) + 4);
        checkValue("sin(3.14159265358979/2) + cos(0) * 2^2", none, 5);
        double x = 1.0;
        double y = 0.5;
        Map<String, Double> xy = Map.of("x", x, "y", y);
        checkValue("sen(x) + cos(y)", xy, Math.sin(x) + Math.cos(y));
        checkValue("sen(cos(x))+ tan(y)/cos(x)", xy,
                Math.sin(Math.cos(x)) + Math.tan(y) / Math.cos(x));
        checkValue("cos(x)^2 + sen(x)^2", xy, 1);
        checkValue("sin(x)", xy, Math.sin(x));
        checkValue("cos(x + y)", xy, Math.cos(x + y));
        checkValue("tan(x^2 + sin(x))", xy, Math.tan(x * x + Math.sin(x)));
        // cot, sec, csc y la tercera variable.
        Map<String, Double> xyz = Map.of("x", 1.0, "y", 0.5, "z", 2.0);
        checkValue("cot(y)", xyz, Math.cos(0.5) / Math.sin(0.5));
        checkValue("sec(z)", xyz, 1.0 / Math.cos(2.0));
        checkValue("csc(x)", xyz, 1.0 / Math.sin(1.0));
        checkValue("x-y-z", xyz, 1.0 - 0.5 - 2.0);
        // Un mismo parser se puede evaluar varias veces con valores distintos.
        Parser parser = new Parser(scan("x*2"));
        check(parser.evaluar(Map.of("x", 3.0)) == 6.0, "Primera evaluacion");
        check(parser.evaluar(Map.of("x", -1.5)) == -3.0, "Segunda evaluacion");
    }

    /** Identidades matematicas: validan la evaluacion sin depender de valores precalculados. */
    private static void testIdentities() {
        // Se evitan los multiplos de pi/2, donde tan, cot, sec o csc no estan definidas.
        double[] angles = {-7.3, -1, -0.25, 0.5, 1, 2, 10, 123.456};
        for (double a : angles) {
            Map<String, Double> v = Map.of("x", a);
            checkValue("cos(x)^2 + sen(x)^2", v, 1);
            checkValue("sin(-x) + sin(x)", v, 0);
            checkValue("cos(-x) - cos(x)", v, 0);
            checkValue("sin(x)/cos(x) - tan(x)", v, 0);
            checkValue("1/cos(x) - sec(x)", v, 0);
            checkValue("1/sin(x) - csc(x)", v, 0);
            checkValue("cos(x)/sin(x) - cot(x)", v, 0);
            checkValue("1 + tan(x)^2 - sec(x)^2", v, 0);
        }
    }

    private static void expectSemantic(String input, Map<String, Double> values, String fragment) {
        try {
            double result = eval(input, values);
            throw new AssertionError("Debe fallar en semantico: " + input + " -> " + result);
        } catch (SemanticException e) {
            check(e.getMessage().startsWith("Error semantico:"), "Tipo de error: " + input);
            check(e.getMessage().contains(fragment), input + ": " + e.getMessage());
        }
    }

    private static void testSemanticErrors() {
        Map<String, Double> none = Map.of();
        expectSemantic("1/0", none, "division por cero");
        expectSemantic("x/(x-x)", Map.of("x", 2.0), "division por cero");
        expectSemantic("cot(0)", none, "cot");
        expectSemantic("csc(0)", none, "csc");
        expectSemantic("tan(x)", Map.of("x", Math.PI / 2), "tan");
        expectSemantic("sec(x)", Map.of("x", Math.PI / 2), "sec");
        expectSemantic("(-1)^0.5", none, "potencia");
        expectSemantic("(-8)^(1/3)", none, "potencia");
        expectSemantic("0^-1", none, "potencia");
        expectSemantic("1e9999", none, "numero");
        expectSemantic("10^400", none, "potencia");
        // Falta el valor de una variable: error de uso, no semantico.
        try {
            eval("x+y", Map.of("x", 1.0));
            throw new AssertionError("Debe exigir el valor de y");
        } catch (IllegalStateException e) {
            check(e.getMessage().contains("y"), "Variable faltante");
        }
        // Un error sintactico se informa antes que cualquier evaluacion.
        try {
            eval("sin(", none);
            throw new AssertionError("Debe fallar en sintactico");
        } catch (SyntaxException e) {
            check(true, "Sintactico antes que semantico");
        }
    }

    private static ArrayList<Token> scan(String input) {
        Scanner scanner = new Scanner(input.toCharArray());
        return scanner.classifyAllTokens(scanner.getLexemas());
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void testTokens() {
        ArrayList<Token> tokens = scan(
                "sin sen cos tan cot sec csc x y z 3 3.14 1.5e-3 2E+5 + - * / ^ ( )");
        TokenType[] expected = {
            TokenType.TOK_SIN, TokenType.TOK_SIN, TokenType.TOK_COS,
            TokenType.TOK_TAN, TokenType.TOK_COT, TokenType.TOK_SEC,
            TokenType.TOK_CSC, TokenType.TOK_VAR, TokenType.TOK_VAR,
            TokenType.TOK_VAR, TokenType.TOK_NUMBER, TokenType.TOK_NUMBER,
            TokenType.TOK_NUMBER, TokenType.TOK_NUMBER, TokenType.TOK_PLUS,
            TokenType.TOK_MINUS, TokenType.TOK_MUL, TokenType.TOK_DIV,
            TokenType.TOK_EXP, TokenType.TOK_LPAR, TokenType.TOK_RPAR,
            TokenType.TOK_EOF
        };
        check(tokens.size() == expected.length, "Cantidad de tokens");
        for (int i = 0; i < expected.length; i++) {
            check(tokens.get(i).getTipo() == expected[i], "Tipo del token " + i);
        }
        check(tokens.get(1).getLexema().equals("sen"), "Conservar alias sen");
        check(tokens.get(12).getLexema().equals("1.5e-3"), "Conservar literal");
        check(tokens.get(21).getLexema().equals("\u22a3"), "Marca EOF");
        check(scan(" \t\r\n").size() == 1, "Separadores no generan tokens");
        check(scan("1--2").size() == 5, "-- no es comentario");
        check(scan("2x").size() == 3, "Separacion por STOP sin perder caracteres");
        Scanner scanner = new Scanner("cos(0)*2^2".toCharArray());
        List<String> lexemas = scanner.getLexemas();
        check(lexemas.equals(Arrays.asList("cos", "(", "0", ")", "*", "2", "^", "2")),
                "Reprocesar simbolos consecutivos");
        check(scanner.getLexemas().equals(lexemas), "Escaner reutilizable");
        scanner.getLastLexemas().clear();
        check(scanner.getLastLexemas().equals(lexemas), "Copia de lexemas parciales");
    }

    private static void testValidExpressions() {
        // Incluye los ejemplos del documento y todas sus producciones.
        String[] inputs = {
            "sen(cos(x)) + tan(y)/cos(x)", "sin(0)", "cos(0)", "tan(x)",
            "cot(y)", "sec(z)", "csc(1)", "3", "3.14", "1.5e2", "1.5e-3",
            "x+y-z", "2+3*4", "(2+3)*4", "-x", "--x", "-2^2",
            "2^-3", "2^3^2", "((x))", "sin(x)^2+cos(x)^2",
            "x \t+\r\n y", "1--2", "8/4/2", "x-y-z", "2^-3^2",
            "sin(-cos(tan(cot(sec(csc(z))))))",
            // Errores de dominio no son errores lexicos ni sintacticos.
            "1/0", "cot(0)", "csc(0)", "(-1)^0.5", "1e9999"
        };
        for (String input : inputs) {
            Parser parser = new Parser(scan(input));
            parser.parse();
            parser.parse();
            check(true, "Expresion valida: " + input);
        }
    }

    private static void testLexicalErrors() {
        String[] inputs = {
            ".5", "3.", "2.e3", "4e", "4e+", "4e-", "1e+x",
            "arcsin(x)", "arccos(x)", "arctan(x)", "sinh(x)", "w", "SIN(x)",
            "_", "sinx", "x1", "pi", "x_y", "@", "2%3", "\"x\"", "\u00f1",
            "x:=1", "\u22a3", "1.2.3"
        };
        for (String input : inputs) {
            boolean failed = false;
            try {
                scan(input);
            } catch (IllegalArgumentException e) {
                failed = true;
            }
            check(failed, "Debe fallar en lexico: " + input);
        }
        Scanner scanner = new Scanner("x + 4e+".toCharArray());
        try {
            scanner.getLexemas();
            throw new AssertionError("Debe rechazar exponente incompleto");
        } catch (IllegalArgumentException e) {
            check(e.getMessage().contains("posicion 4"), "Posicion del numero incompleto");
            check(scanner.getLastLexemas().equals(Arrays.asList("x", "+")),
                    "Lexemas validos anteriores al error");
        }
    }

    private static void testSyntaxErrors() {
        String[] inputs = {
            "", " \n\t", "+x", "sin x", "sin()", "(x", "x)", "x+",
            "2**3", "2^^3", "2x", "x y", "x(y)", "sin(x)y", "sin(x)(y)",
            "sin(x)cos(y)", "()", "2^", "*x", "2(+3)", "x/+y", "cos((x)"
        };
        for (String input : inputs) {
            ArrayList<Token> tokens = scan(input);
            boolean failed = false;
            try {
                new Parser(tokens).parse();
            } catch (SyntaxException e) {
                failed = true;
                check(e.getMessage().startsWith("Error sintactico:"), "Tipo de error");
            }
            check(failed, "Debe fallar en sintactico: " + input);
        }
        try {
            new Parser(List.of(new Token(TokenType.TOK_NUMBER, "1")));
            throw new AssertionError("Debe exigir EOF");
        } catch (IllegalArgumentException e) {
            check(true, "Contrato EOF");
        }
    }

    private static void testReader() {
        ExpressionFileReader reader = new ExpressionFileReader();
        char[] input = reader.readFile("src/input/ejemplo.txt");
        check(input != null, "Lectura por ruta");
        new Parser(scan(new String(input))).parse();
        char[] bomInput = reader.readFile("tests/fixtures/bom.txt");
        check(bomInput != null && new String(bomInput).trim().equals("sin(x)"),
                "El lector elimina BOM UTF-8");
    }
}
