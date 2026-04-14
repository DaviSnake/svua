package cl.aracridav.svua.inventario.movimientoinventario.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.aracridav.svua.inventario.movimientoinventario.dto.request.MovimientoInventarioRequest;
import cl.aracridav.svua.inventario.movimientoinventario.dto.response.MovimientoInventarioResponse;
import cl.aracridav.svua.inventario.movimientoinventario.service.MovimientoInventarioService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/svua/movimientos-inventario")
@RequiredArgsConstructor
public class MovimientoInventarioController {

    private final MovimientoInventarioService service;

    @PreAuthorize(
        "hasRole('SUPER_ADMIN') or " +
        "(hasAuthority('MOVIMIENTO_CREATE')) "
    )
    @PostMapping
    public MovimientoInventarioResponse crear(
            @RequestBody MovimientoInventarioRequest request
    ) {
        return service.crear(request);
    }

    @PreAuthorize(
        "hasRole('SUPER_ADMIN') or " +
        "(hasAuthority('MOVIMIENTO_VIEW')) "
    )
    @GetMapping
    public List<MovimientoInventarioResponse> listar() {
        return service.listar();
    }

}
