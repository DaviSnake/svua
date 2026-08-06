package cl.aracridav.svua.usuario.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.aracridav.svua.usuario.entity.PasswordResetToken;
import cl.aracridav.svua.usuario.entity.Usuario;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long>{

    Optional<PasswordResetToken> findByToken(String token);

    void deleteByUser(Usuario user);

}
