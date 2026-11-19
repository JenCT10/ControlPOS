package com.controlpos.controlpos.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class NavegacionController {
    @ModelAttribute("puedeCargar")
    public boolean puedeCargar(Authentication usuario) {
        return usuario != null && usuario.getAuthorities().stream()
            .anyMatch(permiso -> permiso.getAuthority().equals("ROLE_SOPORTISTA")
                || permiso.getAuthority().equals("ROLE_ADMINISTRADOR"));
    }

    @ModelAttribute("revisor")
    public boolean esRevisor(Authentication usuario) {
        return usuario != null && usuario.getAuthorities().stream()
            .anyMatch(permiso -> permiso.getAuthority().equals("ROLE_SUPERVISOR") || permiso.getAuthority().equals("ROLE_ADMINISTRADOR"));
    }

    @ModelAttribute("administrador")
    public boolean esAdministrador(Authentication usuario) {
        return usuario != null && usuario.getAuthorities().stream()
            .anyMatch(permiso -> permiso.getAuthority().equals("ROLE_ADMINISTRADOR"));
    }
}
