package cl.aracridav.svua.controlturno.entity;

import java.math.BigDecimal;

import cl.aracridav.svua.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 🔥 Catalogo de "puntos de control" que se monitorean manualmente cada
// turno (ej. Camara de Fermentacion, Horno, Sala de Proceso): reemplaza
// el registro en planilla Excel (SISTEMA_DE_CONTROL_DE_MANTENCION) por
// una pantalla dentro de svua. Ver LecturaControl para las lecturas
// horarias asociadas a cada punto.
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "punto_control")
public class PuntoControl extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_punto_control")
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    // 🔥 Unidad de medida del punto de control (ej. "°C", "%").
    @Column(nullable = false, length = 20)
    private String unidad;

    // 🔥 Rango aceptable (opcional): si ambos vienen informados, el
    // dashboard usa este rango para el grafico de dona de "lecturas
    // dentro vs fuera de rango" (ver LecturaControlServiceImpl.dashboard).
    @Column(name = "valor_min", precision = 10, scale = 2)
    private BigDecimal valorMin;

    @Column(name = "valor_max", precision = 10, scale = 2)
    private BigDecimal valorMax;

    @Column(nullable = false)
    private Boolean activo;
}
