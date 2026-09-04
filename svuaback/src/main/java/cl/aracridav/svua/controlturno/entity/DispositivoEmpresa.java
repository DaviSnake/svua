package cl.aracridav.svua.controlturno.entity;

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

// 🔥 Mapea el codigo de un dispositivo fisico de monitoreo (ej.
// "INS-877", ver el campo "Dispositivo: XXX" de cada hoja de un
// reporte importado por correo) a la empresa duena de ese sensor.
// Necesario porque el correo lo envia un proveedor externo (mismo
// remitente para todas las empresas): la unica forma de saber a que
// empresa pertenecen las lecturas es por el dispositivo mismo. Ver
// CorreoLecturaImportador.
@Entity
@Table(name = "dispositivo_empresa")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DispositivoEmpresa extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dispositivo_empresa")
    private Long id;

    @Column(name = "codigo_dispositivo", nullable = false, unique = true, length = 100)
    private String codigoDispositivo;

    @Column(length = 150)
    private String descripcion;

    @Column(nullable = false)
    private Boolean activo;
}
