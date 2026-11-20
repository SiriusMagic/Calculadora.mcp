# Calculadora MCP

Calculadora Android sencilla y exacta. La meta del proyecto es que una IA (Claude, ChatGPT…) pueda **usar tu
calculadora y consultar su historial**, igual que Stone ofrece su música por MCP.

## Estado (0.3.0)

- Calculadora con el aspecto de la del teléfono, **modo científico** (sin, cos, tan en grados, ln, log, √, xʸ, x!, π, e)
  y cuentas exactas en decimal (`0,1 + 0,2` = `0,3`); porcentaje de bolsillo (`100 + 10 %` = 110).
- **Ventana flotante** sobre otras apps, **historial** compartido, **conversor de divisas** (tasas guardadas sin
  conexión) y **conversor de unidades**.
- **Modo MCP:** tu IA calcula, convierte, lee el historial y (con permiso) escribe en tu pantalla: herramientas `calc_*`.
- Internet solo para descargar las tasas de cambio (de una dirección fija) y para el Modo MCP (apagado por defecto); sin anuncios ni copias en la nube.

APK: [`android/Calculadora-0.3.0.apk`](android/Calculadora-0.3.0.apk) (`com.zirakusa.calculadora`, firmado como
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
