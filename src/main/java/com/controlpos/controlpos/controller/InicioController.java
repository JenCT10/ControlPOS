package com.controlpos.controlpos.controller;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InicioController {
    private final Environment entorno;

    public InicioController(Environment entorno) {
        this.entorno = entorno;
    }

    @GetMapping("/login")
    public String login(Model modelo) {
        if (entorno.acceptsProfiles(Profiles.of("demo"))) {
            String clave = entorno.getProperty("CONTROLPOS_DEMO_PASSWORD", "");
            modelo.addAttribute("demoSinClave", clave.length() < 12
                || clave.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72);
        }
        return "login";
    }
}
