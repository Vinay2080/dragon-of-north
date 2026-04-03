package org.miniProjectTwo.DragonOfNorth.modules.user.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthCommonServices;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @InjectMocks
    private UserController userController;

    @Mock
    private AuthCommonServices authCommonServices;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Test
    void deleteCurrentUser_shouldReturnNoContent() {
        ResponseEntity<Void> result = userController.deleteCurrentUser(request, response, "device-1");

        verify(authCommonServices).deleteCurrentUser(any(), any());
        assertEquals(204, result.getStatusCode().value());
    }
}
