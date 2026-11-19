package com.controlpos.controlpos.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@org.springframework.context.annotation.Profile("!demo")
@Controller
public class ModulosController {




    private String mostrar(Model modelo, String seccion, String titulo, String descripcion) {
        modelo.addAttribute("seccion", seccion);
        modelo.addAttribute("titulo", titulo);
        modelo.addAttribute("descripcion", descripcion);
        return "modulo";
    }
}

