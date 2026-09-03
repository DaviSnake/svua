package cl.aracridav.svua.shared.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;

import cl.aracridav.svua.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;

// Vuelca el resultado de un SELECT como un .csv dentro de un
// ZipOutputStream ya abierto. Compartido por los distintos respaldos
// tabla-por-tabla (EmpresaBackupService, RespaldoGeneralService) para
// no duplicar el mismo escritor de CSV en cada uno.
@Component
@RequiredArgsConstructor
public class CsvZipExporter {

    private final JdbcTemplate jdbcTemplate;

    public void exportarComoCsv(ZipOutputStream zip, String nombreArchivo, String sql) throws IOException {

        StringBuilder csv = new StringBuilder();
        boolean[] encabezadoEscrito = { false };

        RowCallbackHandler handler = rs -> {
            if (!encabezadoEscrito[0]) {
                escribirEncabezado(csv, rs.getMetaData());
                encabezadoEscrito[0] = true;
            }
            escribirFila(csv, rs);
        };

        try {
            jdbcTemplate.query(sql, handler);
        } catch (DataAccessException ex) {
            throw new BusinessException("No fue posible exportar la tabla " + nombreArchivo, ex);
        }

        zip.putNextEntry(new ZipEntry(nombreArchivo + ".csv"));
        zip.write(csv.toString().getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private void escribirEncabezado(StringBuilder csv, ResultSetMetaData meta) throws SQLException {
        int columnas = meta.getColumnCount();
        for (int i = 1; i <= columnas; i++) {
            if (i > 1) csv.append(',');
            csv.append(escaparCsv(meta.getColumnName(i)));
        }
        csv.append('\n');
    }

    private void escribirFila(StringBuilder csv, ResultSet rs) throws SQLException {
        int columnas = rs.getMetaData().getColumnCount();
        for (int i = 1; i <= columnas; i++) {
            if (i > 1) csv.append(',');
            csv.append(escaparCsv(rs.getString(i)));
        }
        csv.append('\n');
    }

    private String escaparCsv(String valor) {
        if (valor == null) return "";
        boolean necesitaComillas = valor.contains(",") || valor.contains("\"")
                || valor.contains("\n") || valor.contains("\r");
        String escapado = valor.replace("\"", "\"\"");
        return necesitaComillas ? "\"" + escapado + "\"" : escapado;
    }
}
