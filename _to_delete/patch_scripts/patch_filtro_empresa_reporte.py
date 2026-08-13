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


MNT = "/sessions/rcw-01u2gwnbk5zzyctqiimc1ak9/mnt/svua/svuaback/src/main/java/cl/aracridav/svua"

DASHBOARD_CONTROLLER = f"{MNT}/inventario/dashboard/controller/DashboardController.java"

ORDEN_BASE = f"{MNT}/mantenimiento/orden"
ORDEN_SERVICE = f"{ORDEN_BASE}/service/OrdenMantenimientoService.java"
ORDEN_SERVICEIMPL = f"{ORDEN_BASE}/service/OrdenMantenimientoServiceImpl.java"
ORDEN_CONTROLLER = f"{ORDEN_BASE}/controller/OrdenMantenimientoController.java"

# ============================================================
# 1) DashboardController.java: filtro por empresa opcional, solo
#    efectivo para SUPER_ADMIN (para el resto de roles se ignora y se
#    sigue usando la empresa del propio usuario, igual que antes).
# ============================================================

apply_edit(
    DASHBOARD_CONTROLLER,
    "import org.springframework.http.ResponseEntity;\r\n"
    "import org.springframework.web.bind.annotation.GetMapping;\r\n"
    "import org.springframework.web.bind.annotation.RequestMapping;\r\n"
    "import org.springframework.web.bind.annotation.RestController;\r\n",
    "import org.springframework.http.ResponseEntity;\r\n"
    "import org.springframework.web.bind.annotation.GetMapping;\r\n"
    "import org.springframework.web.bind.annotation.RequestMapping;\r\n"
    "import org.springframework.web.bind.annotation.RequestParam;\r\n"
    "import org.springframework.web.bind.annotation.RestController;\r\n"
)

apply_edit(
    DASHBOARD_CONTROLLER,
    "    @GetMapping\r\n"
    "    public ResponseEntity<DashboardIndicadoresResponse> getDashboard() {\r\n"
    "\r\n"
    "        Long empresaId = SecurityUtils.getEmpresaId();\r\n"
    "\r\n"
    "        return ResponseEntity.ok(\r\n"
    "                dashboardService.obtenerDashboard(empresaId)\r\n"
    "        );\r\n"
    "    }\r\n",
    "    // \U0001F512 Filtro por empresa opcional, solo para SUPER_ADMIN: si el\r\n"
    "    // usuario no es SUPER_ADMIN se ignora cualquier empresaId recibido y\r\n"
    "    // se usa siempre la empresa del propio usuario (igual que antes).\r\n"
    "    @GetMapping\r\n"
    "    public ResponseEntity<DashboardIndicadoresResponse> getDashboard(\r\n"
    "            @RequestParam(required = false) Long empresaId) {\r\n"
    "\r\n"
    "        Long empresaIdEfectivo = (empresaId != null && SecurityUtils.esSuperAdmin())\r\n"
    "            ? empresaId\r\n"
    "            : SecurityUtils.getEmpresaId();\r\n"
    "\r\n"
    "        return ResponseEntity.ok(\r\n"
    "                dashboardService.obtenerDashboard(empresaIdEfectivo)\r\n"
    "        );\r\n"
    "    }\r\n"
)

# ============================================================
# 2) OrdenMantenimientoService.java (interfaz)
# ============================================================

apply_edit(
    ORDEN_SERVICE,
    "    // \U0001F525 activoId es opcional: filtra el grafico de evolucion de\r\n"
    "    // costos a un solo activo (disponible para todos los usuarios).\r\n"
    "    public CostosGraficoReponse obtenerGraficoCostosUltimos6Meses(Long activoId);\r\n",
    "    // \U0001F525 activoId es opcional: filtra el grafico de evolucion de\r\n"
    "    // costos a un solo activo (disponible para todos los usuarios).\r\n"
    "    // \U0001F512 empresaId es opcional y solo tiene efecto para SUPER_ADMIN.\r\n"
    "    public CostosGraficoReponse obtenerGraficoCostosUltimos6Meses(\r\n"
    "            Long activoId, Long empresaId);\r\n"
)

# ============================================================
# 3) OrdenMantenimientoServiceImpl.java
# ============================================================

apply_edit(
    ORDEN_SERVICEIMPL,
    "    public CostosGraficoReponse obtenerGraficoCostosUltimos6Meses(Long activoId) {\r\n"
    "\r\n"
    "        Long empresaId = SecurityUtils.getEmpresaId();\r\n",
    "    public CostosGraficoReponse obtenerGraficoCostosUltimos6Meses(\r\n"
    "            Long activoId, Long empresaIdFiltro) {\r\n"
    "\r\n"
    "        // \U0001F512 Filtro por empresa opcional, solo para SUPER_ADMIN: para\r\n"
    "        // el resto de roles se ignora y se usa siempre la empresa del\r\n"
    "        // propio usuario (igual que antes).\r\n"
    "        Long empresaId = (empresaIdFiltro != null && SecurityUtils.esSuperAdmin())\r\n"
    "            ? empresaIdFiltro\r\n"
    "            : SecurityUtils.getEmpresaId();\r\n"
)

# ============================================================
# 4) OrdenMantenimientoController.java
# ============================================================

apply_edit(
    ORDEN_CONTROLLER,
    "    @GetMapping(\"/grafico/costos\")\r\n"
    "    public ResponseEntity<CostosGraficoReponse> obtenerGraficoCostos(\r\n"
    "            @RequestParam(required = false) Long activoId) {\r\n"
    "\r\n"
    "        return ResponseEntity.ok(\r\n"
    "            ordenMantenimientoService\r\n"
    "                .obtenerGraficoCostosUltimos6Meses(activoId)\r\n"
    "        );\r\n"
    "    }\r\n",
    "    // \U0001F512 empresaId es opcional y solo tiene efecto si quien llama es\r\n"
    "    // SUPER_ADMIN (ver OrdenMantenimientoServiceImpl).\r\n"
    "    @GetMapping(\"/grafico/costos\")\r\n"
    "    public ResponseEntity<CostosGraficoReponse> obtenerGraficoCostos(\r\n"
    "            @RequestParam(required = false) Long activoId,\r\n"
    "            @RequestParam(required = false) Long empresaId) {\r\n"
    "\r\n"
    "        return ResponseEntity.ok(\r\n"
    "            ordenMantenimientoService\r\n"
    "                .obtenerGraficoCostosUltimos6Meses(activoId, empresaId)\r\n"
    "        );\r\n"
    "    }\r\n"
)

print("DONE filtro empresa reporte")
