package com.controlpos.controlpos.repository;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("sqlserver")
public class UsuarioRepository {
    public record Cuenta(long id, String nombre, String login, String claveHash, boolean activo, String rol) {}

    private final JdbcTemplate jdbc;

    public UsuarioRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Cuenta> buscarPorLogin(String login) {
        return listarConsulta(" WHERE u.login = ?", login).stream().findFirst();
    }

    public List<Cuenta> listar() {
        return listarConsulta(" ORDER BY u.nombre, u.login");
    }

    private List<Cuenta> listarConsulta(String filtro, Object... parametros) {
        return jdbc.query("""
            SELECT u.id, u.nombre, u.login, u.clave_hash, u.activo, r.nombre AS rol
            FROM dbo.usuario u JOIN dbo.rol r ON r.id = u.rol_id
            """ + filtro, (fila, numero) -> new Cuenta(fila.getLong("id"), fila.getString("nombre"),
                fila.getString("login"), fila.getString("clave_hash"), fila.getBoolean("activo"),
                fila.getString("rol")), parametros);
    }

    public void crear(String nombre, String login, String hash, String rol) {
        jdbc.update("""
            INSERT INTO dbo.usuario(nombre,login,clave_hash,rol_id)
            SELECT ?,?,?,id FROM dbo.rol WHERE nombre = ?
            """, nombre, login, hash, rol);
    }

    public int actualizar(long id, String nombre, String login, String hash, String rol) {
        return jdbc.update("""
            UPDATE dbo.usuario SET nombre=?, login=?, clave_hash=?,
                rol_id=(SELECT id FROM dbo.rol WHERE nombre=?), actualizado_en=SYSUTCDATETIME()
            WHERE id=?
            """, nombre, login, hash, rol, id);
    }

    public int eliminar(long id) {
        return jdbc.update("DELETE FROM dbo.usuario WHERE id=?", id);
    }

    public void crearAdministradorInicial(String login, String nombre, String hash) {
        jdbc.execute("SELECT id FROM dbo.usuario WITH (TABLOCKX, HOLDLOCK)");
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM dbo.usuario", Integer.class);
        if (total != null && total == 0) crear(nombre, login, hash, "ADMINISTRADOR");
    }
}
