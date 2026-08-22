package cl.aracridav.svua.inventario.activo.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cl.aracridav.svua.depreciacion.entity.Depreciacion;
import cl.aracridav.svua.inventario.historial.entity.HistorialEstadoActivo;
import cl.aracridav.svua.inventario.tipoactivo.entity.TipoActivo;
import cl.aracridav.svua.inventario.ubicacion.entity.Ubicacion;
import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;
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

    // 🔥 fechaCreacion ahora se hereda de BaseEntity (columna
    // "fecha_creacion" ya existente en esta tabla) — se saco la
    // declaracion propia para no mapear la misma columna dos veces.
    // getFechaCreacion()/setFechaCreacion() siguen funcionando igual
    // (heredados), asi que el resto del codigo no cambia.

    @Column(name = "valor_adquisicion", nullable = false)
    private BigDecimal valorAdquisicion;

    @Column(name = "valor_residual", nullable = false)
    private BigDecimal valorResidual;

    @Column(name = "vida_util_meses", nullable = false)
    private Integer vidaUtilMeses;

    @Enumerated(EnumType.STRING)
    private EstadoActivo estadoActual;

    @Column(name = "cuenta_contable")
    private String cuentaContable;

    // 🔳 Generados automaticamente al crear el activo (o al renombrar su
    // codigo interno), no los ingresa el usuario. Ver ActivoCodigoGenerador.
    @Column(name = "codigo_qr", length = 150)
    private String codigoQr;

    @Column(name = "codigo_ean13", length = 13)
    private String codigoEan13;

    // 🔓 Editables desde el mantenedor de Activo (antes tenian
    // updatable = false por error: el formulario permitia cambiarlos
    // pero Hibernate ignoraba el cambio silenciosamente al guardar).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ubicacion_id", nullable = false)
    private Ubicacion ubicacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @Column(name = "fecha_baja")
    private LocalDate fechaBaja;

    @Column(name = "motivo_baja", length = 255)
    private String motivoBaja;

    @OneToMany(mappedBy = "activo", cascade = CascadeType.ALL)
    private List<Depreciacion> depreciaciones;

    @Builder.Default
    @OneToMany(mappedBy = "activo", cascade = CascadeType.ALL)
    private Set<HistorialEstadoActivo> historialEstados = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "activo")
    private Set<OrdenMantenimiento> ordenesMantenimiento = new HashSet<>();

}
