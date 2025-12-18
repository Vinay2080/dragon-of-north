package org.miniProjectTwo.DragonOfNorth.services;

import org.miniProjectTwo.DragonOfNorth.dto.auth.request.IdentifierEmail;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;

public interface AuthenticationService {
     AppUserStatus emailStatusIdentifier(IdentifierEmail identifierEmail);

}
