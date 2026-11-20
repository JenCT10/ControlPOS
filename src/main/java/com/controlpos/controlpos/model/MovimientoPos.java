package com.controlpos.controlpos.model;

import jakarta.persistence.*;

@Entity
@Table(name = "movimiento_pos", schema = "dbo")
public class MovimientoPos {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="gestion_id")
    private Gestion gestion;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="terminal_id")
    private Terminal terminal;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="usuario_id")
    private Usuario usuario;
    private String tipo;
    private String estadoAnterior;
    private String estadoNuevo;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="bodega_origen_id")
    private Bodega bodegaOrigen;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="bodega_destino_id")
    private Bodega bodegaDestino;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="comercio_origen_id")
    private Comercio comercioOrigen;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="comercio_destino_id")
    private Comercio comercioDestino;
    private java.time.LocalDateTime fecha = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);
    private String detalle;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Gestion getGestion() { return gestion; }
    public void setGestion(Gestion gestion) { this.gestion = gestion; }
    public Terminal getTerminal() { return terminal; }
    public void setTerminal(Terminal terminal) { this.terminal = terminal; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(String estadoAnterior) { this.estadoAnterior = estadoAnterior; }
    public String getEstadoNuevo() { return estadoNuevo; }
    public void setEstadoNuevo(String estadoNuevo) { this.estadoNuevo = estadoNuevo; }
    public Bodega getBodegaOrigen() { return bodegaOrigen; }
    public void setBodegaOrigen(Bodega bodegaOrigen) { this.bodegaOrigen = bodegaOrigen; }
    public Bodega getBodegaDestino() { return bodegaDestino; }
    public void setBodegaDestino(Bodega bodegaDestino) { this.bodegaDestino = bodegaDestino; }
    public Comercio getComercioOrigen() { return comercioOrigen; }
    public void setComercioOrigen(Comercio comercioOrigen) { this.comercioOrigen = comercioOrigen; }
    public Comercio getComercioDestino() { return comercioDestino; }
    public void setComercioDestino(Comercio comercioDestino) { this.comercioDestino = comercioDestino; }
    public java.time.LocalDateTime getFecha() { return fecha; }
    public void setFecha(java.time.LocalDateTime fecha) { this.fecha = fecha; }
    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
}
