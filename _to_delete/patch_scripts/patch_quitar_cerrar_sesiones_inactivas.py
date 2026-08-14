#!/usr/bin/env python3
import sys

def apply_edit(path, old, new, expected_count=1):
    with open(path, 'rb') as f:
        content = f.read().decode('utf-8')

    count = content.count(old)
    if count != expected_count:
        print(f"FAIL [{path}]: expected {expected_count} occurrence(s) of anchor, found {count}")
        print("----- ANCHOR -----")
        print(old)
        print("------------------")
        sys.exit(1)

    content = content.replace(old, new, expected_count)

    with open(path, 'wb') as f:
        f.write(content.encode('utf-8'))

    print(f"OK [{path}]: replaced {count} occurrence(s)")


PATH = "/sessions/rcw-01u2gwnbk5zzyctqiimc1ak9/mnt/svua/svuaback/src/main/java/cl/aracridav/svua/scheduler/MantencionScheduler.java"

# 1) Imports que quedan sin uso una vez removido el método
apply_edit(
    PATH,
    "import cl.aracridav.svua.usuario.entity.SesionUsuario;\r\n"
    "import cl.aracridav.svua.usuario.repository.SesionUsuarioRepository;\r\n",
    ""
)

# 2) Constante usada solo por el método a eliminar
apply_edit(
    PATH,
    "    private static final int MINUTOS_INACTIVIDAD_SESION = 2;\r\n",
    ""
)

# 3) Campo inyectado usado solo por el método a eliminar
apply_edit(
    PATH,
    "    private final SesionUsuarioRepository sesionRepository;\r\n",
    ""
)

# 4) El método en sí (job que cerraba sesiones inactivas cada 60s a nivel
#    de backend; se reemplaza por el cierre de sesión por inactividad ya
#    implementado en el frontend).
apply_edit(
    PATH,
    "\r\n"
    "    @Scheduled(fixedRate = 60000)\r\n"
    "    @Transactional\r\n"
    "    public void cerrarSesionesInactivas() {\r\n"
    "\r\n"
    "        LocalDateTime limite =\r\n"
    "            LocalDateTime.now()\r\n"
    "                .minusMinutes(MINUTOS_INACTIVIDAD_SESION);\r\n"
    "\r\n"
    "        List<SesionUsuario> sesiones =\r\n"
    "            sesionRepository\r\n"
    "                .findByActivaTrueAndUltimaActividadBefore(\r\n"
    "                    limite\r\n"
    "                );\r\n"
    "\r\n"
    "        sesiones.forEach(sesion -> {\r\n"
    "\r\n"
    "            sesion.setActiva(false);\r\n"
    "\r\n"
    "            sesion.setFechaLogout(\r\n"
    "                LocalDateTime.now()\r\n"
    "            );\r\n"
    "\r\n"
    "            log.info(\r\n"
    "                \"Sesión cerrada por inactividad: {}\",\r\n"
    "                sesion.getUsuario()\r\n"
    "                    .getNombre()\r\n"
    "            );\r\n"
    "\r\n"
    "        });\r\n"
    "    }\r\n"
    "\r\n",
    "\r\n"
)

print("DONE quitar cerrarSesionesInactivas")
