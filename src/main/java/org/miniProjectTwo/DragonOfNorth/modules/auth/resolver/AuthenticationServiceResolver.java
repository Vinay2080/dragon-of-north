package org.miniProjectTwo.DragonOfNorth.modules.auth.resolver;

import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthenticationService;
import org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode.IDENTIFIER_MISMATCH;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType.EMAIL;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType.PHONE;

/**
 * Resolves identifier-specific authentication service implementations.
 */
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

    /**
     * Validates identifier/type consistency and returns the matching service.
     */
    public AuthenticationService resolve(String identifier, IdentifierType type) {
        if (type == EMAIL && !isMail(identifier)) {
            throw new BusinessException(IDENTIFIER_MISMATCH, EMAIL);
        }
        if (type == PHONE && !isPhone(identifier)) {
            throw new BusinessException(IDENTIFIER_MISMATCH, PHONE);
        }
        return serviceMap.get(type);
    }

    /**
     * Returns whether the identifier matches email format.
     */
    public boolean isMail(String identifier) {
        return identifier.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private boolean isPhone(String identifier) {
        return identifier.matches("[6-9]\\d{9}$");
    }
}
