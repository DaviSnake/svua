#!/bin/sh
set -eu

# =====================================================================
# Restaura un dump de Postgres en formato "custom" (-Fc, el que genera
# backup-postgres.sh, o un "pg_dump -Fc" manual) sobre una base de
# datos. Complementa a backup-postgres.sh -- un respaldo que nunca se
# probo restaurar no es una estrategia de continuidad confiable (ver
# docs/continuidad-negocio.md).
#
# Uso:
#   sh restore-postgres.sh <archivo.dump> [nombre_base_destino]
#
# Sin segundo argumento, restaura sobre POSTGRES_DB (la base REAL de
# este ambiente) -- es DESTRUCTIVO: --clean --if-exists borra los
# objetos existentes antes de recrearlos. Para probar sin riesgo, pasar
# un segundo argumento con el nombre de una base de prueba que ya
# exista (crearla antes a mano si hace falta -- este script no la crea,
# para no depender de que el rol de conexion tenga privilegio CREATEDB).
#
# ⚠️ Si vas a restaurar sobre la base real (sin segundo argumento):
#   - Detener el backend ANTES de restaurar (docker compose stop
#     backend) -- restaurar mientras la app sigue conectada y usando
#     esa misma base puede dejarla en un estado inconsistente.
#   - Volver a levantarlo despues (docker compose start backend).
#   - Este script pide una confirmacion explicita antes de tocar la
#     base real; no la pide si el destino es otra base (de prueba).
#
# Como correrlo (dentro del contenedor "postgres-backup", que ya tiene
# pg_restore -- ver docker-compose.yml):
#   docker compose exec postgres-backup sh /restore-postgres.sh /backups/svua_20260101_120000.dump
#   docker compose exec postgres-backup sh /restore-postgres.sh /backups/svua_20260101_120000.dump svua_restore_test
#
# IMPORTANTE: correrlo SIN "-T" (a diferencia del backup) -- este
# script pide confirmacion por teclado cuando el destino es la base
# real, y "-T" desactiva la terminal interactiva.
# =====================================================================

ARCHIVO="${1:?Uso: restore-postgres.sh <archivo.dump> [nombre_base_destino]}"
BASE_DESTINO="${2:-${POSTGRES_DB}}"

if [ ! -f "${ARCHIVO}" ]; then
    echo "Error: no existe el archivo '${ARCHIVO}'" >&2
    exit 1
fi

EXISTE=$(PGPASSWORD="${POSTGRES_PASSWORD}" psql -h "${POSTGRES_HOST}" -p "${POSTGRES_PORT}" -U "${POSTGRES_USER}" -d postgres -tAc \
    "SELECT 1 FROM pg_database WHERE datname = '${BASE_DESTINO}'")

if [ "${EXISTE}" != "1" ]; then
    echo "Error: la base '${BASE_DESTINO}' no existe. Crearla antes a mano, por ejemplo:" >&2
    echo "  docker compose exec postgres-backup psql -h \"\${POSTGRES_HOST}\" -U \"\${POSTGRES_USER}\" -d postgres -c \"CREATE DATABASE ${BASE_DESTINO}\"" >&2
    exit 1
fi

if [ "${BASE_DESTINO}" = "${POSTGRES_DB}" ]; then
    echo "⚠️  ADVERTENCIA: vas a restaurar sobre la base REAL de este ambiente ('${POSTGRES_DB}')."
    echo "    Esto reemplaza los datos actuales. Si el backend sigue corriendo contra"
    echo "    esta base, detenlo primero: docker compose stop backend"
    printf "¿Confirmas? Escribe exactamente 'si' para continuar: "
    read -r CONFIRMACION
    if [ "${CONFIRMACION}" != "si" ]; then
        echo "Cancelado. No se toco nada."
        exit 1
    fi
fi

echo "[$(date -Iseconds)] Restaurando ${ARCHIVO} -> base '${BASE_DESTINO}'..."

PGPASSWORD="${POSTGRES_PASSWORD}" pg_restore \
    -h "${POSTGRES_HOST}" \
    -p "${POSTGRES_PORT}" \
    -U "${POSTGRES_USER}" \
    -d "${BASE_DESTINO}" \
    --clean \
    --if-exists \
    --no-owner \
    --no-privileges \
    "${ARCHIVO}"

echo "[$(date -Iseconds)] Restauracion completa. Verificacion rapida (conteo de filas):"

PGPASSWORD="${POSTGRES_PASSWORD}" psql -h "${POSTGRES_HOST}" -p "${POSTGRES_PORT}" -U "${POSTGRES_USER}" -d "${BASE_DESTINO}" -c \
    "SELECT 'empresa' AS tabla, count(*) FROM empresa
     UNION ALL SELECT 'usuario', count(*) FROM usuario
     UNION ALL SELECT 'activo', count(*) FROM activo
     ORDER BY 1;"

if [ "${BASE_DESTINO}" = "${POSTGRES_DB}" ]; then
    echo "[$(date -Iseconds)] Si detuviste el backend antes de restaurar, ahora puedes levantarlo:"
    echo "  docker compose start backend"
fi
