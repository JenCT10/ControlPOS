package com.controlpos.controlpos.repository;

import com.controlpos.controlpos.model.MovimientoMaterial;
import org.springframework.data.jpa.repository.*;
import org.springframework.context.annotation.Profile;

@Profile("sqlserver")
public interface MovimientoMaterialRepository extends JpaRepository<MovimientoMaterial, Long> {
    java.util.List<MovimientoMaterial> findAllByOrderByFechaDescIdDesc();
}
