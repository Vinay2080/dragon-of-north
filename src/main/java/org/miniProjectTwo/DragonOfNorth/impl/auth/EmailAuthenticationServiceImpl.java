package org.miniProjectTwo.DragonOfNorth.impl.auth;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserSignUpRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.enums.RoleName;
import org.miniProjectTwo.DragonOfNorth.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.exception.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.model.Role;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.repositories.RoleRepository;
import org.miniProjectTwo.DragonOfNorth.services.AuthenticationService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

import static org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus.*;
import static org.miniProjectTwo.DragonOfNorth.exception.ErrorCode.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class EmailAuthenticationServiceImpl implements AuthenticationService {

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public IdentifierType supports() {
        return IdentifierType.EMAIL;
    }

    /**
     * Finds user status or returns nonexistence status
     */
    @Override
    public AppUserStatusFinderResponse getUserStatus(String identifier) {
        return appUserRepository
                .findAppUserStatusByEmail(identifier).map(AppUserStatusFinderResponse::new)
                .orElseGet(() -> new AppUserStatusFinderResponse(NOT_EXIST));
    }

    @Override
    public AppUserStatusFinderResponse signUpUser(AppUserSignUpRequest request) {
        AppUser user = new AppUser();
        user.setEmail(request.identifier());
        user.setPassword(passwordEncoder.encode(request.password()));
        appUserRepository.save(user);
        updateStatusById(user.getId(), CREATED);
        return getUserStatus(request.identifier());
    }

    @Override
    public void updateStatusById(UUID userId, AppUserStatus appUserStatus) {

        int rowsUpdated = appUserRepository.updateUserStatusById(userId, appUserStatus);
        if (rowsUpdated == 0) {
            throw new BusinessException(USER_NOT_FOUND);
        }
    }

    @Override
    public void assignDefaultRole(AppUser appUser) {
        if (!appUser.hasAnyRoles()) {
            Role userRole = roleRepository.findByRoleName(RoleName.USER)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND, RoleName.USER.toString()));
            appUser.setRoles(Set.of(userRole));
        }
    }

    @Override
    public void updateUserStatus(AppUserStatus appUserStatus, AppUser appUser) {
        if (appUser.getAppUserStatus() == VERIFIED) {
            throw new BusinessException(ErrorCode.USER_ALREADY_VERIFIED);
        }

        if (appUserStatus == CREATED) {
            appUser.setAppUserStatus(VERIFIED);
        } else {
            throw new BusinessException(ErrorCode.STATUS_MISMATCH, CREATED.toString());
        }
    }

    @Transactional
    @Override
    public AppUserStatusFinderResponse completeSignUp(String identifier) {
        AppUser appUser = appUserRepository.findByEmail(identifier).orElseThrow(() -> new BusinessException(USER_NOT_FOUND));
        updateUserStatus(appUser.getAppUserStatus(), appUser);
        assignDefaultRole(appUser);
        appUserRepository.save(appUser);
        return getUserStatus(identifier);
    }


}

