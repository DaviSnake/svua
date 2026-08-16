// Proxy de desarrollo para "npm run start:remoto": reenvia las llamadas
// que hace el frontend local hacia la API real de produccion, evitando
// el bloqueo de CORS (el navegador solo ve http://localhost:4200; quien
// habla con produccion es este proceso de Node, no el navegador).
//
// IMPORTANTE: Angular 18 con el builder "application" usa el dev-server
// de Vite, que corre sobre "http-proxy" (node-http-proxy) directo, NO
// sobre "http-proxy-middleware". Por eso los hooks se enganchan con la
// opcion "configure" (API de Vite: https://vite.dev/config/server-options.html#server-proxy),
// no con "onProxyReq" (v2) ni con "on: { proxyReq }" (v3 de
// http-proxy-middleware) — ninguna de esas dos la lee Vite.
//
// El "changeOrigin" de por si SOLO reescribe el header Host, no el
// Origin. El navegador manda "Origin: http://localhost:4200" en
// requests POST/PUT/DELETE aunque sean same-origin (asi es el spec de
// fetch), y ese Origin se reenvia tal cual a produccion. Como
// CORS_ALLOWED_ORIGINS en la VPS NO incluye localhost:4200, el filtro
// de CORS de Spring Security rechaza la request con 403 Forbidden
// ANTES de llegar al controlador (incluso en endpoints permitAll(),
// porque el chequeo de CORS corre antes que la logica de
// autorizacion). Por eso se reescribe el Origin al del dominio real
// permitido antes de reenviar la request.
const ORIGIN_PERMITIDO = "https://www.svua.cl";

function configurarProxy(proxy, options) {
  proxy.on("proxyReq", function (proxyReq, req) {
    proxyReq.setHeader("origin", ORIGIN_PERMITIDO);
  });

  proxy.on("error", function (err, req) {
    console.error("[proxy] error reenviando " + req.url + ":", err.message);
  });

  proxy.on("proxyRes", function (proxyRes, req) {
    console.log("[proxy] " + req.method + " " + req.url + " -> " + proxyRes.statusCode);
  });
}

const PROXY_CONFIG = {
  "/api/v1/svua": {
    target: "https://api.svua.cl",
    secure: true,
    changeOrigin: true,
    configure: configurarProxy
  },
  "/ws": {
    target: "https://api.svua.cl",
    secure: true,
    changeOrigin: true,
    ws: true,
    configure: configurarProxy
  }
};

module.exports = PROXY_CONFIG;
