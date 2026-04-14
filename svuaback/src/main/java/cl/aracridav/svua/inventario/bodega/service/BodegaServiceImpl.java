package cl.aracridav.svua.inventario.bodega.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.inventario.bodega.dto.request.BodegaRequest;
import cl.aracridav.svua.inventario.bodega.dto.response.BodegaResponse;
import cl.aracridav.svua.inventario.bodega.entity.Bodega;
import cl.aracridav.svua.inventario.bodega.repository.BodegaRepository;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.mappers.GeneralMapper;
import cl.aracridav.svua.shared.util.SecurityUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class BodegaServiceImpl implements BodegaService {

    private final BodegaRepository repository;
    private final GeneralMapper generalMapper;
    private final EmpresaRepository empresaRepository;


    @Override
    public BodegaResponse crear(BodegaRequest request) {

        Long empresaId = SecurityUtils.getEmpresaId();

        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new BusinessException("Empresa no encontrada"));

        Bodega bodega = new Bodega();
        bodega.setNombre(request.getNombre());
        bodega.setUbicacionFisica(request.getUbicacionFisica());
        bodega.setActiva(true);
        bodega.setEmpresa(empresa);

        Bodega bodegaGuardada = repository.save(bodega);

        return generalMapper.mapBodegaResponse(bodegaGuardada);
    }

    @Override
    public Page<BodegaResponse> listar(Pageable pegeable) {

        Page<Bodega> bodegas = Page.empty();
        
        Long empresaId = SecurityUtils.getEmpresaId();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean esSuperAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        
        boolean esAdminEmpresa = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN_EMPRESA"));

        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new BusinessException("Empresa no encontrada"));

        if (esSuperAdmin) {
            bodegas = repository.findAll(pegeable);
        }

        if (esAdminEmpresa) {
            bodegas = repository.findByEmpresaId(empresa.getId(), pegeable);
        }

        return bodegas.map(generalMapper::mapBodegaResponse);
    }

    @Override
    public BodegaResponse obtener(Long id) {

        Bodega bodega = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bodega no encontrada"));

        return generalMapper.mapBodegaResponse(bodega);
    }

    @Override
    public BodegaResponse actualizar(Long id, BodegaRequest request) {

        Bodega bodega = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bodega no encontrada"));

        bodega.setNombre(request.getNombre());
        bodega.setUbicacionFisica(request.getUbicacionFisica());

        repository.save(bodega);

        return generalMapper.mapBodegaResponse(bodega);
    }

    @Override
    public void eliminar(Long id) {

        Bodega bodega = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bodega no encontrada"));

        bodega.setActiva(false);

        repository.delete(bodega);

    }


}
