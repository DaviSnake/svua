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


PATH = "/sessions/rcw-01u2gwnbk5zzyctqiimc1ak9/mnt/svua/svuaback/src/main/java/cl/aracridav/svua/mantenimiento/orden/service/OrdenMantenimientoServiceImpl.java"

# 1) java.time.LocalTime
apply_edit(
    PATH,
    "import java.time.LocalDateTime;\r\n"
    "import java.time.format.DateTimeFormatter;\r\n",
    "import java.time.LocalDateTime;\r\n"
    "import java.time.LocalTime;\r\n"
    "import java.time.format.DateTimeFormatter;\r\n"
)

# 2) Page / PageRequest / Sort / Specification
apply_edit(
    PATH,
    "import org.springframework.core.io.Resource;\r\n"
    "import org.springframework.core.io.UrlResource;\r\n"
    "import org.springframework.stereotype.Service;\r\n",
    "import org.springframework.core.io.Resource;\r\n"
    "import org.springframework.core.io.UrlResource;\r\n"
    "import org.springframework.data.domain.Page;\r\n"
    "import org.springframework.data.domain.PageRequest;\r\n"
    "import org.springframework.data.domain.Pageable;\r\n"
    "import org.springframework.data.domain.Sort;\r\n"
    "import org.springframework.data.jpa.domain.Specification;\r\n"
    "import org.springframework.stereotype.Service;\r\n"
)

# 3) jakarta.persistence.criteria.*
apply_edit(
    PATH,
    "import cl.aracridav.svua.usuario.entity.Usuario;\r\n"
    "import cl.aracridav.svua.usuario.repository.UsuarioRepository;\r\n"
    "import lombok.RequiredArgsConstructor;\r\n",
    "import cl.aracridav.svua.usuario.entity.Usuario;\r\n"
    "import cl.aracridav.svua.usuario.repository.UsuarioRepository;\r\n"
    "import jakarta.persistence.criteria.Join;\r\n"
    "import jakarta.persistence.criteria.JoinType;\r\n"
    "import jakarta.persistence.criteria.Predicate;\r\n"
    "import lombok.RequiredArgsConstructor;\r\n"
)

# 4) OrdenMantenimientoReporteResponse import
apply_edit(
    PATH,
    "import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenEjecucionResponse;\r\n"
    "import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenMantenimientoResponse;\r\n"
    "import cl.aracridav.svua.mantenimiento.orden.entity.EstadoOrden;\r\n",
    "import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenEjecucionResponse;\r\n"
    "import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenMantenimientoReporteResponse;\r\n"
    "import cl.aracridav.svua.mantenimiento.orden.dto.response.OrdenMantenimientoResponse;\r\n"
    "import cl.aracridav.svua.mantenimiento.orden.entity.EstadoOrden;\r\n"
)

# 5) OrdenRepuestoResponse import
apply_edit(
    PATH,
    "import cl.aracridav.svua.mantenimiento.ordenrepuesto.dto.request.OrdenRepuestoRequest;\r\n"
    "import cl.aracridav.svua.mantenimiento.ordenrepuesto.entity.OrdenRepuesto;\r\n",
    "import cl.aracridav.svua.mantenimiento.ordenrepuesto.dto.request.OrdenRepuestoRequest;\r\n"
    "import cl.aracridav.svua.mantenimiento.ordenrepuesto.dto.response.OrdenRepuestoResponse;\r\n"
    "import cl.aracridav.svua.mantenimiento.ordenrepuesto.entity.OrdenRepuesto;\r\n"
)

