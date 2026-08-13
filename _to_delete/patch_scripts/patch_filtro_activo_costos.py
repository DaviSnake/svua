#!/usr/bin/env python3
import sys

def apply_edit(path, old, new, expected_count=1):
    with open(path, 'rb') as f:
        content = f.read().decode('utf-8')

    count = content.count(old)
    if count != expected_count:
        print(f"FAIL [{path}]: expected {expected_count} occurrence(s) of anchor, found {count}")
        print("----- ANCHOR -----")
        print(old)
        print("------------------")
        sys.exit(1)

    content = content.replace(old, new, expected_count)

    with open(path, 'wb') as f:
        f.write(content.encode('utf-8'))

    print(f"OK [{path}]: replaced {count} occurrence(s)")


BASE = "/sessions/rcw-01u2gwnbk5zzyctqiimc1ak9/mnt/svua/svuaback/src/main/java/cl/aracridav/svua/mantenimiento/orden"

REPOSITORY = f"{BASE}/repository/OrdenMantenimientoRepository.java"
SERVICE = f"{BASE}/service/OrdenMantenimientoService.java"
SERVICEIMPL = f"{BASE}/service/OrdenMantenimientoServiceImpl.java"
CONTROLLER = f"{BASE}/controller/OrdenMantenimientoController.java"

# ============================================================
# 1) OrdenMantenimientoRepository.java: nuevo overload con filtro por
#    activo, calcado del que ya filtra solo por empresa.
# ============================================================

apply_edit(
    REPOSITORY,
    "    @Query(\"\"\"\r\n"
    "        SELECT \r\n"
    "            YEAR(o.fechaEjecucion),\r\n"
    "            MONTH(o.fechaEjecucion),\r\n"
    "            COALESCE(SUM(o.costoTotal), 0)\r\n"
    "        FROM OrdenMantenimiento o\r\n"
    "        WHERE o.fechaEjecucion >= :fechaInicio\r\n"
    "        AND o.activo.empresa.id = :empresaId\r\n"
    "        AND o.estado = 'COMPLETADA'\r\n"
    "        GROUP BY YEAR(o.fechaEjecucion), MONTH(o.fechaEjecucion)\r\n"
    "        ORDER BY YEAR(o.fechaEjecucion), MONTH(o.fechaEjecucion)\r\n"
    "    \"\"\")\r\n"
    "    List<Object[]> obtenerCostosUltimosMeses(\r\n"
    "        @Param(\"fechaInicio\") LocalDateTime fechaInicio,\r\n"
    "        @Param(\"empresaId\") Long empresaId);\r\n",

    "    @Query(\"\"\"\r\n"
    "        SELECT \r\n"
    "            YEAR(o.fechaEjecucion),\r\n"
    "            MONTH(o.fechaEjecucion),\r\n"
    "            COALESCE(SUM(o.costoTotal), 0)\r\n"
    "        FROM OrdenMantenimiento o\r\n"
    "        WHERE o.fechaEjecucion >= :fechaInicio\r\n"
    "        AND o.activo.empresa.id = :empresaId\r\n"
    "        AND o.estado = 'COMPLETADA'\r\n"
    "        GROUP BY YEAR(o.fechaEjecucion), MONTH(o.fechaEjecucion)\r\n"
    "        ORDER BY YEAR(o.fechaEjecucion), MONTH(o.fechaEjecucion)\r\n"
    "    \"\"\")\r\n"
    "    List<Object[]> obtenerCostosUltimosMeses(\r\n"
    "        @Param(\"fechaInicio\") LocalDateTime fechaInicio,\r\n"
    "        @Param(\"empresaId\") Long empresaId);\r\n"
    "\r\n"
    "    // \U0001F525 Mismo grafico de costos, pero acotado a un activo puntual\r\n"
    "    // (filtro opcional en el frontend: \"Evolucion Costo de Mantencion\").\r\n"
    "    // Se hace un metodo separado en vez de un \":activoId IS NULL OR ...\"\r\n"
    "    // en la misma query, siguiendo el mismo patron ya usado arriba para\r\n"
    "    // empresaId: en Postgres un bind param que solo aparece dentro de\r\n"
    "    // \"? IS NULL\" no siempre puede inferir su tipo.\r\n"
    "    @Query(\"\"\"\r\n"
    "        SELECT \r\n"
    "            YEAR(o.fechaEjecucion),\r\n"
    "            MONTH(o.fechaEjecucion),\r\n"
    "            COALESCE(SUM(o.costoTotal), 0)\r\n"
    "        FROM OrdenMantenimiento o\r\n"
    "        WHERE o.fechaEjecucion >= :fechaInicio\r\n"
    "        AND o.activo.empresa.id = :empresaId\r\n"
    "        AND o.activo.id = :activoId\r\n"
    "        AND o.estado = 'COMPLETADA'\r\n"
    "        GROUP BY YEAR(o.fechaEjecucion), MONTH(o.fechaEjecucion)\r\n"
    "        ORDER BY YEAR(o.fechaEjecucion), MONTH(o.fechaEjecucion)\r\n"
    "    \"\"\")\r\n"
    "    List<Object[]> obtenerCostosUltimosMesesPorActivo(\r\n"
    "        @Param(\"fechaInicio\") LocalDateTime fechaInicio,\r\n"
    "        @Param(\"empresaId\") Long empresaId,\r\n"
    "        @Param(\"activoId\") Long activoId);\r\n"
)

