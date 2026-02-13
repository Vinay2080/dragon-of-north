package org.miniProjectTwo.DragonOfNorth.enums;

import org.miniProjectTwo.DragonOfNorth.dto.otp.request.EmailOtpRequest;
import org.miniProjectTwo.DragonOfNorth.resolver.AuthenticationServiceResolver;

/**
 * User identifier types supporting multi-method authentication.
 * Determines validation rules, OTP delivery channels, and authentication service
 * routing. EMAIL uses SES, PHONE uses SMS. Critical for AuthenticationServiceResolver
 * to delegate to appropriate authentication handlers.
 *
 * @see AuthenticationServiceResolver for type-based routing
 * @see EmailOtpRequest and PhoneOtpRequest for validation patterns
 */
public enum IdentifierType {
    EMAIL,
    PHONE
}
