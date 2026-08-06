package cl.aracridav.svua.camara.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import cl.aracridav.svua.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "lectura_temperatura")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LecturaTemperatura extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal temperatura;

    @Column(name = "texto_ocr", columnDefinition = "TEXT")
    private String textoOcr;

    @Column(name = "fecha_lectura", nullable = false)
    private LocalDateTime fechaLectura;
}
