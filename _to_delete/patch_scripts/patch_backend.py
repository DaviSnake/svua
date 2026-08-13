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

    content = content.replace(old, new, expected_count if expected_count > 0 else None)

    with open(path, 'wb') as f:
        f.write(content.encode('utf-8'))

    print(f"OK [{path}]: replaced {count} occurrence(s)")


BASE = "/sessions/rcw-01u2gwnbk5zzyctqiimc1ak9/mnt/svua/svuaback/src/main/java/cl/aracridav/svua"

# =========================================================================
# 1) Repository: agregar JpaSpecificationExecutor
# =========================================================================
REPO = f"{BASE}/mantenimiento/orden/repository/OrdenMantenimientoRepository.java"

apply_edit(
    REPO,
    "import org.springframework.data.jpa.repository.EntityGraph;\r\n"
    "import org.springframework.data.jpa.repository.JpaRepository;\r\n"
    "import org.springframework.data.jpa.repository.Modifying;\r\n",
    "import org.springframework.data.jpa.repository.EntityGraph;\r\n"
    "import org.springframework.data.jpa.repository.JpaRepository;\r\n"
    "import org.springframework.data.jpa.repository.JpaSpecificationExecutor;\r\n"
    "import org.springframework.data.jpa.repository.Modifying;\r\n"
)

apply_edit(
    REPO,
    "public interface OrdenMantenimientoRepository extends JpaRepository<OrdenMantenimiento, Long> {\r\n",
    "public interface OrdenMantenimientoRepository extends JpaRepository<OrdenMantenimiento, Long>,\r\n"
    "        // \U0001F525 Informe de Mantenciones: se arma el filtro con Specification\r\n"
    "        // (Criteria API), igual que en SesionUsuarioRepository, para no\r\n"
    "        // enviar nunca un bind param sin tipo (\"? IS NULL\") a Postgres.\r\n"
    "        JpaSpecificationExecutor<OrdenMantenimiento> {\r\n"
)

# =========================================================================
# 2) Service interface: agregar metodo obtenerInformeMantenciones
# =========================================================================
SVC_IFACE = f"{BASE}/mantenimiento/orden/service/OrdenMantenimientoService.java"

apply_edit(
    SVC_IFACE,
    "import java.math.BigDecimal;\r\n"
    "import java.time.LocalDateTime;\r\n"
    "import java.util.List;\r\n"
    "\r\n"
    "import org.springframework.core.io.Resource;\r\n"
    "import org.springframework.web.multipart.MultipartFile;\r\n"
    "\r\n"
    "import cl.aracridav.svua.mantenimiento.orden.dto.request.ActualizarOrdenMantenimientoRequest;\r\n"
    "import cl.aracridav.svua.mantenimiento.orden.dto.request.OrdenMantenimientoRequest;\r\n"
    "import cl.aracridav.svua.mantenimiento.orden.dto.response.CostosGraficoReponse;\r\n"
    "import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenEjecucionResponse;\r\n"
    "import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenMantenimientoResponse;\r\n"
    "import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;\r\n",

    "import java.math.BigDecimal;\r\n"
    "import java.time.LocalDate;\r\n"
    "import java.time.LocalDateTime;\r\n"
    "import java.util.List;\r\n"
    "\r\n"
    "import org.springframework.core.io.Resource;\r\n"
    "import org.springframework.data.domain.Page;\r\n"
    "import org.springframework.data.domain.Pageable;\r\n"
    "import org.springframework.web.multipart.MultipartFile;\r\n"
    "\r\n"
    "import cl.aracridav.svua.mantenimiento.orden.dto.request.ActualizarOrdenMantenimientoRequest;\r\n"
    "import cl.aracridav.svua.mantenimiento.orden.dto.request.OrdenMantenimientoRequest;\r\n"
    "import cl.aracridav.svua.mantenimiento.orden.dto.response.CostosGraficoReponse;\r\n"
    "import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenEjecucionResponse;\r\n"
    "import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenMantenimientoReporteResponse;\r\n"
    "import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenMantenimientoResponse;\r\n"
    "import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;\r\n"
)

apply_edit(
    SVC_IFACE,
    "    public Resource obtenerArchivo(Long id);\r\n}\r\n",
    "    public Resource obtenerArchivo(Long id);\r\n"
    "\r\n"
    "    // \U0001F525 Informe de Mantenciones: historial paginado y filtrable de\r\n"
    "    // ordenes COMPLETADAS, visible solo para SUPER_ADMIN.\r\n"
    "    public Page<OrdenMantenimientoReporteResponse> obtenerInformeMantenciones(\r\n"
    "            String usuario,\r\n"
    "            Long empresaId,\r\n"
    "            LocalDate fecha,\r\n"
    "            Pageable pageable);\r\n"
    "}\r\n"
)

print("DONE service interface + repository")
