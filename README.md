# ControlPOS

Aplicación local de inventario de datáfonos y materiales. Java 21, Spring Boot,
Thymeleaf, Spring Security, Spring Data JPA y Microsoft SQL Server.

## Base de datos

Ejecutar ControlPOS.sql en SQL Server Management Studio sobre una base nueva.
El script crea ControlPOS_Proyecto, sus 15 tablas, tres roles y el catálogo inicial
de materiales. El administrador se crea al arrancar si no hay usuarios.

La configuración local está en src/main/resources/application-sqlserver.properties.
Ajustar servidor y puerto según la instancia. La autenticación integrada utiliza
la cuenta de Windows y la biblioteca mssql-jdbc_auth correspondiente al driver.

## Ejecución con SQL Server

En PowerShell, desde esta carpeta, ejecutar cada línea por separado.

    $env:PATH = "$env:USERPROFILE\.m2\repository\com\microsoft\sqlserver\mssql-jdbc_auth\13.4.0.x64;$env:PATH"
    $env:CONTROLPOS_ADMIN_PASSWORD = Read-Host "Contraseña inicial de administrador (mínimo 12 caracteres)"
    .\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=sqlserver --server.port=18127"

Abrir http://127.0.0.1:18127/login. La cuenta inicial es admin.
La variable configura exclusivamente la contraseña de creación inicial.
Los arranques posteriores conservan el hash almacenado en la base.
No guardar contraseñas en el repositorio.

## Organización

- model contiene las entidades y los datos de las vistas.
- repository contiene interfaces Spring Data JPA.
- service contiene validaciones y transacciones.
- controller recibe formularios y prepara las vistas.
- resources/templates contiene las pantallas Thymeleaf.
- resources/static contiene CSS y JavaScript.

La lectura y la escritura persistentes utilizan JPA. Las consultas nativas
de filtro y composición de materiales se ejecutan desde repositorios JPA.
El perfil demo conserva una demostración independiente en memoria.

## Verificación y pendientes

Se comprobó la compilación y el inicio de los repositorios JPA con metadatos
de SQL Server sin acceder a la instancia. La ejecución integral contra
SQL Server y la persistencia después de reiniciar requieren verificación
en la sesión de Windows con acceso a la base.

Traslados, anulaciones, edición y desactivación completas de comercios y POS,
administración de materiales y exportación Excel siguen pendientes en SQL Server.
