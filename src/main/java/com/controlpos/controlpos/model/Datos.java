package com.controlpos.controlpos.model;

import java.util.List;

public record Datos(Tipo tipo, String comercio, String serie, String reemplazo, String marca,
                        String modelo, String material, int cantidad, Long origen, List<Integer> articulos) {}

