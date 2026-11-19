package com.controlpos.controlpos.controller;

import com.controlpos.controlpos.model.EstadoTerminal;
import com.controlpos.controlpos.service.GestionService;

import com.controlpos.controlpos.model.Tipo;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;




@Controller
@Profile("demo")
public class ComercioController {
    private final GestionService operaciones;

    public ComercioController(GestionService operaciones) {
        this.operaciones = operaciones;
    }

    @GetMapping("/comercios")
    public String ver(Authentication usuario, Model modelo) {
        modelo.addAttribute("seccion", "comercios");
        modelo.addAttribute("comercios", operaciones.comercios());
        modelo.addAttribute("altasComercio", operaciones.solicitudes().stream()
            .filter(s -> s.datos().tipo() == Tipo.ALTA_COMERCIO)
            .filter(s -> s.solicitante().equals(operaciones.identidad(usuario.getName())))
            .filter(s -> !s.estado().equals("Aprobada")).toList());
        modelo.addAttribute("puedeSolicitar", usuario.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_SOPORTISTA") || a.getAuthority().equals("ROLE_ADMINISTRADOR")));
        return "operaciones";
    }
}
