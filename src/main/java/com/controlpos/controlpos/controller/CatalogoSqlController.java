package com.controlpos.controlpos.controller;

import com.controlpos.controlpos.model.Tipo;
import com.controlpos.controlpos.service.CatalogoSqlService;
import com.controlpos.controlpos.repository.UsuarioRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Locale;

@Controller
@Profile("sqlserver")
public class CatalogoSqlController {
    private final CatalogoSqlService catalogo;
    private final UsuarioRepository usuarios;
    public CatalogoSqlController(CatalogoSqlService catalogo,UsuarioRepository usuarios) {
        this.catalogo=catalogo;
        this.usuarios=usuarios;
    }

    @GetMapping({"/comercios","/suministros","/aprobaciones"})
    public String ver(jakarta.servlet.http.HttpServletRequest peticion,Model modelo,
                      @RequestParam(defaultValue="") String bodega) {
        modelo.addAttribute("seccion",peticion.getServletPath().substring(1));
        modelo.addAttribute("comercios",catalogo.comercios());
        modelo.addAttribute("bodegas",catalogo.bodegas());
        modelo.addAttribute("usuarios",usuarios.listar());
        modelo.addAttribute("bodega",bodega);
        modelo.addAttribute("materiales",catalogo.materiales(bodega));
        modelo.addAttribute("pendientes",catalogo.pendientes());
        return "catalogos";
    }

    @GetMapping("/gestiones")
    public String gestiones(Authentication usuario,Model modelo) {
        boolean admin=usuario.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMINISTRADOR"));
        boolean soportista=usuario.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_SOPORTISTA"));
        modelo.addAttribute("seccion","gestiones");
        var comercios=new java.util.TreeMap<String,String>();
        catalogo.comercios().stream().filter(c->c.estado().equals("Registrado")).forEach(c->comercios.put(c.key(),c.value()));
        modelo.addAttribute("comercios",comercios);
        modelo.addAttribute("solicitudes",catalogo.gestiones());
        modelo.addAttribute("seriesDisponibles",catalogo.disponibles(usuario.getName(),admin));
        modelo.addAttribute("responsable",usuario.getName());
        modelo.addAttribute("usuarioActual",usuario.getName());
        modelo.addAttribute("puedeSolicitar",admin || soportista);
        return "operaciones";
    }

    @PostMapping("/bodegas")
    public String bodega(@RequestParam String nombre,@RequestParam(required=false) Long responsable,RedirectAttributes mensaje) {
        try {
            catalogo.crearBodega(nombre.strip(),responsable);
            mensaje.addFlashAttribute("aviso","Bodega guardada.");
        } catch(IllegalArgumentException | DataAccessException error) {
            mensaje.addFlashAttribute("error",detalle(error));
        }
        return "redirect:/suministros";
    }

    @PostMapping("/solicitudes")
    public String solicitar(@RequestParam Tipo tipo,@RequestParam(defaultValue="") String comercio,
                            @RequestParam(defaultValue="") String serie,@RequestParam(defaultValue="") String marca,
                            @RequestParam(defaultValue="") String modelo,@RequestParam(defaultValue="") String material,
                            @RequestParam(defaultValue="0") int cantidad,@RequestParam(defaultValue="") String reemplazo,
                            Authentication usuario,RedirectAttributes mensaje) {
        boolean admin=usuario.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMINISTRADOR"));
        try {
            if(tipo==Tipo.ALTA_COMERCIO) catalogo.crearComercio(comercio.strip().toUpperCase(Locale.ROOT),modelo.strip(),usuario.getName(),admin);
            else if (tipo==Tipo.INGRESO_POS || tipo==Tipo.INGRESO_SUMINISTRO) catalogo.ingreso(tipo,comercio.strip(),serie.strip().toUpperCase(Locale.ROOT),marca.strip(),modelo.strip(),material.strip(),cantidad,usuario.getName(),admin);
            else catalogo.registrarGestion(tipo,comercio.strip().toUpperCase(Locale.ROOT),serie.strip(),reemplazo.strip(),
                material.strip(),cantidad,usuario.getName(),admin);
            mensaje.addFlashAttribute("aviso",admin?"Registro guardado en la base de datos.":"Solicitud registrada. Requiere aprobación del supervisor.");
        } catch(IllegalArgumentException | DataAccessException error) {
            mensaje.addFlashAttribute("error",detalle(error));
        }
        return switch(tipo) {
            case ALTA_COMERCIO -> "redirect:/comercios";
            case INGRESO_POS, INGRESO_SUMINISTRO -> "redirect:/terminales";
            default -> "redirect:/gestiones";
        };
    }

    @PostMapping("/aprobaciones/{id}")
    public String aprobar(@PathVariable long id,@RequestParam boolean aprobar,@RequestParam(defaultValue="") String motivo,
                          Authentication usuario,RedirectAttributes mensaje) {
        try {
            catalogo.revisar(id,usuario.getName(),aprobar,motivo.strip());
            mensaje.addFlashAttribute("aviso",aprobar?"Ingreso aprobado. Inventario actualizado.":"Solicitud rechazada.");
        } catch(IllegalArgumentException | DataAccessException error) {
            mensaje.addFlashAttribute("error",detalle(error));
        }
        return "redirect:/aprobaciones";
    }

    @PostMapping("/aprobaciones/comercio/{codigo}")
    public String aprobarComercio(@PathVariable String codigo,Authentication usuario,RedirectAttributes mensaje) {
        try {
            catalogo.aprobarComercio(codigo,usuario.getName());
            mensaje.addFlashAttribute("aviso","Comercio aprobado.");
        } catch(IllegalArgumentException | DataAccessException error) {
            mensaje.addFlashAttribute("error",detalle(error));
        }
        return "redirect:/aprobaciones";
    }

    private String detalle(Exception error) {
        return error instanceof IllegalArgumentException?error.getMessage()
            :"No se pudo guardar. Comprueba que el registro no esté duplicado y que sus datos sean válidos.";
    }
}
