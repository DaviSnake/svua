package cl.aracridav.svua.config.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import cl.aracridav.svua.multitenancy.EmpresaRequestFilter;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final JwtAuthenticationFilter jwtFilter;
    private final EmpresaRequestFilter empresaRequestFilter;
    private final RateLimitFilter rateLimitFilter;

    @Value("${app.cors.allowed-origins:http://localhost:4200,http://localhost}")
    private List<String> corsAllowedOrigins;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/svua/auth/**").permitAll()
                .requestMatchers("/api/v1/svua/public/**").permitAll()
                .requestMatchers("/api/v1/svua/notificacion/no-leidas/count").permitAll()
                .requestMatchers("/api/v1/svua/temperaturas/**").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/ws").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(customAuthenticationEntryPoint)
                .accessDeniedHandler(customAccessDeniedHandler)
            )
            // 🐛 FIX: JwtAuthenticationFilter.class debe registrarse PRIMERO
            // (via addFilterBefore relativo a UsernamePasswordAuthenticationFilter,
            // un filtro conocido por Spring Security) antes de poder usarlo
            // como referencia para rateLimitFilter -- si el
            // addFilterBefore(rateLimitFilter, JwtAuthenticationFilter.class)
            // se ejecuta ANTES de registrar el orden de JwtAuthenticationFilter,
            // Spring lanza "The Filter class ... does not have a registered
            // order" (falla al arrancar la app). El orden de estas líneas
            // importa: se ejecutan en el orden en que aparecen.
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            // 🔐 Rate limit de intentos por IP en login/request-reset/
            // reset-password, ANTES del filtro JWT.
            .addFilterBefore(rateLimitFilter, JwtAuthenticationFilter.class)
            .addFilterAfter(empresaRequestFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 🔥 ESTE BEAN ES EL QUE TE FALTABA
    @Bean
    AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // Configuración CORS: los orígenes vienen de app.cors.allowed-origins (application.properties / env var)
    private UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsAllowedOrigins);
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true); // si usas cookies/token en headers

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }

}
