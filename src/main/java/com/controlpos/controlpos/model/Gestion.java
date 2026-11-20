package com.controlpos.controlpos.model;

import jakarta.persistence.*;

@Entity
@Table(name = "gestion", schema = "dbo")
public class Gestion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String tipo;
    private String estado = "PENDIENTE";
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="comercio_id")
    private Comercio comercio;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="bodega_origen_id")
    private Bodega bodegaOrigen;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="bodega_destino_id")
    private Bodega bodegaDestino;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="solicitante_id")
    private Usuario solicitante;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="asignado_id")
    private Usuario asignado;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="revisor_id")
    private Usuario revisor;
    private String motivoRevision;
    private java.time.LocalDateTime creadaEn = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);
    private java.time.LocalDateTime revisadaEn;
    private java.time.LocalDateTime aplicadaEn;
    @Column(name="clave_operacion", columnDefinition="uniqueidentifier")
    private java.util.UUID claveOperacion = java.util.UUID.randomUUID();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Comercio getComercio() { return comercio; }
    public void setComercio(Comercio comercio) { this.comercio = comercio; }
    public Bodega getBodegaOrigen() { return bodegaOrigen; }
    public void setBodegaOrigen(Bodega bodegaOrigen) { this.bodegaOrigen = bodegaOrigen; }
    public Bodega getBodegaDestino() { return bodegaDestino; }
    public void setBodegaDestino(Bodega bodegaDestino) { this.bodegaDestino = bodegaDestino; }
    public Usuario getSolicitante() { return solicitante; }
    public void setSolicitante(Usuario solicitante) { this.solicitante = solicitante; }
    public Usuario getAsignado() { return asignado; }
    public void setAsignado(Usuario asignado) { this.asignado = asignado; }
    public Usuario getRevisor() { return revisor; }
    public void setRevisor(Usuario revisor) { this.revisor = revisor; }
    public String getMotivoRevision() { return motivoRevision; }
    public void setMotivoRevision(String motivoRevision) { this.motivoRevision = motivoRevision; }
    public java.time.LocalDateTime getCreadaEn() { return creadaEn; }
    public void setCreadaEn(java.time.LocalDateTime creadaEn) { this.creadaEn = creadaEn; }
    public java.time.LocalDateTime getRevisadaEn() { return revisadaEn; }
    public void setRevisadaEn(java.time.LocalDateTime revisadaEn) { this.revisadaEn = revisadaEn; }
    public java.time.LocalDateTime getAplicadaEn() { return aplicadaEn; }
    public void setAplicadaEn(java.time.LocalDateTime aplicadaEn) { this.aplicadaEn = aplicadaEn; }
    public java.util.UUID getClaveOperacion() { return claveOperacion; }
    public void setClaveOperacion(java.util.UUID claveOperacion) { this.claveOperacion = claveOperacion; }
}
