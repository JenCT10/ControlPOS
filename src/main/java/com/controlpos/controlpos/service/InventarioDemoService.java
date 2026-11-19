package com.controlpos.controlpos.service;

import com.controlpos.controlpos.model.EstadoTerminal;
import com.controlpos.controlpos.model.Terminal;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@Profile("demo")
public class InventarioDemoService implements InventarioService {
    private final GestionService operaciones;

    public InventarioDemoService(GestionService operaciones) {
        this.operaciones = operaciones;
    }
    @Override
    public List<Terminal> consultar(String serie, EstadoTerminal estado, String comercio) {
        String serieBuscada = serie.strip().toLowerCase(Locale.ROOT);
        String comercioBuscado = comercio.strip().toLowerCase(Locale.ROOT);
        var comercios = operaciones.comercios();
        return operaciones.equipos().stream()
            .filter(e -> e.terminal().getSerie().toLowerCase(Locale.ROOT).contains(serieBuscada))
            .filter(e -> estado == null || e.terminal().getEstado() == estado)
            .filter(e -> comercioBuscado.isEmpty() || e.terminal().getEstado() == EstadoTerminal.ASIGNADO
                && (e.ubicacion() + " " + comercios.getOrDefault(e.ubicacion(), "")).toLowerCase(Locale.ROOT).contains(comercioBuscado))
            .map(com.controlpos.controlpos.model.Equipo::terminal)
            .toList();
    }
}
