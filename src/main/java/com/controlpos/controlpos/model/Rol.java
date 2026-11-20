package com.controlpos.controlpos.model;

import jakarta.persistence.*;

@Entity
@Table(name = "rol", schema = "dbo")
public class Rol {
    @Id
    private Short id;
    private String nombre;

    public Short getId() { return id; }
    public void setId(Short id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
