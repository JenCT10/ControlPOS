package com.controlpos.controlpos.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EstadoTerminalConverter implements AttributeConverter<EstadoTerminal, String> {
    @Override
    public String convertToDatabaseColumn(EstadoTerminal estado) {
        if (estado == null) return null;
        return estado == EstadoTerminal.ASIGNADO ? "INSTALADO" : estado.name();
    }

    @Override
    public EstadoTerminal convertToEntityAttribute(String estado) {
        if (estado == null) return null;
        return "INSTALADO".equals(estado) ? EstadoTerminal.ASIGNADO : EstadoTerminal.valueOf(estado);
    }
}
