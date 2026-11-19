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
public class BodegaController {
    private final GestionService operaciones;

    public BodegaController(GestionService operaciones) {
        this.operaciones = operaciones;
    }

    @GetMapping("/suministros")
    public String ver(@RequestParam(defaultValue = "") String bodega, Authentication usuario, Model modelo) {
        modelo.addAttribute("seccion", "suministros");
        modelo.addAttribute("bodegas", operaciones.bodegas());
        modelo.addAttribute("bodega", bodega);
        modelo.addAttribute("equiposBodega", operaciones.equipos().stream()
            .filter(e -> e.ubicacion().equals(bodega)).toList());
        modelo.addAttribute("usuarioActual", operaciones.identidad(usuario.getName()));
        modelo.addAttribute("puedeSolicitar", usuario.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_SOPORTISTA") || a.getAuthority().equals("ROLE_ADMINISTRADOR")));
        return "operaciones";
    }
    @PostMapping("/bodegas")
    public String crearBodega(@RequestParam String nombre, RedirectAttributes mensaje) {
        try {
            operaciones.crearBodega(nombre.strip());
            mensaje.addFlashAttribute("aviso", "Bodega creada.");
        } catch (IllegalArgumentException error) {
            mensaje.addFlashAttribute("error", error.getMessage());
        }
        return "redirect:/suministros";
    }


}
