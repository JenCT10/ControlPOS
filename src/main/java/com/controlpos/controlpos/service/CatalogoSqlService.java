package com.controlpos.controlpos.service;

import com.controlpos.controlpos.model.*;
import com.controlpos.controlpos.repository.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
@Profile("sqlserver")
@Transactional(readOnly=true)
public class CatalogoSqlService {
    public record Comercio(String key,String value,String estado) {}
    public record Bodega(long id,String nombre,String responsable) {}
    public record Alta(long id,String tipo,String solicitante,String estado,String detalle) {}

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entidades;

    private final UsuarioRepository usuarios;
    private final BodegaRepository almacenes;
    private final ComercioRepository negocios;
    private final MaterialRepository articulos;
    private final TerminalRepository terminales;
    private final GestionRepository solicitudes;
    private final GestionPosRepository detallesPos;
    private final GestionMaterialRepository detallesMaterial;
    private final ExistenciaMaterialRepository existencias;
    private final MovimientoPosRepository movimientosPos;
    private final MovimientoMaterialRepository movimientosMaterial;

    public CatalogoSqlService(UsuarioRepository usuarios,BodegaRepository almacenes,ComercioRepository negocios,
        MaterialRepository articulos,TerminalRepository terminales,GestionRepository solicitudes,
        GestionPosRepository detallesPos,GestionMaterialRepository detallesMaterial,ExistenciaMaterialRepository existencias,
        MovimientoPosRepository movimientosPos,MovimientoMaterialRepository movimientosMaterial) {
        this.usuarios=usuarios;
        this.almacenes=almacenes;
        this.negocios=negocios;
        this.articulos=articulos;
        this.terminales=terminales;
        this.solicitudes=solicitudes;
        this.detallesPos=detallesPos;
        this.detallesMaterial=detallesMaterial;
        this.existencias=existencias;
        this.movimientosPos=movimientosPos;
        this.movimientosMaterial=movimientosMaterial;
    }

    public List<Comercio> comercios() {
        return negocios.findByActivoTrueOrderByNombre().stream().map(c->new Comercio(c.getCodigo(),c.getNombre(),
            c.getAprobadoEn()==null?"Pendiente":"Registrado")).toList();
    }

    public List<Bodega> bodegas() {
        return almacenes.findByActivaTrueOrderByNombre().stream().map(b->new Bodega(b.getId(),b.getNombre(),
            b.getResponsable()==null?"":b.getResponsable().getLogin())).toList();
    }

    public Map<String,String> ubicaciones() {
        var ubicaciones=new HashMap<String,String>();
        for(var t:terminales.findAll()) {
            ubicaciones.put(t.getSerie(),t.getBodega()!=null?t.getBodega().getNombre():
                (t.getComercio()!=null?t.getComercio().getNombre():"Banco Nacional"));
        }
        return ubicaciones;
    }

    public Map<String,Integer> materiales(String bodega) {
        var resultado=new TreeMap<String,Integer>();
        almacenes.findByNombreAndActivaTrue(bodega).ifPresent(b->{
            for(var e:existencias.findById_BodegaId(b.getId())) {
                var material=articulos.findById(e.getId().getMaterialId()).orElseThrow();
                resultado.put(material.getNombre(),e.getCantidad());
            }
        });
        return resultado;
    }

    public List<Alta> pendientes() {
        return solicitudes.findByEstadoOrderById("PENDIENTE").stream().map(g->{
            var pos=detallesPos.findByGestion_IdOrderById(g.getId());
            var materiales=detallesMaterial.findByGestion_IdOrderById(g.getId());
            var nombres=new ArrayList<String>();
            pos.forEach(p->nombres.add(p.getTerminal()==null?p.getSerieDeclarada():p.getTerminal().getSerie()));
            materiales.forEach(m->nombres.add(m.getMaterial().getNombre()+" x "+m.getCantidad()));
            return new Alta(g.getId(),g.getTipo(),g.getSolicitante().getLogin(),g.getEstado(),String.join(", ",nombres));
        }).toList();
    }

    private Usuario usuario(String login) {
        return usuarios.findByLoginIgnoreCase(login).filter(Usuario::getActivo)
            .orElseThrow(()->new IllegalArgumentException("Usuario no disponible."));
    }

