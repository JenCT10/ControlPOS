package com.controlpos.controlpos.controller;

import org.springframework.context.annotation.Profile;
import com.controlpos.controlpos.repository.MovimientoPosRepository;
import com.controlpos.controlpos.repository.MovimientoMaterialRepository;
import org.springframework.transaction.annotation.Transactional;
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
    private final MovimientoPosRepository pos;
    private final MovimientoMaterialRepository materialesRepository;

    public HistorialSqlController(MovimientoPosRepository pos,MovimientoMaterialRepository materialesRepository) {
        this.pos=pos;
        this.materialesRepository=materialesRepository;
    }
    @GetMapping("/movimientos")
    public String movimientos() { return "redirect:/historial"; }

    @Transactional(readOnly=true)
    @GetMapping("/historial")
    public String ver(@RequestParam(defaultValue="pos") String categoria,@RequestParam(defaultValue="") String buscar,
                      @RequestParam(defaultValue="") String bodega,@RequestParam(defaultValue="") String tipo,
                      @RequestParam(required=false) LocalDate fecha,Model modelo) {
        boolean materiales="materiales".equals(categoria);
        var filas=new java.util.ArrayList<Fila>();
        if(materiales) {
            for(var m:materialesRepository.findAllByOrderByFechaDescIdDesc()) {
                filas.add(fila(m.getGestion().getId(),m.getFecha(),m.getMaterial().getNombre(),m.getTipo(),
                    m.getBodega().getNombre(),m.getCantidad(),m.getUsuario().getLogin()));
            }
        } else {
            for(var m:pos.findAllByOrderByFechaDescIdDesc()) {
                var almacen=m.getBodegaDestino()!=null?m.getBodegaDestino():m.getBodegaOrigen();
                filas.add(fila(m.getGestion().getId(),m.getFecha(),m.getTerminal().getSerie(),m.getTipo(),
                    almacen==null?"":almacen.getNombre(),1,m.getUsuario().getLogin()));
            }
        }
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
    private Fila fila(long gestion,LocalDateTime fecha,String articulo,String tipo,String bodega,int cantidad,String usuario) {
        return new Fila(gestion,fecha.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
            fecha.format(DateTimeFormatter.ofPattern("HH:mm")),articulo,tipo,bodega,cantidad,usuario,fecha.toLocalDate());
    }}