# ============================================================
# 2) OrdenMantenimientoService.java (interfaz)
# ============================================================

apply_edit(
    SERVICE,
    "    public CostosGraficoReponse obtenerGraficoCostosUltimos6Meses();\r\n",
    "    // \U0001F525 activoId es opcional: filtra el grafico de evolucion de\r\n"
    "    // costos a un solo activo (disponible para todos los usuarios).\r\n"
    "    public CostosGraficoReponse obtenerGraficoCostosUltimos6Meses(Long activoId);\r\n"
)

# ============================================================
# 3) OrdenMantenimientoServiceImpl.java
# ============================================================

apply_edit(
    SERVICEIMPL,
    "    public CostosGraficoReponse obtenerGraficoCostosUltimos6Meses() {\r\n"
    "\r\n"
    "        Long empresaId = SecurityUtils.getEmpresaId();\r\n"
    "\r\n"
    "        LocalDateTime fechaInicio = LocalDateTime.now()\r\n"
    "            .minusMonths(5)\r\n"
    "            .withDayOfMonth(1)\r\n"
    "            .withHour(0)\r\n"
    "            .withMinute(0)\r\n"
    "            .withSecond(0);\r\n"
    "\r\n"
    "        List<Object[]> resultados =\r\n"
    "            ordenRepository\r\n"
    "                .obtenerCostosUltimosMeses(fechaInicio, empresaId);\r\n",

    "    public CostosGraficoReponse obtenerGraficoCostosUltimos6Meses(Long activoId) {\r\n"
    "\r\n"
    "        Long empresaId = SecurityUtils.getEmpresaId();\r\n"
    "\r\n"
    "        LocalDateTime fechaInicio = LocalDateTime.now()\r\n"
    "            .minusMonths(5)\r\n"
    "            .withDayOfMonth(1)\r\n"
    "            .withHour(0)\r\n"
    "            .withMinute(0)\r\n"
    "            .withSecond(0);\r\n"
    "\r\n"
    "        // \U0001F525 Filtro por activo opcional: si no viene, se mantiene el\r\n"
    "        // comportamiento actual (costos de toda la empresa).\r\n"
    "        List<Object[]> resultados = activoId != null\r\n"
    "            ? ordenRepository\r\n"
    "                .obtenerCostosUltimosMesesPorActivo(fechaInicio, empresaId, activoId)\r\n"
    "            : ordenRepository\r\n"
    "                .obtenerCostosUltimosMeses(fechaInicio, empresaId);\r\n"
)

# ============================================================
# 4) OrdenMantenimientoController.java
# ============================================================

apply_edit(
    CONTROLLER,
    "    @GetMapping(\"/grafico/costos\")\r\n"
    "    public ResponseEntity<CostosGraficoReponse> obtenerGraficoCostos() {\r\n"
    "\r\n"
    "        return ResponseEntity.ok(\r\n"
    "            ordenMantenimientoService\r\n"
    "                .obtenerGraficoCostosUltimos6Meses()\r\n"
    "        );\r\n"
    "    }\r\n",

    "    @GetMapping(\"/grafico/costos\")\r\n"
    "    public ResponseEntity<CostosGraficoReponse> obtenerGraficoCostos(\r\n"
    "            @RequestParam(required = false) Long activoId) {\r\n"
    "\r\n"
    "        return ResponseEntity.ok(\r\n"
    "            ordenMantenimientoService\r\n"
    "                .obtenerGraficoCostosUltimos6Meses(activoId)\r\n"
    "        );\r\n"
    "    }\r\n"
)

print("DONE filtro activo costos")
