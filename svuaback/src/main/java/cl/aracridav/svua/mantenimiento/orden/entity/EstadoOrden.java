package cl.aracridav.svua.mantenimiento.orden.entity;

public enum EstadoOrden {
    PENDIENTE,
    PROGRAMADA,
    EN_EJECUCION,
    COMPLETADA,
    CANCELADA,
    ATRASADA;

    public boolean puedePasarA(EstadoOrden nuevo) {
        return switch (this) {
            case PENDIENTE -> nuevo == PROGRAMADA;
            case PROGRAMADA -> nuevo == EN_EJECUCION;
            case EN_EJECUCION -> (nuevo == COMPLETADA || nuevo == CANCELADA);
            case COMPLETADA, CANCELADA, ATRASADA -> false;
        };
    }
}
