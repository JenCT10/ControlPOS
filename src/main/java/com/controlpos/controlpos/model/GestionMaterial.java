package com.controlpos.controlpos.model;

import jakarta.persistence.*;

@Entity
@Table(name = "gestion_material", schema = "dbo")
public class GestionMaterial {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="gestion_id")
    private Gestion gestion;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="material_id")
    private Material material;
    private int cantidad;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Gestion getGestion() { return gestion; }
    public void setGestion(Gestion gestion) { this.gestion = gestion; }
    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
}
