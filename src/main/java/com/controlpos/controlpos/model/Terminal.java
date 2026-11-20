package com.controlpos.controlpos.model;

import jakarta.persistence.*;

@Entity
@Table(name = "terminal", schema = "dbo")
public class Terminal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String serie;
    private String marca;
    private String modelo;

    @Convert(converter = EstadoTerminalConverter.class)
    private EstadoTerminal estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="bodega_id")
    private Bodega bodega;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="comercio_id")
    private Comercio comercio;
    private boolean codigoInterno;
    private java.time.LocalDateTime fechaIngreso = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);

    public Bodega getBodega() { return bodega; }
    public void setBodega(Bodega bodega) { this.bodega = bodega; }
    public Comercio getComercio() { return comercio; }
    public void setComercio(Comercio comercio) { this.comercio = comercio; }
    public boolean getCodigoInterno() { return codigoInterno; }
    public void setCodigoInterno(boolean codigoInterno) { this.codigoInterno = codigoInterno; }
    public void setEstado(EstadoTerminal estado) { this.estado = estado; }


    public Terminal() {
    }

    public Terminal(String serie, String marca, String modelo, EstadoTerminal estado) {
        this.serie = serie;
        this.marca = marca;
        this.modelo = modelo;
        this.estado = estado;
    }

    public Long getId() { return id; }
    public String getSerie() { return serie; }
    public String getMarca() { return marca; }
    public String getModelo() { return modelo; }
    public EstadoTerminal getEstado() { return estado; }
}

