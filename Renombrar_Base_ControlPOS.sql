-- Ejecutar en SSMS desde master después de detener la aplicación.
USE master;
GO
IF DB_ID(N'ControlPOS_Proyecto') IS NOT NULL
    THROW 50001, N'ControlPOS_Proyecto ya existe. Verifique la base antes de continuar.', 1;
IF DB_ID(N'LAROTracker') IS NULL
    THROW 50002, N'No se encontró la base original. Ejecute ControlPOS.sql si necesita crear una base nueva.', 1;
ALTER DATABASE [LAROTracker] MODIFY NAME = [ControlPOS_Proyecto];
GO
