package cl.aracridav.svua.shared.service;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    public void sendResetEmail(String to, String link) {

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("SVUA <" + to + ">");

            helper.setTo(to);
            helper.setSubject("Recuperar contraseña");

            String html = """
                <div style="font-family: Arial, sans-serif; text-align: center; padding: 20px;">
                    <h2>Recuperar contraseña</h2>
                    <p>Haz clic en el botón para restablecer tu contraseña</p>

                    <a href="%s" 
                    style="
                        display:inline-block;
                        padding:12px 20px;
                        background:linear-gradient(135deg,#00a8ff,#6c5ce7);
                        color:white;
                        text-decoration:none;
                        border-radius:8px;
                        font-weight:bold;
                    ">
                    Restablecer contraseña
                    </a>

                    <p style="margin-top:20px; font-size:12px; color:gray;">
                        Si no solicitaste esto, puedes ignorar este correo.
                    </p>
                </div>
            """.formatted(link);

            helper.setText(html, true);

            mailSender.send(mimeMessage);

            System.out.println("Correo enviado a: " + to);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
