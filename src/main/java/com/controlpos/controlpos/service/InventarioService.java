package com.controlpos.controlpos.service;

import com.controlpos.controlpos.model.EstadoTerminal;
import com.controlpos.controlpos.model.Terminal;
import java.util.List;

public interface InventarioService {
    List<Terminal> consultar(String serie, EstadoTerminal estado, String comercio);
}