    private com.controlpos.controlpos.model.Bodega bloquearBodega(Long id) {
        return almacenes.bloquear(id).orElseThrow(()->new IllegalArgumentException("La bodega no está activa."));
    }

    @Transactional
    public void crearBodega(String nombre,Long responsable) {
        exigir(!nombre.isBlank() && nombre.length()<=100,"Indica un nombre de bodega válido.");
        var b=new com.controlpos.controlpos.model.Bodega();
        b.setNombre(nombre);
        if(responsable!=null) b.setResponsable(usuarios.findById(responsable).filter(Usuario::getActivo)
            .orElseThrow(()->new IllegalArgumentException("Responsable no disponible.")));
        almacenes.saveAndFlush(b);
    }

    @Transactional
    public void crearComercio(String codigo,String nombre,String login,boolean administrador) {
        exigir(codigo.matches("[A-Z0-9._-]{1,30}") && !nombre.isBlank() && nombre.length()<=100,"Revisa el código y el nombre.");
        var c=new com.controlpos.controlpos.model.Comercio();
        c.setCodigo(codigo);
        c.setNombre(nombre);
        c.setCreadoPor(usuario(login));
        if(administrador) { c.setAprobadoPor(c.getCreadoPor()); c.setAprobadoEn(ahora()); }
        negocios.saveAndFlush(c);
    }

    @Transactional
    public void aprobarComercio(String codigo,String login) {
        var revisor=usuario(login);
        var c=negocios.bloquear(codigo).orElseThrow(()->new IllegalArgumentException("Comercio no encontrado."));
        exigir(c.getAprobadoPor()==null && !c.getCreadoPor().getId().equals(revisor.getId()),
            "Otra persona debe aprobar un comercio pendiente.");
        c.setAprobadoPor(revisor);
        c.setAprobadoEn(ahora());
    }

    @Transactional
    public void ingreso(Tipo tipo,String bodega,String serie,String marca,String modelo,String material,int cantidad,
                        String login,boolean administrador) {
        exigir(tipo==Tipo.INGRESO_POS || tipo==Tipo.INGRESO_SUMINISTRO,"Selecciona un tipo de ingreso válido.");
        var autor=usuario(login);
        var destino=almacenes.findByNombreAndActivaTrue(bodega)
            .orElseThrow(()->new IllegalArgumentException("Selecciona una bodega activa."));
        destino=bloquearBodega(destino.getId());
        exigir(administrador || destino.getResponsable()!=null && destino.getResponsable().getId().equals(autor.getId()),
            "Solo puedes cargar tu bodega asignada.");
        var g=new Gestion();
        g.setTipo(tipo.name());
        g.setSolicitante(autor);
        g.setBodegaDestino(destino);
        if(tipo==Tipo.INGRESO_POS) {
            exigir(!serie.isBlank() && serie.length()<=60 && !marca.isBlank() && marca.length()<=60
                && !modelo.isBlank() && modelo.length()<=60,"Revisa la serie, la marca y el modelo.");
            exigir(!terminales.existsBySerie(serie),"La serie ya existe.");
            solicitudes.saveAndFlush(g);
            var p=new GestionPos();
            p.setGestion(g); p.setSerieDeclarada(serie); p.setMarcaDeclarada(marca);
            p.setModeloDeclarado(modelo); p.setAccion("ENTRADA");
            detallesPos.saveAndFlush(p);
        } else {
            exigir(cantidad>0 && cantidad<=100000,"Indica una cantidad válida.");
            var articulo=articulos.findByNombreAndActivoTrue(material)
                .orElseThrow(()->new IllegalArgumentException("Selecciona un material registrado."));
            solicitudes.saveAndFlush(g);
            var m=new GestionMaterial(); m.setGestion(g); m.setMaterial(articulo); m.setCantidad(cantidad);
            detallesMaterial.saveAndFlush(m);
        }
        if(administrador) aplicarIngreso(g,autor);
    }

