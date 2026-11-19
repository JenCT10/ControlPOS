package com.controlpos.controlpos.service;

import com.controlpos.controlpos.model.*;
import com.controlpos.controlpos.model.Terminal;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Profile("demo")
public class GestionService {
    private record Efecto(Movimiento movimiento, String serie, Equipo anterior, Equipo nuevo) {}

    private final Map<String, Equipo> equipos = new TreeMap<>();
    private final Map<String, String> comercios = new TreeMap<>(Map.of("COM-001", "Comercio Central"));
    private final Map<String, Map<String, Integer>> bodegas = new TreeMap<>();
    private final Map<Long, Solicitud> solicitudes = new LinkedHashMap<>();
    private final Map<Long, List<Efecto>> efectos = new HashMap<>();
    private final Map<Long, Set<Integer>> revertidos = new HashMap<>();
    private final Map<String, String> identidades = new HashMap<>();
    private final Set<String> usuariosReservados = new HashSet<>();
    private long consecutivo;

    public synchronized String identidad(String login) {
        return identidades.getOrDefault(login, login);
    }

    public synchronized boolean nombreReservado(String login) {
        return usuariosReservados.contains(login) || bodegas.containsKey(login);
    }

    public synchronized void vincularUsuarioRenombrado(String anterior, String nuevo) {
        identidades.put(nuevo, identidad(anterior));
        retirarUsuario(anterior);
    }

    public synchronized void retirarUsuario(String login) {
        identidades.remove(login);
        usuariosReservados.add(login);
    }

    public synchronized void crearBodega(String nombre) {
        exigir(!nombre.isBlank() && nombre.length() <= 28 && !bodegas.containsKey(nombre),
            "Indica un nombre nuevo de hasta 28 caracteres.");
        bodegas.put(nombre, new TreeMap<>());
    }

    public GestionService() {
        equipos.put("POS-001", equipo("POS-001", "PAX", "A920", EstadoTerminal.DISPONIBLE, "inventario.demo"));
        equipos.put("POS-002", equipo("POS-002", "Ingenico", "DX8000", EstadoTerminal.ASIGNADO, "COM-001"));
        equipos.put("POS-003", equipo("POS-003", "PAX", "A920", EstadoTerminal.MANTENIMIENTO, "inventario.demo"));
        equipos.put("POS-004", equipo("POS-004", "Ingenico", "DX8000", EstadoTerminal.INACTIVO, "inventario.demo"));
        bodegas.put("bodega.central", new TreeMap<>());
        bodegas.put("inventario.demo", new TreeMap<>(Map.of("Tramo de cable", 10, "RJ45", 10,
            "DB9 hembra", 10, "Cobertor", 10)));
    }

    private Equipo equipo(String serie, String marca, String modelo, EstadoTerminal estado, String ubicacion) {
        return new Equipo(new Terminal(serie, marca, modelo, estado), ubicacion);
    }

    public synchronized List<Equipo> equipos() { return List.copyOf(equipos.values()); }
    public synchronized Map<String, String> comercios() { return new TreeMap<>(comercios); }
    public synchronized Map<String, Map<String, Integer>> bodegas() {
        var copia = new TreeMap<String, Map<String, Integer>>();
        bodegas.forEach((nombre, stock) -> copia.put(nombre, new TreeMap<>(stock)));
        return copia;
    }
    public synchronized List<Solicitud> solicitudes() { return List.copyOf(solicitudes.values()); }

    public synchronized void solicitar(Datos datos, String usuario, boolean administrador) {
        long id = ++consecutivo;
        var solicitud = new Solicitud(id, datos, usuario, "Pendiente", "", "", List.of(), null);
        preparar(solicitud);
        solicitudes.put(id, solicitud);
        if (administrador && datos.tipo() != Tipo.ANULACION) aplicar(solicitud, usuario);
    }

    public synchronized void revisar(long id, String usuario, boolean aprobar, String motivo) {
        var solicitud = solicitudes.get(id);
        exigir(solicitud != null && solicitud.estado().equals("Pendiente"), "La solicitud ya no está pendiente.");
        exigir(!solicitud.solicitante().equals(usuario), "Otra persona debe revisar esta solicitud.");
        if (aprobar) {
            aplicar(solicitud, usuario);
        } else {
            exigir(!motivo.isBlank(), "Indica el motivo del rechazo.");
            solicitudes.put(id, new Solicitud(id, solicitud.datos(), solicitud.solicitante(),
                "Rechazada", usuario, motivo, List.of(), null));
        }
    }

