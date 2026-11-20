package com.controlpos.controlpos.repository;

import com.controlpos.controlpos.model.Bodega;
import org.springframework.data.jpa.repository.*;
import org.springframework.context.annotation.Profile;

@Profile("sqlserver")
public interface BodegaRepository extends JpaRepository<Bodega, Long> {
    java.util.List<Bodega> findByActivaTrueOrderByNombre();
    java.util.Optional<Bodega> findByNombreAndActivaTrue(String nombre);
    java.util.List<Bodega> findByResponsable_IdAndActivaTrueOrderById(Long id);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Bodega b where b.id=:id and b.activa=true")
    java.util.Optional<Bodega> bloquear(@org.springframework.data.repository.query.Param("id") Long id);
}
