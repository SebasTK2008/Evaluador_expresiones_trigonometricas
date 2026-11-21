import input.ExpressionFileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lexer.Scanner;
import parser.Parser;
import parser.SemanticException;
import parser.SyntaxException;
import token.Token;
import token.TokenType;

/**
 * Entrada adaptada de Toy: Main [--tokens] [ruta/al/archivo.txt].
 * Sin ruta abre el selector. --tokens ejecuta solamente el analisis lexico.
 */
public class Main {
    public static void main(String[] args) {
        boolean soloTokens = args.length > 0 && args[0].equals("--tokens");
        int indiceArchivo = soloTokens ? 1 : 0;
        ExpressionFileReader reader = new ExpressionFileReader();
        char[] buffer = args.length > indiceArchivo
                ? reader.readFile(args[indiceArchivo]) : reader.readFile();
        if (buffer == null) {
            System.out.println("Error al leer el archivo.");
            return;
        }

        Scanner scanner = new Scanner(buffer);
        ArrayList<Token> tokens;
        System.out.println("=== ANALISIS LEXICO ===");
        try {
            tokens = scanner.classifyAllTokens(scanner.getLexemas());
            int index = 1;
            for (Token token : tokens) {
                System.out.println(index++ + ": " + token);
            }
            System.out.println("Analisis lexico exitoso: " + tokens.size()
                    + " tokens obtenidos (incluye EOF).");
        } catch (IllegalArgumentException e) {
            imprimirTokensParciales(scanner);
            System.out.println("Error lexico: " + e.getMessage());
            if (!soloTokens) {
                System.out.println();
                System.out.println("=== ANALISIS SINTACTICO ===");
                System.out.println("Omitido por error lexico previo.");
            }
            return;
        }
        if (soloTokens) {
            return;
        }

        System.out.println();
        System.out.println("=== ANALISIS SINTACTICO ===");
        try {
            new Parser(tokens).parse();
            System.out.println("Analisis sintactico exitoso: la expresion es valida.");
        } catch (SyntaxException e) {
            System.out.println(e.getMessage());
            return;
        }

        Map<String, Double> valores = pedirValores(tokens);

        System.out.println();
        System.out.println("=== EVALUACION SEMANTICA ===");
        try {
            double resultado = new Parser(tokens).evaluar(valores);
            System.out.println("Resultado: " + resultado);
        } catch (SemanticException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void imprimirTokensParciales(Scanner scanner) {
        ArrayList<String> parciales = scanner.getLastLexemas();
        if (parciales.isEmpty()) {
            return;
        }
        System.out.println("Tokens obtenidos hasta el error:");
        int index = 1;
        for (String lexema : parciales) {
            try {
                System.out.println(index++ + ": " + scanner.classifyToken(lexema));
            } catch (IllegalArgumentException e) {
                // La clasificacion se detiene en el primer identificador invalido.
                break;
            }
        }
    }

    /** Pide una sola vez el valor de cada variable, en el orden en que aparece. */
    private static Map<String, Double> pedirValores(List<Token> tokens) {
        Set<String> variables = new LinkedHashSet<>();
        for (Token token : tokens) {
            if (token.getTipo() == TokenType.TOK_VAR) {
                variables.add(token.getLexema());
            }
        }
        Map<String, Double> valores = new HashMap<>();
        if (variables.isEmpty()) {
            return valores;
        }
        java.util.Scanner teclado = new java.util.Scanner(System.in);
        System.out.println();
        System.out.println("=== VALORES DE LAS VARIABLES (radianes) ===");
        for (String variable : variables) {
            valores.put(variable, leerNumero(teclado, variable));
        }
        return valores;
    }

    /** Repite la pregunta hasta recibir un numero real finito. Acepta coma o punto. */
    private static double leerNumero(java.util.Scanner teclado, String variable) {
        while (true) {
            System.out.print(variable + " = ");
            if (!teclado.hasNextLine()) {
                throw new IllegalStateException("No se recibio un valor para " + variable);
            }
            String linea = teclado.nextLine().trim().replace(',', '.');
            try {
                double valor = Double.parseDouble(linea);
                if (Double.isFinite(valor)) {
                    return valor;
                }
            } catch (NumberFormatException e) {
                // Se vuelve a preguntar.
            }
            System.out.println("Valor invalido: escribe un numero real (por ejemplo 1.57 o -2,5).");
        }
    }
}
