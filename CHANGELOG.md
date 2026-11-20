# Calculadora 0.3.0

- **Modo MCP** (botón de arriba, junto a la ventana flotante), como en Stone y Luma: Activar MCP, Crear token (LLM o
  Hub, uso, solo lectura o lectura y escritura, validez), pausar, revocar y **Redireccionar a Hub** («Ir al Hub»).
- Tu IA usa la calculadora: `calc_evaluate` (cualquier operación, también científica), `calc_add`, `calc_subtract`,
  `calc_multiply`, `calc_divide`, `calc_percent`, `calc_power`, `calc_sqrt`, `calc_history`, `calc_convert_currency`,
  `calc_currencies`, `calc_convert_units` y `calc_units`. Con un token de escritura también `calc_type` (escribe en tu
  pantalla y puede pulsar «=») y `calc_history_clear`.
- Todo pasa por el mismo motor exacto: los números se validan y nunca se ejecuta texto como código.
- Llega por el mismo relay que Stone (`…/mcp?token=cal_…`); en el hub aparece como `calc_`.
- 22 pruebas.

# Calculadora 0.2.0

- **Aspecto como la calculadora del teléfono:** fondo negro, teclas redondas y «=» naranja.
- **Ventana flotante** (botón de arriba a la izquierda): una calculadora pequeña sobre otras apps, que se arrastra por su
  barra; comparte el historial. Android pide «Mostrar sobre otras apps» la primera vez; mientras está abierta hay una
  notificación (tocarla la cierra).
- **Historial** como en el teléfono: cálculos a la izquierda (tocar = reutilizar), operadores a la derecha, volver y borrar.
- **Modo científico** (botón Σ): ( ), x!, √, xʸ, sin, cos, tan (en grados), x², ln, log, π y e.
- **Conversor de divisas:** tres monedas con banderas, tasas de ExchangeRate-API guardadas para usarlas sin conexión,
  «Actualizar tasa de cambio» y «Usar la moneda local».
- **Conversor de unidades:** longitud, masa (con quintales), temperatura, área, volumen, velocidad, datos y tiempo.
- Permisos nuevos: internet (solo para las tasas), mostrar sobre otras apps y notificación de la ventana flotante.
- 15 pruebas (motor científico, teclado, conversores, tasas y capturas).

# Calculadora 0.1.0

- Primera versión: operaciones básicas, paréntesis, porcentaje de bolsillo, cuentas exactas en decimal, resultado en
  vivo, errores claros e historial de 100 cálculos solo en el teléfono.
- Tema oscuro con el amarillo de Stone, español e inglés, sin permisos.
- 9 pruebas (motor, teclado, historial y captura).
