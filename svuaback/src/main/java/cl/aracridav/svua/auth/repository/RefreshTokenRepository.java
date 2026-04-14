package cl.aracridav.svua.auth.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.aracridav.svua.auth.entity.RefreshToken;
import cl.aracridav.svua.usuario.entity.Usuario;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long>{
    Optional<RefreshToken> findByToken(String usuario);

    List<RefreshToken> findByUsuario(Usuario usuario);

    void deleteByToken(String token);

    void deleteByUsuario(Usuario usuario);

    void deleteByExpiryDateBefore(Instant now);

    void deleteByUsuario(Optional<Usuario> usuario);
}