# 6) Nuevo metodo + helpers, al final de la clase
METHOD = '''
    // \U0001F525 Informe de Mantenciones: comprobante de ordenes COMPLETADAS,
    // filtrable por usuario/empresa/fecha, visible solo para SUPER_ADMIN.
    // Se arma con Specification (Criteria API) por la misma razon que el
    // informe de conexiones (SesionUsuarioServiceImpl): cada predicado solo
    // se agrega si el filtro viene informado, para no enviar nunca un
    // bind param sin tipo a Postgres.
    @Override
    @Transactional(readOnly = true)
    public Page<OrdenMantenimientoReporteResponse> obtenerInformeMantenciones(
            String usuario,
            Long empresaId,
            LocalDate fecha,
            Pageable pageable) {

        String usuarioFiltro =
            (usuario == null || usuario.isBlank())
                ? null
                : usuario.trim().toLowerCase();

        LocalDateTime desde = fecha != null ? fecha.atStartOfDay() : null;
        LocalDateTime hasta = fecha != null ? fecha.atTime(LocalTime.MAX) : null;

        Specification<OrdenMantenimiento> spec = (root, query, cb) -> {

            Join<OrdenMantenimiento, Empresa> empresaJoin =
                root.join("empresa", JoinType.INNER);
            Join<OrdenMantenimiento, Usuario> usuarioEjecucionJoin =
                root.join("usuarioEjecucion", JoinType.LEFT);
            Join<OrdenMantenimiento, Usuario> usuarioCreadorJoin =
                root.join("usuario", JoinType.INNER);

            // \U0001F525 El fetch solo aplica a la consulta de datos: en la
            // consulta de conteo (para la paginacion) Spring Data pide
            // resultType Long, y ahi un fetch no tiene sentido. No se hace
            // fetch de "repuestosUtilizados" (coleccion @OneToMany) porque
            // combinado con la paginacion produce el clasico problema de
            // Hibernate de paginar en memoria; se deja como lazy load
            // normal al mapear cada fila (la sesion sigue abierta porque
            // el metodo es @Transactional).
            if (query.getResultType() != Long.class) {
                root.fetch("activo", JoinType.INNER);
                root.fetch("empresa", JoinType.INNER);
                root.fetch("usuarioEjecucion", JoinType.LEFT);
                root.fetch("usuario", JoinType.INNER);
            }

            List<Predicate> predicates = new ArrayList<>();

            // \U0001F512 Este informe es un comprobante de trabajos ya
            // ejecutados: solo ordenes completadas.
            predicates.add(cb.equal(root.get("estado"), EstadoOrden.COMPLETADA));

            if (usuarioFiltro != null) {
                predicates.add(
                    cb.or(
                        cb.like(cb.lower(usuarioEjecucionJoin.get("nombre")), "%" + usuarioFiltro + "%"),
                        cb.like(cb.lower(usuarioCreadorJoin.get("nombre")), "%" + usuarioFiltro + "%")
                    ));
            }

            if (empresaId != null) {
                predicates.add(cb.equal(empresaJoin.get("id"), empresaId));
            }

            if (desde != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaEjecucion"), desde));
            }

            if (hasta != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaEjecucion"), hasta));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // \U0001F525 Orden fijo por fecha de ejecucion descendente, sin importar
        // el sort que traiga el Pageable del controller.
        Pageable pageableOrdenado = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(Sort.Direction.DESC, "fechaEjecucion"));

        return ordenRepository.findAll(spec, pageableOrdenado)
            .map(this::mapearInformeMantencion);
    }

    private OrdenMantenimientoReporteResponse mapearInformeMantencion(OrdenMantenimiento o) {

        List<OrdenRepuestoResponse> repuestos =
            o.getRepuestosUtilizados() == null
                ? List.of()
                : o.getRepuestosUtilizados().stream()
                    .map(this::mapearRepuestoInforme)
                    .toList();

        return OrdenMantenimientoReporteResponse.builder()
            .id(o.getId())
            .titulo(o.getTitulo())
            .estado(o.getEstado())
            .tipoMantenimiento(o.getTipoMantenimiento())
            .fechaProgramada(o.getFechaProgramada())
            .fechaEjecucion(o.getFechaEjecucion())
            .activoNombre(o.getActivo().getNombre())
            .empresaNombre(o.getEmpresa().getNombre())
            .usuarioNombre(
                o.getUsuarioEjecucion() != null
                    ? o.getUsuarioEjecucion().getNombre()
                    : o.getUsuario().getNombre())
            .proveedorNombre(
                o.getProveedor() != null
                    ? o.getProveedor().getNombre()
                    : null)
            .valorHoraProveedor(o.getValorHoraProveedor())
            .costoManoObraProveedor(o.getCostoManoObraProveedor())
            .costoTotal(o.getCostoTotal())
            .repuestos(repuestos)
            .build();
    }

    private OrdenRepuestoResponse mapearRepuestoInforme(OrdenRepuesto r) {

        OrdenRepuestoResponse dto = new OrdenRepuestoResponse();

        dto.setId(r.getId());
        dto.setRepuestoId(r.getRepuesto().getId());
        dto.setRepuestoNombre(r.getRepuesto().getNombre());
        dto.setCantidad(r.getCantidad());
        dto.setCostoUnitario(r.getCostoUnitario());
        dto.setCostoTotal(r.getCostoTotal());

        return dto;
    }
}
'''
METHOD_CRLF = METHOD.replace('\n', '\r\n')
# quitar el CRLF inicial extra que dejo el primer \n del triple-quote
METHOD_CRLF = METHOD_CRLF[2:]

apply_edit(
    PATH,
    "        ordenReprogramacionRepository.save(r);\r\n"
    "    }\r\n"
    "}\r\n",
    "        ordenReprogramacionRepository.save(r);\r\n"
    "    }\r\n"
    + METHOD_CRLF
)

print("DONE service impl")
