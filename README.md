# Evaluador de expresiones trigonométricas

Analizador léxico, sintáctico y evaluador semántico en Java, adaptado del
proyecto `toy_programming_language`, rama `feature/parser`, siguiendo la
especificación de `Compiladores-taller-3-v3.docx`.

## Alcance

El programa reconoce y **evalúa** expresiones aritméticas y trigonométricas:

- El análisis léxico utiliza el AFD tabular de la especificación.
- El análisis sintáctico es **descendente recursivo**, basado en las 24
  producciones de la gramática LL(1) del documento.
- La evaluación semántica es **dirigida por la sintaxis**: cada método del
  parser devuelve el valor (`double`) de la subexpresión que reconoce, según la
  gramática de atributos del documento.
- Se solicita por consola el valor de cada variable (`x`, `y`, `z`) que aparece
  en la expresión, una sola vez por variable, y se sustituye durante la
  evaluación.
- Se detectan errores semánticos: división por cero, funciones no definidas en
  el argumento dado (por ejemplo `tan(π/2)` o `cot(0)`) y resultados que no son
  números reales finitos (por ejemplo `(-8)^(1/3)` o un desbordamiento).
- Por decisión de alcance, no se implementa el autómata de pila de la sección
  4.3; el reconocimiento se hace con el descenso recursivo equivalente.
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
Suma, resta, multiplicación y división se evalúan de izquierda a derecha
(`8 - 3 - 2 = 3`); la potencia es asociativa por la derecha (`2^3^2 = 512`).
El menos unario tiene menor precedencia que la potencia (`-2^2 = -4`) y puede
aparecer en el exponente (`2^-3 = 0.125`).

## Evaluación semántica

| Elemento | Cálculo |
|---|---|
| `+`, `-`, `*`, `/` | Aritmética `double` de Java |
| `^` | `Math.pow(base, exponente)` |
| `sin`, `cos`, `tan` | `Math.sin`, `Math.cos`, `Math.tan` |
| `cot`, `sec`, `csc` | `cos/sin`, `1/cos`, `1/sin` |

Los argumentos de las funciones se interpretan en **radianes**. El resultado se
entrega con precisión `double`.

Los atributos heredados de la gramática (valor acumulado a la izquierda en
`<expSuma'>`, `<expMult'>` y `<expPotencia'>`) se implementan como variables
locales de los ciclos (`acumulado`) y de `expPotencia` (`base`); el atributo
sintetizado `val` es el valor de retorno de cada método.

| Error semántico | Condición |
|---|---|
| División por cero | El divisor vale exactamente 0 |
| Función no definida | `tan`, `sec`: \|cos(arg)\| < 10⁻¹²; `cot`, `csc`: \|sin(arg)\| < 10⁻¹² |
| Resultado no real o no finito | Una operación produce `NaN` o infinito |

Se usa una tolerancia de 10⁻¹² porque, en `double`, `cos(π/2)` no es
exactamente 0 sino aproximadamente 6.1·10⁻¹⁷.

Los valores de las variables se leen por consola; se aceptan punto o coma
decimal (`0.5` o `0,5`). Si el valor no es un número real finito, se vuelve a
preguntar.

## Requisitos y compilación

Se requiere **JDK 11 o superior**, sin dependencias externas.
Ejecuta los comandos desde la raíz del proyecto:

```powershell
javac --release 11 -encoding UTF-8 -d bin -sourcepath src src\Main.java
```

## Ejecución

Analizar y evaluar un archivo UTF-8 que contenga una única expresión:

```powershell
java -cp bin Main src\input\ejemplo.txt
```

El archivo de ejemplo contiene:

```text
sen(cos(x)) + tan(y)/cos(x)
```

Salida con `x = 1` y `y = 0.5` (se omiten los tokens):

```text
=== ANALISIS SINTACTICO ===
Analisis sintactico exitoso: la expresion es valida.

=== VALORES DE LAS VARIABLES (radianes) ===
x = 1
y = 0.5

=== EVALUACION SEMANTICA ===
Resultado: 1.5255004933346608
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

La salida muestra los tokens, incluido `TOK_EOF`, el resultado del análisis
sintáctico y, si la expresión es válida, el valor calculado. El EOF es una
marca interna: no debe escribirse en el archivo. Su símbolo `⊣` puede mostrarse
como `?` si la codificación de salida de la consola no lo admite.

Las fases se ejecutan en orden y cada error detiene las siguientes: ante un
error léxico se omite el análisis sintáctico; ante un error sintáctico no se
piden valores ni se evalúa. Los errores léxicos por caracteres inválidos o
números incompletos indican posiciones de carácter desde cero; los errores
sintácticos identifican el token inesperado o el fin de entrada; los errores
semánticos indican la operación o función que no se pudo calcular. Como en
Toy, los errores se informan por consola; el código de salida del proceso no
distingue aceptación de rechazo.

## Estructura

```text
src/
  Main.java                       CLI, coordinación de las fases y lectura de variables
  input/ExpressionFileReader.java  Lectura por ruta o diálogo
  input/ejemplo.txt                Expresión de ejemplo
  lexer/Alphabet.java             Categorías del alfabeto
  lexer/Status.java               Estados del AFD
  lexer/Automaton.java            Tabla de transiciones y aceptación
  lexer/Scanner.java              Obtención y clasificación de lexemas
  token/Token.java                Par tipo/lexema
  token/TokenType.java            Tokens del lenguaje
  parser/Parser.java              Reconocedor descendente recursivo y evaluador
  parser/SyntaxException.java     Error sintáctico
  parser/SemanticException.java   Error semántico
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
archivos, además de 5 verificaciones de CLI. La verificación de la expresión
válida envía los valores `x = 1` y `y = 0.5` por la entrada estándar y comprueba
el resultado de la evaluación.

El diálogo gráfico y la resistencia a anidamiento extremo no están cubiertos
por estas pruebas. El parser utiliza la pila de llamadas de Java, por lo que
una entrada extremadamente anidada puede agotarla.
