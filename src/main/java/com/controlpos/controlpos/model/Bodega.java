package com.controlpos.controlpos.model;

import jakarta.persistence.*;

@Entity
@Table(name = "bodega", schema = "dbo")
public class Bodega {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="responsable_id")
    private Usuario responsable;
    private boolean activa = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Usuario getResponsable() { return responsable; }
    public void setResponsable(Usuario responsable) { this.responsable = responsable; }
    public boolean getActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
}
