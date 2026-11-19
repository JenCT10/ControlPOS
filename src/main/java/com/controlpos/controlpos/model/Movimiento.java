package com.controlpos.controlpos.model;

import java.util.List;

public record Movimiento(String categoria, String articulo, String detalle, String bodega, int cantidad) {}

