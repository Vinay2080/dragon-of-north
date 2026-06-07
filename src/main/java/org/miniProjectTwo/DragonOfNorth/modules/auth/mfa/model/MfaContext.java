package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.model;

import java.util.Map;
import java.util.UUID;

/**
 * Contextual information about an MFA attempt, used for both challenge generation and verification.
 * <p>
 * This context is created after primary authentication and is used to inform MFA challenge generation and verification. It contains information about the session, device, and client environment to allow MFA providers to make informed decisions about which challenges to present and how to evaluate verification attempts. The {@code stepUp} flag indicates whether this MFA attempt is a step-up authentication (triggered by a high-risk action) or a standard MFA challenge during login. The {@code attributes} map allows for extensibility, enabling the inclusion of additional contextual information as needed by specific MFA providers or future features without requiring changes to the core MFA service or data model.
 *
 * @param stepUp     Indicates whether this MFA attempt is a step-up authentication (triggered by a high-risk action) or a standard MFA challenge during login.
 * @param sessionId  Unique identifier for the current session, used to track and manage MFA challenges within the session.
 * @param ipAddress  The IP address of the client making the authentication request, used for geolocation and risk assessment.
 * @param userAgent  The user agent string of the client, used for device fingerprinting and risk assessment.
 * @param deviceId   A unique identifier for the client device, used for device-specific risk assessment and to track device-specific MFA challenges.
 * @param attributes A map of additional contextual attributes, allowing for extensibility and the inclusion of custom data as needed by specific MFA providers or future features.
 */
public record MfaContext(
        boolean stepUp,
        UUID sessionId,
        String ipAddress,
        String userAgent,
        String deviceId,
        Map<String, Object> attributes
) {
    public static MfaContext empty() {
        return new MfaContext(false, null, null, null, null, Map.of());
    }
}
