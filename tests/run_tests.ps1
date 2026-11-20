# Adaptado del ejecutor de Toy: compila y verifica analizadores y CLI.
# Ejecutar desde cualquier carpeta: powershell -ExecutionPolicy Bypass -File tests\run_tests.ps1
$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    & javac --release 11 -encoding UTF-8 -Xlint:all -d bin -sourcepath src src/Main.java tests/AnalyzerTest.java
    if ($LASTEXITCODE -ne 0) {
        throw "Fallo la compilacion."
    }
    & java -cp bin AnalyzerTest
    if ($LASTEXITCODE -ne 0) {
        throw "Fallaron las pruebas de los analizadores."
    }

    $valid = (& java -cp bin Main tests/fixtures/valid.txt | Out-String)
    if ($valid -notmatch "Analisis sintactico exitoso") {
        throw "CLI: no reconoce la expresion valida."
    }
    $tokens = (& java -cp bin Main --tokens tests/fixtures/valid.txt | Out-String)
    if ($tokens -notmatch "TOK_EOF" -or $tokens -match "ANALISIS SINTACTICO") {
        throw "CLI: --tokens no respeta el modo lexico."
    }
    $lexicalError = (& java -cp bin Main tests/fixtures/lexical_error.txt | Out-String)
    if ($lexicalError -notmatch "Error lexico:" -or $lexicalError -notmatch "Omitido por error lexico previo") {
        throw "CLI: manejo incorrecto del error lexico."
    }
    $syntaxError = (& java -cp bin Main tests/fixtures/syntax_error.txt | Out-String)
    if ($syntaxError -notmatch "Error sintactico:") {
        throw "CLI: manejo incorrecto del error sintactico."
    }
    $missingFile = (& java -cp bin Main tests/fixtures/no_existe.txt | Out-String)
    if ($missingFile -notmatch "Error al leer el archivo") {
        throw "CLI: manejo incorrecto del archivo inexistente."
    }
    Write-Host "OK: 5 comprobaciones de CLI."
} finally {
    Pop-Location
}
