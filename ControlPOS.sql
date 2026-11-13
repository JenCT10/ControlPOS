-- ControlPOS para Microsoft SQL Server. Ejecutar en una instancia personal.
USE master;
GO
IF DB_ID(N'controlpos') IS NULL
    EXEC(N'CREATE DATABASE [controlpos]');
GO
USE [controlpos];
GO
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;

BEGIN TRY
    IF DB_NAME() <> N'controlpos'
        THROW 50002, N'Seleccione la base controlpos antes de continuar.', 1;
    BEGIN TRANSACTION;
    IF EXISTS (
        SELECT 1 FROM sys.tables
        WHERE schema_id = SCHEMA_ID(N'dbo')
          AND name IN (N'rol', N'usuario', N'comercio', N'terminal', N'asignacion', N'cambio_estado')
    )
        THROW 50001, N'Ya existen tablas de ControlPOS. El script no modifica instalaciones existentes.', 1;

CREATE TABLE dbo.rol (
  id SMALLINT PRIMARY KEY,
  nombre NVARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE dbo.usuario (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  rol_id SMALLINT NOT NULL,
  nombre NVARCHAR(100) NOT NULL,
  login NVARCHAR(60) NOT NULL UNIQUE,
  clave_hash NVARCHAR(255) NOT NULL,
  activo BIT NOT NULL DEFAULT 1,
  FOREIGN KEY (rol_id) REFERENCES dbo.rol(id)
);

CREATE TABLE dbo.comercio (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  codigo NVARCHAR(30) NOT NULL UNIQUE,
  nombre NVARCHAR(100) NOT NULL,
  ubicacion NVARCHAR(180) NOT NULL,
  activo BIT NOT NULL DEFAULT 1
);

CREATE TABLE dbo.terminal (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  serie NVARCHAR(60) NOT NULL UNIQUE,
  marca NVARCHAR(60) NOT NULL,
  modelo NVARCHAR(60) NOT NULL,
  estado NVARCHAR(13) NOT NULL CHECK (estado IN ('DISPONIBLE','ASIGNADO','MANTENIMIENTO','INACTIVO')) DEFAULT N'DISPONIBLE'
);

CREATE TABLE dbo.asignacion (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  terminal_id BIGINT NOT NULL,
  comercio_id BIGINT NOT NULL,
  entrega_por BIGINT NOT NULL,
  entrega DATETIME2(0) NOT NULL,
  devolucion_por BIGINT,
  devolucion DATETIME2(0),
  FOREIGN KEY (terminal_id) REFERENCES dbo.terminal(id),
  FOREIGN KEY (comercio_id) REFERENCES dbo.comercio(id),
  FOREIGN KEY (entrega_por) REFERENCES dbo.usuario(id),
  FOREIGN KEY (devolucion_por) REFERENCES dbo.usuario(id),
  CHECK ((devolucion IS NULL AND devolucion_por IS NULL)
      OR (devolucion IS NOT NULL AND devolucion_por IS NOT NULL AND devolucion >= entrega))
);

CREATE TABLE dbo.cambio_estado (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  terminal_id BIGINT NOT NULL,
  usuario_id BIGINT NOT NULL,
  fecha DATETIME2(0) NOT NULL,
  anterior NVARCHAR(13) NOT NULL CHECK (anterior IN ('DISPONIBLE','ASIGNADO','MANTENIMIENTO','INACTIVO')),
  nuevo NVARCHAR(13) NOT NULL CHECK (nuevo IN ('DISPONIBLE','ASIGNADO','MANTENIMIENTO','INACTIVO')),
  FOREIGN KEY (terminal_id) REFERENCES dbo.terminal(id),
  FOREIGN KEY (usuario_id) REFERENCES dbo.usuario(id),
  CHECK (anterior <> nuevo)
);

CREATE INDEX idx_terminal_estado ON dbo.terminal(estado);
CREATE INDEX idx_asignacion_comercio ON dbo.asignacion(comercio_id, devolucion);
CREATE UNIQUE INDEX ux_asignacion_activa
  ON dbo.asignacion(terminal_id) WHERE devolucion IS NULL;
INSERT INTO dbo.rol VALUES (1,'ADMINISTRADOR'),(2,'INVENTARIO');
-- Cuentas de evidencia desactivadas. El marcador no es una contraseña ni un hash utilizable.
SET IDENTITY_INSERT dbo.usuario ON;
INSERT INTO dbo.usuario VALUES (1,1,'Administracion demo','admin.demo','CUENTA_INACTIVA',0),
(2,2,'Inventario demo','inventario.demo','CUENTA_INACTIVA',0);
SET IDENTITY_INSERT dbo.usuario OFF;
SET IDENTITY_INSERT dbo.comercio ON;
INSERT INTO dbo.comercio VALUES (1,'COM-001','Comercio Central','Perez Zeledon',1),
(2,'COM-002','Comercio Sur','Buenos Aires',1);
SET IDENTITY_INSERT dbo.comercio OFF;
SET IDENTITY_INSERT dbo.terminal ON;
INSERT INTO dbo.terminal VALUES (1,'POS-001','PAX','A920','DISPONIBLE'),
(2,'POS-002','Ingenico','DX8000','ASIGNADO'),
(3,'POS-003','PAX','A920','MANTENIMIENTO'),
(4,'POS-004','Ingenico','DX8000','INACTIVO');
SET IDENTITY_INSERT dbo.terminal OFF;
INSERT INTO dbo.asignacion (terminal_id,comercio_id,entrega_por,entrega,devolucion_por,devolucion)
VALUES (1,2,2,'2026-10-01T09:00:00',2,'2026-10-03T10:00:00'),(2,1,2,'2026-10-04T08:00:00',NULL,NULL);
INSERT INTO dbo.cambio_estado (terminal_id,usuario_id,fecha,anterior,nuevo)
VALUES (3,2,'2026-10-05T09:00:00','DISPONIBLE','MANTENIMIENTO');
COMMIT TRANSACTION;
SELECT serie, estado FROM dbo.terminal ORDER BY serie;

END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;

