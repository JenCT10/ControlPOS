package com.controlpos.controlpos.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ExistenciaId implements Serializable {
    @Column(name="bodega_id") private Long bodegaId;
    @Column(name="material_id") private Long materialId;
    public ExistenciaId() {}
    public ExistenciaId(Long bodegaId, Long materialId) { this.bodegaId=bodegaId; this.materialId=materialId; }
    public Long getBodegaId() { return bodegaId; }
    public Long getMaterialId() { return materialId; }
    @Override public boolean equals(Object otro) {
        return otro instanceof ExistenciaId id && Objects.equals(bodegaId,id.bodegaId) && Objects.equals(materialId,id.materialId);
    }
    @Override public int hashCode() { return Objects.hash(bodegaId,materialId); }
}
