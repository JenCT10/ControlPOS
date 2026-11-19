package com.controlpos.controlpos.model;

import java.util.List;

public record Solicitud(long id, Datos datos, String solicitante, String estado, String revisor,
                            String motivo, List<Movimiento> movimientos, java.time.LocalDateTime fechaAplicacion) {}

