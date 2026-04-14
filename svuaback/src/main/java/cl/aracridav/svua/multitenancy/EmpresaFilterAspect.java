package cl.aracridav.svua.multitenancy;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Aspect
@Component
@RequiredArgsConstructor
public class EmpresaFilterAspect {

    private final EmpresaFilter empresaFilter;

    @Before("execution(* cl.aracridav.svua..service..*(..))")
    public void activarFiltro() {
        empresaFilter.activarFiltroEmpresa();
    }
}
