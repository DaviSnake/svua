package cl.aracridav.svua.inventario.activo.service;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import cl.aracridav.svua.depreciacion.service.DepreciacionService;
import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.inventario.activo.dto.request.ActivoCreateRequest;
import cl.aracridav.svua.inventario.activo.dto.response.ActivoResponse;
import cl.aracridav.svua.inventario.activo.entity.Activo;
import cl.aracridav.svua.inventario.activo.repository.ActivoRepository;
import cl.aracridav.svua.inventario.historial.service.HistorialEstadoActivoService;
import cl.aracridav.svua.inventario.tipoactivo.entity.TipoActivo;
import cl.aracridav.svua.inventario.tipoactivo.repository.TipoActivoRepository;
import cl.aracridav.svua.inventario.ubicacion.entity.Ubicacion;
import cl.aracridav.svua.inventario.ubicacion.repository.UbicacionRepository;
import cl.aracridav.svua.proveedor.entity.Proveedor;
import cl.aracridav.svua.proveedor.repository.ProveedorRepository;
import cl.aracridav.svua.shared.enums.EstadoActivo;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.mappers.GeneralMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ActivoServiceImpl implements ActivoService {

    private final ActivoRepository activoRepository;
    private final TipoActivoRepository tipoActivoRepository;
    private final UbicacionRepository ubicacionRepository;
    private final ProveedorRepository proveedorRepository;
    private final EmpresaRepository empresaRepository;

    private final HistorialEstadoActivoService hEstadoActivoService;
    private final DepreciacionService depreciacionService;
    
    private final GeneralMapper generalMapper;

    @Override
    public ActivoResponse crearActivo(Long empresaId, ActivoCreateRequest activoCreateRequest) {

         if (activoRepository.existsByCodigoInterno(activoCreateRequest.getCodigoInterno())) {
            throw new BusinessException("El código interno ya existe");
        }

        TipoActivo tipoActivo = tipoActivoRepository.findById(activoCreateRequest.getTipoActivoId())
                .orElseThrow(() -> new BusinessException("Tipo de activo no existe"));

        Ubicacion ubicacion = ubicacionRepository.findById(activoCreateRequest.getUbicacionId())
                .orElseThrow(() -> new BusinessException("Ubicación no existe"));

        Proveedor proveedor = proveedorRepository.findById(activoCreateRequest.getProveedorId())
                .orElseThrow(() -> new BusinessException("Proveedor no existe"));

        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new BusinessException("Empresa no encontrado")
        );

        if (activoRepository.existsByCodigoInterno(
                activoCreateRequest.getCodigoInterno()
        )) {
            throw new BusinessException("Código interno duplicado");
        }

        activoCreateRequest.setEstadoActual(EstadoActivo.OPERATIVO);
        activoCreateRequest.setFechaAdquisicion(LocalDate.now());

        Activo activo = new Activo();
        activo.setCodigoInterno(activoCreateRequest.getCodigoInterno());       
        activo.setNombre(activoCreateRequest.getNombre());       
        activo.setDescripcion(activoCreateRequest.getDescripcion());       
        activo.setTipoActivo(tipoActivo);       
        activo.setMarca(activoCreateRequest.getMarca());       
        activo.setModelo(activoCreateRequest.getModelo());       
        activo.setNumeroSerie(activoCreateRequest.getNumeroSerie());       
        activo.setFechaAdquisicion(activoCreateRequest.getFechaAdquisicion());       
        activo.setValorAdquisicion(activoCreateRequest.getValorAdquisicion());  
        activo.setValorResidual(activoCreateRequest.getValorResidual());     
        activo.setVidaUtilMeses(activoCreateRequest.getVidaUtilMeses());       
        activo.setEstadoActual(EstadoActivo.OPERATIVO);       
        activo.setUbicacion(ubicacion);       
        activo.setProveedor(proveedor);       
        activo.setEmpresa(empresa);   
        
        Activo activoGuardado = activoRepository.save(activo);

        depreciacionService.guardarDepreciacion(activoGuardado);

        depreciacionService.calcularYGuardarDepreciacionMensual(activoGuardado);

        return generalMapper.mapActivoResponse(activoGuardado);
    }
    
    @Override
    public Page<ActivoResponse> mostrarActivos(Pageable pageable) {  
        
        Page<Activo> activos = activoRepository.findAll(pageable);

        return activos.map(generalMapper::mapActivoResponse);
    }

    @Override
    public Activo darDeBaja(Long idActivo, String motivo) {

        Activo activo = activoRepository.findById(idActivo)
            .orElseThrow(() ->
                new BusinessException("Activo no existe")
            );

        activo.setEstadoActual(EstadoActivo.BAJA);
        activo.setFechaBaja(LocalDate.now());
        activo.setMotivoBaja(motivo);

        return activo;
    }

    @Override
    public void actualizarEstado(Long idActivo, EstadoActivo nuevoEstado) {

        Activo activo = activoRepository.findById(idActivo)
            .orElseThrow(() ->
                new BusinessException("Activo no encontrado")
        );

        activo.setEstadoActual(nuevoEstado);

        hEstadoActivoService.registrarCambioEstado(
            activo.getId(),
            nuevoEstado,
            "Cambio automático de estado"
        );
    }

}
