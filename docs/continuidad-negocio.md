# Continuidad del negocio (NCh-ISO/IEC 27001 A.17)

Estrategia de respaldo y recuperacion de datos. Estado real al
2026-08-26 -- que esta cubierto, que NO, y como restaurar en un
incidente.

---

## Que se respalda automaticamente

**Base de datos Postgres** (todo lo transaccional: empresas, usuarios,
activos, ordenes de mantenimiento, repuestos, stock, y el historial de
auditoria de V29/V30): `pg_dump` diario, formato `-Fc` (comprimido,
restaurable con `pg_restore`), via el servicio `postgres-backup` de
`docker-compose.yml` (ver `scripts/backup-postgres.sh`).

- **Donde quedan:** `./backups/postgres/` en el disco de la VPS
  (bind mount, NO es un volumen interno de Docker -- se puede copiar
  con `scp`/`rsync` directo).
- **Frecuencia:** cada 24 horas (corre tambien una vez al levantar el
  stack, ej. despues de un redeploy).
- **Retencion:** `BACKUP_RETENTION_DIAS` (`.env`, default 14 dias) --
  dumps mas viejos se borran solos.
- **RPO (Recovery Point Objective) real:** hasta 24 horas de datos
  perdidos en el peor caso (justo antes del proximo backup
  programado). Si esto es demasiado para el negocio, hay que subir la
  frecuencia (ej. cada 6h) editando el `sleep 24h` del servicio
  `postgres-backup` en `docker-compose.yml`.

## Que NO esta cubierto todavia

Esto es lo que hace que A.17 siga sin estar 100% implementado incluso
despues de agregar el backup automatico:

1. **Sin copia offsite.** Los backups viven en el mismo disco que la
   base de datos real. Si la VPS completa se pierde (falla de disco,
   error humano, incidente del proveedor de hosting), el backup se
   pierde CON el dato original -- de nada sirve. Falta:
   - Elegir un destino externo (bucket S3-compatible, otro servidor,
     etc.).
   - Agregar `rclone` o `restic` al final de
     `scripts/backup-postgres.sh` para subir cada dump ahi.
   - Esto requiere credenciales de ese destino externo, que no existen
     en este repo -- es una decision + configuracion que le
     corresponde a David, no algo que se pueda adivinar desde el
     codigo.

2. **`./uploads` y `./log` sin respaldar.** Son bind mounts del
   contenedor `backend` (adjuntos de ordenes de mantenimiento, logs de
   carga masiva). No son datos transaccionales de la BD, pero tambien
   viven solo en el disco de la VPS y se pierden si ese disco falla.
   Se pueden sumar al mismo `rclone`/`restic` del punto 1, o respaldar
   aparte con un simple `tar` + copia.

3. **Nunca se probo restaurar.** Un backup que nunca se restauro no es
   una garantia real -- puede estar corrupto, incompleto, o el
   procedimiento de restauracion puede tener un paso que nadie
   documento. Recomendacion: una vez al trimestre, restaurar el dump
   mas reciente en un Postgres de prueba (no en produccion) y verificar
   que la app levanta con esos datos. Ver el procedimiento abajo.

4. **Sin RTO (Recovery Time Objective) definido.** No hay un numero
   acordado de "en cuanto tiempo tiene que estar todo de vuelta
   funcionando" despues de un incidente. Es una decision de negocio,
   no tecnica -- define cuanto esfuerzo/costo vale la pena invertir en
   automatizar la recuperacion.

## Como restaurar un backup (procedimiento manual)

En caso de incidente real, o para la prueba trimestral del punto 3:

```bash
# 1. Copiar el dump mas reciente fuera del contenedor si hace falta
#    (si ya esta en ./backups/postgres/ en el host, este paso sobra).

# 2. Restaurar contra un Postgres NUEVO (nunca directo sobre produccion
#    sin respaldar antes lo que hay, aunque este roto -- podria servir
#    para diagnosticar despues).
docker exec -i <container_postgres> pg_restore \
    -U svua \
    -d svua \
    --clean --if-exists \
    < ./backups/postgres/svua_20260826_030000.dump

# 3. Verificar: contar filas de una tabla conocida, o levantar el
#    backend apuntando a esta BD restaurada y probar login.
```

`--clean --if-exists` hace que `pg_restore` borre los objetos
existentes antes de recrearlos -- pensado para restaurar sobre una BD
vacia o de prueba, no para "fusionar" con datos ya presentes.

## Responsable y proxima revision

**Responsable:** David Medina.
**Proxima revision sugerida:** cuando se resuelva el punto 1 (copia
offsite), y trimestralmente para la prueba de restauracion del punto 3.