    private void aplicar(Solicitud solicitud, String usuario) {
        var plan = preparar(solicitud);
        var datos = solicitud.datos();
        if (datos.tipo() == Tipo.ALTA_COMERCIO) comercios.put(datos.comercio(), datos.modelo());
        for (var efecto : plan) {
            var movimiento = efecto.movimiento();
            if (efecto.serie() != null) {
                if (efecto.nuevo() == null) equipos.remove(efecto.serie());
                else equipos.put(efecto.serie(), efecto.nuevo());
            } else {
                bodegas.computeIfAbsent(movimiento.bodega(), clave -> new TreeMap<>())
                    .merge(movimiento.articulo(), movimiento.cantidad(), Integer::sum);
            }
        }
        if (datos.tipo() == Tipo.ANULACION) {
            revertidos.computeIfAbsent(datos.origen(), clave -> new HashSet<>()).addAll(indices(datos));
        }
        efectos.put(solicitud.id(), plan);
        solicitudes.put(solicitud.id(), new Solicitud(solicitud.id(), datos, solicitud.solicitante(),
            "Aprobada", usuario, "", plan.stream().map(Efecto::movimiento).toList(), java.time.LocalDateTime.now()));
    }

    private List<Efecto> preparar(Solicitud solicitud) {
        var d = solicitud.datos();
        String bodega = solicitud.solicitante();
        if ((d.tipo() == Tipo.INGRESO_POS || d.tipo() == Tipo.INGRESO_SUMINISTRO) && !d.comercio().isBlank()) {
            exigir(bodegas.containsKey(d.comercio()), "Selecciona una bodega disponible.");
            bodega = d.comercio();
        }
        var plan = new ArrayList<Efecto>();
        switch (d.tipo()) {
            case ALTA_COMERCIO -> exigir(!d.comercio().isBlank() && !d.modelo().isBlank()
                && !comercios.containsKey(d.comercio()), "Indica un código de comercio nuevo y su nombre.");
            case INGRESO_POS -> {
                exigir(!d.serie().isBlank() && !d.marca().isBlank() && !d.modelo().isBlank()
                    && !equipos.containsKey(d.serie()), "Indica una serie nueva, marca y modelo.");
                pos(plan, d.serie(), equipo(d.serie(), d.marca(), d.modelo(), EstadoTerminal.DISPONIBLE, bodega),
                    "Ingreso al inventario", bodega);
            }
            case INGRESO_SUMINISTRO -> materiales(plan, d, bodega, 1, "Ingreso al inventario");
            case INSTALACION, ENCOMIENDA, RETIRO, CAMBIO -> {
                exigir(comercios.containsKey(d.comercio()), "Selecciona un comercio aprobado.");
                if (d.tipo() == Tipo.RETIRO || d.tipo() == Tipo.CAMBIO) {
                    String serie = d.serie().isBlank() ? "INTERNO-" + solicitud.id() : d.serie();
                    var anterior = equipos.get(serie);
                    exigir(anterior == null || (anterior.terminal().getEstado() == EstadoTerminal.ASIGNADO
                        && anterior.ubicacion().equals(d.comercio())), "El equipo registrado debe estar asignado a este comercio.");
                    pos(plan, serie, equipo(serie, anterior == null ? "Sin identificar" : anterior.terminal().getMarca(),
                        anterior == null ? "Sin identificar" : anterior.terminal().getModelo(),
                        EstadoTerminal.PENDIENTE_REVISION, bodega), "Retiro de comercio", bodega);
                }
                String series = d.tipo() == Tipo.CAMBIO ? d.reemplazo() : d.serie();
                if (d.tipo() == Tipo.INSTALACION || d.tipo() == Tipo.CAMBIO
                    || (d.tipo() == Tipo.ENCOMIENDA && !series.isBlank())) {
                    exigir(!series.isBlank() && (d.tipo() != Tipo.CAMBIO || (!series.contains(",") && !series.equals(d.serie()))),
                        "Indica la serie del equipo disponible.");
                    var registradas = new HashSet<String>();
                    for (String valor : series.split(",", -1)) {
                        String serie = valor.strip();
                        var actual = equipos.get(serie);
                        exigir(registradas.add(serie) && actual != null
                            && actual.terminal().getEstado() == EstadoTerminal.DISPONIBLE
                            && actual.ubicacion().equals(bodega), "El POS debe estar disponible en tu bodega y aparecer una sola vez: " + serie);
                        var terminal = actual.terminal();
                        boolean envio = d.tipo() == Tipo.ENCOMIENDA;
                        pos(plan, serie, equipo(serie, terminal.getMarca(), terminal.getModelo(),
                            envio ? EstadoTerminal.EN_TRANSITO : EstadoTerminal.ASIGNADO, d.comercio()),
                            envio ? "Envío por encomienda" : "Instalación", bodega);
                    }
                }
                if (d.tipo() == Tipo.INSTALACION || d.tipo() == Tipo.ENCOMIENDA) {
                    if (!d.material().isBlank()) materiales(plan, d, bodega, -1,
                        d.tipo() == Tipo.INSTALACION ? "Consumo en gestión" : "Envío por encomienda");
                }
                exigir(!plan.isEmpty(), "Indica al menos un equipo o suministro.");
            }
            case TRASLADO_POS, TRASLADO_MATERIAL -> {
                String origen = d.comercio();
                String destino = d.reemplazo();
                exigir(bodegas.containsKey(origen) && bodegas.containsKey(destino) && !origen.equals(destino),
                    "Selecciona dos bodegas distintas.");
                if (d.tipo() == Tipo.TRASLADO_POS) {
                    var actual = equipos.get(d.serie());
                    exigir(actual != null && actual.ubicacion().equals(origen)
                        && actual.terminal().getEstado() == EstadoTerminal.DISPONIBLE,
                        "El equipo debe estar disponible en la bodega de origen.");
                    pos(plan, d.serie(), new Equipo(actual.terminal(), destino), "Traslado desde " + origen, destino);
                } else {
                    materiales(plan, d, origen, -1, "Traslado hacia " + destino);
                    materiales(plan, d, destino, 1, "Traslado desde " + origen);
                }
            }
            case ANULACION -> {
                var original = solicitudes.get(d.origen());
                exigir(original != null && original.solicitante().equals(bodega)
                    && original.datos().tipo() != Tipo.ANULACION, "Selecciona una operación propia aprobada.");
                exigir(original.datos().tipo() != Tipo.TRASLADO_MATERIAL,
                    "Los traslados de materiales se corrigen con un traslado inverso.");
                var anteriores = efectos.get(d.origen());
                for (int indice : indices(d)) {
                    exigir(!revertidos.getOrDefault(d.origen(), Set.of()).contains(indice),
                        "Uno de los artículos ya fue revertido.");
                    var efecto = anteriores.get(indice - 1);
                    var mov = efecto.movimiento();
                    exigir(efecto.serie() == null || equipos.get(efecto.serie()) == efecto.nuevo(),
                        "El equipo tuvo un movimiento posterior.");
                    plan.add(new Efecto(new Movimiento(mov.categoria(), mov.articulo(),
                        mov.cantidad() < 0 ? "Reintegro por anulación" : "Reversión por anulación",
                        mov.bodega(), -mov.cantidad()), efecto.serie(), efecto.nuevo(), efecto.anterior()));
                }
            }
        }
        for (var efecto : plan) {
            var mov = efecto.movimiento();
            if (efecto.serie() == null) {
                long saldo = (long) bodegas.getOrDefault(mov.bodega(), Map.of()).getOrDefault(mov.articulo(), 0)
                    + mov.cantidad();
                exigir(saldo >= 0 && saldo <= Integer.MAX_VALUE, "Existencia insuficiente o fuera de rango: " + mov.articulo());
            }
        }
        return List.copyOf(plan);
    }

