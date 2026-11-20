package com.controlpos.controlpos.model;

import jakarta.persistence.*;

@Entity
@Table(name = "comercio", schema = "dbo")
public class Comercio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String codigo;
    private String nombre;
    private String ubicacion;
    private boolean activo = true;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="creado_por")
    private Usuario creadoPor;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="aprobado_por")
    private Usuario aprobadoPor;
    private java.time.LocalDateTime creadoEn = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);
    private java.time.LocalDateTime aprobadoEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }
    public boolean getActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public Usuario getCreadoPor() { return creadoPor; }
    public void setCreadoPor(Usuario creadoPor) { this.creadoPor = creadoPor; }
    public Usuario getAprobadoPor() { return aprobadoPor; }
    public void setAprobadoPor(Usuario aprobadoPor) { this.aprobadoPor = aprobadoPor; }
    public java.time.LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(java.time.LocalDateTime creadoEn) { this.creadoEn = creadoEn; }
    public java.time.LocalDateTime getAprobadoEn() { return aprobadoEn; }
    public void setAprobadoEn(java.time.LocalDateTime aprobadoEn) { this.aprobadoEn = aprobadoEn; }
}
