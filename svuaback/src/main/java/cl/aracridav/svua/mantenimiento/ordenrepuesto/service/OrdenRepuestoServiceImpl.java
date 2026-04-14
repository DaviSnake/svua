package cl.aracridav.svua.mantenimiento.ordenrepuesto.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.inventario.movimientoinventario.service.MovimientoInventarioService;
import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;
import cl.aracridav.svua.mantenimiento.orden.repository.OrdenMantenimientoRepository;
import cl.aracridav.svua.mantenimiento.ordenrepuesto.dto.request.OrdenRepuestoRequest;
import cl.aracridav.svua.mantenimiento.ordenrepuesto.dto.response.OrdenRepuestoResponse;
import cl.aracridav.svua.mantenimiento.ordenrepuesto.entity.OrdenRepuesto;
import cl.aracridav.svua.mantenimiento.ordenrepuesto.repository.OrdenRepuestoRepository;
import cl.aracridav.svua.mantenimiento.repuesto.entity.Repuesto;
import cl.aracridav.svua.mantenimiento.repuesto.repository.RepuestoRepository;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.mappers.GeneralMapper;
import cl.aracridav.svua.shared.util.SecurityUtils;
import cl.aracridav.svua.usuario.entity.Usuario;
import cl.aracridav.svua.usuario.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrdenRepuestoServiceImpl implements OrdenRepuestoService {

    private final OrdenRepuestoRepository repository;
    private final OrdenMantenimientoRepository ordenRepository;
    private final RepuestoRepository repuestoRepository;
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final GeneralMapper generalMapper;

    private final MovimientoInventarioService movimientoInventarioService;

    @Transactional
    public OrdenRepuestoResponse agregarRepuesto(OrdenRepuestoRequest request) {

        Long usuarioId = SecurityUtils.getUsuarioId();

        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        Empresa empresa = empresaRepository.findById(usuario.getEmpresa().getId())
            .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        OrdenMantenimiento orden = ordenRepository.findById(request.getOrdenId())
                .orElseThrow(() -> new BusinessException("Orden no encontrada"));

        Repuesto repuesto = repuestoRepository.findById(request.getRepuestoId())
                .orElseThrow(() -> new BusinessException("Repuesto no encontrado"));

        BigDecimal costoTotal =
                request.getCostoUnitario()
                        .multiply(BigDecimal.valueOf(request.getCantidad()));

        OrdenRepuesto ordenRepuesto = new OrdenRepuesto();

        ordenRepuesto.setOrden(orden);
        ordenRepuesto.setRepuesto(repuesto);
        ordenRepuesto.setCantidad(request.getCantidad());
        ordenRepuesto.setCostoUnitario(request.getCostoUnitario());
        ordenRepuesto.setCostoTotal(costoTotal);
        ordenRepuesto.setUsuario(usuario);
        ordenRepuesto.setEmpresa(empresa);

         OrdenRepuesto ordenRepuestoGuardado = repository.save(ordenRepuesto);

        // 🔥 Descontar stock
        movimientoInventarioService.salidaPorMantenimiento(
                repuesto.getId(),
                request.getCantidad(),
                "Orden mantenimiento #" + orden.getId()
        ); 

        return generalMapper.mapOrdenRepuestoResponse(ordenRepuestoGuardado);
    }

    @Transactional
    public List<OrdenRepuestoResponse> listarPorOrden(Long ordenId) {

        return repository.findByOrdenId(ordenId)
                .stream()
                .map(generalMapper::mapOrdenRepuestoResponse)
                .collect(Collectors.toList());
    }


}
