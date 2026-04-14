package cl.aracridav.svua.mantenimiento.repuesto.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.mantenimiento.repuesto.dto.request.RepuestoRequest;
import cl.aracridav.svua.mantenimiento.repuesto.dto.response.RepuestoResponse;
import cl.aracridav.svua.mantenimiento.repuesto.entity.Repuesto;
import cl.aracridav.svua.mantenimiento.repuesto.repository.RepuestoRepository;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.mappers.GeneralMapper;
import cl.aracridav.svua.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RepuestoServiceImpl implements RepuestoService {

    private final RepuestoRepository repository;
    private final EmpresaRepository empresaRepository;
    private final GeneralMapper generalMapper;

    @Override
    public RepuestoResponse crear(RepuestoRequest request) {

        Long empresaId = SecurityUtils.getEmpresaId();
        
        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new BusinessException("Empresa no encontrada"));
        
        if(repository.existsByCodigoAndEmpresa(request.getCodigo(), empresa)){
            throw new BusinessException("El código ya existe");
        }

        Repuesto repuesto = new Repuesto();
        repuesto.setCodigo(request.getCodigo());
        repuesto.setNombre(request.getNombre());
        repuesto.setDescripcion(request.getDescripcion());
        repuesto.setCostoUnitario(request.getCostoUnitario());
        repuesto.setStockMinimo(request.getStockMinimo());
        repuesto.setActivo(request.getActivo());
        repuesto.setEmpresa(empresa);

        Repuesto repuestoGuardado = repository.save(repuesto);

        return generalMapper.mapRepuestoResponse(repuestoGuardado);
    }

    @Override
    public Page<RepuestoResponse> listarRepuestos(Pageable pageable) {

        Page<Repuesto> repuestos = Page.empty();

        Long empresaId = SecurityUtils.getEmpresaId();

        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new BusinessException("Empresa no encontrada"));

        repuestos = repository.findByEmpresa(empresa, pageable);
        
        return repuestos.map(generalMapper::mapRepuestoResponse);
    }

    @Override
    public RepuestoResponse obtener(Long id) {

        Repuesto repuesto = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Repuesto no encontrado"));

        return generalMapper.mapRepuestoResponse(repuesto);
    }

    @Override
    public RepuestoResponse actualizar(Long id, RepuestoRequest request) {

        Repuesto repuesto = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Repuesto no encontrado"));

        repuesto.setCodigo(request.getCodigo());
        repuesto.setNombre(request.getNombre());
        repuesto.setDescripcion(request.getDescripcion());
        repuesto.setCostoUnitario(request.getCostoUnitario());
        repuesto.setStockMinimo(request.getStockMinimo());
        repuesto.setActivo(request.getActivo());

        Repuesto repuestoGuardado = repository.save(repuesto);

        return generalMapper.mapRepuestoResponse(repuestoGuardado);
    }

    @Override
    public void eliminar(Long id) {

        Repuesto repuesto = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Repuesto no encontrado"));

        repuesto.setActivo(false);
        
        repository.save(repuesto);

    }

}
