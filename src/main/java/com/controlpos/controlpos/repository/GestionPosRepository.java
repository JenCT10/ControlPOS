package com.controlpos.controlpos.repository;

import com.controlpos.controlpos.model.GestionPos;
import org.springframework.data.jpa.repository.*;
import org.springframework.context.annotation.Profile;

@Profile("sqlserver")
public interface GestionPosRepository extends JpaRepository<GestionPos, Long> {
    java.util.List<GestionPos> findByGestion_IdOrderById(Long id);
}
