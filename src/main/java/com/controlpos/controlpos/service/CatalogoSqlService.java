package com.controlpos.controlpos.service;

import com.controlpos.controlpos.model.Tipo;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@Profile("sqlserver")
public class CatalogoSqlService {
    public record Comercio(String key, String value, String estado) {}
    public record Bodega(long id, String nombre, String responsable) {}
    public record Alta(long id, String tipo, String solicitante, String estado, String detalle) {}

    private final JdbcTemplate jdbc;
    public CatalogoSqlService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Comercio> comercios() {
        return jdbc.query("""
            SELECT codigo,nombre,CASE WHEN aprobado_en IS NULL THEN 'Pendiente' ELSE 'Registrado' END estado
            FROM dbo.comercio WHERE activo=1 ORDER BY nombre
            """, (r,n) -> new Comercio(r.getString(1),r.getString(2),r.getString(3)));
    }

    public List<Bodega> bodegas() {
        return jdbc.query("""
            SELECT b.id,b.nombre,COALESCE(u.login,'') FROM dbo.bodega b
            LEFT JOIN dbo.usuario u ON u.id=b.responsable_id WHERE b.activa=1 ORDER BY b.nombre
            """, (r,n) -> new Bodega(r.getLong(1),r.getString(2),r.getString(3)));
    }

    public Map<String,String> ubicaciones() {
        var resultado = new HashMap<String,String>();
        jdbc.query("""
            SELECT t.serie,COALESCE(b.nombre,c.nombre,'Banco Nacional') ubicacion FROM dbo.terminal t
            LEFT JOIN dbo.bodega b ON b.id=t.bodega_id LEFT JOIN dbo.comercio c ON c.id=t.comercio_id
            """, r -> { resultado.put(r.getString(1),r.getString(2)); });
        return resultado;
    }

    public Map<String,Integer> materiales(String bodega) {
        var resultado = new TreeMap<String,Integer>();
        jdbc.query("""
            SELECT m.nombre,e.cantidad FROM dbo.existencias_material e
            JOIN dbo.material m ON m.id=e.material_id JOIN dbo.bodega b ON b.id=e.bodega_id
            WHERE b.nombre=?
            """, r -> { resultado.put(r.getString(1),r.getInt(2)); }, bodega);
        return resultado;
    }

