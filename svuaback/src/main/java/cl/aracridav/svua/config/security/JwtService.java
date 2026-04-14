package cl.aracridav.svua.config.security;

import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
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
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("role", user.getAuthorities().iterator().next().getAuthority())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + JWT_TIME_VALIDITY)) // 2min
                .signWith(Keys.hmacShaKeyFor(TU_SECRET_BASE64.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateToken(UsuarioPrincipal user) {

        return Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .claim("rol",
                        user.getAuthorities()
                                .iterator()
                                .next()
                                .getAuthority()
                                .replace("ROLE_", "")
                )
                .claim("empresaId", user.getEmpresaId())
                .claim("userName", user.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + JWT_TIME_VALIDITY))
                .signWith(Keys.hmacShaKeyFor(TU_SECRET_BASE64.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(TU_SECRET_BASE64.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody();
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

}
