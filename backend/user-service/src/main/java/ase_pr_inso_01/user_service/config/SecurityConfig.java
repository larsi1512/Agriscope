package ase_pr_inso_01.user_service.config;

import ase_pr_inso_01.user_service.security.JwtAuthFilter;
import jakarta.ws.rs.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;


@Configuration
public class SecurityConfig {
  private final JwtAuthFilter jwtAuthFilter;

  public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
    this.jwtAuthFilter = jwtAuthFilter;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
            .cors(cors -> cors.disable());

    http.sessionManagement(sm ->
            sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

    //TODO: Test this part
    http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/authentication/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
//            .requestMatchers(HttpMethod.GET, "/api/users/password-reset").permitAll()
//            .requestMatchers(HttpMethod.PUT, "/api/users/password-reset").permitAll()
            .requestMatchers( "/api/users/**").permitAll()
            .anyRequest().authenticated()
    );

    http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
    return authConfig.getAuthenticationManager();
  }
}
