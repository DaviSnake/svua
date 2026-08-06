package cl.aracridav.svua.shared.service;

import cl.aracridav.svua.mantenimiento.orden.entity.OrdenMantenimiento;

public interface EmailService {

    public void sendResetEmail(String to, String link);

    public void sendEmailOrdenProgramada(String to, OrdenMantenimiento orden);

}
