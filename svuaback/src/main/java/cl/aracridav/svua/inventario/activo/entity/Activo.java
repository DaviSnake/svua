package cl.aracridav.svua.inventario.activo.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import cl.aracridav.svua.depreciacion.entity.Depreciacion;
import cl.aracridav.svua.inventario.historial.entity.HistorialEstadoActivo;
import cl.aracridav.svua.inventario.tipoactivo.entity.TipoActivo;
import cl.aracridav.svua.inventario.ubicacion.entity.Ubicacion;
import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;
import cl.aracridav.svua.mantenimiento.plan.entity.PlanMantenimiento;
import cl.aracridav.svua.proveedor.entity.Proveedor;
import cl.aracridav.svua.shared.entity.BaseEntity;
import cl.aracridav.svua.shared.enums.EstadoActivo;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "activo")
public class Activo extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_activo")
    private Long id;

    @Column(name = "codigo_interno", nullable = false, unique = true, length = 50)
    private String codigoInterno;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_activo", nullable = false)
    private TipoActivo tipoActivo;

    @Column(length = 100)
    private String marca;

    @Column(length = 100)
    private String modelo;

    @Column(name = "numero_serie", length = 100)
    private String numeroSerie;

    @Column(name = "fecha_adquisicion", nullable = false)
    private LocalDate fechaAdquisicion;

    @Column(name = "valor_adquisicion", nullable = false)
    private BigDecimal valorAdquisicion;

    @Column(name = "valor_residual", nullable = false)
    private BigDecimal valorResidual;

    @Column(name = "vida_util_meses", nullable = false)
    private Integer vidaUtilMeses;

    @Enumerated(EnumType.STRING)
    private EstadoActivo estadoActual;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ubicacion_id", nullable = false, updatable = false)
    private Ubicacion ubicacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false, updatable = false)
    private Proveedor proveedor;

    @Column(name = "fecha_baja")
    private LocalDate fechaBaja;

    @Column(name = "motivo_baja", length = 255)
    private String motivoBaja;

    @OneToMany(mappedBy = "activo", cascade = CascadeType.ALL)
    private List<PlanMantenimiento> planesMantenimiento;

    @OneToOne(mappedBy = "activo", cascade = CascadeType.ALL)
    private Depreciacion depreciacion;

    @OneToMany(mappedBy = "activo", cascade = CascadeType.ALL)
    private List<HistorialEstadoActivo> historialEstados;

    @OneToMany(mappedBy = "activo")
    private List<OrdenMantenimiento> ordenesMantenimiento;

}
