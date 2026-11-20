package com.controlpos.controlpos.repository;

import com.controlpos.controlpos.model.GestionMaterial;
import org.springframework.data.jpa.repository.*;
import org.springframework.context.annotation.Profile;

@Profile("sqlserver")
public interface GestionMaterialRepository extends JpaRepository<GestionMaterial, Long> {
    java.util.List<GestionMaterial> findByGestion_IdOrderById(Long id);
}
