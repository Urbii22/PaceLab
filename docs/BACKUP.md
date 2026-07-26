# Backup local

El MVP conserva los datos en el almacenamiento privado de Android. No se incluyen bases de datos reales ni exports en el repositorio.

El formato versionado `.pacelab` se implementará antes de depender de la base local para uso diario. La restauración deberá validar versión, integridad, duplicados y aplicar la importación de forma transaccional sin sobrescribir notas más recientes.
