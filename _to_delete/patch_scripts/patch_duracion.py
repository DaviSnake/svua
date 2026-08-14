t1 = "/sessions/rcw-01u2gwnbk5zzyctqiimc1ak9/mnt/svua/svuaback/src/main/java/cl/aracridav/svua/mantenimiento/orden/dto/response/OrdenMantenimientoReporteResponse.java"
t2 = "/sessions/rcw-01u2gwnbk5zzyctqiimc1ak9/mnt/svua/svuaback/src/main/java/cl/aracridav/svua/mantenimiento/orden/service/OrdenMantenimientoServiceImpl.java"

def apply_edit(path, old, new, expected_count=1):
    with open(path, 'rb') as f:
        data = f.read()
    text = data.decode('utf-8')
    count = text.count(old)
    assert count == expected_count, f"{path}: expected {expected_count} occurrences, found {count}"
    text = text.replace(old, new)
    with open(path, 'wb') as f:
        f.write(text.encode('utf-8'))

# 1) DTO: agregar duracionSegundos
old1 = (
    "    private LocalDateTime fechaProgramada;\r\n"
    "    private LocalDateTime fechaEjecucion;\r\n"
)
new1 = (
    "    private LocalDateTime fechaProgramada;\r\n"
    "    private LocalDateTime fechaEjecucion;\r\n"
    "    private Long duracionSegundos;\r\n"
)
apply_edit(t1, old1, new1, 1)

# 2) Service: mapear duracionSegundos en mapearInformeMantencion()
old2 = (
    "            .fechaProgramada(o.getFechaProgramada())\r\n"
    "            .fechaEjecucion(o.getFechaEjecucion())\r\n"
    "            .activoNombre(o.getActivo().getNombre())\r\n"
)
new2 = (
    "            .fechaProgramada(o.getFechaProgramada())\r\n"
    "            .fechaEjecucion(o.getFechaEjecucion())\r\n"
    "            .duracionSegundos(o.getDuracionSegundos())\r\n"
    "            .activoNombre(o.getActivo().getNombre())\r\n"
)
apply_edit(t2, old2, new2, 1)

print("OK patched both files")
