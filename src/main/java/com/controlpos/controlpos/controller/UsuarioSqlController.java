package com.controlpos.controlpos.controller;

import com.controlpos.controlpos.repository.UsuarioRepository;
import com.controlpos.controlpos.service.UsuarioSqlService;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Locale;

@Controller
@Profile("sqlserver")
@RequestMapping("/usuarios")
public class UsuarioSqlController {
    public record CuentaVisible(String usuario, String nombre, String rol, String etiquetaRol) {}
    private final UsuarioRepository repositorio;
    private final UsuarioSqlService servicio;
    private final SessionRegistry sesiones;

    public UsuarioSqlController(UsuarioRepository repositorio, UsuarioSqlService servicio, SessionRegistry sesiones) {
        this.repositorio = repositorio;
        this.servicio = servicio;
        this.sesiones = sesiones;
    }

    @GetMapping
    public String ver(@RequestParam(defaultValue="") String ver, @RequestParam(defaultValue="") String editar,
                      Authentication actual, Model modelo) {
        var cuentas = repositorio.listar().stream().map(c ->
            new CuentaVisible(c.login(), c.nombre(), c.rol(), UsuarioSqlService.ROLES.get(c.rol()))).toList();
        modelo.addAttribute("cuentas", cuentas);
        modelo.addAttribute("actual", actual.getName());
        modelo.addAttribute("detalle", cuentas.stream().filter(c -> c.usuario().equals(ver)).findFirst().orElse(null));
        modelo.addAttribute("edicion", cuentas.stream()
            .filter(c -> c.usuario().equals(editar) && !c.usuario().equalsIgnoreCase(actual.getName())).findFirst().orElse(null));
        return "usuarios";
    }

    @PostMapping
    public String guardar(@RequestParam String login, @RequestParam String nombre, @RequestParam String rol,
                          @RequestParam(defaultValue="") String clave, @RequestParam(defaultValue="") String loginOriginal,
                          Authentication actual, RedirectAttributes mensaje) {
        login = login.strip().toLowerCase(Locale.ROOT);
        loginOriginal = loginOriginal.strip().toLowerCase(Locale.ROOT);
        try {
            servicio.guardar(loginOriginal, login, nombre.strip(), rol, clave, actual.getName());
            cerrarSesiones(loginOriginal);
            mensaje.addFlashAttribute("aviso", "Usuario guardado en la base de datos.");
        } catch (IllegalArgumentException | DataIntegrityViolationException error) {
            mensaje.addFlashAttribute("error", error instanceof IllegalArgumentException
                ? error.getMessage() : "El nombre de usuario ya está registrado o los datos no son válidos.");
            if (!loginOriginal.isBlank()) mensaje.addAttribute("editar", loginOriginal);
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/{login}/eliminar")
    public String eliminar(@PathVariable String login, Authentication actual, RedirectAttributes mensaje) {
        try {
            servicio.eliminar(login, actual.getName());
            cerrarSesiones(login);
            mensaje.addFlashAttribute("aviso", "Usuario eliminado.");
        } catch (IllegalArgumentException | DataIntegrityViolationException error) {
            mensaje.addFlashAttribute("error", error instanceof IllegalArgumentException ? error.getMessage()
                : "El usuario tiene registros asociados y debe conservarse para el historial.");
        }
        return "redirect:/usuarios";
    }

    private void cerrarSesiones(String login) {
        sesiones.getAllPrincipals().stream()
            .filter(p -> p instanceof UserDetails cuenta && cuenta.getUsername().equalsIgnoreCase(login))
            .flatMap(p -> sesiones.getAllSessions(p, false).stream())
            .forEach(s -> s.expireNow());
    }
}
