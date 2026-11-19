package com.controlpos.controlpos.service;

import com.controlpos.controlpos.model.EstadoTerminal;
import com.controlpos.controlpos.model.Terminal;
import com.controlpos.controlpos.repository.TerminalRepository;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Profile("!demo")
public class InventarioSqlService implements InventarioService {
    private final TerminalRepository repositorio;

    public InventarioSqlService(TerminalRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Terminal> consultar(String serie, EstadoTerminal estado, String comercio) {
        return repositorio.buscar(serie.strip(), estado == null ? "" : (estado == EstadoTerminal.ASIGNADO ? "INSTALADO" : estado.name()), comercio.strip());
    }
}

