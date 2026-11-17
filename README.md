# Evaluador de expresiones trigonométricas

Analizador léxico y sintáctico en Java, adaptado del proyecto
`toy_programming_language`, rama `feature/parser`, siguiendo la especificación
de `Compiladores-taller-3-v3.docx`.

## Alcance

Esta etapa **solo reconoce expresiones**: no calcula resultados, no solicita
valores para las variables ni detecta errores semánticos como una división por
cero. Por ejemplo, `1/0` es sintácticamente válido.

- El análisis léxico utiliza el AFD tabular de la especificación.
- El análisis sintáctico es **descendente recursivo**, basado en las 24
  producciones de la gramática LL(1) del documento.
- Por decisión de alcance, no se implementa el autómata de pila de la sección
  4.3 ni la evaluación semántica descrita en el documento.
- Se conserva la entrada de Toy por ruta de archivo o diálogo de selección,
  además de la opción `--tokens`.

## Lenguaje admitido

| Elemento | Formas admitidas |
|---|---|
| Funciones | `sin`, `sen` (alias de `sin`), `cos`, `tan`, `cot`, `sec`, `csc` |
| Variables | `x`, `y`, `z` |
| Números | Enteros, decimales y notación científica: `3`, `3.14`, `1.5e-3`, `2E+5` |
| Operadores | `+`, `-`, `*`, `/`, `^` y menos unario |
| Agrupación | Paréntesis |
| Separadores | Espacios, tabulaciones y saltos de línea |

Los números siguen la expresión regular
`[0-9]+(\.[0-9]+)?([eE][+-]?[0-9]+)?`: `.5`, `3.` y exponentes incompletos
se rechazan. Los nombres distinguen mayúsculas de minúsculas; cualquier
identificador fuera de la tabla es un error léxico, incluidas las funciones
inversas e hiperbólicas.

Las funciones requieren un argumento entre paréntesis. No hay multiplicación
implícita, comentarios, cadenas ni sentencias de Toy. `--x` representa dos
menos unarios, no un comentario.

La gramática establece, de menor a mayor precedencia: suma/resta,
multiplicación/división, menos unario, potencia y elementos primarios.
Suma, resta, multiplicación y división se recorren de izquierda a derecha;
la potencia conserva la recursión por la derecha. Se reconocen expresiones
como `-2^2`, `2^-3` y `2^3^2`, sin evaluarlas.

## Requisitos y compilación

Se requiere **JDK 11 o superior**, sin dependencias externas.
Ejecuta los comandos desde la raíz del proyecto:

```powershell
javac --release 11 -encoding UTF-8 -d bin -sourcepath src src\Main.java
```

## Ejecución

Analizar un archivo UTF-8 que contenga una única expresión:

```powershell
java -cp bin Main src\input\ejemplo.txt
```

El archivo de ejemplo contiene:

```text
sen(cos(x)) + tan(y)/cos(x)
```

Ejecutar únicamente el análisis léxico:

```powershell
java -cp bin Main --tokens src\input\ejemplo.txt
```

Abrir el diálogo nativo de selección de archivo:

```powershell
java -cp bin Main
```

El diálogo requiere un entorno gráfico. Las rutas con espacios deben ir entre
comillas. El lector elimina un BOM UTF-8 inicial si está presente.

La salida muestra los tokens, incluido `TOK_EOF`, y el resultado del análisis
sintáctico cuando corresponde. El EOF es una marca interna: no debe escribirse
en el archivo. Su símbolo `⊣` puede mostrarse como `?` si la codificación de
salida de la consola no lo admite.

Ante un error léxico se omite el análisis sintáctico. Los errores léxicos por
caracteres inválidos o números incompletos indican posiciones de carácter
desde cero; los errores sintácticos identifican el token inesperado o el fin
de entrada. Como en Toy, los errores se informan por consola; el código de
salida del proceso no distingue aceptación de rechazo.

## Estructura

```text
src/
  Main.java                       CLI y coordinación de las fases
  input/ExpressionFileReader.java  Lectura por ruta o diálogo
  input/ejemplo.txt                Expresión de ejemplo
  lexer/Alphabet.java             Categorías del alfabeto
  lexer/Status.java               Estados del AFD
  lexer/Automaton.java            Tabla de transiciones y aceptación
  lexer/Scanner.java              Obtención y clasificación de lexemas
  token/Token.java                Par tipo/lexema
  token/TokenType.java            Tokens del lenguaje
  parser/Parser.java              Reconocedor descendente recursivo
  parser/SyntaxException.java     Error sintáctico
tests/
  AnalyzerTest.java               Pruebas de los analizadores y lector
  fixtures/                      Archivos de prueba
  run_tests.ps1                   Compilación y pruebas automatizadas
```

## Pruebas

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tests\run_tests.ps1
```

El script compila con `--release 11` y `-Xlint:all`, ejecuta 138 comprobaciones
de tokens, expresiones válidas, errores léxicos/sintácticos y lectura de
archivos, además de 5 verificaciones de CLI.

El diálogo gráfico y la resistencia a anidamiento extremo no están cubiertos
por estas pruebas. El parser utiliza la pila de llamadas de Java, por lo que
una entrada extremadamente anidada puede agotarla.