    @Transactional
    public void revisar(long id,String login,boolean aprobar,String motivo) {
        var revisor=usuario(login);
        var g=solicitudes.bloquear(id).orElseThrow(()->new IllegalArgumentException("Solicitud no encontrada."));
        exigir(g.getEstado().equals("PENDIENTE"),"La solicitud ya fue revisada.");
        exigir(!g.getSolicitante().getId().equals(revisor.getId()),"Otra persona debe revisar la solicitud.");
        if(!aprobar) {
            exigir(!motivo.isBlank() && motivo.length()<=500,"Indica el motivo del rechazo.");
            g.setEstado("RECHAZADA"); g.setRevisor(revisor); g.setRevisadaEn(ahora()); g.setMotivoRevision(motivo);
            return;
        }
        if(g.getTipo().equals("INGRESO_POS") || g.getTipo().equals("INGRESO_SUMINISTRO")) aplicarIngreso(g,revisor);
        else aplicarGestion(g,revisor);
    }

    private void aplicarIngreso(Gestion g,Usuario revisor) {
        var bodega=bloquearBodega(g.getBodegaDestino().getId());
        if(g.getTipo().equals("INGRESO_POS")) {
            var p=detallesPos.findByGestion_IdOrderById(g.getId()).getFirst();
            var t=new Terminal(p.getSerieDeclarada(),p.getMarcaDeclarada(),p.getModeloDeclarado(),EstadoTerminal.DISPONIBLE);
            t.setBodega(bodega);
            terminales.saveAndFlush(t);
            p.setTerminal(t);
            var m=new MovimientoPos(); m.setGestion(g); m.setTerminal(t); m.setUsuario(revisor);
            m.setTipo("INGRESO_POS"); m.setEstadoNuevo("DISPONIBLE"); m.setBodegaDestino(bodega);
            m.setDetalle("Ingreso al inventario");
            movimientosPos.save(m);
        } else {
            var m=detallesMaterial.findByGestion_IdOrderById(g.getId()).getFirst();
            ajustarMaterial(g,m.getMaterial(),bodega,revisor,m.getCantidad(),"INGRESO_SUMINISTRO");
        }
        aprobar(g,revisor);
    }

    private void ajustarMaterial(Gestion g,Material material,com.controlpos.controlpos.model.Bodega bodega,
                                 Usuario usuario,int cantidad,String tipo) {
        var id=new ExistenciaId(bodega.getId(),material.getId());
        var e=existencias.findById(id).orElseGet(()->new ExistenciaMaterial(id));
        int saldo=Math.addExact(e.getCantidad(),cantidad);
        exigir(saldo>=0,"No hay suficientes unidades de "+material.getNombre()+" en la bodega.");
        e.setCantidad(saldo); existencias.save(e);
        var m=new MovimientoMaterial(); m.setGestion(g); m.setMaterial(material); m.setBodega(bodega);
        m.setUsuario(usuario); m.setTipo(tipo); m.setCantidad(cantidad); m.setSaldoResultante(saldo);
        movimientosMaterial.save(m);
    }

    public List<Equipo> disponibles(String login,boolean admin) {
        var autor=usuario(login);
        return terminales.findAll().stream().filter(t->t.getEstado()==EstadoTerminal.DISPONIBLE
            && t.getBodega()!=null && t.getBodega().getActiva())
            .filter(t->admin || t.getBodega().getResponsable()!=null
                && t.getBodega().getResponsable().getId().equals(autor.getId()))
            .sorted(Comparator.comparing(Terminal::getSerie)).map(t->new Equipo(t,t.getBodega().getNombre())).toList();
    }

    public List<Solicitud> gestiones() {
        return solicitudes.findAllByOrderByIdDesc().stream().map(g->{
            String series=detallesPos.findByGestion_IdOrderById(g.getId()).stream()
                .map(p->p.getTerminal()==null?p.getSerieDeclarada():p.getTerminal().getSerie())
                .filter(Objects::nonNull).reduce((a,b)->a+","+b).orElse("");
            String estado=switch(g.getEstado()) { case "APROBADA"->"Aprobada"; case "RECHAZADA"->"Rechazada"; default->"Pendiente"; };
            return new Solicitud(g.getId(),new Datos(Tipo.valueOf(g.getTipo()),g.getComercio()==null?"":g.getComercio().getCodigo(),
                series,"","","","",0,null,List.of()),g.getSolicitante().getLogin(),estado,
                g.getRevisor()==null?"":g.getRevisor().getLogin(),Objects.toString(g.getMotivoRevision(),""),
                List.of(),g.getAplicadaEn());
        }).toList();
    }

