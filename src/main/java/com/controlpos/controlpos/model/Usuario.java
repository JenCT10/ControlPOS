package com.controlpos.controlpos.model;

import jakarta.persistence.*;

@Entity
@Table(name = "usuario", schema = "dbo")
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private String login;
    private String claveHash;
    private boolean activo = true;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="rol_id")
    private Rol rol;
    private java.time.LocalDateTime creadoEn = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);
    private java.time.LocalDateTime actualizadoEn = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getClaveHash() { return claveHash; }
    public void setClaveHash(String claveHash) { this.claveHash = claveHash; }
    public boolean getActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public java.time.LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(java.time.LocalDateTime creadoEn) { this.creadoEn = creadoEn; }
    public java.time.LocalDateTime getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(java.time.LocalDateTime actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}
