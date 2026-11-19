package com.controlpos.controlpos.controller;

import com.controlpos.controlpos.service.GestionService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TreeSet;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@Profile("demo")
public class HistorialController {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private final GestionService operaciones;

    public HistorialController(GestionService operaciones) {
        this.operaciones = operaciones;
    }

    public record Fila(long gestion, String fecha, String hora, String articulo, String detalle,
                       String bodega, int cantidad, String responsable) {}

    @GetMapping("/movimientos")
    public String movimientos() {
        return "redirect:/historial";
    }

    @GetMapping("/historial")
    public String historial(@RequestParam(defaultValue = "pos") String categoria,
                           @RequestParam(defaultValue = "") String buscar,
                           @RequestParam(defaultValue = "") String bodega,
                           @RequestParam(defaultValue = "") String tipo,
                           @RequestParam(required = false) LocalDate fecha, Model modelo) {
        boolean materiales = "materiales".equals(categoria);
        String categoriaMovimiento = materiales ? "Materiales" : "Equipos POS";
        String consulta = buscar.strip().toLowerCase(Locale.ROOT);
        var filas = new ArrayList<Fila>();
        var tipos = new TreeSet<String>();
        var bodegas = new TreeSet<>(operaciones.bodegas().keySet());

        for (var solicitud : operaciones.solicitudes().reversed()) {
            for (var movimiento : solicitud.movimientos()) {
                if (!categoriaMovimiento.equals(movimiento.categoria())) continue;
                tipos.add(movimiento.detalle());
                bodegas.add(movimiento.bodega());
                var aplicada = solicitud.fechaAplicacion();
                String texto = (movimiento.articulo() + " " + movimiento.detalle() + " " + solicitud.id())
                    .toLowerCase(Locale.ROOT);
                if (!texto.contains(consulta) || (!bodega.isEmpty() && !bodega.equals(movimiento.bodega()))
                        || (!tipo.isEmpty() && !tipo.equals(movimiento.detalle()))
                        || (fecha != null && !fecha.equals(aplicada.toLocalDate()))) continue;

                filas.add(new Fila(solicitud.id(), aplicada.format(FECHA), aplicada.format(HORA),
                    movimiento.articulo(), movimiento.detalle(), movimiento.bodega(), movimiento.cantidad(),
                    solicitud.revisor()));
            }
        }
        modelo.addAttribute("filas", filas);
        modelo.addAttribute("materiales", materiales);
        modelo.addAttribute("categoria", materiales ? "materiales" : "pos");
        modelo.addAttribute("bodegas", bodegas);
        modelo.addAttribute("tipos", tipos);
        modelo.addAttribute("buscar", buscar);
        modelo.addAttribute("bodega", bodega);
        modelo.addAttribute("tipo", tipo);
        modelo.addAttribute("fecha", fecha);
        return "historial";
    }
}