    public List<Alta> pendientes() {
        return jdbc.query("""
            SELECT g.id,g.tipo,u.login,g.estado,COALESCE(p.serie_declarada,m.nombre,'')
            FROM dbo.gestion g JOIN dbo.usuario u ON u.id=g.solicitante_id
            LEFT JOIN dbo.gestion_pos p ON p.gestion_id=g.id
            LEFT JOIN dbo.gestion_material gm ON gm.gestion_id=g.id
            LEFT JOIN dbo.material m ON m.id=gm.material_id
            WHERE g.estado='PENDIENTE' ORDER BY g.id
            """, (r,n) -> new Alta(r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5)));
    }

    private long usuario(String login) {
        return jdbc.queryForObject("SELECT id FROM dbo.usuario WHERE login=? AND activo=1",Long.class,login);
    }

    @Transactional
    public void crearBodega(String nombre, Long responsable) {
        exigir(!nombre.isBlank() && nombre.length()<=100,"Indica un nombre de bodega válido.");
        if (responsable != null) exigir(jdbc.queryForObject(
            "SELECT COUNT(*) FROM dbo.usuario WHERE id=? AND activo=1",Integer.class,responsable)==1,"Responsable no disponible.");
        jdbc.update("INSERT INTO dbo.bodega(nombre,responsable_id) VALUES (?,?)",nombre,responsable);
    }

    @Transactional
    public void crearComercio(String codigo,String nombre,String login,boolean administrador) {
        exigir(codigo.matches("[A-Z0-9._-]{1,30}") && !nombre.isBlank() && nombre.length()<=100,"Revisa el código y el nombre.");
        long autor=usuario(login);
        jdbc.update("""
            INSERT INTO dbo.comercio(codigo,nombre,creado_por,aprobado_por,aprobado_en)
            VALUES (?,?,?,?,CASE WHEN ?=1 THEN SYSUTCDATETIME() ELSE NULL END)
            """,codigo,nombre,autor,administrador?autor:null,administrador?1:0);
    }

    @Transactional
    public void ingreso(Tipo tipo,String bodega,String serie,String marca,String modelo,
                        String material,int cantidad,String login,boolean administrador) {
        exigir(tipo==Tipo.INGRESO_POS || tipo==Tipo.INGRESO_SUMINISTRO,"Selecciona un tipo de ingreso válido.");
        long autor=usuario(login);
        var bodegas=jdbc.queryForList("SELECT id,responsable_id FROM dbo.bodega WITH (UPDLOCK,HOLDLOCK) WHERE nombre=? AND activa=1",bodega);
        exigir(!bodegas.isEmpty(),"Selecciona una bodega activa.");
        var destino=bodegas.getFirst();
        exigir(administrador || Objects.equals(destino.get("responsable_id"),autor),"Solo puedes cargar tu bodega asignada.");
        long bodegaId=((Number)destino.get("id")).longValue();
        if(tipo==Tipo.INGRESO_POS) {
            exigir(!serie.isBlank() && serie.length()<=60 && !marca.isBlank() && marca.length()<=60
                && !modelo.isBlank() && modelo.length()<=60,"Revisa la serie, la marca y el modelo.");
            exigir(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.terminal WHERE serie=?",Integer.class,serie)==0,"La serie ya existe.");
        } else {
            exigir(!material.isBlank() && cantidad>0 && cantidad<=100000,"Revisa el material y la cantidad.");
            exigir(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.material WHERE nombre=? AND activo=1",Integer.class,material)==1,
                "Selecciona un material registrado.");
        }
        long gestion=jdbc.queryForObject("""
            INSERT INTO dbo.gestion(tipo,bodega_destino_id,solicitante_id,clave_operacion)
            OUTPUT INSERTED.id VALUES (?,?,?,NEWID())
            """,Long.class,tipo.name(),bodegaId,autor);
        if(tipo==Tipo.INGRESO_POS) {
            jdbc.update("""
                INSERT INTO dbo.gestion_pos(gestion_id,serie_declarada,marca_declarada,modelo_declarado,accion)
                VALUES (?,?,?,?,'ENTRADA')
                """,gestion,serie,marca,modelo);
        } else {
            jdbc.update("""
                INSERT INTO dbo.gestion_material(gestion_id,material_id,cantidad)
                SELECT ?,id,? FROM dbo.material WHERE nombre=? AND activo=1
                """,gestion,cantidad,material);
        }
        if(administrador) aplicarIngreso(gestion,autor);
    }

    @Transactional
    public void revisar(long id,String login,boolean aprobar,String motivo) {
        long revisor=usuario(login);
        var solicitudes=jdbc.queryForList("SELECT * FROM dbo.gestion WITH (UPDLOCK,HOLDLOCK) WHERE id=?",id);
        exigir(!solicitudes.isEmpty(),"Solicitud no encontrada.");
        var solicitud=solicitudes.getFirst();
        exigir("PENDIENTE".equals(solicitud.get("estado")),"La solicitud ya fue revisada.");
        exigir(!Objects.equals(solicitud.get("solicitante_id"),revisor),"Otra persona debe revisar la solicitud.");
        if(aprobar) {
            String tipo=solicitud.get("tipo").toString();
            if(tipo.equals("INGRESO_POS") || tipo.equals("INGRESO_SUMINISTRO")) aplicarIngreso(id,revisor);
            else aplicarGestion(id,revisor);
        }
        else {
            exigir(!motivo.isBlank() && motivo.length()<=500,"Indica el motivo del rechazo.");
            jdbc.update("""
                UPDATE dbo.gestion SET estado='RECHAZADA',revisor_id=?,revisada_en=SYSUTCDATETIME(),motivo_revision=? WHERE id=?
                """,revisor,motivo,id);
        }
    }

    private void aplicarIngreso(long id,long revisor) {
        var g=jdbc.queryForMap("SELECT * FROM dbo.gestion WHERE id=?",id);
        long bodega=((Number)g.get("bodega_destino_id")).longValue();
        if("INGRESO_POS".equals(g.get("tipo"))) {
            var p=jdbc.queryForMap("SELECT * FROM dbo.gestion_pos WHERE gestion_id=?",id);
            long terminal=jdbc.queryForObject("""
                INSERT INTO dbo.terminal(serie,marca,modelo,bodega_id)
                OUTPUT INSERTED.id VALUES (?,?,?,?)
                """,Long.class,p.get("serie_declarada"),p.get("marca_declarada"),p.get("modelo_declarado"),bodega);
            jdbc.update("UPDATE dbo.gestion_pos SET terminal_id=? WHERE id=?",terminal,p.get("id"));
            jdbc.update("""
                INSERT INTO dbo.movimiento_pos(gestion_id,terminal_id,usuario_id,tipo,estado_nuevo,bodega_destino_id,detalle)
                VALUES (?,?,?,'INGRESO_POS','DISPONIBLE',?,N'Ingreso al inventario')
                """,id,terminal,revisor,bodega);
        } else {
            exigir("INGRESO_SUMINISTRO".equals(g.get("tipo")),"Este tipo de solicitud requiere su módulo de aprobación.");
            var m=jdbc.queryForMap("SELECT material_id,cantidad FROM dbo.gestion_material WHERE gestion_id=?",id);
            long material=((Number)m.get("material_id")).longValue();
            int cantidad=((Number)m.get("cantidad")).intValue();
            var saldo=jdbc.queryForList("""
                SELECT cantidad FROM dbo.existencias_material WITH (UPDLOCK,HOLDLOCK) WHERE bodega_id=? AND material_id=?
                """,Integer.class,bodega,material);
            int nuevo=Math.addExact(saldo.isEmpty()?0:saldo.getFirst(),cantidad);
            if(saldo.isEmpty()) jdbc.update("INSERT INTO dbo.existencias_material(bodega_id,material_id,cantidad) VALUES (?,?,?)",bodega,material,nuevo);
            else jdbc.update("UPDATE dbo.existencias_material SET cantidad=? WHERE bodega_id=? AND material_id=?",nuevo,bodega,material);
            jdbc.update("""
                INSERT INTO dbo.movimiento_material(gestion_id,material_id,bodega_id,usuario_id,tipo,cantidad,saldo_resultante)
                VALUES (?,?,?,?,'INGRESO_SUMINISTRO',?,?)
                """,id,material,bodega,revisor,cantidad,nuevo);
        }
        jdbc.update("""
            UPDATE dbo.gestion SET estado='APROBADA',revisor_id=?,revisada_en=SYSUTCDATETIME(),aplicada_en=SYSUTCDATETIME() WHERE id=?
            """,revisor,id);
    }

    @Transactional
    public void aprobarComercio(String codigo,String login) {
        long revisor=usuario(login);
        int cambio=jdbc.update("""
            UPDATE dbo.comercio SET aprobado_por=?,aprobado_en=SYSUTCDATETIME()
            WHERE codigo=? AND aprobado_por IS NULL AND creado_por<>?
            """,revisor,codigo,revisor);
        exigir(cambio==1,"Otra persona debe aprobar un comercio pendiente.");
    }

    public List<com.controlpos.controlpos.model.Equipo> disponibles(String login,boolean admin) {
        return jdbc.query("""
            SELECT t.serie,t.marca,t.modelo,b.nombre FROM dbo.terminal t
            JOIN dbo.bodega b ON b.id=t.bodega_id LEFT JOIN dbo.usuario u ON u.id=b.responsable_id
            WHERE t.estado='DISPONIBLE' AND b.activa=1 AND (?=1 OR u.login=?) ORDER BY t.serie
            """,(r,n)->new com.controlpos.controlpos.model.Equipo(
                new com.controlpos.controlpos.model.Terminal(r.getString(1),r.getString(2),r.getString(3),
                    com.controlpos.controlpos.model.EstadoTerminal.DISPONIBLE),r.getString(4)),admin?1:0,login);
    }

    public List<com.controlpos.controlpos.model.Solicitud> gestiones() {
        return jdbc.query("""
            SELECT g.*,u.login solicitante,COALESCE(r.login,'') revisor,COALESCE(c.codigo,'') comercio,
                COALESCE((SELECT STRING_AGG(COALESCE(p.serie_declarada,t.serie),',') FROM dbo.gestion_pos p
                    LEFT JOIN dbo.terminal t ON t.id=p.terminal_id WHERE p.gestion_id=g.id),'') series
            FROM dbo.gestion g JOIN dbo.usuario u ON u.id=g.solicitante_id
            LEFT JOIN dbo.usuario r ON r.id=g.revisor_id LEFT JOIN dbo.comercio c ON c.id=g.comercio_id
            ORDER BY g.id DESC
            """,(r,n)->new com.controlpos.controlpos.model.Solicitud(r.getLong("id"),
                new com.controlpos.controlpos.model.Datos(Tipo.valueOf(r.getString("tipo")),r.getString("comercio"),
                    r.getString("series"),"","","","",0,null,List.of()),r.getString("solicitante"),
                switch(r.getString("estado")) {case "APROBADA"->"Aprobada";case "RECHAZADA"->"Rechazada";default->"Pendiente";},
                r.getString("revisor"),Objects.toString(r.getString("motivo_revision"),""),List.of(),
                r.getTimestamp("aplicada_en")==null?null:r.getTimestamp("aplicada_en").toLocalDateTime()));
    }

    @Transactional
    public void registrarGestion(Tipo tipo,String comercio,String series,String reemplazo,String material,int cantidad,
                                 String login,boolean admin) {
        exigir(Set.of(Tipo.INSTALACION,Tipo.ENCOMIENDA,Tipo.RETIRO,Tipo.CAMBIO).contains(tipo),"Tipo de gestión no válido.");
        long autor=usuario(login);
        var comercios=jdbc.queryForList("SELECT id FROM dbo.comercio WHERE codigo=? AND activo=1 AND aprobado_en IS NOT NULL",comercio);
        exigir(!comercios.isEmpty(),"Selecciona un comercio aprobado.");
        long comercioId=((Number)comercios.getFirst().get("id")).longValue();
        String salida=tipo==Tipo.CAMBIO?reemplazo:series;
        exigir(tipo!=Tipo.CAMBIO || !salida.contains(","),"Selecciona un solo equipo de reemplazo.");
        Long bodega=null;
        if(tipo==Tipo.INSTALACION || tipo==Tipo.CAMBIO || (tipo==Tipo.ENCOMIENDA && !salida.isBlank())) {
            exigir(!salida.isBlank(),"Selecciona una serie disponible.");
            var vistas=new HashSet<String>();
            for(String valor:salida.split(",",-1)) {
                String serie=valor.strip().toUpperCase(Locale.ROOT);
                exigir(vistas.add(serie),"La serie está repetida.");
                var encontrados=jdbc.queryForList("""
                    SELECT t.id,t.bodega_id,b.responsable_id FROM dbo.terminal t WITH (UPDLOCK,HOLDLOCK)
                    JOIN dbo.bodega b ON b.id=t.bodega_id WHERE t.serie=? AND t.estado='DISPONIBLE' AND b.activa=1
                    """,serie);
                exigir(!encontrados.isEmpty(),"El equipo ya no está disponible: "+serie);
                var equipo=encontrados.getFirst();
                exigir(admin || Objects.equals(equipo.get("responsable_id"),autor),"El equipo no pertenece a tu bodega.");
                long origen=((Number)equipo.get("bodega_id")).longValue();
                exigir(bodega==null || bodega==origen,"Selecciona equipos de una misma bodega.");
                bodega=origen;
            }
        }
        if(bodega==null) {
            var propias=jdbc.queryForList("SELECT id FROM dbo.bodega WHERE activa=1 AND responsable_id=? ORDER BY id",Long.class,autor);
            if(propias.isEmpty() && admin) propias=jdbc.queryForList("SELECT TOP 1 id FROM dbo.bodega WHERE activa=1 ORDER BY id",Long.class);
            exigir(!propias.isEmpty(),"Asigna una bodega al soportista antes de registrar esta gestión.");
            bodega=propias.getFirst();
        }
        long id=jdbc.queryForObject("""
            INSERT INTO dbo.gestion(tipo,comercio_id,bodega_origen_id,bodega_destino_id,solicitante_id,asignado_id,clave_operacion)
            OUTPUT INSERTED.id VALUES (?,?,?,?,?,?,NEWID())
            """,Long.class,tipo.name(),comercioId,bodega,bodega,autor,autor);
        if(tipo==Tipo.RETIRO || tipo==Tipo.CAMBIO) {
            String retirada=series.isBlank()?"INTERNO-"+id:series.strip().toUpperCase(Locale.ROOT);
            exigir(retirada.length()<=60 && !retirada.contains(","),"Indica una sola serie para el retiro.");
            var anterior=jdbc.queryForList("SELECT id,estado,comercio_id FROM dbo.terminal WITH (UPDLOCK,HOLDLOCK) WHERE serie=?",retirada);
            if(!anterior.isEmpty()) {
                var t=anterior.getFirst();
                exigir("INSTALADO".equals(t.get("estado")) && Objects.equals(t.get("comercio_id"),comercioId),
                    "El equipo retirado debe estar instalado en este comercio.");
            }
            jdbc.update("""
                INSERT INTO dbo.gestion_pos(gestion_id,terminal_id,serie_declarada,accion)
                VALUES (?,?,?,'RETIRO')
                """,id,anterior.isEmpty()?null:anterior.getFirst().get("id"),retirada);
        }
        if(tipo==Tipo.INSTALACION || tipo==Tipo.CAMBIO || (tipo==Tipo.ENCOMIENDA && !salida.isBlank())) {
            for(String valor:salida.split(",")) {
                jdbc.update("""
                    INSERT INTO dbo.gestion_pos(gestion_id,terminal_id,accion)
                    SELECT ?,id,'SALIDA' FROM dbo.terminal WHERE serie=?
                    """,id,valor.strip().toUpperCase(Locale.ROOT));
            }
        }
        if(!material.isBlank()) {
            exigir(tipo==Tipo.INSTALACION || tipo==Tipo.ENCOMIENDA,"Esta gestión no admite suministros.");
            exigir(cantidad>0 && cantidad<=100000,"Indica una cantidad válida.");
            var articulo=jdbc.queryForList("SELECT id FROM dbo.material WHERE nombre=? AND activo=1",Long.class,material);
            exigir(!articulo.isEmpty(),"Material no registrado.");
            jdbc.update("INSERT INTO dbo.gestion_material(gestion_id,material_id,cantidad) VALUES (?,?,?)",id,articulo.getFirst(),cantidad);
        }
        exigir(jdbc.queryForObject("""
            SELECT (SELECT COUNT(*) FROM dbo.gestion_pos WHERE gestion_id=?)
                 + (SELECT COUNT(*) FROM dbo.gestion_material WHERE gestion_id=?)
            """,Integer.class,id,id)>0,"Selecciona al menos un equipo o material.");
        if(admin) aplicarGestion(id,autor);
    }

    private void aplicarGestion(long id,long revisor) {
        var g=jdbc.queryForMap("SELECT * FROM dbo.gestion WHERE id=?",id);
        long bodega=((Number)g.get("bodega_origen_id")).longValue();
        long comercio=((Number)g.get("comercio_id")).longValue();
        String tipo=g.get("tipo").toString();
        for(var p:jdbc.queryForList("SELECT * FROM dbo.gestion_pos WHERE gestion_id=? ORDER BY id",id)) {
            boolean retiro="RETIRO".equals(p.get("accion"));
            Long terminal=p.get("terminal_id")==null?null:((Number)p.get("terminal_id")).longValue();
            String anterior=null;
            if(terminal==null) {
                exigir(retiro,"Equipo no encontrado.");
                terminal=jdbc.queryForObject("""
                    INSERT INTO dbo.terminal(serie,codigo_interno,marca,modelo,estado,bodega_id)
                    OUTPUT INSERTED.id VALUES (?,?,N'Sin identificar',N'Sin identificar','PENDIENTE_REVISION',?)
                    """,Long.class,p.get("serie_declarada"),p.get("serie_declarada").toString().startsWith("INTERNO-")?1:0,bodega);
                jdbc.update("UPDATE dbo.gestion_pos SET terminal_id=? WHERE id=?",terminal,p.get("id"));
            } else {
                var t=jdbc.queryForMap("SELECT * FROM dbo.terminal WITH (UPDLOCK,HOLDLOCK) WHERE id=?",terminal);
                anterior=t.get("estado").toString();
                exigir(retiro?("INSTALADO".equals(anterior) && Objects.equals(t.get("comercio_id"),comercio)):
                    ("DISPONIBLE".equals(anterior) && Objects.equals(t.get("bodega_id"),bodega)),
                    "El equipo cambió de estado o ubicación. Revisa la gestión.");
            }
            String nuevo=retiro?"PENDIENTE_REVISION":("ENCOMIENDA".equals(tipo)?"EN_TRANSITO":"INSTALADO");
            jdbc.update("UPDATE dbo.terminal SET estado=?,bodega_id=?,comercio_id=? WHERE id=?",
                nuevo,retiro?bodega:null,retiro?null:comercio,terminal);
            jdbc.update("""
                INSERT INTO dbo.movimiento_pos(gestion_id,terminal_id,usuario_id,tipo,estado_anterior,estado_nuevo,
                    bodega_origen_id,bodega_destino_id,comercio_origen_id,comercio_destino_id)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                """,id,terminal,revisor,tipo,anterior,nuevo,retiro?null:bodega,retiro?bodega:null,retiro?comercio:null,retiro?null:comercio);
        }
        var consumos=new TreeMap<Long,Integer>();
        for(var m:jdbc.queryForList("SELECT material_id,cantidad FROM dbo.gestion_material WHERE gestion_id=?",id)) {
            long articulo=((Number)m.get("material_id")).longValue();
            int cantidad=((Number)m.get("cantidad")).intValue();
            var componentes=jdbc.queryForList("SELECT componente_id,cantidad FROM dbo.componente_material WHERE material_id=?",articulo);
            if(componentes.isEmpty()) consumos.merge(articulo,cantidad,Math::addExact);
            else for(var c:componentes) consumos.merge(((Number)c.get("componente_id")).longValue(),
                Math.multiplyExact(cantidad,((Number)c.get("cantidad")).intValue()),Math::addExact);
        }
        for(var consumo:consumos.entrySet()) {
            var saldos=jdbc.queryForList("""
                SELECT cantidad FROM dbo.existencias_material WITH (UPDLOCK,HOLDLOCK) WHERE bodega_id=? AND material_id=?
                """,Integer.class,bodega,consumo.getKey());
            exigir(!saldos.isEmpty() && saldos.getFirst()>=consumo.getValue(),"No hay suficientes materiales en la bodega.");
            int saldo=saldos.getFirst()-consumo.getValue();
            jdbc.update("UPDATE dbo.existencias_material SET cantidad=? WHERE bodega_id=? AND material_id=?",saldo,bodega,consumo.getKey());
            jdbc.update("""
                INSERT INTO dbo.movimiento_material(gestion_id,material_id,bodega_id,usuario_id,tipo,cantidad,saldo_resultante)
                VALUES (?,?,?,?,?, ?,?)
                """,id,consumo.getKey(),bodega,revisor,"ENCOMIENDA".equals(tipo)?"ENVIO_ENCOMIENDA":"CONSUMO_GESTION",-consumo.getValue(),saldo);
        }
        jdbc.update("""
            UPDATE dbo.gestion SET estado='APROBADA',revisor_id=?,revisada_en=SYSUTCDATETIME(),aplicada_en=SYSUTCDATETIME() WHERE id=?
            """,revisor,id);
    }
    private void exigir(boolean condicion,String mensaje) {
        if(!condicion) throw new IllegalArgumentException(mensaje);
    }
}
