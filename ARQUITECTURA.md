# Arquitectura de la Calculadora

Carpeta = package (`android/app/src/main/java/com/zirakusa/calculadora/`):

| Carpeta | Qué hay |
|---|---|
| `engine/` | `Calculator`: analizador y cálculo exacto (`BigDecimal`), `CalcError` con códigos estables. Sin Android: se prueba solo. |
| `data/` | `HistoryStore` (últimos 100, JSON) y `PrefsHistory` (preferencias del teléfono). |
| `ui/` | `CalculatorState` (único dueño del estado: lo escrito, resultado en vivo, error, historial), `CalculatorScreen`, `Keypad`, `Theme`. |
| `MainActivity.kt` | Solo monta la pantalla. |

Reglas:
- Texto visible en `res/values/strings.xml` (español) y `values-en`.
- El motor nunca usa `eval`, scripts ni reflexión: todo pasa por el analizador.
