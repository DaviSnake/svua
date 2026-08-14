// Genera dist/frontend-svua/browser/assets/version.json despues del build
// de produccion (ver "postbuild" en package.json). El frontend lo consulta
// al iniciar sesion (ver AuthService.verificarVersionYRecargarSiCorresponde)
// para detectar si hay una version mas nueva desplegada en el servidor y,
// si corresponde, forzar una recarga real de la pagina en vez de dejar que
// el navegador siga usando el bundle viejo que ya tenia cargado en memoria.
//
// No se escribe en src/assets/ a proposito: dist/ ya esta en .gitignore,
// asi que esto no ensucia el repo con un archivo que cambia en cada build.

const fs = require('fs');
const path = require('path');

const destino = path.join(
  __dirname,
  '..',
  'dist',
  'frontend-svua',
  'browser',
  'assets',
  'version.json'
);

const contenido = JSON.stringify({
  version: new Date().toISOString()
});

fs.mkdirSync(path.dirname(destino), { recursive: true });
fs.writeFileSync(destino, contenido, 'utf-8');

console.log('version.json generado:', contenido);
