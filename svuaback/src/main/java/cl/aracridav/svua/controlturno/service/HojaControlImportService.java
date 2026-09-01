package cl.aracridav.svua.controlturno.service;

import org.springframework.web.multipart.MultipartFile;

import cl.aracridav.svua.controlturno.dto.response.ImportHojaControlResponse;

public interface HojaControlImportService {

    ImportHojaControlResponse importar(MultipartFile archivo);

}
