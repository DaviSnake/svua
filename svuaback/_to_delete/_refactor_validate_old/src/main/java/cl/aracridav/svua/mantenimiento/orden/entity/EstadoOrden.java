package cl.aracridav.svua.mantenimiento.orden.entity;

public enum EstadoOrden {
    PENDIENTE,
    PROGRAMADA,
    EN_EJECUCION,
    PRE_COMPLETADA,
    COMPLETADA,
    CANCELADA,
    ATRASADA;

    public boolean puedePasarA(EstadoOrden nuevo) {
        return switch (this) {
            case PENDIENTE -> (nuevo == PROGRAMADA || nuevo == CANCELADA);
            case PROGRAMADA -> (nuevo == EN_EJECUCION || nuevo == CANCELADA);
            case EN_EJECUCION -> nuevo == PRE_COMPLETADA;
            case PRE_COMPLETADA -> nuevo == COMPLETADA;
            case COMPLETADA, CANCELADA, ATRASADA -> false;
        };
    }
}
