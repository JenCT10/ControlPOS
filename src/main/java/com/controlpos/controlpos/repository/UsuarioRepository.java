package com.controlpos.controlpos.repository;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Profile("!demo")
public class UsuarioRepository {
    public record Cuenta(String login, String claveHash, boolean activo, String rol) {}

    private final JdbcTemplate jdbc;

    public UsuarioRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Cuenta> buscarPorLogin(String login) {
        return jdbc.query("""
                SELECT u.login, u.clave_hash, u.activo, r.nombre AS rol
                FROM dbo.usuario u
                JOIN dbo.rol r ON r.id = u.rol_id
                WHERE u.login = ?
                """, (fila, numero) -> new Cuenta(fila.getString("login"),
                    fila.getString("clave_hash"), fila.getBoolean("activo"),
                    fila.getString("rol")), login).stream().findFirst();
    }
}
