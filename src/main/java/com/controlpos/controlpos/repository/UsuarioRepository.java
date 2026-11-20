package com.controlpos.controlpos.repository;

import com.controlpos.controlpos.model.Rol;
import com.controlpos.controlpos.model.Usuario;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Profile("sqlserver")
public interface UsuarioRepository extends JpaRepository<Usuario,Long> {
    record Cuenta(long id, String nombre, String login, String claveHash, boolean activo, String rol) {}

    @EntityGraph(attributePaths="rol")
    Optional<Usuario> findByLoginIgnoreCase(String login);

    @EntityGraph(attributePaths="rol")
    List<Usuario> findAllByOrderByNombreAscLoginAsc();

    @Query("select r from Rol r where r.nombre=:nombre")
    Rol rol(@Param("nombre") String nombre);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Rol r where r.nombre='ADMINISTRADOR'")
    Rol bloquearRolAdministrador();

    @Transactional(readOnly=true)
    default Optional<Cuenta> buscarPorLogin(String login) {
        return findByLoginIgnoreCase(login).map(this::visible);
    }

    @Transactional(readOnly=true)
    default List<Cuenta> listar() {
        return findAllByOrderByNombreAscLoginAsc().stream().map(this::visible).toList();
    }

    private Cuenta visible(Usuario u) {
        return new Cuenta(u.getId(),u.getNombre(),u.getLogin(),u.getClaveHash(),u.getActivo(),u.getRol().getNombre());
    }

    @Transactional
    default void crear(String nombre,String login,String hash,String nombreRol) {
        var usuario=new Usuario();
        usuario.setNombre(nombre);
        usuario.setLogin(login);
        usuario.setClaveHash(hash);
        usuario.setRol(rol(nombreRol));
        saveAndFlush(usuario);
    }

    @Transactional
    default int actualizar(long id,String nombre,String login,String hash,String nombreRol) {
        var existente=findById(id);
        if(existente.isEmpty()) return 0;
        var usuario=existente.get();
        usuario.setNombre(nombre);
        usuario.setLogin(login);
        usuario.setClaveHash(hash);
        usuario.setRol(rol(nombreRol));
        usuario.setActualizadoEn(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
        saveAndFlush(usuario);
        return 1;
    }

    @Transactional
    default int eliminar(long id) {
        if(!existsById(id)) return 0;
        deleteById(id);
        flush();
        return 1;
    }

    @Transactional
    default void crearAdministradorInicial(String login,String nombre,String hash) {
        if(bloquearRolAdministrador()==null) throw new IllegalStateException("Falta el rol ADMINISTRADOR.");
        if(count()==0) crear(nombre,login,hash,"ADMINISTRADOR");
    }
}
