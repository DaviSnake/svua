package cl.aracridav.svua.auth.dto.response;

import lombok.*;

@Data
@Builder
public class AuthLoginResponse {

    private String accessToken;
    private String refreshToken;

}
