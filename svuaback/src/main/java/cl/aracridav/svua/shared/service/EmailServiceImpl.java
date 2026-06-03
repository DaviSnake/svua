package cl.aracridav.svua.shared.service;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailFrom;

    @Override
    public void sendResetEmail(String to, String link) {

        try {

            MimeMessage mimeMessage = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // REMITENTE REAL DEL SMTP
            helper.setFrom("Soporte SVUA <" + mailFrom + ">");

            // OPCIONAL:
            // si quieres responder desde otro correo
            // helper.setReplyTo("dmedinac@gmail.com");

            helper.setTo(to);

            helper.setSubject("Recuperar contraseña");

            String html = """
                <div style="
                    font-family: Arial, sans-serif;
                    text-align: center;
                    padding: 20px;
                    background-color: #f5f6fa;
                ">

                    <div style="
                        max-width: 500px;
                        margin: auto;
                        background: white;
                        border-radius: 12px;
                        padding: 40px;
                        box-shadow: 0 2px 10px rgba(0,0,0,0.08);
                    ">

                        <h2 style="color:#2d3436;">
                            Recuperar contraseña
                        </h2>

                        <p style="color:#636e72;">
                            Haz clic en el botón para restablecer tu contraseña.
                        </p>

                        <a href="%s"
                        style="
                                display:inline-block;
                                margin-top:20px;
                                padding:14px 24px;
                                background:linear-gradient(135deg,#00a8ff,#6c5ce7);
                                color:white;
                                text-decoration:none;
                                border-radius:8px;
                                font-weight:bold;
                        ">
                            Restablecer contraseña
                        </a>

                        <p style="
                            margin-top:30px;
                            font-size:12px;
                            color:gray;
                        ">
                            Si no solicitaste este cambio, puedes ignorar este correo.
                        </p>

                    </div>

                </div>
            """.formatted(link);

            helper.setText(html, true);

            mailSender.send(mimeMessage);

            System.out.println("Correo enviado a: " + to);

        } catch (Exception e) {

            System.err.println("Error enviando correo");

            e.printStackTrace();
        }
    }

    @Transactional
    @Override
    public void sendEmailOrdenProgramada(String to, OrdenMantenimiento orden) {

        DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        NumberFormat formatoMoneda =
            NumberFormat.getInstance(Locale.of("es", "CL"));

        try {

            String fechaProgramada =
                orden.getFechaProgramada().format(formatter);

            String fechaTermino =
                orden.getFechaTermino().format(formatter);

            String valorHora =
                formatoMoneda.format(
                    orden.getValorHoraProveedor()
                );

            String costo =
                formatoMoneda.format(
                    orden.getCostoManoObraEstimadasProveedor()
                );

            MimeMessage mimeMessage = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("Soporte SVUA <" + mailFrom + ">");

            helper.setTo(to);

            helper.setSubject(
                    "Mantención programada - Orden #" + orden.getId());

            String html = """
                <div style="
                    font-family: Arial, sans-serif;
                    background-color: #f5f6fa;
                    padding: 30px;
                ">

                    <div style="
                        max-width: 700px;
                        margin: auto;
                        background: white;
                        border-radius: 12px;
                        padding: 30px;
                        box-shadow: 0 2px 10px rgba(0,0,0,0.08);
                    ">

                        <h2 style="
                            color:#2d3436;
                            margin-bottom:20px;
                        ">
                            🔧 Mantención Programada
                        </h2>

                        <p style="color:#2d3436;">
                            Estimado(a) <strong>%s</strong>,
                        </p>

                        <p style="color:#636e72;">
                            Le informamos que existe una orden de mantención
                            programada próxima a ejecutarse.
                        </p>

                        <table style="
                            width:100%%;
                            border-collapse:collapse;
                            margin-top:20px;
                        ">

                            <tr style="background:#f1f5f9;">
                                <th style="
                                    border:1px solid #ddd;
                                    padding:10px;
                                    text-align:left;
                                ">
                                    Campo
                                </th>

                                <th style="
                                    border:1px solid #ddd;
                                    padding:10px;
                                    text-align:left;
                                ">
                                    Valor
                                </th>
                            </tr>

                            <tr>
                                <td style="border:1px solid #ddd;padding:10px;">
                                    Código
                                </td>
                                <td style="border:1px solid #ddd;padding:10px;">
                                    %s
                                </td>
                            </tr>

                            <tr>
                                <td style="border:1px solid #ddd;padding:10px;">
                                    Título
                                </td>
                                <td style="border:1px solid #ddd;padding:10px;">
                                    %s
                                </td>
                            </tr>

                            <tr>
                                <td style="border:1px solid #ddd;padding:10px;">
                                    Estado
                                </td>
                                <td style="border:1px solid #ddd;padding:10px;">
                                    %s
                                </td>
                            </tr>

                            <tr>
                                <td style="border:1px solid #ddd;padding:10px;">
                                    Tipo Mantención
                                </td>
                                <td style="border:1px solid #ddd;padding:10px;">
                                    %s
                                </td>
                            </tr>

                            <tr>
                                <td style="border:1px solid #ddd;padding:10px;">
                                    Fecha Inicio
                                </td>
                                <td style="border:1px solid #ddd;padding:10px;">
                                    %s
                                </td>
                            </tr>

                            <tr>
                                <td style="border:1px solid #ddd;padding:10px;">
                                    Fecha Término
                                </td>
                                <td style="border:1px solid #ddd;padding:10px;">
                                    %s
                                </td>
                            </tr>

                            <tr>
                                <td style="border:1px solid #ddd;padding:10px;">
                                    Horas Estimadas
                                </td>
                                <td style="border:1px solid #ddd;padding:10px;">
                                    %s
                                </td>
                            </tr>

                            <tr>
                                <td style="border:1px solid #ddd;padding:10px;">
                                    Valor Horas
                                </td>
                                <td style="border:1px solid #ddd;padding:10px;">
                                    $%s
                                </td>
                            </tr>

                            <tr>
                                <td style="border:1px solid #ddd;padding:10px;">
                                    Costo Estimado
                                </td>
                                <td style="border:1px solid #ddd;padding:10px;">
                                    $%s
                                </td>
                            </tr>

                        </table>

                        <p style="
                            margin-top:25px;
                            color:#636e72;
                        ">
                            Favor considerar esta actividad dentro de la
                            planificación operativa correspondiente.
                        </p>

                        <p style="
                            margin-top:30px;
                            font-size:12px;
                            color:#95a5a6;
                        ">
                            Este es un correo automático generado por SVUA.
                        </p>

                    </div>

                </div>
                """
                .formatted(
                    orden.getProveedor().getContacto(),
                    orden.getId(),
                    orden.getTitulo(),
                    orden.getEstado(),
                    orden.getTipoMantenimiento(),
                    fechaProgramada,
                    fechaTermino,
                    orden.getHorasEstimadasProveedor(),
                    valorHora,
                    costo
                );

            helper.setText(html, true);

            mailSender.send(mimeMessage);

        } catch (Exception e) {

            e.printStackTrace();
        }

    }


}
