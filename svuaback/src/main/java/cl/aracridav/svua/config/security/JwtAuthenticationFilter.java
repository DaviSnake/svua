package cl.aracridav.svua.config.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final CustomUserDetailsService userDetailsService;

  @Override
  protected void doFilterInternal(HttpServletRequest request,
    HttpServletResponse response,
    FilterChain filterChain)
  throws ServletException, IOException {

    try {
        String authHeader = request.getHeader("Authorization");
    
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
          filterChain.doFilter(request, response);
          return;
        }
    
        String token = authHeader.substring(7);
        String username = jwtService.extractUserName(token);
    
        if (username != null &&
          SecurityContextHolder.getContext().getAuthentication() == null) {
    
          UserDetails user = userDetailsService.loadUserByUsername(username);
    
          UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(
              user,
              null,
              user.getAuthorities()
            );
    
          authToken.setDetails(
            new WebAuthenticationDetailsSource().buildDetails(request)
          );
    
          SecurityContextHolder.getContext().setAuthentication(authToken);
        }
    
        filterChain.doFilter(request, response);
    
    } catch (ExpiredJwtException e) {

      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType("application/json;charset=UTF-8");

      response.getWriter().write("""
        {
            "error": "TOKEN_EXPIRED",
            "message": "El token ha expirado"
        }
      """);

    }

  }

}
