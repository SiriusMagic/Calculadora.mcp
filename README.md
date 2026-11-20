# Calculadora MCP

Calculadora Android sencilla y exacta. La meta del proyecto es que una IA (Claude, ChatGPT…) pueda **usar tu
calculadora y consultar su historial**, igual que Stone ofrece su música por MCP.

## Estado (0.1.0)

- Suma, resta, multiplicación, división, paréntesis, signo negativo y **porcentaje como en una calculadora de bolsillo**
  (`100 + 10 %` = 110).
- **Cuentas exactas en decimal** (`0,1 + 0,2` = `0,3`), resultado en vivo mientras escribes y errores claros
  (dividir entre cero, paréntesis sin cerrar…), nunca un cierre de la app.
- **Historial** de los últimos 100 cálculos, solo en el teléfono: tocar uno lo reutiliza; «Borrar todo».
- Sin permisos ni internet, sin anuncios, sin copias en la nube. Español e inglés.

APK: [`android/Calculadora-0.1.0.apk`](android/Calculadora-0.1.0.apk) (`com.zirakusa.calculadora`, firmado como
Stone y Gateway Hub).

## Compilar y probar

```bash
cd android
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
```

## Seguridad del motor

`engine/Calculator.kt` es un analizador propio con `BigDecimal`: **nunca ejecuta texto como código** (sin `eval`),
limita el largo (500 caracteres) y la profundidad (50 paréntesis o signos) y devuelve un código de error estable.

## Siguiente

Ver [ROADMAP.md](ROADMAP.md): Modo MCP (tokens, permisos, analítica, «Redireccionar a Hub»).
