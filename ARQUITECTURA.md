# Arquitectura de la Calculadora

Carpeta = package (`android/app/src/main/java/com/zirakusa/calculadora/`):

| Carpeta | Qué hay |
|---|---|
| `engine/` | `Calculator` (analizador: exacto con `BigDecimal`; funciones científicas con 15 cifras), `Units` (categorías y factores exactos), `Currency` (`Rates`: lee la respuesta del proveedor y convierte). Sin Android. |
| `data/` | `HistoryStore`/`PrefsHistory` (últimos 100) y `RatesSource` (tasas guardadas + descarga de una dirección fija). |
| `ui/` | `CalculatorState` (lo escrito, resultado en vivo, error, modo científico), `CalculatorScreen`, `Keypad`, `HistoryPanel`, `TopBar`, `Theme`. |
| `ui/convert/` | `ConverterState` y `ConverterScreen` (filas, teclado 00/AC/⌫), `CurrencyScreen`, `UnitsScreen`. |
| `floating/` | `FloatingService` (ventana sobre otras apps, servicio en primer plano) y `FloatingCalculator`. |
| `MainActivity.kt` | Navegación entre calculadora, divisas y unidades. |

Reglas:
- Texto visible en `res/values/strings.xml` (español) y `values-en`.
- El motor nunca usa `eval`, scripts ni reflexión: todo pasa por el analizador.
