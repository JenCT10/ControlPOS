-- SQL Server. Ejecutar con una cuenta autorizada para crear bases de datos.
USE master;
GO
IF DB_ID(N'LAROTracker') IS NULL
    EXEC(N'CREATE DATABASE [LAROTracker]');
GO
USE [LAROTracker];
GO
SET XACT_ABORT ON;
GO
BEGIN TRY
    BEGIN TRANSACTION;
    IF EXISTS (SELECT 1 FROM sys.tables WHERE schema_id = SCHEMA_ID(N'dbo'))
        THROW 50001, N'LAROTracker ya contiene tablas. Revise la base seleccionada.', 1;

    CREATE TABLE dbo.rol (
        id SMALLINT NOT NULL PRIMARY KEY,
        nombre VARCHAR(20) NOT NULL UNIQUE,
        CONSTRAINT ck_rol_nombre CHECK (nombre IN ('ADMINISTRADOR','SUPERVISOR','SOPORTISTA'))
    );

    CREATE TABLE dbo.usuario (
        id BIGINT IDENTITY PRIMARY KEY,
        nombre NVARCHAR(120) NOT NULL,
        login VARCHAR(60) NOT NULL UNIQUE,
        clave_hash VARCHAR(255) NOT NULL,
        rol_id SMALLINT NOT NULL REFERENCES dbo.rol(id),
        activo BIT NOT NULL DEFAULT 1,
        creado_en DATETIME2(3) NOT NULL DEFAULT SYSUTCDATETIME(),
        actualizado_en DATETIME2(3) NOT NULL DEFAULT SYSUTCDATETIME(),
        version ROWVERSION,
        CONSTRAINT ck_usuario_nombre CHECK (LEN(TRIM(nombre)) > 0),
        CONSTRAINT ck_usuario_login CHECK (LEN(TRIM(login)) >= 3)
    );

    CREATE TABLE dbo.bodega (
        id BIGINT IDENTITY PRIMARY KEY,
        nombre NVARCHAR(100) NOT NULL UNIQUE,
        responsable_id BIGINT NULL REFERENCES dbo.usuario(id),
        activa BIT NOT NULL DEFAULT 1,
        CONSTRAINT ck_bodega_nombre CHECK (LEN(TRIM(nombre)) > 0)
    );

    CREATE TABLE dbo.comercio (
        id BIGINT IDENTITY PRIMARY KEY,
        codigo VARCHAR(30) NOT NULL UNIQUE,
        nombre NVARCHAR(100) NOT NULL,
        ubicacion NVARCHAR(250) NULL,
        activo BIT NOT NULL DEFAULT 1,
        creado_por BIGINT NOT NULL REFERENCES dbo.usuario(id),
        creado_en DATETIME2(3) NOT NULL DEFAULT SYSUTCDATETIME(),
        aprobado_por BIGINT NULL REFERENCES dbo.usuario(id),
        aprobado_en DATETIME2(3) NULL,
        CONSTRAINT ck_comercio_nombre CHECK (LEN(TRIM(nombre)) > 0),
        CONSTRAINT ck_comercio_aprobacion CHECK (
            (aprobado_por IS NULL AND aprobado_en IS NULL) OR
            (aprobado_por IS NOT NULL AND aprobado_en IS NOT NULL))
    );

    CREATE TABLE dbo.material (
        id BIGINT IDENTITY PRIMARY KEY,
        nombre NVARCHAR(100) NOT NULL UNIQUE,
        activo BIT NOT NULL DEFAULT 1,
        CONSTRAINT ck_material_nombre CHECK (LEN(TRIM(nombre)) > 0)
    );

    CREATE TABLE dbo.componente_material (
        material_id BIGINT NOT NULL REFERENCES dbo.material(id),
        componente_id BIGINT NOT NULL REFERENCES dbo.material(id),
        cantidad INT NOT NULL,
        PRIMARY KEY (material_id, componente_id),
        CONSTRAINT ck_componente_distinto CHECK (material_id <> componente_id),
        CONSTRAINT ck_componente_cantidad CHECK (cantidad > 0)
    );

    CREATE TABLE dbo.existencias_material (
        bodega_id BIGINT NOT NULL REFERENCES dbo.bodega(id),
        material_id BIGINT NOT NULL REFERENCES dbo.material(id),
        cantidad INT NOT NULL DEFAULT 0,
        version ROWVERSION,
        PRIMARY KEY (bodega_id, material_id),
        CONSTRAINT ck_existencias_no_negativas CHECK (cantidad >= 0)
    );

    CREATE TABLE dbo.terminal (
        id BIGINT IDENTITY PRIMARY KEY,
        serie VARCHAR(60) NOT NULL UNIQUE,
        codigo_interno BIT NOT NULL DEFAULT 0,
        marca NVARCHAR(60) NOT NULL,
        modelo NVARCHAR(60) NOT NULL,
        consecutivo_bncr NVARCHAR(60) NULL,
        fecha_ingreso DATETIME2(3) NOT NULL DEFAULT SYSUTCDATETIME(),
        estado VARCHAR(30) NOT NULL DEFAULT 'DISPONIBLE',
        bodega_id BIGINT NULL REFERENCES dbo.bodega(id),
        comercio_id BIGINT NULL REFERENCES dbo.comercio(id),
        version ROWVERSION,
        CONSTRAINT ck_terminal_estado CHECK (estado IN
            ('DISPONIBLE','INSTALADO','EN_TRANSITO','PENDIENTE_REVISION',
             'MANTENIMIENTO','INACTIVO','ENVIADO_BNCR')),
        CONSTRAINT ck_terminal_ubicacion CHECK (
            (estado IN ('DISPONIBLE','PENDIENTE_REVISION','MANTENIMIENTO','INACTIVO')
                AND bodega_id IS NOT NULL AND comercio_id IS NULL)
            OR (estado IN ('INSTALADO','EN_TRANSITO')
                AND bodega_id IS NULL AND comercio_id IS NOT NULL)
            OR (estado = 'ENVIADO_BNCR' AND bodega_id IS NULL AND comercio_id IS NULL)),
        CONSTRAINT ck_terminal_serie CHECK (LEN(TRIM(serie)) > 0)
    );

    CREATE TABLE dbo.gestion (
        id BIGINT IDENTITY PRIMARY KEY,
        tipo VARCHAR(30) NOT NULL,
        estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
        comercio_id BIGINT NULL REFERENCES dbo.comercio(id),
        bodega_origen_id BIGINT NULL REFERENCES dbo.bodega(id),
        bodega_destino_id BIGINT NULL REFERENCES dbo.bodega(id),
        solicitante_id BIGINT NOT NULL REFERENCES dbo.usuario(id),
        asignado_id BIGINT NULL REFERENCES dbo.usuario(id),
        revisor_id BIGINT NULL REFERENCES dbo.usuario(id),
        gestion_original_id BIGINT NULL REFERENCES dbo.gestion(id),
        asunto NVARCHAR(250) NULL,
        descripcion NVARCHAR(2000) NULL,
        motivo_revision NVARCHAR(500) NULL,
        contacto_nombre NVARCHAR(120) NULL,
        contacto_telefono NVARCHAR(30) NULL,
        contacto_email NVARCHAR(254) NULL,
        creada_en DATETIME2(3) NOT NULL DEFAULT SYSUTCDATETIME(),
        revisada_en DATETIME2(3) NULL,
        aplicada_en DATETIME2(3) NULL,
        clave_operacion UNIQUEIDENTIFIER NOT NULL UNIQUE,
        version ROWVERSION,
        CONSTRAINT ck_gestion_tipo CHECK (tipo IN ('INSTALACION','ENCOMIENDA','RETIRO','CAMBIO',
            'INGRESO_POS','INGRESO_SUMINISTRO','TRASLADO_POS','TRASLADO_MATERIAL',
            'ANULACION','AJUSTE_MATERIAL','ENVIO_BNCR')),
        CONSTRAINT ck_gestion_estado CHECK (estado IN ('PENDIENTE','APROBADA','RECHAZADA')),
        CONSTRAINT ck_gestion_comercio CHECK (
            tipo NOT IN ('INSTALACION','ENCOMIENDA','RETIRO','CAMBIO') OR comercio_id IS NOT NULL),
        CONSTRAINT ck_gestion_anulacion CHECK (
            (tipo = 'ANULACION' AND gestion_original_id IS NOT NULL) OR
            (tipo <> 'ANULACION' AND gestion_original_id IS NULL)),
        CONSTRAINT ck_gestion_revision CHECK (
            (estado = 'PENDIENTE' AND revisor_id IS NULL AND revisada_en IS NULL AND aplicada_en IS NULL)
            OR (estado = 'APROBADA' AND revisor_id IS NOT NULL AND revisada_en IS NOT NULL AND aplicada_en IS NOT NULL)
            OR (estado = 'RECHAZADA' AND revisor_id IS NOT NULL AND revisada_en IS NOT NULL
                AND aplicada_en IS NULL AND LEN(TRIM(motivo_revision)) > 0)),
        CONSTRAINT ck_gestion_traslado CHECK (
            tipo NOT IN ('TRASLADO_POS','TRASLADO_MATERIAL') OR
            (bodega_origen_id IS NOT NULL AND bodega_destino_id IS NOT NULL AND bodega_origen_id <> bodega_destino_id))
    );

    CREATE TABLE dbo.gestion_pos (
        id BIGINT IDENTITY PRIMARY KEY,
        gestion_id BIGINT NOT NULL REFERENCES dbo.gestion(id),
        terminal_id BIGINT NULL REFERENCES dbo.terminal(id),
        serie_declarada VARCHAR(60) NULL,
        marca_declarada NVARCHAR(60) NULL,
        modelo_declarado NVARCHAR(60) NULL,
        accion VARCHAR(15) NOT NULL,
        CONSTRAINT ck_gestion_pos_accion CHECK (accion IN ('ENTRADA','SALIDA','RETIRO','REEMPLAZO','TRASLADO')),
        CONSTRAINT uq_gestion_pos UNIQUE (gestion_id, terminal_id, accion)
    );

    CREATE TABLE dbo.gestion_material (
        id BIGINT IDENTITY PRIMARY KEY,
        gestion_id BIGINT NOT NULL REFERENCES dbo.gestion(id),
        material_id BIGINT NOT NULL REFERENCES dbo.material(id),
        cantidad INT NOT NULL,
        CONSTRAINT ck_gestion_material_cantidad CHECK (cantidad > 0),
        CONSTRAINT uq_gestion_material UNIQUE (gestion_id, material_id)
    );

    CREATE TABLE dbo.movimiento_pos (
        id BIGINT IDENTITY PRIMARY KEY,
        gestion_id BIGINT NOT NULL REFERENCES dbo.gestion(id),
        terminal_id BIGINT NOT NULL REFERENCES dbo.terminal(id),
        usuario_id BIGINT NOT NULL REFERENCES dbo.usuario(id),
        tipo VARCHAR(30) NOT NULL,
        estado_anterior VARCHAR(30) NULL,
        estado_nuevo VARCHAR(30) NOT NULL,
        bodega_origen_id BIGINT NULL REFERENCES dbo.bodega(id),
        bodega_destino_id BIGINT NULL REFERENCES dbo.bodega(id),
        comercio_origen_id BIGINT NULL REFERENCES dbo.comercio(id),
        comercio_destino_id BIGINT NULL REFERENCES dbo.comercio(id),
        movimiento_revertido_id BIGINT NULL REFERENCES dbo.movimiento_pos(id),
        fecha DATETIME2(3) NOT NULL DEFAULT SYSUTCDATETIME(),
        detalle NVARCHAR(500) NULL,
        CONSTRAINT uq_movimiento_pos UNIQUE (gestion_id, terminal_id),
        CONSTRAINT ck_movimiento_estado CHECK (estado_nuevo IN
            ('DISPONIBLE','INSTALADO','EN_TRANSITO','PENDIENTE_REVISION','MANTENIMIENTO','INACTIVO','ENVIADO_BNCR'))
    );

    CREATE UNIQUE INDEX ux_reversion_pos ON dbo.movimiento_pos(movimiento_revertido_id)
        WHERE movimiento_revertido_id IS NOT NULL;

    CREATE TABLE dbo.movimiento_material (
        id BIGINT IDENTITY PRIMARY KEY,
        gestion_id BIGINT NOT NULL REFERENCES dbo.gestion(id),
        material_id BIGINT NOT NULL REFERENCES dbo.material(id),
        bodega_id BIGINT NOT NULL REFERENCES dbo.bodega(id),
        usuario_id BIGINT NOT NULL REFERENCES dbo.usuario(id),
        tipo VARCHAR(40) NOT NULL,
        cantidad INT NOT NULL,
        saldo_resultante INT NOT NULL,
        movimiento_revertido_id BIGINT NULL REFERENCES dbo.movimiento_material(id),
        fecha DATETIME2(3) NOT NULL DEFAULT SYSUTCDATETIME(),
        detalle NVARCHAR(500) NULL,
        CONSTRAINT ck_movimiento_cantidad CHECK (cantidad <> 0),
        CONSTRAINT ck_movimiento_saldo CHECK (saldo_resultante >= 0),
        CONSTRAINT uq_movimiento_material UNIQUE (gestion_id, material_id, bodega_id)
    );

    CREATE UNIQUE INDEX ux_reversion_material ON dbo.movimiento_material(movimiento_revertido_id)
        WHERE movimiento_revertido_id IS NOT NULL;

    CREATE TABLE dbo.anulacion_pos (
        gestion_id BIGINT NOT NULL REFERENCES dbo.gestion(id),
        movimiento_id BIGINT NOT NULL REFERENCES dbo.movimiento_pos(id),
        PRIMARY KEY (gestion_id, movimiento_id)
    );

    CREATE TABLE dbo.anulacion_material (
        gestion_id BIGINT NOT NULL REFERENCES dbo.gestion(id),
        movimiento_id BIGINT NOT NULL REFERENCES dbo.movimiento_material(id),
        PRIMARY KEY (gestion_id, movimiento_id)
    );

    CREATE INDEX ix_terminal_filtros ON dbo.terminal(estado, bodega_id, comercio_id);
    CREATE INDEX ix_gestion_pendientes ON dbo.gestion(estado, creada_en);
    CREATE INDEX ix_movimiento_pos_fecha ON dbo.movimiento_pos(fecha, terminal_id);
    CREATE INDEX ix_movimiento_material_fecha ON dbo.movimiento_material(fecha, bodega_id, material_id);

    INSERT INTO dbo.rol(id,nombre) VALUES (1,'ADMINISTRADOR'),(2,'SUPERVISOR'),(3,'SOPORTISTA');
    INSERT INTO dbo.bodega(nombre) VALUES (N'Bodega central');
    INSERT INTO dbo.material(nombre) VALUES
        (N'Tramo de cable'),(N'Conector RJ45'),(N'Conector DB9 hembra'),
        (N'Cobertor'),(N'Cable de datos'),(N'Fuente de poder'),(N'Base para datáfono'),(N'Cable Manhattan');
    INSERT INTO dbo.componente_material(material_id,componente_id,cantidad)
        SELECT padre.id,hijo.id,1 FROM dbo.material padre CROSS JOIN dbo.material hijo
        WHERE padre.nombre = N'Cable de datos'
        AND hijo.nombre IN (N'Tramo de cable',N'Conector RJ45',N'Conector DB9 hembra',N'Cobertor');

    COMMIT TRANSACTION;
    SELECT name AS tabla FROM sys.tables WHERE schema_id = SCHEMA_ID(N'dbo') ORDER BY name;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
