package com.controlpos.controlpos.model;

import jakarta.persistence.*;

@Entity
@Table(name = "movimiento_material", schema = "dbo")
public class MovimientoMaterial {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="gestion_id")
    private Gestion gestion;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="material_id")
    private Material material;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="bodega_id")
    private Bodega bodega;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="usuario_id")
    private Usuario usuario;
    private String tipo;
    private int cantidad;
    private int saldoResultante;
    private java.time.LocalDateTime fecha = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);
    private String detalle;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Gestion getGestion() { return gestion; }
    public void setGestion(Gestion gestion) { this.gestion = gestion; }
    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }
    public Bodega getBodega() { return bodega; }
    public void setBodega(Bodega bodega) { this.bodega = bodega; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public int getSaldoResultante() { return saldoResultante; }
    public void setSaldoResultante(int saldoResultante) { this.saldoResultante = saldoResultante; }
    public java.time.LocalDateTime getFecha() { return fecha; }
    public void setFecha(java.time.LocalDateTime fecha) { this.fecha = fecha; }
    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
}
