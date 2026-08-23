# 🚀 Proyecto Full Stack (Angular + Spring Boot + PostgreSQL)

Este repositorio contiene una aplicación full stack compuesta por:

* **Frontend:** Angular
* **Backend:** Spring Boot
* **Base de datos:** PostgreSQL

Este README explica cómo **levantar el proyecto en local paso a paso** después de clonarlo desde GitHub.

---

# 📦 Requisitos previos

Antes de comenzar, instala:

* Node.js (v18 o superior)
* Angular CLI

  ```bash
  npm install -g @angular/cli
  ```
* Java 17+
* Maven
* Docker (recomendado para la base de datos)

---

# 📥 Clonar el repositorio

```bash
git clone <URL_DEL_REPOSITORIO>
cd <NOMBRE_DEL_PROYECTO>
```

---

# 📁 Estructura del proyecto

```
/
 ├── backend/    → API en Spring Boot
 ├── frontend/   → Aplicación Angular
 └── README.md
```

---

# 🐘 1. Levantar PostgreSQL (OBLIGATORIO)

La base de datos debe estar corriendo antes del backend.

## Opción recomendada: Docker

```bash
docker run -d \
  --name postgres_db \
  -e POSTGRES_DB=mi_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:15
```

---

# ⚙️ 2. Levantar Backend (Spring Boot)

## 1. Ir al backend

```bash
cd backend
```

## 2. Configurar variables de entorno

### Linux / Mac

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/mi_db
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
```

### Windows (PowerShell)

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/mi_db"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="postgres"
```

## 3. Ejecutar backend

```bash
mvn spring-boot:run
```

📍 Backend disponible en:
http://localhost:8080

---

# 🌐 3. Levantar Frontend (Angular)

## 1. Ir al frontend

```bash
cd frontend
```

## 2. Instalar dependencias

```bash
npm install
ng add @angular/material
```

## 3. Configurar URL del backend

Editar archivo:

```
src/environments/environment.ts
```

Configurar:

```ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080'
};
```

## 4. Ejecutar aplicación

```bash
ng serve
```

📍 Frontend disponible en:
http://localhost:4200

---

# 🔁 Flujo correcto de ejecución

1. Levantar PostgreSQL
2. Levantar Backend
3. Levantar Frontend

---

# 🧪 Verificación rápida

* ✔️ Base de datos corriendo en puerto 5432
* ✔️ Backend respondiendo en http://localhost:8080
* ✔️ Frontend en http://localhost:4200

---

# ⚠️ Problemas comunes

### ❌ Error de conexión a la base de datos

* Verifica que PostgreSQL esté corriendo
* Revisa usuario, password y nombre de la DB

### ❌ Puerto ocupado

* Cambia puertos si ya están en uso (8080, 4200, 5432)

### ❌ Angular no conecta al backend

* Revisa `environment.ts`
* Asegúrate de que el backend esté corriendo

---

# 🐳 (Opcional) Ejecutar con Docker

## Backend

```bash
docker build -t backend ./backend
docker run -p 8080:8080 backend
```

## Frontend

```bash
docker build -t frontend ./frontend
docker run -p 80:80 frontend
```

---

# 🛡️ Buenas prácticas

* No subir archivos `.env` al repositorio
* Usar variables de entorno
* No exponer PostgreSQL innecesariamente
* Mantener versiones de Node y Java compatibles


---

# 🚀 Despliegue a Dev y Producción

El despliegue NO es manual: se hace con GitHub Actions (`.github/workflows/deploy-dev.yml` y `deploy-prod.yml`), que se conectan por SSH a la VPS, actualizan el código y levantan los contenedores con `docker compose`. Como developer, tu trabajo se reduce a hacer push/tag en el lugar correcto — el resto es automático.

## Dev

1. Trabaja normalmente sobre la rama `desarrollo` (o mergea tu feature branch ahí).
2. Al hacer push a `desarrollo`:

   ```bash
   git checkout desarrollo
   git push origin desarrollo
   ```

   se dispara automáticamente el workflow **Deploy Dev**, que en la VPS:
   * clona o actualiza `/home/davisnake/svua-dev` (`git reset --hard origin/desarrollo`),
   * genera el `.env` de dev la primera vez (con los secrets `DEV_*` configurados en GitHub),
   * corre `docker compose down` + `docker compose up -d --build`.
3. Revisa el resultado en el ambiente de dev (dev.svua.cl / api-dev.svua.cl) y en la pestaña *Actions* de GitHub si algo falla.

No hace falta correr migraciones a mano: Flyway las aplica solo al levantar el backend.

## Producción

Producción **no se despliega en cada push a master** — un merge a `master` no tumba el sitio. El deploy real ocurre solo al pushear un tag `vX.Y.Z`, o disparando el workflow manualmente eligiendo un tag existente.

1. Una vez probado en dev, mergea `desarrollo` a `master`:

   ```bash
   git checkout master
   git pull origin master
   git merge desarrollo
   git push origin master
   ```

2. Crea y sube el tag de la nueva versión (siguiendo semver: `vMAJOR.MINOR.PATCH`):

   ```bash
   git tag v1.0.2
   git push origin v1.0.2
   ```

   Esto dispara **Deploy Prod**, que en la VPS:
   * clona o actualiza `/home/davisnake/svua-prod`,
   * hace `git checkout "$TAG_NAME"` (detached HEAD, exactamente ese tag),
   * genera el `.env` de prod la primera vez (secrets sin prefijo `DEV_`),
   * corre `docker compose down` + `docker compose --profile prod up -d --build`,
   * valida/genera el certificado SSL con certbot si todavía no existe.

3. **Rollback o reintento sin crear un tag nuevo**: en GitHub → *Actions* → *Deploy Prod* → *Run workflow*, indicando el tag a desplegar (puede ser uno anterior, ej. `v1.0.1`, para volver atrás).

### ⚠️ Antes de taguear una versión con cambios de base de datos

* Toda migración nueva en `svuaback/src/main/resources/db/migration` se aplica sola al arrancar el backend (Flyway) — pruébala primero en dev.
* Si la migración es sensible (ej. la de Row Level Security, `V27`), confirma que el `POSTGRES_USER` de producción **no sea superuser** (`rolsuper=false`, `rolbypassrls=false` en `pg_roles`) — con un superuser, RLS queda sin efecto en silencio.
* Las carpetas `uploads/` y `log/` ya están montadas como volumen en `docker-compose.yml`, así que sobreviven a `docker compose down/up` — no se pierden con cada deploy.

---

# 🚀 Listo

Si seguiste todos los pasos:

👉 Tu aplicación debería estar corriendo en local sin problemas.