    @Transactional
    public void registrarGestion(Tipo tipo,String comercio,String series,String reemplazo,String material,int cantidad,
                                 String login,boolean admin) {
        exigir(Set.of(Tipo.INSTALACION,Tipo.ENCOMIENDA,Tipo.RETIRO,Tipo.CAMBIO).contains(tipo),"Tipo de gestión no válido.");
        var autor=usuario(login);
        var negocio=negocios.findByCodigoAndActivoTrue(comercio).filter(c->c.getAprobadoEn()!=null)
            .orElseThrow(()->new IllegalArgumentException("Selecciona un comercio aprobado."));
        String salida=tipo==Tipo.CAMBIO?reemplazo:series;
        exigir(tipo!=Tipo.CAMBIO || !salida.contains(","),"Selecciona un solo equipo de reemplazo.");
        var seleccionados=new ArrayList<Terminal>();
        com.controlpos.controlpos.model.Bodega bodega=null;
        if(tipo==Tipo.INSTALACION || tipo==Tipo.CAMBIO || tipo==Tipo.ENCOMIENDA && !salida.isBlank()) {
            exigir(!salida.isBlank(),"Selecciona una serie disponible.");
            var vistas=new HashSet<String>();
            for(String valor:salida.split(",",-1)) {
                String serie=valor.strip().toUpperCase(Locale.ROOT);
                exigir(vistas.add(serie),"La serie está repetida.");
                var t=terminales.findBySerie(serie).filter(e->e.getEstado()==EstadoTerminal.DISPONIBLE && e.getBodega()!=null)
                    .orElseThrow(()->new IllegalArgumentException("El equipo ya no está disponible: "+serie));
                exigir(admin || t.getBodega().getResponsable()!=null
                    && t.getBodega().getResponsable().getId().equals(autor.getId()),"El equipo no pertenece a tu bodega.");
                exigir(bodega==null || bodega.getId().equals(t.getBodega().getId()),"Selecciona equipos de una misma bodega.");
                bodega=t.getBodega(); seleccionados.add(t);
            }
        }
        if(bodega==null) {
            var propias=almacenes.findByResponsable_IdAndActivaTrueOrderById(autor.getId());
            if(propias.isEmpty() && admin) propias=almacenes.findByActivaTrueOrderByNombre();
            exigir(!propias.isEmpty(),"Asigna una bodega al soportista.");
            bodega=propias.getFirst();
        }
        bodega=bloquearBodega(bodega.getId());
        var g=new Gestion(); g.setTipo(tipo.name()); g.setSolicitante(autor); g.setAsignado(autor);
        g.setComercio(negocio); g.setBodegaOrigen(bodega); g.setBodegaDestino(bodega);
        solicitudes.saveAndFlush(g);
        if(tipo==Tipo.RETIRO || tipo==Tipo.CAMBIO) {
            String retirada=series.isBlank()?"INTERNO-"+g.getId():series.strip().toUpperCase(Locale.ROOT);
            exigir(retirada.length()<=60 && !retirada.contains(","),"Indica una sola serie para el retiro.");
            var anterior=terminales.findBySerie(retirada);
            anterior.ifPresent(t->exigir(t.getEstado()==EstadoTerminal.ASIGNADO && t.getComercio()!=null
                && t.getComercio().getId().equals(negocio.getId()),"El equipo retirado debe estar instalado en este comercio."));
            var p=new GestionPos(); p.setGestion(g); p.setTerminal(anterior.orElse(null));
            p.setSerieDeclarada(retirada); p.setAccion("RETIRO"); detallesPos.save(p);
        }
        for(var t:seleccionados) {
            var p=new GestionPos(); p.setGestion(g); p.setTerminal(t); p.setAccion("SALIDA"); detallesPos.save(p);
        }
        if(!material.isBlank()) {
            exigir(tipo==Tipo.INSTALACION || tipo==Tipo.ENCOMIENDA,"Esta gestión no admite suministros.");
            exigir(cantidad>0 && cantidad<=100000,"Indica una cantidad válida.");
            var articulo=articulos.findByNombreAndActivoTrue(material)
                .orElseThrow(()->new IllegalArgumentException("Material no registrado."));
            var m=new GestionMaterial(); m.setGestion(g); m.setMaterial(articulo); m.setCantidad(cantidad); detallesMaterial.save(m);
        }
        detallesPos.flush(); detallesMaterial.flush();
        exigir(!detallesPos.findByGestion_IdOrderById(g.getId()).isEmpty()
            || !detallesMaterial.findByGestion_IdOrderById(g.getId()).isEmpty(),"Selecciona al menos un equipo o material.");
        if(admin) aplicarGestion(g,autor);
    }

