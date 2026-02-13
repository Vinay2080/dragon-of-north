package org.miniProjectTwo.DragonOfNorth.resolver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.serviceInterfaces.AuthenticationService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.miniProjectTwo.DragonOfNorth.enums.IdentifierType.EMAIL;
import static org.miniProjectTwo.DragonOfNorth.enums.IdentifierType.PHONE;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceResolverTest {

    private AuthenticationServiceResolver resolver;

    @Mock
    private AuthenticationService emailService;

    @Mock
    private AuthenticationService phoneService;

    @BeforeEach
    void setUp() {
        when(emailService.supports()).thenReturn(EMAIL);
        when(phoneService.supports()).thenReturn(PHONE);
        
        resolver = new AuthenticationServiceResolver(List.of(emailService, phoneService));
    }

    @Test
    void resolve_ShouldReturnEmailService_WhenTypeIsEmailAndIdentifierIsValid() {
        // arrange
        String identifier = "test@example.com";

        // act
        AuthenticationService result = resolver.resolve(identifier, EMAIL);

        // assert
        assertEquals(emailService, result);
    }

    @Test
    void resolve_ShouldReturnPhoneService_WhenTypeIsPhoneAndIdentifierIsValid() {
        // arrange
        String identifier = "9876543210";

        // act
        AuthenticationService result = resolver.resolve(identifier, PHONE);

        // assert
        assertEquals(phoneService, result);
    }

    @Test
    void resolve_ShouldThrowException_WhenTypeIsEmailAndIdentifierIsInvalid() {
        // arrange
        String identifier = "invalid-email";

        // act & assert
        BusinessException exception = assertThrows(BusinessException.class, () -> resolver.resolve(identifier, EMAIL));
        assertEquals(ErrorCode.IDENTIFIER_MISMATCH, exception.getErrorCode());
    }

    @Test
    void resolve_ShouldThrowException_WhenTypeIsPhoneAndIdentifierIsInvalid() {
        // arrange
        String identifier = "12345"; // too short and doesn't start with 6-9

        // act & assert
        BusinessException exception = assertThrows(BusinessException.class, () -> resolver.resolve(identifier, PHONE));
        assertEquals(ErrorCode.IDENTIFIER_MISMATCH, exception.getErrorCode());
    }

    @Test
    void isMail_ShouldReturnTrue_ForValidEmail() {
        assertTrue(resolver.isMail("user@domain.com"));
        assertTrue(resolver.isMail("user.name@domain.co.in"));
    }

    @Test
    void isMail_ShouldReturnFalse_ForInvalidEmail() {
        assertFalse(resolver.isMail("plain-address"));
        assertFalse(resolver.isMail("@missinguser.com"));
        assertFalse(resolver.isMail("user@.com"));
    }
}
