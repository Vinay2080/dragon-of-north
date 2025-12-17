package org.miniProjectTwo.DragonOfNorth.impl;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.IdentifierEmail;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.mapper.IdentifierEmailMapper;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.services.AuthenticationService;
import org.springframework.stereotype.Service;

import static org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus.CREATED;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final AppUserRepository appUserRepository;
    private final IdentifierEmailMapper identifierEmailMapper;

    @Override
    public AppUserStatus emailStatusIdentifier(IdentifierEmail identifierEmail) {
        if (!appUserRepository.existsByEmail(identifierEmail.email())) {
            final AppUser appUser = identifierEmailMapper.toEntity(identifierEmail);
            appUser.setAppUserStatus(CREATED);
            appUserRepository.save(appUser);
            return CREATED;
        }
        return AppUserStatus.ACTIVE;
    }
}
