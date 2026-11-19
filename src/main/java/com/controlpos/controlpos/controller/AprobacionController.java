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
public class AprobacionController {
    private final GestionService operaciones;

    public AprobacionController(GestionService operaciones) {
        this.operaciones = operaciones;
    }

    @GetMapping("/aprobaciones")
    public String ver(Model modelo) {
        modelo.addAttribute("seccion", "aprobaciones");
        modelo.addAttribute("solicitudes", operaciones.solicitudes());
        return "operaciones";
    }
    @PostMapping("/aprobaciones/{id}")
    public String revisar(@PathVariable long id, @RequestParam boolean aprobar,
                          @RequestParam(defaultValue = "") String motivo, Authentication usuario, RedirectAttributes mensaje) {
        try {
            operaciones.revisar(id, operaciones.identidad(usuario.getName()), aprobar, motivo.strip());
            mensaje.addFlashAttribute("aviso", aprobar ? "Solicitud aprobada." : "Solicitud rechazada.");
        } catch (IllegalArgumentException error) {
            mensaje.addFlashAttribute("error", error.getMessage());
        }
        return "redirect:/aprobaciones";
    }


}
