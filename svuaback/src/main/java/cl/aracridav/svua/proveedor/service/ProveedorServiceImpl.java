package cl.aracridav.svua.proveedor.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.proveedor.dto.request.ProveedorCreateRequest;
import cl.aracridav.svua.proveedor.dto.response.ProveedorResponse;
import cl.aracridav.svua.proveedor.entity.Proveedor;
import cl.aracridav.svua.proveedor.repository.ProveedorRepository;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.mappers.GeneralMapper;
import cl.aracridav.svua.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProveedorServiceImpl implements ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final EmpresaRepository empresaRepository;
    private final GeneralMapper generalMapper;

    public ProveedorResponse registrarProveedor(ProveedorCreateRequest request) {

        Long empresaId = SecurityUtils.getEmpresaId();

        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new BusinessException("El nombre del proveedor es obligatorio");
        }

        if (request.getRut() == null || request.getRut().isBlank()) {
            throw new BusinessException("El RUT del proveedor es obligatorio");
        }

        if (proveedorRepository.existsByRut(request.getRut())) {
            throw new BusinessException("Ya existe un proveedor con ese RUT");
        }

        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() ->
                    new BusinessException("Empresa no encontrado"));

        Proveedor proveedor = new Proveedor();
        proveedor.setNombre(request.getNombre());
        proveedor.setRut(request.getRut());
        proveedor.setContacto(request.getContacto());
        proveedor.setTelefono(request.getTelefono());
        proveedor.setEmail(request.getEmail());
        proveedor.setEmpresa(empresa);
        proveedor.setActivo(true);

        Proveedor proveedorGuardado = proveedorRepository.save(proveedor);

        return generalMapper.mapProeedorResponse(proveedorGuardado);
    }

    public Page<ProveedorResponse> listarProveedores(Pageable pageable){

        Page<Proveedor> proveedores = Page.empty();

        Long empresaId = SecurityUtils.getEmpresaId();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean esSuperAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        
        boolean esAdminEmpresa = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN_EMPRESA"));

        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new BusinessException("Empresa no encontrada"));

        if (esSuperAdmin) {
            proveedores = proveedorRepository.findAll(pageable);
        }

        if (esAdminEmpresa) {
            proveedores = proveedorRepository.findByEmpresaId(empresa.getId(), pageable);
        }

        return proveedores.map(generalMapper::mapProeedorResponse);
        
    }

}
