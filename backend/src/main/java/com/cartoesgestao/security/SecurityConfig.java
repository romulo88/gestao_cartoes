package com.cartoesgestao.security;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Sem isto, requisição sem token recebe 403 vazio; o frontend precisa do 401
                // para saber que a sessão venceu e mandar para o login.
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (req, res, e) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/cadastro",
                                // Painel somente leitura via link (token próprio no header,
                                // validado em CompartilhamentoService.resolver).
                                "/api/publico/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/error")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** O Next repassa o header Origin do navegador ao chamar o backend pelo proxy /api/*,
     * então o domínio de produção precisa estar aqui (mesma pegadinha do Commander). */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://192.168.*.*:*",
                "https://*.ngrok-free.app",
                "https://*.ngrok-free.dev",
                "https://cartoes.romtechsolucoes.com.br"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
