package org.miniProjectTwo.DragonOfNorth.resolver;

import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.services.AuthenticationService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.miniProjectTwo.DragonOfNorth.enums.IdentifierType.EMAIL;
import static org.miniProjectTwo.DragonOfNorth.enums.IdentifierType.PHONE;
import static org.miniProjectTwo.DragonOfNorth.exception.ErrorCode.IDENTIFIER_MISMATCH;

@Service
public class AuthenticationServiceResolver {

    private final Map<IdentifierType, AuthenticationService> serviceMap;

    public AuthenticationServiceResolver(List<AuthenticationService> services) {
        this.serviceMap = services
                .stream()
                .collect(Collectors.toMap(
                        AuthenticationService::supports,
                        Function.identity()
                ));
    }

    public AuthenticationService resolve(String identifier, IdentifierType type) {
        if (type == EMAIL && !isMail(identifier)) {
            throw new BusinessException(IDENTIFIER_MISMATCH, EMAIL);
        }
        if (type == PHONE && !isPhone(identifier)) {
            throw new BusinessException(IDENTIFIER_MISMATCH, PHONE);
        }
        return serviceMap.get(type);
    }

    public boolean isMail(String identifier) {
        return identifier.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private boolean isPhone(String identifier) {
        return identifier.matches("[6-9]\\d{9}$");
    }
}
