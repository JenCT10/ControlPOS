package com.controlpos.controlpos.repository;

import com.controlpos.controlpos.model.Terminal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TerminalRepository extends JpaRepository<Terminal, Long> {
    boolean existsBySerie(String serie);
    java.util.Optional<Terminal> findBySerie(String serie);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Terminal t where t.serie=:serie")
    java.util.Optional<Terminal> bloquear(@Param("serie") String serie);

    @Query(value = """
            SELECT t.*
            FROM dbo.terminal t
            WHERE (:serie = '' OR CHARINDEX(:serie, t.serie) > 0)
              AND (:estado = '' OR t.estado = :estado)
              AND (:comercio = '' OR EXISTS (
                  SELECT 1 FROM dbo.comercio c
                  WHERE c.id = t.comercio_id
                    AND (CHARINDEX(:comercio, c.nombre) > 0
                      OR CHARINDEX(:comercio, c.codigo) > 0)
              ))
            ORDER BY t.serie
            """, nativeQuery = true)
    List<Terminal> buscar(@Param("serie") String serie, @Param("estado") String estado,
                          @Param("comercio") String comercio);
}
