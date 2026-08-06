package cl.aracridav.svua.multitenancy;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class EmpresaRequestFilter extends OncePerRequestFilter {

    private final EmpresaFilter empresaFilter;

    public EmpresaRequestFilter(EmpresaFilter empresaFilter) {
        this.empresaFilter = empresaFilter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        empresaFilter.activarFiltroEmpresa();

        filterChain.doFilter(request, response);
    }

}
