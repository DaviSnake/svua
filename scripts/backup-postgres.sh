#!/bin/sh
set -eu

# =====================================================================
# Backup automatico de Postgres (NCh-ISO/IEC 27001 A.17 - Continuidad
# del negocio). Lo ejecuta el servicio "postgres-backup" de
# docker-compose.yml, una vez al levantar y despues cada 24h.
#
# Que hace: pg_dump en formato "custom" (-Fc, comprimido, restaurable
# con pg_restore y permite restaurar tablas sueltas si hace falta) del
# POSTGRES_DB configurado, a /backups (bind mount -> ./backups/postgres
# en el host, ver docker-compose.yml). Al final borra los dumps mas
# viejos que BACKUP_RETENTION_DIAS (default 14).
#
# Lo que este script NO hace (ver docs/continuidad-negocio.md para el
# detalle y los pasos manuales pendientes):
#   - Copiar los backups FUERA de la VPS (si el disco de la VPS se
#     pierde entero, este backup se pierde con el. Falta configurar
#     una copia offsite -- ej. rclone/restic a un bucket S3-compatible.)
#   - Backup de ./uploads ni ./log (archivos adjuntos y logs de carga
#     masiva: no son datos transaccionales de la BD, pero tambien se
#     pierden si el disco de la VPS falla).
#   - Probar la restauracion automaticamente (una copia de seguridad
#     que nunca se probo restaurar no es una estrategia de continuidad
#     confiable -- ver docs/continuidad-negocio.md, seccion "prueba de
#     restauracion").
# =====================================================================

FECHA=$(date +%Y%m%d_%H%M%S)
ARCHIVO="/backups/${POSTGRES_DB}_${FECHA}.dump"
RETENCION_DIAS="${BACKUP_RETENTION_DIAS:-14}"

echo "[$(date -Iseconds)] Iniciando backup de '${POSTGRES_DB}' -> ${ARCHIVO}"

PGPASSWORD="${POSTGRES_PASSWORD}" pg_dump \
    -h "${POSTGRES_HOST}" \
    -p "${POSTGRES_PORT}" \
    -U "${POSTGRES_USER}" \
    -d "${POSTGRES_DB}" \
    -Fc \
    -f "${ARCHIVO}"

echo "[$(date -Iseconds)] Backup completo: $(du -h "${ARCHIVO}" | cut -f1)"

# Retencion: borra dumps de ESTA base (POSTGRES_DB) mas viejos que
# RETENCION_DIAS. No toca dumps de otra base si el directorio se
# comparte entre ambientes (no deberia pasar, pero por si acaso el
# nombre de archivo incluye POSTGRES_DB).
find /backups -name "${POSTGRES_DB}_*.dump" -mtime "+${RETENCION_DIAS}" -print -delete

echo "[$(date -Iseconds)] Retencion aplicada (${RETENCION_DIAS} dias). Backups actuales:"
ls -lh /backups | grep "^-" || echo "  (ninguno todavia)"
