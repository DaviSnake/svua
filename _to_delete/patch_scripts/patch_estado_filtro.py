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

SERVICE = f"{BASE}/service/OrdenMantenimientoService.java"
SERVICEIMPL = f"{BASE}/service/OrdenMantenimientoServiceImpl.java"
CONTROLLER = f"{BASE}/controller/OrdenMantenimientoController.java"

# ============================================================
# 1) OrdenMantenimientoService.java (interfaz)
# ============================================================

# 1a) import EstadoOrden
apply_edit(
    SERVICE,
    "import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenMantenimientoResponse;\r\n"
    "import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;\r\n",
    "import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenMantenimientoResponse;\r\n"
    "import cl.aracridav.svua.mantenimiento.orden.entity.EstadoOrden;\r\n"
    "import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;\r\n"
)

# 1b) firma del metodo
apply_edit(
    SERVICE,
    "    public Page<OrdenMantenimientoReporteResponse> obtenerInformeMantenciones(\r\n"
    "            String usuario,\r\n"
    "            Long empresaId,\r\n"
    "            LocalDate fecha,\r\n"
    "            Pageable pageable);\r\n"
    "}\r\n",
    "    public Page<OrdenMantenimientoReporteResponse> obtenerInformeMantenciones(\r\n"
    "            String usuario,\r\n"
    "            Long empresaId,\r\n"
    "            EstadoOrden estado,\r\n"
    "            LocalDate fecha,\r\n"
    "            Pageable pageable);\r\n"
    "}\r\n"
)

# ============================================================
# 2) OrdenMantenimientoServiceImpl.java
# ============================================================

# 2a) firma del metodo
apply_edit(
    SERVICEIMPL,
    "    public Page<OrdenMantenimientoReporteResponse> obtenerInformeMantenciones(\r\n"
    "            String usuario,\r\n"
    "            Long empresaId,\r\n"
    "            LocalDate fecha,\r\n"
    "            Pageable pageable) {\r\n",
    "    public Page<OrdenMantenimientoReporteResponse> obtenerInformeMantenciones(\r\n"
    "            String usuario,\r\n"
    "            Long empresaId,\r\n"
    "            EstadoOrden estado,\r\n"
    "            LocalDate fecha,\r\n"
    "            Pageable pageable) {\r\n"
)

# 2b) predicado de estado: de fijo (solo COMPLETADA) a filtro opcional
apply_edit(
    SERVICEIMPL,
    "            List<Predicate> predicates = new ArrayList<>();\r\n"
    "\r\n"
    "            // \U0001F512 Este informe es un comprobante de trabajos ya\r\n"
    "            // ejecutados: solo ordenes completadas.\r\n"
    "            predicates.add(cb.equal(root.get(\"estado\"), EstadoOrden.COMPLETADA));\r\n"
    "\r\n"
    "            if (usuarioFiltro != null) {\r\n",
    "            List<Predicate> predicates = new ArrayList<>();\r\n"
    "\r\n"
    "            // \U0001F512 Filtro por estado opcional: el frontend parte con\r\n"
    "            // \"Completada\" seleccionada (para que el informe siga\r\n"
    "            // funcionando por defecto como comprobante de trabajos ya\r\n"
    "            // ejecutados), pero el usuario puede elegir otro estado o\r\n"
    "            // \"Todos\" para no filtrar por estado.\r\n"
    "            if (estado != null) {\r\n"
    "                predicates.add(cb.equal(root.get(\"estado\"), estado));\r\n"
    "            }\r\n"
    "\r\n"
    "            if (usuarioFiltro != null) {\r\n"
)

# ============================================================
# 3) OrdenMantenimientoController.java
# ============================================================

# 3a) import EstadoOrden
apply_edit(
    CONTROLLER,
    "import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenMantenimientoResponse;\r\n"
    "import cl.aracridav.svua.mantenimiento.orden.service.OrdenMantenimientoService;\r\n",
    "import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenMantenimientoResponse;\r\n"
    "import cl.aracridav.svua.mantenimiento.orden.entity.EstadoOrden;\r\n"
    "import cl.aracridav.svua.mantenimiento.orden.service.OrdenMantenimientoService;\r\n"
)

# 3b) parametro + llamada al service
apply_edit(
    CONTROLLER,
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
    "    }\r\n",
    "    public Page<OrdenMantenimientoReporteResponse> obtenerInformeMantenciones(\r\n"
    "            @RequestParam(required = false) String usuario,\r\n"
    "            @RequestParam(required = false) Long empresaId,\r\n"
    "            @RequestParam(required = false) EstadoOrden estado,\r\n"
    "            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,\r\n"
    "            @RequestParam(defaultValue = \"0\") int page,\r\n"
    "            @RequestParam(defaultValue = \"10\") int size) {\r\n"
    "\r\n"
    "        Pageable pageable = PageRequest.of(page, size);\r\n"
    "\r\n"
    "        return ordenMantenimientoService\r\n"
    "            .obtenerInformeMantenciones(usuario, empresaId, estado, fecha, pageable);\r\n"
    "    }\r\n"
)

print("DONE estado filtro")
