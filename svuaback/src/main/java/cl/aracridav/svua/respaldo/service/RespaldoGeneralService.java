package cl.aracridav.svua.respaldo.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipOutputStream;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.aracridav.svua.multitenancy.RlsContextService;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.util.CsvZipExporter;
import lombok.RequiredArgsConstructor;

// 🔒 Respaldo de TODA la base (todas las empresas): un .zip con un .csv
// por cada tabla del schema "public", sin ningun filtro de Row Level
// Security -- a diferencia de EmpresaBackupService, aca la falta de
// filtro es la intencion (es un respaldo general, no por tenant).
//
// Pensado solo para archivo/auditoria puntual desde la UI (SUPER_ADMIN,
// ver RespaldoController). La RESTAURACION deliberadamente NO se
// expone por la app -- restaurar implica reemplazar la base de la que
// el propio backend depende para seguir funcionando, y eso se hace con
// el backend detenido (ver scripts/backup-postgres.sh y
// docs/continuidad-negocio.md), no desde un request HTTP en caliente.
@Service
@RequiredArgsConstructor
public class RespaldoGeneralService {

    private static final String SQL_TODAS_LAS_TABLAS = """
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = 'public'
              AND table_type = 'BASE TABLE'
            ORDER BY table_name
            """;

    private final JdbcTemplate jdbcTemplate;
    private final RlsContextService rlsContextService;
    private final CsvZipExporter csvZipExporter;

    @Transactional(readOnly = true)
    public byte[] generarBackup() {

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try (ZipOutputStream zip = new ZipOutputStream(buffer)) {

            // A proposito: se quiere TODO, de TODAS las empresas.
            rlsContextService.aplicarBypass();

            List<String> tablas = jdbcTemplate.queryForList(SQL_TODAS_LAS_TABLAS, String.class);
            for (String tabla : tablas) {
                csvZipExporter.exportarComoCsv(zip, tabla, "SELECT * FROM \"" + tabla + "\"");
            }

        } catch (IOException ex) {
            throw new BusinessException("No fue posible generar el respaldo general", ex);
        } finally {
            // El unico rol que llega aca es SUPER_ADMIN (ver
            // RespaldoController): ese es su estado normal.
            rlsContextService.aplicarBypass();
        }

        return buffer.toByteArray();
    }
}
