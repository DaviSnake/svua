package cl.aracridav.svua.config.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtService {

    @Value("${jwt.secret}")
    private String TU_SECRET_BASE64;

    @Value("${jwt.expiration-token}")
    private Long JWT_TIME_VALIDITY;

    public String generateToken(UserDetails user) {

        SecretKey key = Keys.hmacShaKeyFor(
                Base64.getDecoder().decode(TU_SECRET_BASE64));

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("role", user.getAuthorities().iterator().next().getAuthority())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + JWT_TIME_VALIDITY))
                .signWith(key)
                .compact();
    }

    public String generateToken(UsuarioPrincipal user, String tokenJti) {

        SecretKey key = Keys.hmacShaKeyFor(TU_SECRET_BASE64.getBytes());

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("rol",
                        user.getAuthorities()
                                .iterator()
                                .next()
                                .getAuthority()
                                .replace("ROLE_", "")
                )
                .claim("empresaId", user.getEmpresaId())
                .claim("userName", user.getUsername())
                .claim("jti", tokenJti)
                .claim("demo", user.getDemo())
                .claim("codigoQrHabilitado", user.getCodigoQrHabilitado())
                .claim("codigoEan13Habilitado", user.getCodigoEan13Habilitado())
                .claim("controlTurnoHabilitado", user.getControlTurnoHabilitado())
                .claim("hojaControlHabilitado", user.getHojaControlHabilitado())
                .claim("informeMantencionesHabilitado", user.getInformeMantencionesHabilitado())
                .claim("colorPrimario", user.getColorPrimario())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + JWT_TIME_VALIDITY))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    private Claims getClaims(String token) {

        SecretKey key = Keys.hmacShaKeyFor(
                TU_SECRET_BASE64.getBytes(StandardCharsets.UTF_8));

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractRol(String token) {
        return getClaims(token).get("rol", String.class);
    }
    
    public Long extractEmpresaId(String token) {
        return getClaims(token).get("empresaId", Long.class);
    }

    public String extractUserName(String token) {
        return getClaims(token).get("userName", String.class);
    }

    public String extractTokenJti(String token) {
        return getClaims(token).get("jti", String.class);
    }

}
