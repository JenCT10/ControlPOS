package com.controlpos.controlpos.controller;

import com.controlpos.controlpos.model.EstadoTerminal;
import com.controlpos.controlpos.model.Terminal;
import com.controlpos.controlpos.service.InventarioService;
import com.controlpos.controlpos.service.InventarioDemoService;
import java.util.EnumMap;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class InventarioController {
    private final InventarioService servicio;
    private final org.springframework.beans.factory.ObjectProvider<com.controlpos.controlpos.service.CatalogoSqlService> catalogos;

    private final org.springframework.beans.factory.ObjectProvider<com.controlpos.controlpos.service.GestionService> operaciones;
    
    public InventarioController(InventarioService servicio,
        org.springframework.beans.factory.ObjectProvider<com.controlpos.controlpos.service.CatalogoSqlService> catalogos,
        org.springframework.beans.factory.ObjectProvider<com.controlpos.controlpos.service.GestionService> operaciones) {
        this.servicio = servicio;
        this.catalogos = catalogos;
        this.operaciones = operaciones;
    }

    @GetMapping({"/", "/terminales"})
    public String inventario(@RequestParam(defaultValue = "") String serie,
                             @RequestParam(required = false) EstadoTerminal estado,
                             @RequestParam(defaultValue = "") String comercio,
                             @RequestParam(defaultValue = "") String bodega, java.security.Principal usuario, Model modelo) {
        modelo.addAttribute("demo", servicio instanceof InventarioDemoService);
        var demo = operaciones.getIfAvailable();
        var ubicaciones = new java.util.HashMap<String, String>();
        if (demo != null) {
            demo.equipos().forEach(equipo -> ubicaciones.put(equipo.terminal().getSerie(), equipo.ubicacion()));
            var bodegas = demo.bodegas().keySet();
            modelo.addAttribute("bodegas", bodegas);
            modelo.addAttribute("usuarioActual", demo.identidad(usuario.getName()));
            var porBodega = new java.util.LinkedHashMap<String, Long>();
            for (String nombre : bodegas) {
                porBodega.put(nombre, ubicaciones.values().stream().filter(nombre::equals).count());
            }
            modelo.addAttribute("porBodega", porBodega);
        }
        var sql = catalogos.getIfAvailable();
        modelo.addAttribute("inventarioEditable", demo != null || sql != null);
        if (sql != null) {
            modelo.addAttribute("bodegas", sql.bodegas().stream().map(com.controlpos.controlpos.service.CatalogoSqlService.Bodega::nombre).toList());
            var cuenta = usuario.getName();
            modelo.addAttribute("bodegasPropias", sql.bodegas().stream().filter(b -> b.responsable().equals(cuenta))
                .map(com.controlpos.controlpos.service.CatalogoSqlService.Bodega::nombre).toList());
            modelo.addAttribute("usuarioActual", cuenta);
            ubicaciones.putAll(sql.ubicaciones());
            var porBodega = new java.util.LinkedHashMap<String, Long>();
            for (var almacen : sql.bodegas()) {
                porBodega.put(almacen.nombre(), ubicaciones.values().stream().filter(almacen.nombre()::equals).count());
            }
            modelo.addAttribute("porBodega", porBodega);
        }
        var terminales = servicio.consultar(serie, estado, comercio);
        if ((demo != null || sql != null) && !bodega.isBlank()) {
            terminales = terminales.stream().filter(t -> bodega.equals(ubicaciones.get(t.getSerie()))).toList();
        }
        modelo.addAttribute("ubicaciones", ubicaciones);
        modelo.addAttribute("bodega", bodega);
        var totales = new EnumMap<EstadoTerminal, Integer>(EstadoTerminal.class);
        for (EstadoTerminal opcion : EstadoTerminal.values()) {
            totales.put(opcion, 0);
        }
        for (Terminal terminal : terminales) {
            totales.merge(terminal.getEstado(), 1, Integer::sum);
        }
        modelo.addAttribute("terminales", terminales);
        modelo.addAttribute("totales", totales);
        modelo.addAttribute("serie", serie);
        modelo.addAttribute("estado", estado);
        modelo.addAttribute("comercio", comercio);
        modelo.addAttribute("estados", EstadoTerminal.values());
        return "inventario";
    }
}

