package cl.aracridav.svua.usuario.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cl.aracridav.svua.usuario.entity.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    Optional<Usuario> findByEmail(String email);

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


}
