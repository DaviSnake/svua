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

# 🚀 Listo

Si seguiste todos los pasos:

👉 Tu aplicación debería estar corriendo en local sin problemas.

---

💡 Recomendación: luego puedes desplegar fácilmente usando Docker + Dokploy o CI/CD desde GitHub.
