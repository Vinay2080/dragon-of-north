package org.miniProjectTwo.DragonOfNorth.impl.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.services.AuthCommonServices;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.miniProjectTwo.DragonOfNorth.enums.IdentifierType.EMAIL;

@ExtendWith(MockitoExtension.class)
class EmailAuthenticationServiceImplTest {

    @InjectMocks
    private EmailAuthenticationServiceImpl emailAuthenticationService;

    @Mock
    private AppUserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthCommonServices authCommonServices;

    private final String email = "test@mockito.com";

    private final String password = "encoded@Password123";

    @Test
    void support_ShouldReturnEMAIL_whenCalled(){
        //act
        IdentifierType identifierType = emailAuthenticationService.supports();

        //assert
        assertEquals(EMAIL, identifierType, "support method should return type EMAIL");
    }

}