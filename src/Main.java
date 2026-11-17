import input.ExpressionFileReader;
import java.util.ArrayList;
import lexer.Scanner;
import parser.Parser;
import parser.SyntaxException;
import token.Token;

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
}
