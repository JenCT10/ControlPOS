package com.controlpos.controlpos.repository;

import com.controlpos.controlpos.model.Rol;
import org.springframework.data.jpa.repository.*;
import org.springframework.context.annotation.Profile;

@Profile("sqlserver")
public interface RolRepository extends JpaRepository<Rol, Short> {
    java.util.Optional<Rol> findByNombre(String nombre);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Rol r where r.nombre = :nombre")
    java.util.Optional<Rol> bloquearPorNombre(@org.springframework.data.repository.query.Param("nombre") String nombre);
}
