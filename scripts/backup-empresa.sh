#!/bin/sh
set -eu

# =====================================================================
# Backup de UNA sola empresa (tenant), no de toda la base.
#
# Que hace: usa la misma Row Level Security que ya protege la app en
# tiempo real (ver V27__enable_row_level_security_por_empresa.sql) para
# que pg_dump filtre automaticamente, tabla por tabla, solo las filas de
# la empresa pedida -- sin mantener a mano una lista de que tablas
# tienen empresa_id (son casi todas, via BaseEntity).
#
# Como: PGOPTIONS le pasa a pg_dump las mismas dos variables de sesion
# que RlsContextService setea en cada request real
# (app.current_empresa_id / app.bypass_rls). Con --enable-row-security,
# pg_dump respeta esas policies igual que una consulta comun.
#
# ADVERTENCIA CRITICA: esto SOLO filtra si POSTGRES_USER es un rol
# normal (sin SUPERUSER ni BYPASSRLS -- "svua", no "postgres"). Un
# superuser ignora RLS incondicionalmente en Postgres, con o sin
# --enable-row-security: el dump saldria con TODAS las empresas sin
# ningun aviso de error. Por eso este script verifica el rol antes de
# dumpear (mismo caveat que ya documenta V27).
#
# La propia tabla "empresa" tambien queda cubierta: su PK esta mapeada
# a la columna "empresa_id" (ver Empresa.java), asi que V27 le creo su
# propia policy y el mismo pg_dump trae su fila puntual (nombre, rut,
# flags, logo, color) sin necesidad de un paso aparte.
#
# Esto es un backup de archivo/auditoria puntual (por ejemplo antes de
# desactivar o eliminar una empresa), NO un mecanismo de restauracion en
# caliente: restaurar este .sql sobre una base que ya tiene otras
# empresas puede chocar con IDs/secuencias existentes. Pensado para
# restaurar sobre una base vacia (mismo schema, sin datos) o para
# revisar el contenido a mano.
# =====================================================================

EMPRESA_ID="${1:?Uso: backup-empresa.sh <empresa_id>}"

case "${EMPRESA_ID}" in
    ''|*[!0-9]*)
        echo "Error: <empresa_id> debe ser un numero (recibido: '${EMPRESA_ID}')" >&2
        exit 1
        ;;
esac

FECHA=$(date +%Y%m%d_%H%M%S)
DIR="/backups/empresa_${EMPRESA_ID}"
ARCHIVO_DATOS="${DIR}/${POSTGRES_DB}_empresa${EMPRESA_ID}_${FECHA}.sql"

mkdir -p "${DIR}"

echo "[$(date -Iseconds)] Verificando que POSTGRES_USER='${POSTGRES_USER}' no sea superuser/bypassrls..."
BYPASS_CHECK=$(PGPASSWORD="${POSTGRES_PASSWORD}" psql -h "${POSTGRES_HOST}" -p "${POSTGRES_PORT}" -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -tAc \
    "SELECT rolsuper OR rolbypassrls FROM pg_roles WHERE rolname = '${POSTGRES_USER}'")

if [ "${BYPASS_CHECK}" = "t" ]; then
    echo "Error: el rol '${POSTGRES_USER}' es SUPERUSER o tiene BYPASSRLS." >&2
    echo "Este backup filtrado por empresa NO puede funcionar con ese rol (dumpearia TODAS las empresas)." >&2
    exit 1
fi

echo "[$(date -Iseconds)] Verificando que la empresa ${EMPRESA_ID} exista..."
EXISTE=$(PGPASSWORD="${POSTGRES_PASSWORD}" psql -h "${POSTGRES_HOST}" -p "${POSTGRES_PORT}" -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -tAc \
    "SELECT 1 FROM empresa WHERE empresa_id = ${EMPRESA_ID}")

if [ "${EXISTE}" != "1" ]; then
    echo "Error: no existe una empresa con id ${EMPRESA_ID}." >&2
    exit 1
fi

echo "[$(date -Iseconds)] Backup de datos de la empresa ${EMPRESA_ID} -> ${ARCHIVO_DATOS}"

PGOPTIONS="-c app.current_empresa_id=${EMPRESA_ID} -c app.bypass_rls=off" \
PGPASSWORD="${POSTGRES_PASSWORD}" pg_dump \
    -h "${POSTGRES_HOST}" \
    -p "${POSTGRES_PORT}" \
    -U "${POSTGRES_USER}" \
    -d "${POSTGRES_DB}" \
    --data-only \
    --enable-row-security \
    --column-inserts \
    -f "${ARCHIVO_DATOS}"

echo "[$(date -Iseconds)] Backup completo:"
ls -lh "${DIR}"
