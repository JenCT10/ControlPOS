package com.controlpos.controlpos.model;

import jakarta.persistence.*;

@Entity
@Table(name="existencias_material",schema="dbo")
public class ExistenciaMaterial {
    @EmbeddedId private ExistenciaId id;
    private int cantidad;
    public ExistenciaMaterial() {}
    public ExistenciaMaterial(ExistenciaId id) { this.id=id; }
    public ExistenciaId getId() { return id; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad=cantidad; }
}
