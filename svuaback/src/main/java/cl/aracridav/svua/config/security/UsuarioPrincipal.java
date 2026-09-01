package cl.aracridav.svua.config.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import cl.aracridav.svua.usuario.entity.Usuario;


public class UsuarioPrincipal implements UserDetails {

    private Long id;
    private Long empresaId;
    private Boolean demo;
    private Boolean codigoQrHabilitado;
    private Boolean codigoEan13Habilitado;
    private Boolean controlTurnoHabilitado;
    private Boolean hojaControlHabilitado;
    private Boolean informeMantencionesHabilitado;
    private String email;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;

    public UsuarioPrincipal(Usuario usuario) {
        this.id = usuario.getId();
        this.empresaId = usuario.getEmpresa().getId();
        this.demo = usuario.getEmpresa().getDemo();
        this.codigoQrHabilitado = usuario.getEmpresa().getCodigoQrHabilitado();
        this.codigoEan13Habilitado = usuario.getEmpresa().getCodigoEan13Habilitado();
        this.controlTurnoHabilitado = usuario.getEmpresa().getControlTurnoHabilitado();
        this.hojaControlHabilitado = usuario.getEmpresa().getHojaControlHabilitado();
        this.informeMantencionesHabilitado = usuario.getEmpresa().getInformeMantencionesHabilitado();
        this.email = usuario.getEmail();
        this.password = usuario.getPassword();
        
        List<GrantedAuthority> auths = new ArrayList<>();

        // 🔹 Rol
        auths.add(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()));

        // 🔹 Permisos del rol
        usuario.getRol().getPermisos()
                .forEach(permiso ->
                        auths.add(
                                new SimpleGrantedAuthority(permiso.name())
                        )
                );

        this.authorities = auths;
    }

    public Long getId() {
        return id;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public Boolean getDemo() {
        return demo;
    }

    public Boolean getCodigoQrHabilitado() {
        return codigoQrHabilitado;
    }

    public Boolean getCodigoEan13Habilitado() {
        return codigoEan13Habilitado;
    }

    public Boolean getControlTurnoHabilitado() {
        return controlTurnoHabilitado;
    }

    public Boolean getHojaControlHabilitado() {
        return hojaControlHabilitado;
    }

    public Boolean getInformeMantencionesHabilitado() {
        return informeMantencionesHabilitado;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    // otros métodos en true

}
