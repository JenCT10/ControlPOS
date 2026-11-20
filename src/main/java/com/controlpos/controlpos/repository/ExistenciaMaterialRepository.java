package com.controlpos.controlpos.repository;

import com.controlpos.controlpos.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.context.annotation.Profile;

@Profile("sqlserver")
public interface ExistenciaMaterialRepository extends JpaRepository<ExistenciaMaterial,ExistenciaId> {
    java.util.List<ExistenciaMaterial> findById_BodegaId(Long id);
}
