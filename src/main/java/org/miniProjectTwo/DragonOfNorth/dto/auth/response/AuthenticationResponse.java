package org.miniProjectTwo.DragonOfNorth.dto.auth.response;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter
public class AuthenticationResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
}
