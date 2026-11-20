# Calculadora MCP: reglas para agentes

- Lee `ARQUITECTURA.md` y abre solo los archivos que necesites.
- Pruebas: `cd android && ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease` (debe pasar todo).
  Nunca desactives ni saltes una prueba. Lint sin avisos nuevos.
- Prohibido: `eval`, motores de scripts o reflexión para calcular; todo pasa por `engine/Calculator.kt`.
- Cada versión: sube `versionCode` y `versionName`, copia el APK a `android/Calculadora-<versión>.apk` (borra el
  anterior) y anótalo en `CHANGELOG.md`.
- Firma: la de depuración del entorno, igual que Stone y Gateway Hub.
