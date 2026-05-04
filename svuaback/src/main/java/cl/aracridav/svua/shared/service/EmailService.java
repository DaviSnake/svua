package cl.aracridav.svua.shared.service;

public interface EmailService {

    public void sendResetEmail(String to, String link);

}
