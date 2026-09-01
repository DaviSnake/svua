package cl.aracridav.svua.shared.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import cl.aracridav.svua.config.security.UsuarioPrincipal;
import cl.aracridav.svua.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    public static Long getEmpresaId() {
        return getPrincipal().getEmpresaId();
    }

    public static Long getUsuarioId() {
        return getPrincipal().getId();
    }

    // 🔳 Empresa "demo" (Empresa.demo = true en la BD, ya viaja en el JWT
    // como claim "demo"): usado para funcionalidades habilitadas solo para
    // esa empresa + SUPER_ADMIN (ej. escaneo de QR/EAN13 de activos).
    public static boolean esEmpresaDemo() {
        Boolean demo = getPrincipal().getDemo();
        return Boolean.TRUE.equals(demo);
    }

    // 🔳 Flags de configuracion por empresa (Empresa.codigoQrHabilitado /
    // codigoEan13Habilitado, tambien viajan en el JWT): reemplazan a
    // esEmpresaDemo() como criterio para exponer el QR/EAN13 de un activo
    // y para habilitar el escaneo (ver ActivoServiceImpl).
    public static boolean tieneCodigoQrHabilitado() {
        return Boolean.TRUE.equals(getPrincipal().getCodigoQrHabilitado());
    }

    public static boolean tieneCodigoEan13Habilitado() {
        return Boolean.TRUE.equals(getPrincipal().getCodigoEan13Habilitado());
    }

    // 🔳 Flag de configuracion por empresa (Empresa.controlTurnoHabilitado,
    // tambien viaja en el JWT): controla si el modulo Control de Turno
    // esta disponible para la empresa del usuario logueado (ver V33,
    // PuntoControlServiceImpl, LecturaControlServiceImpl).
    public static boolean tieneControlTurnoHabilitado() {
        return Boolean.TRUE.equals(getPrincipal().getControlTurnoHabilitado());
    }

    // 🔳 Flag de configuracion por empresa (Empresa.hojaControlHabilitado,
    // tambien viaja en el JWT): controla si el boton "Importar Excel
    // (HOJA DE CONTROL)" esta disponible para la empresa del usuario
    // logueado (ver V36, HojaControlImportServiceImpl).
    public static boolean tieneHojaControlHabilitado() {
        return Boolean.TRUE.equals(getPrincipal().getHojaControlHabilitado());
    }

    // 🔳 Flag de configuracion por empresa
    // (Empresa.informeMantencionesHabilitado, tambien viaja en el JWT):
    // controla si el Informe de Mantenciones esta disponible para la
    // empresa del usuario logueado (ver V37,
    // OrdenMantenimientoServiceImpl).
    public static boolean tieneInformeMantencionesHabilitado() {
        return Boolean.TRUE.equals(getPrincipal().getInformeMantencionesHabilitado());
    }

    private static UsuarioPrincipal getPrincipal() {

        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof UsuarioPrincipal principal)) {
            throw new BusinessException("Usuario no autenticado");
        }

        return principal;
    }

    public static boolean esSuperAdmin() {

        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        return auth.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_SUPER_ADMIN"));
    }

    public static boolean esAdminEmpresa() {

        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        return auth.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN_EMPRESA"));
    }

    // 🔒 Ingreso retroactivo de ordenes de mantencion (ver
    // OrdenMantenimientoServiceImpl y ExcelImportServiceImpl): solo
    // Super Admin o Admin Empresa pueden declarar una orden ya
    // completada con tiempo real editable, saltandose el flujo en vivo.
    public static boolean puedeIngresarRetroactivo() {
        return esSuperAdmin() || esAdminEmpresa();
    }

}
