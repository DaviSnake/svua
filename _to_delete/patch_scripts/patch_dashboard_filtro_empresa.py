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


BASE = "/sessions/rcw-01u2gwnbk5zzyctqiimc1ak9/mnt/svua/svuaback/src/main/java/cl/aracridav/svua/inventario/dashboard"

# ==========================================================
# 1) DashboardService.java: el metodo obtenerDashboard() ahora
#    recibe un empresaId (igual al patron de obtenerDashboard(Long)
#    que ya existia para DashboardIndicadoresResponse).
# ==========================================================
SERVICE_IFACE_PATH = f"{BASE}/service/DashboardService.java"

apply_edit(
    SERVICE_IFACE_PATH,
    "    public DashboardResponse obtenerDashboard();\r\n",
    "    public DashboardResponse obtenerDashboard(Long empresaId);\r\n"
)

# ==========================================================
# 2) DashboardServiceImpl.java: usar el empresaId recibido en vez
#    de tomarlo siempre desde SecurityUtils.getEmpresaId().
# ==========================================================
SERVICE_IMPL_PATH = f"{BASE}/service/DashboardServiceImpl.java"

apply_edit(
    SERVICE_IMPL_PATH,
    "    @Override\r\n"
    "    public DashboardResponse obtenerDashboard() {\r\n"
    "\r\n"
    "        Long empresaId = SecurityUtils.getEmpresaId();\r\n"
    "\r\n"
    "        // KPIs\r\n",

    "    @Override\r\n"
    "    public DashboardResponse obtenerDashboard(Long empresaId) {\r\n"
    "\r\n"
    "        // KPIs\r\n"
)

# ==========================================================
# 3) DashboardController.java: endpoint /full acepta empresaId
#    opcional, con efecto solo para SUPER_ADMIN (mismo patron que
#    el endpoint GET /dashboard existente).
# ==========================================================
CONTROLLER_PATH = f"{BASE}/controller/DashboardController.java"

apply_edit(
    CONTROLLER_PATH,
    "    @GetMapping(\"/full\")\r\n"
    "    public ResponseEntity<DashboardResponse> obtenerDashboard() {\r\n"
    "        return ResponseEntity.ok(dashboardService.obtenerDashboard());\r\n"
    "    }\r\n",

    "    // \U0001F512 Filtro por empresa opcional, solo para SUPER_ADMIN: si el\r\n"
    "    // usuario no es SUPER_ADMIN se ignora cualquier empresaId recibido y\r\n"
    "    // se usa siempre la empresa del propio usuario (igual que antes).\r\n"
    "    @GetMapping(\"/full\")\r\n"
    "    public ResponseEntity<DashboardResponse> obtenerDashboard(\r\n"
    "            @RequestParam(required = false) Long empresaId) {\r\n"
    "\r\n"
    "        Long empresaIdEfectivo = (empresaId != null && SecurityUtils.esSuperAdmin())\r\n"
    "            ? empresaId\r\n"
    "            : SecurityUtils.getEmpresaId();\r\n"
    "\r\n"
    "        return ResponseEntity.ok(\r\n"
    "            dashboardService.obtenerDashboard(empresaIdEfectivo)\r\n"
    "        );\r\n"
    "    }\r\n"
)

print("DONE dashboard filtro empresa")
