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


DTO_PATH = "/sessions/rcw-01u2gwnbk5zzyctqiimc1ak9/mnt/svua/svuaback/src/main/java/cl/aracridav/svua/mantenimiento/orden/dto/response/OrdenMantenimientoReporteResponse.java"
SERVICE_PATH = "/sessions/rcw-01u2gwnbk5zzyctqiimc1ak9/mnt/svua/svuaback/src/main/java/cl/aracridav/svua/mantenimiento/orden/service/OrdenMantenimientoServiceImpl.java"

# 1) DTO: agregar campo observaciones (la "descripción" de la mantención)
apply_edit(
    DTO_PATH,
    "    private Long id;\r\n"
    "    private String titulo;\r\n"
    "    private EstadoOrden estado;\r\n",

    "    private Long id;\r\n"
    "    private String titulo;\r\n"
    "    private String observaciones;\r\n"
    "    private EstadoOrden estado;\r\n"
)

# 2) Mapper: poblar observaciones desde la entidad
apply_edit(
    SERVICE_PATH,
    "            .id(o.getId())\r\n"
    "            .titulo(o.getTitulo())\r\n"
    "            .estado(o.getEstado())\r\n",

    "            .id(o.getId())\r\n"
    "            .titulo(o.getTitulo())\r\n"
    "            .observaciones(o.getObservaciones())\r\n"
    "            .estado(o.getEstado())\r\n"
)

print("DONE informe descripcion")
