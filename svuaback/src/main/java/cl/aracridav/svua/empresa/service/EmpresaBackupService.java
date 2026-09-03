package cl.aracridav.svua.empresa.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.aracridav.svua.empresa.repository.EmpresaRepository;
import cl.aracridav.svua.multitenancy.RlsContextService;
import cl.aracridav.svua.shared.exception.BusinessException;
import cl.aracridav.svua.shared.util.CsvZipExporter;
import lombok.RequiredArgsConstructor;

// 🔒 Respaldo de UNA sola empresa (tenant): un .zip con un .csv por
// tabla, pensado para archivo/auditoria puntual (ej. antes de
// desactivar o eliminar una empresa), NO como mecanismo de
// restauracion en caliente. Complementa al backup completo de toda la
// base (scripts/backup-postgres.sh, y su equivalente de UI
// RespaldoGeneralService) y al equivalente por linea de comando
// (scripts/backup-empresa.sh).
//
// Reusa la misma Row Level Security que ya protege la app en tiempo
// real (V27__enable_row_level_security_por_empresa.sql): en vez de
// mantener a mano una lista de que tablas tienen empresa_id, la
// descubre en information_schema (mismo criterio que usa esa
// migracion) y deja que Postgres filtre cada "SELECT *" segun el
// app.current_empresa_id que fija RlsContextService.
//
// La propia tabla "empresa" tambien entra por esa via: su PK esta
// mapeada a la columna "empresa_id" (ver Empresa.java), asi que V27 ya
// le creo su propia policy (empresa_id = current_setting(...)), y el
// loop generico trae exactamente su propia fila sin necesidad de un
// caso especial (uno existio aca antes y estaba mal: usaba "WHERE id"
// -- esa columna no existe en esta tabla -- y ademas hubiera duplicado
// la entrada en el zip, porque el loop generico ya la incluye).
@Service
@RequiredArgsConstructor
public class EmpresaBackupService {

    private static final String SQL_TABLAS_CON_EMPRESA_ID = """
            SELECT DISTINCT c.table_name
            FROM information_schema.columns c
            JOIN information_schema.tables t
              ON t.table_schema = c.table_schema
             AND t.table_name = c.table_name
             AND t.table_type = 'BASE TABLE'
            WHERE c.table_schema = 'public'
              AND c.column_name = 'empresa_id'
            ORDER BY c.table_name
            """;

    private final JdbcTemplate jdbcTemplate;
    private final EmpresaRepository empresaRepository;
    private final RlsContextService rlsContextService;
    private final CsvZipExporter csvZipExporter;

    // 🐛 FIX: sin @Transactional, RlsContextService.aplicarEmpresa()
    // fija el "SET" en la conexion que Hibernate toma prestada del pool
    // en ESE instante, pero cada jdbcTemplate.query() de mas abajo
    // puede tomar prestada una conexion DISTINTA (no hay transaccion
    // que las una), que puede llegar con app.bypass_rls='on' pegado de
    // una operacion anterior de este mismo SUPER_ADMIN -- por eso salia
    // todo sin filtrar en vez de filtrado. Con @Transactional, Spring
    // ata una unica conexion fisica a todo el metodo, y tanto Hibernate
    // como JdbcTemplate la comparten.
    @Transactional(readOnly = true)
    public byte[] generarBackup(Long empresaId) {

        // 🔒 Un rol SUPERUSER o con BYPASSRLS ignora Row Level Security
        // de forma INCONDICIONAL en Postgres (asi lo advierte V27,
        // seccion "IMPORTANTE"): con ese rol, cada "SELECT *" de mas
        // abajo devolveria TODAS las empresas sin ningun error, aunque
        // el "SET" de RlsContextService se aplique perfecto. Se corta
        // ANTES de generar nada en vez de arriesgarse a producir un zip
        // con datos de otras empresas en silencio.
        verificarRolNoBypassRls();

        if (!empresaRepository.existsById(empresaId)) {
            throw new BusinessException("No existe una empresa con id " + empresaId);
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try (ZipOutputStream zip = new ZipOutputStream(buffer)) {

            rlsContextService.aplicarEmpresa(empresaId);

            List<String> tablas = jdbcTemplate.queryForList(SQL_TABLAS_CON_EMPRESA_ID, String.class);
            for (String tabla : tablas) {
                csvZipExporter.exportarComoCsv(zip, tabla, "SELECT * FROM \"" + tabla + "\"");
            }

        } catch (IOException ex) {
            throw new BusinessException("No fue posible generar el respaldo de la empresa", ex);
        } finally {
            // El unico rol que llega aca es SUPER_ADMIN (ver
            // EmpresaController.backup): ese es su estado normal.
            rlsContextService.aplicarBypass();
        }

        return buffer.toByteArray();
    }

    // Mismo chequeo que ya hace scripts/backup-empresa.sh antes de
    // dumpear: SELECT rolsuper OR rolbypassrls FROM pg_roles WHERE
    // rolname = current_user.
    private void verificarRolNoBypassRls() {
        Map<String, Object> rol = jdbcTemplate.queryForMap(
                "SELECT current_user AS nombre, (rolsuper OR rolbypassrls) AS bypass "
                        + "FROM pg_roles WHERE rolname = current_user");

        if (Boolean.TRUE.equals(rol.get("bypass"))) {
            throw new BusinessException(
                    "El rol de conexion a la base de datos ('" + rol.get("nombre") + "') es SUPERUSER "
                            + "o tiene BYPASSRLS: este respaldo no puede filtrar por empresa con ese rol "
                            + "(Postgres ignora Row Level Security incondicionalmente). Debe configurarse "
                            + "un rol normal para la conexion (ver V27__enable_row_level_security_por_empresa.sql, "
                            + "seccion \"IMPORTANTE\").");
        }
    }
}
