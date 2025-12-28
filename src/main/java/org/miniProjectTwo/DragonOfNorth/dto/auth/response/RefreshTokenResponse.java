package org.miniProjectTwo.DragonOfNorth.dto.auth.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenResponse {
    private String accessToken;
    private String tokenType;
}
