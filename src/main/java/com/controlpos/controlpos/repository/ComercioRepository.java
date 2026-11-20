package com.controlpos.controlpos.repository;

import com.controlpos.controlpos.model.Comercio;
import org.springframework.data.jpa.repository.*;
import org.springframework.context.annotation.Profile;

@Profile("sqlserver")
public interface ComercioRepository extends JpaRepository<Comercio, Long> {
    java.util.List<Comercio> findByActivoTrueOrderByNombre();
    java.util.Optional<Comercio> findByCodigoAndActivoTrue(String codigo);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Comercio c where c.codigo=:codigo and c.activo=true")
    java.util.Optional<Comercio> bloquear(@org.springframework.data.repository.query.Param("codigo") String codigo);
}
