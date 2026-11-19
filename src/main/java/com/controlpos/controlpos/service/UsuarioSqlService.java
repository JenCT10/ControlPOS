package com.controlpos.controlpos.service;

import com.controlpos.controlpos.repository.UsuarioRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@Profile("sqlserver")
public class UsuarioSqlService {
    public static final Map<String,String> ROLES = Map.of(
        "ADMINISTRADOR","Administrador", "SUPERVISOR","Supervisor", "SOPORTISTA","Soportista");
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    public UsuarioSqlService(UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    @Transactional
    public void guardar(String original, String login, String nombre, String rol, String clave, String actual) {
        boolean editar = !original.isBlank();
        if (!login.matches("[a-z0-9._-]{3,60}") || nombre.isBlank() || nombre.length() > 120 || !ROLES.containsKey(rol)) {
            throw new IllegalArgumentException("Revisa el nombre, el usuario y el rol.");
        }
        if (editar && original.equalsIgnoreCase(actual)) {
            throw new IllegalArgumentException("Tu propia cuenta está protegida.");
        }
        var existente = usuarios.buscarPorLogin(original);
        if (editar && existente.isEmpty()) throw new IllegalArgumentException("La cuenta ya no existe.");
        boolean cambiarClave = !editar || !clave.isEmpty();
        if (cambiarClave) validarClave(clave);
        var duplicada = usuarios.buscarPorLogin(login);
        if (duplicada.isPresent() && (!editar || duplicada.get().id() != existente.orElseThrow().id())) {
            throw new IllegalArgumentException("El nombre de usuario ya está registrado.");
        }
        String hash = cambiarClave ? encoder.encode(clave) : existente.orElseThrow().claveHash();
        if (editar) {
            if (usuarios.actualizar(existente.orElseThrow().id(), nombre, login, hash, rol) != 1) {
                throw new IllegalArgumentException("La cuenta ya no existe.");
            }
        } else {
            usuarios.crear(nombre, login, hash, rol);
        }
    }

    @Transactional
    public void eliminar(String login, String actual) {
        if (login.equalsIgnoreCase(actual)) throw new IllegalArgumentException("Tu propia cuenta está protegida.");
        var cuenta = usuarios.buscarPorLogin(login).orElseThrow(() -> new IllegalArgumentException("La cuenta ya no existe."));
        usuarios.eliminar(cuenta.id());
    }

    @Transactional
    public void inicializar(String clave) {
        if (!usuarios.listar().isEmpty()) return;
        validarClave(clave);
        usuarios.crearAdministradorInicial("admin", "Administrador", encoder.encode(clave));
    }

    private void validarClave(String clave) {
        if (clave.length() < 12 || clave.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("La contraseña requiere al menos 12 caracteres y hasta 72 bytes.");
        }
    }
}
