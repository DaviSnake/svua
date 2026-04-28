package cl.aracridav.svua.depreciacion.dto;

import java.math.BigDecimal;

public record DepreciacionDTO(
    Integer mes,
    BigDecimal total
) {}
