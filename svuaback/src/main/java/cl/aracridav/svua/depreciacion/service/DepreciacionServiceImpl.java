package cl.aracridav.svua.depreciacion.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.aracridav.svua.depreciacion.entity.Depreciacion;
import cl.aracridav.svua.depreciacion.entity.DepreciacionMensual;
import cl.aracridav.svua.depreciacion.entity.MetodoDepreciacion;
import cl.aracridav.svua.depreciacion.repository.DepreciacionMensualRepository;
import cl.aracridav.svua.depreciacion.repository.DepreciacionRepository;
import cl.aracridav.svua.empresa.entity.Empresa;
import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.inventario.activo.entity.Activo;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DepreciacionServiceImpl implements DepreciacionService {

    private final DepreciacionRepository depreciacionRepository;
    private final DepreciacionMensualRepository depreciacionMensualRepository;
    private final EmpresaRepository empresaRepository;

    @Override
    public void calcularYGuardarDepreciacionMensual(Activo activo) {
        List<DepreciacionMensual> lista = new ArrayList<>();

        Long empresaId = SecurityUtils.getEmpresaId();

        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new BusinessException("Empresa no encontrada"));

        BigDecimal costo = activo.getValorAdquisicion();
        BigDecimal valorResidual = activo.getValorResidual();
        int vidaUtilMeses = activo.getVidaUtilMeses();

        BigDecimal depreciacionMensual = (costo.subtract(valorResidual)).divide(BigDecimal.valueOf(vidaUtilMeses), RoundingMode.HALF_UP);
        BigDecimal depreciacionAcumulada = BigDecimal.valueOf(0);
        BigDecimal valorContable = costo;

        for (int mes = 1; mes <= vidaUtilMeses; mes++) {

            depreciacionAcumulada = depreciacionAcumulada.add(depreciacionMensual); // ✅

            valorContable = valorContable.subtract(depreciacionMensual); // ✅

            if (valorContable.compareTo(valorResidual) < 0) { // ✅
                valorContable = valorResidual;
            }

            DepreciacionMensual dep = new DepreciacionMensual();
            dep.setActivo(activo);
            dep.setMes(mes);
            dep.setDepreciacionMensual(depreciacionMensual);
            dep.setDepreciacionAcumulada(depreciacionAcumulada);
            dep.setValorContable(valorContable);
            dep.setEmpresa(empresa);

            lista.add(dep);
        }

        // Guardar todas las depreciaciones en la base de datos
        depreciacionMensualRepository.saveAll(lista);
    }

    @Override
    public void guardarDepreciacion(Activo activo){
        
        Depreciacion dep = new Depreciacion();
        dep.setActivo(activo);
        dep.setEmpresa(activo.getEmpresa());
        dep.setFechaInicio(activo.getFechaAdquisicion());
        dep.setMetodo(MetodoDepreciacion.LINEA_RECTA);
        dep.setValorInicial(activo.getValorAdquisicion());
        dep.setValorResidual(activo.getValorResidual());
        dep.setVidaUtilMeses(activo.getVidaUtilMeses());

        depreciacionRepository.save(dep);

    }
    
    @Override
    public List<DepreciacionMensual> obtenerDepreciacionesPorActivo(Activo activo) {
        return depreciacionMensualRepository.findByActivoOrderByMesAsc(activo);
    }
   
}
