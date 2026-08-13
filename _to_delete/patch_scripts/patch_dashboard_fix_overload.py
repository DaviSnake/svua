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

# Java no permite dos métodos con mismo nombre y mismos parámetros pero
# distinto tipo de retorno. Renombramos el método del dashboard "KPI
# completo" (DashboardResponse) a obtenerDashboardFull para no chocar
# con el obtenerDashboard(Long) que ya existía (DashboardIndicadoresResponse).

# 1) Interfaz
apply_edit(
    f"{BASE}/service/DashboardService.java",
    "    public DashboardResponse obtenerDashboard(Long empresaId);\r\n",
    "    public DashboardResponse obtenerDashboardFull(Long empresaId);\r\n"
)

# 2) Implementación
apply_edit(
    f"{BASE}/service/DashboardServiceImpl.java",
    "    @Override\r\n"
    "    public DashboardResponse obtenerDashboard(Long empresaId) {\r\n",
    "    @Override\r\n"
    "    public DashboardResponse obtenerDashboardFull(Long empresaId) {\r\n"
)

# 3) Controller: el endpoint /full debe llamar al nuevo nombre
apply_edit(
    f"{BASE}/controller/DashboardController.java",
    "        return ResponseEntity.ok(\r\n"
    "            dashboardService.obtenerDashboard(empresaIdEfectivo)\r\n"
    "        );\r\n"
    "    }\r\n"
    "\r\n"
    "    @GetMapping(\"/cumplimiento/semanal\")\r\n",

    "        return ResponseEntity.ok(\r\n"
    "            dashboardService.obtenerDashboardFull(empresaIdEfectivo)\r\n"
    "        );\r\n"
    "    }\r\n"
    "\r\n"
    "    @GetMapping(\"/cumplimiento/semanal\")\r\n"
)

print("DONE fix overload")
