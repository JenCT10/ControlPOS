package com.controlpos.controlpos.repository;

import com.controlpos.controlpos.model.Material;
import org.springframework.data.jpa.repository.*;
import org.springframework.context.annotation.Profile;

@Profile("sqlserver")
public interface MaterialRepository extends JpaRepository<Material, Long> {
    java.util.Optional<Material> findByNombreAndActivoTrue(String nombre);
    @Query(value="SELECT componente_id, cantidad FROM dbo.componente_material WHERE material_id=:id", nativeQuery=true)
    java.util.List<java.util.Map<String,Object>> componentes(@org.springframework.data.repository.query.Param("id") Long id);
}
