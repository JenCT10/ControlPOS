package com.controlpos.controlpos.model;

import jakarta.persistence.*;

@Entity
@Table(name = "gestion_pos", schema = "dbo")
public class GestionPos {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="gestion_id")
    private Gestion gestion;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="terminal_id")
    private Terminal terminal;
    private String serieDeclarada;
    private String marcaDeclarada;
    private String modeloDeclarado;
    private String accion;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Gestion getGestion() { return gestion; }
    public void setGestion(Gestion gestion) { this.gestion = gestion; }
    public Terminal getTerminal() { return terminal; }
    public void setTerminal(Terminal terminal) { this.terminal = terminal; }
    public String getSerieDeclarada() { return serieDeclarada; }
    public void setSerieDeclarada(String serieDeclarada) { this.serieDeclarada = serieDeclarada; }
    public String getMarcaDeclarada() { return marcaDeclarada; }
    public void setMarcaDeclarada(String marcaDeclarada) { this.marcaDeclarada = marcaDeclarada; }
    public String getModeloDeclarado() { return modeloDeclarado; }
    public void setModeloDeclarado(String modeloDeclarado) { this.modeloDeclarado = modeloDeclarado; }
    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }
}
