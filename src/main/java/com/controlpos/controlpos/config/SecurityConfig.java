package com.controlpos.controlpos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.nio.charset.StandardCharsets;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    org.springframework.security.core.session.SessionRegistry sesiones() {
        return new org.springframework.security.core.session.SessionRegistryImpl();
    }

    @Bean
    org.springframework.security.web.session.HttpSessionEventPublisher eventosSesion() {
        return new org.springframework.security.web.session.HttpSessionEventPublisher();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            org.springframework.security.core.session.SessionRegistry sesiones) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**").permitAll()
                .requestMatchers("/bodegas").hasRole("ADMINISTRADOR")
                .requestMatchers("/usuarios", "/usuarios/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/aprobaciones", "/aprobaciones/**").hasAnyRole("ADMINISTRADOR", "SUPERVISOR")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/solicitudes").hasAnyRole("SOPORTISTA", "ADMINISTRADOR")
                .requestMatchers("/", "/terminales", "/terminales/**", "/comercios", "/movimientos", "/historial", "/gestiones", "/suministros")
                    .hasAnyRole("ADMINISTRADOR", "SOPORTISTA", "SUPERVISOR")
                .anyRequest().denyAll())
            .formLogin(login -> login.loginPage("/login").defaultSuccessUrl("/terminales", true))
            .sessionManagement(sesion -> sesion.maximumSessions(-1).sessionRegistry(sesiones))
            .logout(logout -> logout.logoutSuccessUrl("/login?logout"));
        return http.build();
    }

    @Bean
    @Profile("demo")
    org.springframework.security.provisioning.InMemoryUserDetailsManager usuarioDemo(Environment environment, PasswordEncoder encoder) {
        String clave = environment.getProperty("CONTROLPOS_DEMO_PASSWORD", "");
        if (clave.length() < 12 || clave.getBytes(StandardCharsets.UTF_8).length > 72) {
            return new org.springframework.security.provisioning.InMemoryUserDetailsManager();
        }

        String hash = encoder.encode(clave);
        var cuentas = new org.springframework.security.provisioning.InMemoryUserDetailsManager(
            User.withUsername("inventario.demo").password(hash).roles("SOPORTISTA").build(),
            User.withUsername("supervisor.demo").password(hash).roles("SUPERVISOR").build(),
            User.withUsername("admin.demo").password(hash).roles("ADMINISTRADOR").build());
        return cuentas;
    }
}