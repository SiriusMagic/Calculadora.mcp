# Hoja de ruta de la Calculadora

## Fase 2 · Modo MCP (como Stone)
- Ajustes › **Modo MCP**: Activar MCP, Crear token (LLM o Hub, uso, validez, modo y herramientas), Analítica y
  **Redireccionar a Hub**.
- Herramientas `calc_*`: `calc_evaluate` (devuelve resultado o código de error), `calc_history` (leer, buscar),
  `calc_history_clear` (pide confirmación en el teléfono), `calc_last`.
- Llega a la IA por el relay y, en el hub, con el prefijo `calc_` (el módulo de calculadora integrado del hub se
  retira cuando esta app esté vinculada).

## Fase 3
- Memoria (M+, M−, MR), funciones científicas y conversión de unidades/moneda (esta última necesitaría internet).
