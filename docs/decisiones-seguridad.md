# Registro de decisiones de seguridad

Registro de decisiones tecnicas de seguridad tomadas conscientemente
(no por omision), con su justificacion y la fecha de evaluacion. Sirve
como evidencia para auditorias (ej. NCh-ISO/IEC 27001) de que estas
decisiones fueron analizadas, no descuidos.

---

## 2026-08-26 — CSRF deshabilitado en la API

**Decision:** `SecurityConfig.filterChain()` deshabilita la proteccion
CSRF de Spring Security (`.csrf(csrf -> csrf.disable())`).

**Contexto:** la API es completamente stateless
(`SessionCreationPolicy.STATELESS`) y usa JWT para autenticacion.

**Justificacion:** el ataque CSRF depende de que el navegador adjunte
automaticamente una credencial "ambiente" (tipicamente una cookie de
sesion) a un request cross-site forjado por un sitio malicioso, sin que
el usuario se de cuenta. Esta aplicacion no usa cookies para
autenticacion: el JWT viaja unicamente en el header `Authorization`,
que el navegador NUNCA adjunta automaticamente a un request generado
por un sitio de terceros (`JwtAuthenticationFilter` solo lee
`request.getHeader("Authorization")`, confirmado en el codigo). Por lo
tanto no existe una credencial ambiente que un ataque CSRF pueda
explotar, y la proteccion CSRF de Spring Security (pensada para el
modelo de cookies de sesion) no aporta nada aqui — deshabilitarla es
una simplificacion valida, no un riesgo aceptado a la ligera.

**Mitigacion real del riesgo equivalente (XSS -> robo de token):** como
el JWT no vive en una cookie, el riesgo relevante para esta arquitectura
no es CSRF sino XSS (si un script malicioso corriera en el frontend,
podria leer el token de donde este almacenado y enviarlo el mismo). Esa
es la superficie a vigilar, no CSRF.

**Condicion de revision:** si en el futuro el JWT o un refresh token
pasan a transportarse via cookie (por ejemplo, para permitir refresh
silencioso), esta decision debe revisarse de inmediato y CSRF debe
volver a habilitarse (con proteccion `SameSite`/token CSRF segun
corresponda).

**Revisado por:** David Medina.
**Proxima revision sugerida:** si cambia el mecanismo de transporte del
JWT, o en la proxima auditoria de seguridad formal del proyecto.