    private void aplicarGestion(Gestion g,Usuario revisor) {
        var bodega=bloquearBodega(g.getBodegaOrigen().getId());
        for(var p:detallesPos.findByGestion_IdOrderById(g.getId())) {
            boolean retiro=p.getAccion().equals("RETIRO");
            String serie=p.getTerminal()==null?p.getSerieDeclarada():p.getTerminal().getSerie();
            var existente=terminales.bloquear(serie);
            String anterior=null;
            Terminal t;
            if(existente.isPresent()) {
                t=existente.get();
                entidades.refresh(t,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
                anterior=estadoSql(t.getEstado());
                exigir(retiro?t.getEstado()==EstadoTerminal.ASIGNADO && t.getComercio()!=null
                    && t.getComercio().getId().equals(g.getComercio().getId()):
                    t.getEstado()==EstadoTerminal.DISPONIBLE && t.getBodega()!=null && t.getBodega().getId().equals(bodega.getId()),
                    "El equipo cambió de estado o ubicación. Revisa la gestión.");
            } else {
                exigir(retiro,"Equipo no encontrado.");
                t=new Terminal(serie,"Sin identificar","Sin identificar",EstadoTerminal.PENDIENTE_REVISION);
                t.setCodigoInterno(serie.startsWith("INTERNO-"));
            }
            var nuevo=retiro?EstadoTerminal.PENDIENTE_REVISION:
                (g.getTipo().equals("ENCOMIENDA")?EstadoTerminal.EN_TRANSITO:EstadoTerminal.ASIGNADO);
            t.setEstado(nuevo); t.setBodega(retiro?bodega:null); t.setComercio(retiro?null:g.getComercio());
            terminales.saveAndFlush(t); p.setTerminal(t);
            var m=new MovimientoPos(); m.setGestion(g); m.setTerminal(t); m.setUsuario(revisor);
            m.setTipo(g.getTipo()); m.setEstadoAnterior(anterior); m.setEstadoNuevo(estadoSql(nuevo));
            m.setBodegaOrigen(retiro?null:bodega); m.setBodegaDestino(retiro?bodega:null);
            m.setComercioOrigen(retiro?g.getComercio():null); m.setComercioDestino(retiro?null:g.getComercio());
            movimientosPos.save(m);
        }
        var consumos=new TreeMap<Long,Integer>();
        for(var m:detallesMaterial.findByGestion_IdOrderById(g.getId())) {
            var componentes=articulos.componentes(m.getMaterial().getId());
            if(componentes.isEmpty()) consumos.merge(m.getMaterial().getId(),m.getCantidad(),Math::addExact);
            else for(var c:componentes) consumos.merge(((Number)c.get("componente_id")).longValue(),
                Math.multiplyExact(m.getCantidad(),((Number)c.get("cantidad")).intValue()),Math::addExact);
        }
        for(var consumo:consumos.entrySet()) {
            ajustarMaterial(g,articulos.findById(consumo.getKey()).orElseThrow(),bodega,revisor,-consumo.getValue(),
                g.getTipo().equals("ENCOMIENDA")?"ENVIO_ENCOMIENDA":"CONSUMO_GESTION");
        }
        aprobar(g,revisor);
    }

    private void aprobar(Gestion g,Usuario revisor) {
        g.setEstado("APROBADA"); g.setRevisor(revisor); g.setRevisadaEn(ahora()); g.setAplicadaEn(ahora());
    }

    private String estadoSql(EstadoTerminal estado) {
        return estado==EstadoTerminal.ASIGNADO?"INSTALADO":estado.name();
    }

    private LocalDateTime ahora() { return LocalDateTime.now(ZoneOffset.UTC); }

    private void exigir(boolean condicion,String mensaje) {
        if(!condicion) throw new IllegalArgumentException(mensaje);
    }
}
