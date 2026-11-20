# Casos de prueba y validacion matematica

Generado ejecutando el programa (`java -cp bin Main`) con cada expresion y comparando el resultado con el calculado de forma independiente con `[Math]` de .NET. Los angulos estan en radianes; tolerancia relativa 1e-12.

## Evaluacion correcta

| Expresion | Variables | Resultado del programa | Valor de referencia | Diferencia | Estado | Observacion |
|---|---|---|---|---|---|---|
| `sen(3.14 / 2) + cos(0) * 2^2` | - | 4.9999996829318345 | 4.9999996829318345 | 0.0E+0 | OK | Ejemplo 1 del enunciado (pi aproximado por 3.14) |
| `sin(3.14159265358979 / 2) + cos(0) * 2^2` | - | 5 | 5 | 0.0E+0 | OK | Ejemplo 1 con pi de 15 cifras: tiende a 5.0 |
| `sen(x) + cos(y)` | x = 1, y = 0.5 | 1.7190535466982693 | 1.7190535466982693 | 0.0E+0 | OK | Ejemplo 2 |
| `sen(cos(x))+ tan(y)/cos(x)` | x = 1, y = 0.5 | 1.5255004933346608 | 1.5255004933346608 | 0.0E+0 | OK | Ejemplo 3 |
| `cos(x)^2 + sen(x)^2` | x = 2.5 | 0.99999999999999989 | 1 | 1.1E-16 | OK | Ejemplo 4: identidad pitagorica (debe dar 1) |
| `sin(x)` | x = 0.75 | 0.68163876002333412 | 0.68163876002333412 | 0.0E+0 | OK | Ejemplo 5 |
| `cos(x + y)` | x = 1, y = 2 | -0.98999249660044542 | -0.98999249660044542 | 0.0E+0 | OK | Ejemplo 6 |
| `tan(x^2 + sin(x))` | x = 1 | -3.6038035320088055 | -3.6038035320088055 | 0.0E+0 | OK | Ejemplo 7 |
| `2^3^2` | - | 512 | 512 | 0.0E+0 | OK | Potencia asociativa por la derecha: 2^(3^2) |
| `-2^2` | - | -4 | -4 | 0.0E+0 | OK | Menos unario de menor precedencia que ^ |
| `8 - 3 - 2` | - | 3 | 3 | 0.0E+0 | OK | Resta asociativa por la izquierda |
| `(2 + 3) * 4 / 8` | - | 2.5 | 2.5 | 0.0E+0 | OK | Parentesis y precedencia |
| `1.5e2 + 2^-3` | - | 150.125 | 150.125 | 0.0E+0 | OK | Notacion cientifica y exponente negativo |
| `cot(y) + sec(z) + csc(x)` | y = 1, z = 0.5, x = 2 | 2.8813367135534964 | 2.8813367135534964 | 0.0E+0 | OK | Orden de entrada: y, z, x |

## Errores semanticos detectados

| Expresion | Variables | Mensaje del programa |
|---|---|---|
| `1/0` | - | Error semantico: division por cero |
| `cot(0)` | - | Error semantico: cot(0.0) no esta definida |
| `csc(0)` | - | Error semantico: csc(0.0) no esta definida |
| `tan(x)` | x = 1.5707963267948966 | Error semantico: tan(1.5707963267949) no esta definida |
| `(-8)^(1/3)` | - | Error semantico: la potencia -8.0^0.3333333333333333 no produce un numero real finito |
| `1e9999` | - | Error semantico: el numero 1e9999 no produce un numero real finito |
