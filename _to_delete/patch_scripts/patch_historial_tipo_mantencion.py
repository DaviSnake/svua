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


BASE = "/sessions/rcw-01u2gwnbk5zzyctqiimc1ak9/mnt/svua/svuaback/src/main/java/cl/aracridav/svua/inventario/historial"

# ==========================================================
# 1) HistorialActivoResponse.java: agregar campo tipoMantenimiento
# ==========================================================
RESPONSE_PATH = f"{BASE}/dto/response/HistorialActivoResponse.java"

apply_edit(
    RESPONSE_PATH,
    "import java.math.BigDecimal;\r\n"
    "import java.time.LocalDateTime;\r\n"
    "import java.util.List;\r\n"
    "\r\n"
    "import lombok.Builder;\r\n"
    "import lombok.Data;\r\n",

    "import java.math.BigDecimal;\r\n"
    "import java.time.LocalDateTime;\r\n"
    "import java.util.List;\r\n"
    "\r\n"
    "import cl.aracridav.svua.mantenimiento.orden.entity.TipoMantenimiento;\r\n"
    "import lombok.Builder;\r\n"
    "import lombok.Data;\r\n"
)

apply_edit(
    RESPONSE_PATH,
    "    private String tipo;\r\n"
    "    private String descripcion;\r\n",

    "    private String tipo;\r\n"
    "    private TipoMantenimiento tipoMantenimiento;\r\n"
    "    private String descripcion;\r\n"
)

# ==========================================================
# 2) HistorialEstadoActivoServiceImpl.java: setear tipoMantenimiento
#    en los dos lugares que construyen eventos de tipo MANTENCION.
# ==========================================================
SERVICE_PATH = f"{BASE}/service/HistorialEstadoActivoServiceImpl.java"

# --- Bloque 1: obtenerHistorialCompleto(activoId) ---
apply_edit(
    SERVICE_PATH,
    "                            .fechaEjecucion(\r\n"
    "                                orden.getFechaEjecucion()\r\n"
    "                            )\r\n"
    "                            .tipo(\"MANTENCION\")\r\n"
    "                            .descripcion(\r\n"
    "                                \"Orden #\"\r\n"
    "                                    + orden.getId()\r\n",

    "                            .fechaEjecucion(\r\n"
    "                                orden.getFechaEjecucion()\r\n"
    "                            )\r\n"
    "                            .tipo(\"MANTENCION\")\r\n"
    "                            .tipoMantenimiento(\r\n"
    "                                orden.getTipoMantenimiento()\r\n"
    "                            )\r\n"
    "                            .descripcion(\r\n"
    "                                \"Orden #\"\r\n"
    "                                    + orden.getId()\r\n"
)

# --- Bloque 2: construirHistorialActivo(activo) ---
apply_edit(
    SERVICE_PATH,
    "                            .fechaProgramada(orden.getFechaProgramada())\r\n"
    "                            .fechaEjecucion(orden.getFechaEjecucion())\r\n"
    "                            .tipo(\"MANTENCION\")\r\n"
    "                            .descripcion(\r\n"
    "                                \"Orden #\" + orden.getId()\r\n",

    "                            .fechaProgramada(orden.getFechaProgramada())\r\n"
    "                            .fechaEjecucion(orden.getFechaEjecucion())\r\n"
    "                            .tipo(\"MANTENCION\")\r\n"
    "                            .tipoMantenimiento(orden.getTipoMantenimiento())\r\n"
    "                            .descripcion(\r\n"
    "                                \"Orden #\" + orden.getId()\r\n"
)

print("DONE historial tipo mantencion")
