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


PATH = "/sessions/rcw-01u2gwnbk5zzyctqiimc1ak9/mnt/svua/svuaback/src/main/java/cl/aracridav/svua/mantenimiento/orden/controller/OrdenMantenimientoController.java"

# 1) imports: LocalDate, Page, PageRequest, Pageable, DateTimeFormat
apply_edit(
    PATH,
    "import java.io.IOException;\r\n"
    "import java.nio.file.Files;\r\n"
    "import java.nio.file.Paths;\r\n"
    "import java.util.List;\r\n"
    "\r\n"
    "import org.springframework.core.io.Resource;\r\n"
    "import org.springframework.http.HttpStatus;\r\n",

    "import java.io.IOException;\r\n"
    "import java.nio.file.Files;\r\n"
    "import java.nio.file.Paths;\r\n"
    "import java.time.LocalDate;\r\n"
    "import java.util.List;\r\n"
    "\r\n"
    "import org.springframework.core.io.Resource;\r\n"
    "import org.springframework.data.domain.Page;\r\n"
    "import org.springframework.data.domain.PageRequest;\r\n"
    "import org.springframework.data.domain.Pageable;\r\n"
    "import org.springframework.format.annotation.DateTimeFormat;\r\n"
    "import org.springframework.http.HttpStatus;\r\n"
)

# 2) import DTO nuevo
apply_edit(
    PATH,
    "import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenEjecucionResponse;\r\n"
    "import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenMantenimientoResponse;\r\n"
    "import cl.aracridav.svua.mantenimiento.orden.service.OrdenMantenimientoService;\r\n",

    "import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenEjecucionResponse;\r\n"
    "import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenMantenimientoReporteResponse;\r\n"
    "import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenMantenimientoResponse;\r\n"
    "import cl.aracridav.svua.mantenimiento.orden.service.OrdenMantenimientoService;\r\n"
)

# 3) nuevo endpoint, al final de la clase
apply_edit(
    PATH,
    "    @GetMapping(\"/grafico/costos\")\r\n"
    "    public ResponseEntity<CostosGraficoReponse> obtenerGraficoCostos() {\r\n"
    "\r\n"
    "        return ResponseEntity.ok(\r\n"
    "            ordenMantenimientoService\r\n"
    "                .obtenerGraficoCostosUltimos6Meses()\r\n"
    "        );\r\n"
    "    }\r\n"
    "\r\n"
    "}\r\n",

    "    @GetMapping(\"/grafico/costos\")\r\n"
    "    public ResponseEntity<CostosGraficoReponse> obtenerGraficoCostos() {\r\n"
    "\r\n"
    "        return ResponseEntity.ok(\r\n"
    "            ordenMantenimientoService\r\n"
    "                .obtenerGraficoCostosUltimos6Meses()\r\n"
    "        );\r\n"
    "    }\r\n"
    "\r\n"
    "    // \U0001F525 Informe de Mantenciones: historial paginado y filtrable de\r\n"
    "    // ordenes completadas, visible solo para SUPER_ADMIN.\r\n"
    "    @PreAuthorize(\"hasRole('SUPER_ADMIN')\")\r\n"
    "    @GetMapping(\"/informe\")\r\n"
    "    public Page<OrdenMantenimientoReporteResponse> obtenerInformeMantenciones(\r\n"
    "            @RequestParam(required = false) String usuario,\r\n"
    "            @RequestParam(required = false) Long empresaId,\r\n"
    "            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,\r\n"
    "            @RequestParam(defaultValue = \"0\") int page,\r\n"
    "            @RequestParam(defaultValue = \"10\") int size) {\r\n"
    "\r\n"
    "        Pageable pageable = PageRequest.of(page, size);\r\n"
    "\r\n"
    "        return ordenMantenimientoService\r\n"
    "            .obtenerInformeMantenciones(usuario, empresaId, fecha, pageable);\r\n"
    "    }\r\n"
    "\r\n"
    "}\r\n"
)

print("DONE controller")
