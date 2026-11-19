package com.controlpos.controlpos.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@org.springframework.context.annotation.Profile("!demo")
@Controller
public class ModulosController {
    @GetMapping("/comercios")
    public String comercios(Model modelo) {
        return mostrar(modelo, "comercios", "Comercios",
            "Aquí podrás registrar, consultar y editar los comercios que reciben datáfonos.");
    }

    @GetMapping({"/movimientos", "/historial"})
    public String movimientos(Model modelo) {
        return mostrar(modelo, "movimientos", "Movimientos",
            "Aquí podrás registrar entregas y devoluciones de datáfonos.");
    }

    @GetMapping("/gestiones")
    public String gestiones(Model modelo) {
        return mostrar(modelo, "gestiones", "Gestiones", "Consulta las solicitudes de operación.");
    }

    @GetMapping("/usuarios")
    public String usuarios(Model modelo) {
        return mostrar(modelo, "usuarios", "Usuarios",
            "Aquí podrás administrar las cuentas y sus roles.");
    }

    private String mostrar(Model modelo, String seccion, String titulo, String descripcion) {
        modelo.addAttribute("seccion", seccion);
        modelo.addAttribute("titulo", titulo);
        modelo.addAttribute("descripcion", descripcion);
        return "modulo";
    }
}

