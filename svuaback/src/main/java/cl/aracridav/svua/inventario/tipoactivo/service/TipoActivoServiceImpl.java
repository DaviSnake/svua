package cl.aracridav.svua.inventario.tipoactivo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.inventario.tipoactivo.dto.request.TipoActivoCreateRequest;
import cl.aracridav.svua.inventario.tipoactivo.dto.response.TipoActivoResponse;
import cl.aracridav.svua.inventario.tipoactivo.entity.TipoActivo;
import cl.aracridav.svua.inventario.tipoactivo.repository.TipoActivoRepository;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.mappers.GeneralMapper;
import cl.aracridav.svua.shared.util.SecurityUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TipoActivoServiceImpl implements TipoActivoService{

    private final TipoActivoRepository tipoActivoRepository;
    private final EmpresaRepository empresaRepository;
     private final GeneralMapper generalMapper;

    public TipoActivoResponse crear(Long empresaId, TipoActivoCreateRequest tipoActivoCreateRequest) {

        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new BusinessException("Empresa no encontrado")
        );

        TipoActivo tipoActivo = new TipoActivo();
        tipoActivo.setNombre(tipoActivoCreateRequest.getNombre());
        tipoActivo.setDescripcion(tipoActivoCreateRequest.getDescripcion());
        tipoActivo.setVidaUtilReferencialMeses(tipoActivoCreateRequest.getVidaUtilReferencialMeses());
        tipoActivo.setEmpresa(empresa);
        tipoActivo.setActivo(tipoActivoCreateRequest.getActivo());

        TipoActivo tipoActivoGuardado = tipoActivoRepository.save(tipoActivo);
        
        return generalMapper.mapTipoActivoResponse(tipoActivoGuardado);
    }

    public Page<TipoActivoResponse> listarTipoActivos(Pageable pageable) {

        Page<TipoActivo> tipoActivos = Page.empty();

        Long empresaId = SecurityUtils.getEmpresaId();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean esSuperAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        
        boolean esAdminEmpresa = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN_EMPRESA"));

        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new BusinessException("Empresa no encontrada"));

        if (esSuperAdmin) {
            tipoActivos = tipoActivoRepository.findAll(pageable);
        }

        if (esAdminEmpresa) {
            tipoActivos = tipoActivoRepository.findByEmpresaId(empresa.getId(), pageable);
        }

        return tipoActivos.map(generalMapper::mapTipoActivoResponse);
        
    }

}
