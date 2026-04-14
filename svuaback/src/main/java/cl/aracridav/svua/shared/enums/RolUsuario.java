package cl.aracridav.svua.shared.enums;

import java.util.Set;

public enum RolUsuario {
    
    SUPER_ADMIN(Set.of(Permiso.values())),

    ADMIN_EMPRESA(Set.of(
            Permiso.EMPRESA_VIEW,
            Permiso.ACTIVO_CREATE, Permiso.ACTIVO_UPDATE, Permiso.ACTIVO_VIEW,
            Permiso.PROVEEDOR_CREATE, Permiso.PROVEEDOR_DELETE, Permiso.PROVEEDOR_UPDATE, Permiso.PROVEEDOR_VIEW,
            Permiso.UBICACION_CREATE, Permiso.UBICACION_UPDATE, Permiso.UBICACION_DELETE, Permiso.UBICACION_VIEW,
            Permiso.TIPO_ACTIVO_CREATE, Permiso.TIPO_ACTIVO_UPDATE, Permiso.TIPO_ACTIVO_DELETE, Permiso.TIPO_ACTIVO_VIEW,
            Permiso.USUARIO_CREATE, Permiso.USUARIO_UPDATE, Permiso.USUARIO_DELETE, Permiso.USUARIO_VIEW,
            Permiso.BODEGA_CREATE, Permiso.BODEGA_UPDATE, Permiso.BODEGA_DELETE, Permiso.BODEGA_VIEW,
            Permiso.ORDEN_MANT_CREATE, Permiso.ORDEN_MANT_UPDATE, Permiso.ORDEN_MANT_CLOSE, Permiso.ORDEN_MANT_VIEW,
            Permiso.ORDEN_REPUESTO_CREATE, Permiso.ORDEN_REPUESTO_UPDATE, Permiso.ORDEN_REPUESTO_VIEW,
            Permiso.REPUESTO_CREATE, Permiso.REPUESTO_UPDATE, Permiso.REPUESTO_DELETE, Permiso.REPUESTO_VIEW,
            Permiso.PLAN_CREATE, Permiso.PLAN_UPDATE, Permiso.PLAN_DELETE, Permiso.PLAN_VIEW,
            Permiso.MOVIMIENTO_CREATE, Permiso.MOVIMIENTO_VIEW,
            Permiso.DEPRECIACION_CREATE, Permiso.DEPRECIACION_VIEW,
            Permiso.HISTORIAL_VIEW
    )),

    JEFE_MANTENIMIENTO(Set.of(
            Permiso.ACTIVO_VIEW,
            Permiso.ORDEN_MANT_CREATE,
            Permiso.ORDEN_MANT_UPDATE,
            Permiso.ORDEN_MANT_CLOSE,
            Permiso.PLAN_CREATE,
            Permiso.PLAN_VIEW
    )),

    TECNICO(Set.of(
            Permiso.ACTIVO_VIEW,
            Permiso.ORDEN_MANT_VIEW,
            Permiso.ORDEN_MANT_UPDATE
    )),

    BODEGUERO(Set.of(
            Permiso.REPUESTO_CREATE,
            Permiso.STOCK_UPDATE,
            Permiso.STOCK_VIEW,
            Permiso.MOVIMIENTO_CREATE
    )),

    USUARIO(Set.of(
            Permiso.ACTIVO_VIEW,
            Permiso.USUARIO_VIEW,
            Permiso.EMPRESA_VIEW
    ));

    private final Set<Permiso> permisos;

    RolUsuario(Set<Permiso> permisos) {
        this.permisos = permisos;
    }

    public Set<Permiso> getPermisos() {
        return permisos;
    }
}
