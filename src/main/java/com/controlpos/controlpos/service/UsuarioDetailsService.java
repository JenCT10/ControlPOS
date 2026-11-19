package com.controlpos.controlpos.service;

import com.controlpos.controlpos.repository.UsuarioRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Profile("!demo")
public class UsuarioDetailsService implements UserDetailsService {
    private final UsuarioRepository usuarios;

    public UsuarioDetailsService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public UserDetails loadUserByUsername(String login) {
        var cuenta = usuarios.buscarPorLogin(login)
            .orElseThrow(() -> new UsernameNotFoundException("Cuenta no disponible"));

        return User.withUsername(cuenta.login())
            .password(cuenta.claveHash())
            .disabled(!cuenta.activo())
            .roles(cuenta.rol())
            .build();
    }
}
