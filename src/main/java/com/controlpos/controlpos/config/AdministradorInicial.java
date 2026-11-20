package com.controlpos.controlpos.config;

import com.controlpos.controlpos.service.UsuarioSqlService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="controlpos.admin.inicializar", havingValue="true", matchIfMissing=true)
@Component
@Profile("sqlserver")
public class AdministradorInicial implements ApplicationRunner {
    private final UsuarioSqlService usuarios;
    private final Environment entorno;

    public AdministradorInicial(UsuarioSqlService usuarios, Environment entorno) {
        this.usuarios = usuarios;
        this.entorno = entorno;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        usuarios.inicializar(entorno.getProperty("CONTROLPOS_ADMIN_PASSWORD", ""));
    }
}
