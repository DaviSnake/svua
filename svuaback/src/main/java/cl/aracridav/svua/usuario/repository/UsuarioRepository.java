package cl.aracridav.svua.usuario.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cl.aracridav.svua.usuario.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    Optional<Usuario> findByEmail(String email);

    @Query("""
        SELECT u
        FROM Usuario u
        JOIN FETCH u.empresa
        WHERE u.email = :email
        """)
        Optional<Usuario> findByEmailWithEmpresa(@Param("email") String email);

    boolean existsByEmail(String email);

    @Query("""
        SELECT u FROM Usuario u
        WHERE u.activo = true
    """)
    List<Usuario> findUsuariosActivos();

    @Query("""
        SELECT u FROM Usuario u
        WHERE u.rol = :rol
    """)
    List<Usuario> findByRol(@Param("rol") String rol);

    Page<Usuario> findByEmpresaId(Long empresaId, Pageable pageable);

    // 🔥 Busqueda unificada para la grilla: empresaId es opcional (NULL =
    // todas las empresas, solo aplica para SUPER_ADMIN) y busqueda es
    // opcional (NULL o vacio = sin filtro de texto). Mismo patron que
    // ActivoRepository.buscarActivos.
    @Query("""
        SELECT u FROM Usuario u
        WHERE (:empresaId IS NULL OR u.empresa.id = :empresaId)
        AND (:busqueda IS NULL OR :busqueda = '' OR
             LOWER(u.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR
             LOWER(u.email) LIKE LOWER(CONCAT('%', :busqueda, '%')))
    """)
    Page<Usuario> buscarUsuarios(
        @Param("empresaId") Long empresaId,
        @Param("busqueda") String busqueda,
        Pageable pageable
    );

}
