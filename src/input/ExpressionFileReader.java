package input;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Lector de Toy adaptado para archivos de expresiones trigonometricas. */
public class ExpressionFileReader {
    /** Abre el mismo dialogo nativo de seleccion utilizado en Toy. */
    public char[] readFile() {
        FileDialog dialog = new FileDialog((Frame) null,
                "Selecciona el archivo de expresion trigonometrica (.txt)", FileDialog.LOAD);
        try {
            dialog.setVisible(true);
            String filename = dialog.getFile();
            if (filename == null) {
                System.out.println("Operacion cancelada por el usuario.");
                return null;
            }
            return readFile(new File(dialog.getDirectory(), filename).getAbsolutePath());
        } finally {
            dialog.dispose();
        }
    }

    /** Lee UTF-8 y elimina un BOM inicial, igual que Toy. */
    public char[] readFile(String filePath) {
        try {
            String text = Files.readString(Path.of(filePath));
            if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
                text = text.substring(1);
            }
            return text.toCharArray();
        } catch (IOException e) {
            System.out.println("Ocurrio un error al leer el archivo: " + e.getMessage());
            return null;
        }
    }
}
