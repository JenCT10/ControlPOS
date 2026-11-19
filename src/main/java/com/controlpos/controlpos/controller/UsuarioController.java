package com.controlpos.controlpos.controller;

import com.controlpos.controlpos.service.GestionService;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@Controller
@Profile("demo")
@RequestMapping("/usuarios")
public class UsuarioController {
    private static final Map<String, String> ROLES = Map.of(
        "ADMINISTRADOR", "Administrador", "SUPERVISOR", "Supervisor", "SOPORTISTA", "Soportista"
    );
    private final InMemoryUserDetailsManager usuarios;
    private final PasswordEncoder encoder;
    private final GestionService operaciones;
    private final SessionRegistry sesiones;
    private final Map<String, String> nombres = new TreeMap<>(Map.of(
        "admin.demo", "Administrador de demostración",
        "supervisor.demo", "Supervisor de demostración",
        "inventario.demo", "Soportista de demostración"
    ));

    public record CuentaVisible(String usuario, String nombre, String rol, String etiquetaRol) {}

    public UsuarioController(InMemoryUserDetailsManager usuarios, PasswordEncoder encoder,
                                  GestionService operaciones, SessionRegistry sesiones) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.operaciones = operaciones;
        this.sesiones = sesiones;
    }

    @GetMapping
    public synchronized String ver(@RequestParam(defaultValue = "") String ver,
                                   @RequestParam(defaultValue = "") String editar,
                                   Authentication actual, Model modelo) {
        var cuentas = nombres.keySet().stream().filter(usuarios::userExists).map(this::cuentaVisible).toList();
        modelo.addAttribute("cuentas", cuentas);
        modelo.addAttribute("actual", actual.getName());
        modelo.addAttribute("detalle", cuentas.stream().filter(c -> c.usuario().equals(ver)).findFirst().orElse(null));
        modelo.addAttribute("edicion", cuentas.stream()
            .filter(c -> c.usuario().equals(editar) && !c.usuario().equals(actual.getName()))
            .findFirst().orElse(null));
        return "usuarios";
    }

    @PostMapping
    public synchronized String guardar(@RequestParam String login, @RequestParam String nombre,
                                       @RequestParam String rol, @RequestParam(defaultValue = "") String clave,
                                       @RequestParam(defaultValue = "") String loginOriginal,
                                       Authentication actual, RedirectAttributes mensaje) {
        login = login.strip().toLowerCase(Locale.ROOT);
        nombre = nombre.strip();
        loginOriginal = loginOriginal.strip().toLowerCase(Locale.ROOT);
        boolean editar = !loginOriginal.isEmpty();
        boolean renombrar = editar && !loginOriginal.equals(login);
        boolean cambiarClave = !editar || !clave.isEmpty();
        if (!login.matches("[a-z0-9._-]{3,60}") || nombre.isEmpty() || nombre.length() > 120
                || !ROLES.containsKey(rol) || (cambiarClave &&
                (clave.length() < 12 || clave.getBytes(StandardCharsets.UTF_8).length > 72))) {
            mensaje.addFlashAttribute("error", "Revisa el nombre, el usuario, el rol y la contraseña.");
        } else if (editar && (loginOriginal.equals(actual.getName()) || !usuarios.userExists(loginOriginal))) {
            mensaje.addFlashAttribute("error", "La cuenta no existe o es tu propia cuenta.");
        } else if ((!editar || renombrar) && (usuarios.userExists(login) || operaciones.nombreReservado(login))) {
            mensaje.addFlashAttribute("error", "Ese nombre de usuario ya está en uso o reservado.");
        } else {
            String hash = cambiarClave ? encoder.encode(clave) : usuarios.loadUserByUsername(loginOriginal).getPassword();
            var cuenta = User.withUsername(login).password(hash).roles(rol).build();
            if (renombrar) {
                operaciones.vincularUsuarioRenombrado(loginOriginal, login);
                usuarios.deleteUser(loginOriginal);
                nombres.remove(loginOriginal);
                usuarios.createUser(cuenta);
            } else if (editar) {
                usuarios.updateUser(cuenta);
            } else {
                usuarios.createUser(cuenta);
            }
            if (editar) cerrarSesiones(loginOriginal);
            nombres.put(login, nombre);
            mensaje.addFlashAttribute("aviso", editar ? "Cuenta actualizada." : "Cuenta creada.");
            return "redirect:/usuarios";
        }
        if (editar) mensaje.addAttribute("editar", loginOriginal);
        return "redirect:/usuarios";
    }

    @PostMapping("/{login}/eliminar")
    public synchronized String eliminar(@PathVariable String login, Authentication actual, RedirectAttributes mensaje) {
        login = login.toLowerCase(Locale.ROOT);
        if (login.equals(actual.getName()) || !usuarios.userExists(login)) {
            mensaje.addFlashAttribute("error", "La cuenta no existe o es tu propia cuenta.");
        } else {
            usuarios.deleteUser(login);
            operaciones.retirarUsuario(login);
            nombres.remove(login);
            cerrarSesiones(login);
            mensaje.addFlashAttribute("aviso", "Cuenta eliminada.");
        }
        return "redirect:/usuarios";
    }

    private void cerrarSesiones(String login) {
        sesiones.getAllPrincipals().stream()
            .filter(principal -> principal instanceof UserDetails cuenta && cuenta.getUsername().equals(login))
            .flatMap(principal -> sesiones.getAllSessions(principal, false).stream())
            .forEach(sesion -> sesion.expireNow());
    }

    private CuentaVisible cuentaVisible(String login) {
        var cuenta = usuarios.loadUserByUsername(login);
        String rol = cuenta.getAuthorities().iterator().next().getAuthority().substring("ROLE_".length());
        return new CuentaVisible(login, nombres.get(login), rol, ROLES.get(rol));
    }
}
