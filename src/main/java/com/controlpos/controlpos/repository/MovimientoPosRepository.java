package com.controlpos.controlpos.repository;

import com.controlpos.controlpos.model.MovimientoPos;
import org.springframework.data.jpa.repository.*;
import org.springframework.context.annotation.Profile;

@Profile("sqlserver")
public interface MovimientoPosRepository extends JpaRepository<MovimientoPos, Long> {
    java.util.List<MovimientoPos> findAllByOrderByFechaDescIdDesc();
}
