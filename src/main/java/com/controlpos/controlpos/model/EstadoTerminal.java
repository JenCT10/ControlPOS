package com.controlpos.controlpos.model;

public enum EstadoTerminal {
    DISPONIBLE("Disponible"),
    ASIGNADO("Instalado"),
    MANTENIMIENTO("En mantenimiento"),
    INACTIVO("Inactivo"), EN_TRANSITO("En tránsito"), PENDIENTE_REVISION("Pendiente de revisión");

    private final String etiqueta;

    EstadoTerminal(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}

