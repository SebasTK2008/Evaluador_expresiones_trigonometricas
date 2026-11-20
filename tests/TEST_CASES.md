# Tests del evaluador

Cada archivo `.txt` es una entrada (una sola expresión) y cada `.expected.txt` es su resultado esperado.
Si la expresión tiene variables, la primera línea del `.expected.txt` indica los valores que se deben
ingresar cuando el programa los pide (en radianes). Los casos de error terminan con `Error esperado:`.

1. `01_ejemplo_enunciado`: primer ejemplo del enunciado, con 3.14 como aproximación de π. `sin(1.57) + 4 = 4.9999996829…` (no 5.0 exacto). (Confirmado)
2. `02_suma_funciones`: `sen(x) + cos(y)` con `x = 1`, `y = 0.5`: `0.841470985 + 0.877582562 = 1.719053547`. (Confirmado)
3. `03_funciones_anidadas`: `sen(cos(x))+ tan(y)/cos(x)` con `x = 1`, `y = 0.5`: `0.514395 + 0.546302/0.540302 = 1.525500`. (Confirmado)
4. `04_identidad_pitagorica`: `cos(x)^2 + sen(x)^2` con `x = 2.5`; por la identidad pitagórica debe ser 1 (el error de redondeo es 1.1e-16). (Confirmado)
5. `05_funcion_simple`: `sin(x)` con `x = 0.75`: `sin(0.75) = 0.681638760`. (Confirmado)
6. `06_argumento_suma`: `cos(x + y)` con `x = 1`, `y = 2`: `cos(3) = -0.989992497`. (Confirmado)
7. `07_argumento_compuesto`: `tan(x^2 + sin(x))` con `x = 1`: `tan(1 + 0.841471) = tan(1.841471) = -3.603803532`. (Confirmado)
8. `08_potencia_derecha`: `2^3^2` es asociativa por la derecha: `2^(3^2) = 512`. (Confirmado)
9. `09_menos_unario`: `-2^2` tiene el menos de menor precedencia que `^`: `-(2^2) = -4`. (Confirmado)
10. `10_resta_izquierda`: `8 - 3 - 2` es asociativa por la izquierda: `(8 - 3) - 2 = 3`. (Confirmado)
11. `11_parentesis`: paréntesis y precedencia: `(2 + 3) * 4 / 8 = 20 / 8 = 2.5`. (Confirmado)
12. `12_reales_cientificos`: reales, notación científica y exponente negativo: `150 + 3.14 * 0.125 = 150.3925`. (Confirmado)
13. `13_tokens`: lista de tokens de `sen(x) + cos(0) * 2^2`; se obtiene con `Main --tokens`. (Confirmado)
14. `14_error_lexico`: número incompleto `3.`. (Confirmado)
15. `15_error_sintactico`: falta un operando dentro de `sin(x + )`. (Confirmado)
16. `16_error_division_cero`: error semántico, `1 / (x - x)` con `x = 2` divide por cero. (Confirmado)
17. `17_error_tangente`: error semántico, `tan(π/2)` no está definida (`cos` ≈ 6.1e-17 < 1e-12). (Confirmado)