    private Set<Integer> indices(Datos datos) {
        int total = efectos.getOrDefault(datos.origen(), List.of()).size();
        exigir(total > 0, "La operación no tiene movimientos para revertir.");
        var indices = new LinkedHashSet<Integer>(datos.articulos());
        if (indices.isEmpty()) {
            for (int i = 1; i <= total; i++) {
                if (!revertidos.getOrDefault(datos.origen(), Set.of()).contains(i)) indices.add(i);
            }
        }
        exigir(!indices.isEmpty() && indices.stream().allMatch(i -> i > 0 && i <= total),
            "Selecciona artículos válidos de la operación.");
        return indices;
    }

    private void pos(List<Efecto> plan, String serie, Equipo nuevo, String detalle, String bodega) {
        plan.add(new Efecto(new Movimiento("Equipos POS", serie, detalle + " · "
            + nuevo.terminal().getEstado().getEtiqueta(), bodega, 1), serie, equipos.get(serie), nuevo));
    }

    private void materiales(List<Efecto> plan, Datos d, String bodega, int signo, String detalle) {
        exigir(!d.material().isBlank() && d.cantidad() > 0 && d.cantidad() <= 100000, "Indica un suministro y una cantidad válida.");
        var articulos = d.tipo() == Tipo.INSTALACION && d.material().equalsIgnoreCase("Cable de datos")
            ? List.of("Tramo de cable", "RJ45", "DB9 hembra", "Cobertor") : List.of(d.material());
        for (String articulo : articulos) {
            plan.add(new Efecto(new Movimiento("Materiales", articulo, detalle, bodega, signo * d.cantidad()), null, null, null));
        }
    }

    private void exigir(boolean valido, String mensaje) {
        if (!valido) throw new IllegalArgumentException(mensaje);
    }
}
