package com.controlpos.controlpos.repository;

import com.controlpos.controlpos.model.Gestion;
import org.springframework.data.jpa.repository.*;
import org.springframework.context.annotation.Profile;

@Profile("sqlserver")
public interface GestionRepository extends JpaRepository<Gestion, Long> {
    java.util.List<Gestion> findAllByOrderByIdDesc();
    java.util.List<Gestion> findByEstadoOrderById(String estado);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Gestion g where g.id=:id")
    java.util.Optional<Gestion> bloquear(@org.springframework.data.repository.query.Param("id") Long id);
}
