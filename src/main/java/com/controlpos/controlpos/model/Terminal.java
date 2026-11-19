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

    @Enumerated(EnumType.STRING)
    private EstadoTerminal estado;

    protected Terminal() {
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

