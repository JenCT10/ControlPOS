package com.controlpos.controlpos.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.TreeSet;

@Controller
@Profile("sqlserver")
public class HistorialSqlController {
    public record Fila(long gestion,String fecha,String hora,String articulo,String detalle,String bodega,
                       int cantidad,String responsable,LocalDate dia) {}
    private final JdbcTemplate jdbc;

    public HistorialSqlController(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    @GetMapping("/movimientos")
    public String movimientos() { return "redirect:/historial"; }

    @GetMapping("/historial")
    public String ver(@RequestParam(defaultValue="pos") String categoria,@RequestParam(defaultValue="") String buscar,
                      @RequestParam(defaultValue="") String bodega,@RequestParam(defaultValue="") String tipo,
                      @RequestParam(required=false) LocalDate fecha,Model modelo) {
        boolean materiales="materiales".equals(categoria);
        String consulta=materiales?"""
            SELECT m.gestion_id,m.fecha,a.nombre articulo,m.tipo,COALESCE(b.nombre,'') bodega,m.cantidad,u.login
            FROM dbo.movimiento_material m JOIN dbo.material a ON a.id=m.material_id
            JOIN dbo.bodega b ON b.id=m.bodega_id JOIN dbo.usuario u ON u.id=m.usuario_id
            ORDER BY m.fecha DESC,m.id DESC
            """:"""
            SELECT m.gestion_id,m.fecha,t.serie articulo,m.tipo,COALESCE(b.nombre,bo.nombre,'') bodega,1 cantidad,u.login
            FROM dbo.movimiento_pos m JOIN dbo.terminal t ON t.id=m.terminal_id
            LEFT JOIN dbo.bodega b ON b.id=m.bodega_destino_id
            LEFT JOIN dbo.bodega bo ON bo.id=m.bodega_origen_id
            JOIN dbo.usuario u ON u.id=m.usuario_id ORDER BY m.fecha DESC,m.id DESC
            """;
        var filas=jdbc.query(consulta,(r,n)->{
            LocalDateTime momento=r.getTimestamp("fecha").toLocalDateTime();
            return new Fila(r.getLong("gestion_id"),momento.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
                momento.format(DateTimeFormatter.ofPattern("HH:mm")),r.getString("articulo"),r.getString("tipo"),
                r.getString("bodega"),r.getInt("cantidad"),r.getString("login"),momento.toLocalDate());
        });
        var bodegas=new TreeSet<String>();
        var tipos=new TreeSet<String>();
        filas.forEach(f->{bodegas.add(f.bodega());tipos.add(f.detalle());});
        String texto=buscar.strip().toLowerCase(Locale.ROOT);
        modelo.addAttribute("filas",filas.stream().filter(f->
            (f.articulo()+" "+f.detalle()+" "+f.gestion()).toLowerCase(Locale.ROOT).contains(texto)
            && (bodega.isEmpty() || bodega.equals(f.bodega()))
            && (tipo.isEmpty() || tipo.equals(f.detalle()))
            && (fecha==null || fecha.equals(f.dia()))).toList());
        modelo.addAttribute("bodegas",bodegas);
        modelo.addAttribute("tipos",tipos);
        modelo.addAttribute("materiales",materiales);
        modelo.addAttribute("categoria",materiales?"materiales":"pos");
        modelo.addAttribute("buscar",buscar);
        modelo.addAttribute("bodega",bodega);
        modelo.addAttribute("tipo",tipo);
        modelo.addAttribute("fecha",fecha);
        return "historial";
    }
}
