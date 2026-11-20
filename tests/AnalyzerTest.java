import input.ExpressionFileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lexer.Scanner;
import parser.Parser;
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
        System.out.println("OK: " + checks + " comprobaciones.");
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
