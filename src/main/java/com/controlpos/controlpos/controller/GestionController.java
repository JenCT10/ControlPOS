package com.controlpos.controlpos.controller;

import com.controlpos.controlpos.model.EstadoTerminal;
import com.controlpos.controlpos.service.GestionService;
import com.controlpos.controlpos.model.Datos;
import com.controlpos.controlpos.model.Tipo;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Locale;

@Controller
@Profile("demo")
public class GestionController {
    private final GestionService operaciones;

    public GestionController(GestionService operaciones) {
        this.operaciones = operaciones;
    }

    @GetMapping("/gestiones")
    public String ver(Authentication usuario, Model modelo) {
        modelo.addAttribute("seccion", "gestiones");
        modelo.addAttribute("comercios", operaciones.comercios());
        modelo.addAttribute("solicitudes", operaciones.solicitudes());
        modelo.addAttribute("puedeSolicitar", tieneRol(usuario, "SOPORTISTA") || tieneRol(usuario, "ADMINISTRADOR"));
        modelo.addAttribute("usuarioActual", operaciones.identidad(usuario.getName()));
        modelo.addAttribute("seriesDisponibles", operaciones.equipos().stream()
            .filter(e -> e.ubicacion().equals(operaciones.identidad(usuario.getName())))
            .filter(e -> e.terminal().getEstado() == EstadoTerminal.DISPONIBLE)
            .map(com.controlpos.controlpos.model.Equipo::terminal).toList());
        return "operaciones";
    }
    @PostMapping("/solicitudes")
    public String solicitar(@RequestParam Tipo tipo, @RequestParam(defaultValue = "") String comercio,
                            @RequestParam(defaultValue = "") String serie, @RequestParam(defaultValue = "") String reemplazo,
                            @RequestParam(defaultValue = "") String marca, @RequestParam(defaultValue = "") String modelo,
                            @RequestParam(defaultValue = "") String material, @RequestParam(defaultValue = "0") int cantidad,
                            @RequestParam(required = false) Long origen, @RequestParam(required = false) List<Integer> articulos,
                            Authentication usuario, RedirectAttributes mensaje) {
        try {
            if (serie.length() > 1000 || reemplazo.length() > 60 || marca.length() > 60 || modelo.length() > 100
                    || comercio.length() > 30 || material.length() > 80) {
                throw new IllegalArgumentException("Revisa la longitud de los campos.");
            }
            boolean traslado = tipo == Tipo.TRASLADO_POS || tipo == Tipo.TRASLADO_MATERIAL;
            if (traslado && !tieneRol(usuario, "ADMINISTRADOR") && !operaciones.identidad(usuario.getName()).equals(comercio)) {
                throw new IllegalArgumentException("Solo puedes solicitar traslados desde tu bodega.");
            }
            boolean ingreso = tipo == Tipo.INGRESO_POS || tipo == Tipo.INGRESO_SUMINISTRO;
            if (ingreso && !comercio.isBlank() && !tieneRol(usuario, "ADMINISTRADOR")
                    && !operaciones.identidad(usuario.getName()).equals(comercio)) {
                throw new IllegalArgumentException("Solo puedes solicitar ingresos a tu bodega.");
            }
            operaciones.solicitar(new Datos(tipo, traslado || ingreso ? comercio.strip() : codigo(comercio), codigo(serie),
                traslado ? reemplazo.strip() : codigo(reemplazo),
                marca.strip(), modelo.strip(), material.strip(), cantidad, origen,
                articulos == null ? List.of() : List.copyOf(articulos)), operaciones.identidad(usuario.getName()), tieneRol(usuario, "ADMINISTRADOR"));
            mensaje.addFlashAttribute("aviso", "Solicitud registrada. Consulta su estado en Gestiones.");
        } catch (IllegalArgumentException error) {
            mensaje.addFlashAttribute("error", error.getMessage());
        }
        return switch (tipo) {
            case INGRESO_POS, INGRESO_SUMINISTRO -> "redirect:/terminales";
            case TRASLADO_POS, TRASLADO_MATERIAL -> "redirect:/suministros";
            case ANULACION, INSTALACION, ENCOMIENDA, RETIRO, CAMBIO -> "redirect:/gestiones";
            case ALTA_COMERCIO -> "redirect:/comercios";
        };
    }

    private boolean tieneRol(Authentication usuario, String rol) {
        return usuario.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + rol));
    }

    private String codigo(String valor) {
        return valor.strip().toUpperCase(Locale.ROOT);
    }
}
