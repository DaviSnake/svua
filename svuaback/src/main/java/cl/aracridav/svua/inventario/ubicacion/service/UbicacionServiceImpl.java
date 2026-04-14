package cl.aracridav.svua.inventario.ubicacion.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.inventario.ubicacion.dto.request.UbicacionCreateRequest;
import cl.aracridav.svua.inventario.ubicacion.dto.response.UbicacionResponse;
import cl.aracridav.svua.inventario.ubicacion.entity.Ubicacion;
import cl.aracridav.svua.inventario.ubicacion.repository.UbicacionRepository;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.mappers.GeneralMapper;
import cl.aracridav.svua.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UbicacionServiceImpl implements UbicacionService {

    private final UbicacionRepository ubicacionRepository;
    private final EmpresaRepository empresaRepository;
    private final GeneralMapper generalMapper;

    public UbicacionResponse registrarUbicacion(UbicacionCreateRequest request) {

        Long empresaId = SecurityUtils.getEmpresaId();

        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new BusinessException("El nombre de la ubicación es obligatorio");
        }

        if (ubicacionRepository.existsByNombreIgnoreCase(request.getNombre())) {
            throw new BusinessException("Ya existe una ubicación con ese nombre");
        }

        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() ->
                    new BusinessException("Empresa no encontrado"));

        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setNombre(request.getNombre());
        ubicacion.setDescripcion(request.getDescripcion());
        ubicacion.setDireccion(request.getDireccion());
        ubicacion.setEmpresa(empresa);
        ubicacion.setActivo(true);

        Ubicacion ubicacionGuardad = ubicacionRepository.save(ubicacion);

        return generalMapper.mapUbicacionResponse(ubicacionGuardad);
    }

    public Page<UbicacionResponse> listarUbicaciones(Pageable pageable){

        Page<Ubicacion> ubicaciones = Page.empty();
        
        Long empresaId = SecurityUtils.getEmpresaId();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean esSuperAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        
        boolean esAdminEmpresa = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN_EMPRESA"));

        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new BusinessException("Empresa no encontrada"));

        if (esSuperAdmin) {
            ubicaciones = ubicacionRepository.findAll(pageable);
        }

        if (esAdminEmpresa) {
            ubicaciones = ubicacionRepository.findByEmpresaId(empresa.getId(), pageable);
        }

        return ubicaciones.map(generalMapper::mapUbicacionResponse);
        
    }

}
