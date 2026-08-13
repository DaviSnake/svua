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


PATH = "/sessions/rcw-01u2gwnbk5zzyctqiimc1ak9/mnt/svua/svuaback/src/main/java/cl/aracridav/svua/mantenimiento/orden/service/OrdenMantenimientoServiceImpl.java"

apply_edit(
    PATH,
    "        // \U0001F525 Orden fijo por fecha de ejecucion descendente, sin importar\r\n"
    "        // el sort que traiga el Pageable del controller.\r\n"
    "        Pageable pageableOrdenado = PageRequest.of(\r\n"
    "            pageable.getPageNumber(),\r\n"
    "            pageable.getPageSize(),\r\n"
    "            Sort.by(Sort.Direction.DESC, \"fechaEjecucion\"));\r\n",

    "        // \U0001F525 Orden fijo por fecha programada descendente, sin importar\r\n"
    "        // el sort que traiga el Pageable del controller.\r\n"
    "        Pageable pageableOrdenado = PageRequest.of(\r\n"
    "            pageable.getPageNumber(),\r\n"
    "            pageable.getPageSize(),\r\n"
    "            Sort.by(Sort.Direction.DESC, \"fechaProgramada\"));\r\n"
)

print("DONE orden fecha programada")
