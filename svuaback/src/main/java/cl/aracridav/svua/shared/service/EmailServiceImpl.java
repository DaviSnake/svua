package cl.aracridav.svua.shared.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailFrom;

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
}
