package org.miniProjectTwo.DragonOfNorth.impl;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserSignUpRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.exception.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.services.AuthenticationService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailAuthenticationServiceImpl implements AuthenticationService {

    private final AppUserRepository repository;
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
        return repository
                .findAppUserStatusByEmail(identifier).map(AppUserStatusFinderResponse::new)
                .orElseGet(() -> new AppUserStatusFinderResponse(AppUserStatus.NOT_EXIST));
    }

    @Override
    public AppUserStatusFinderResponse signUpUser(AppUserSignUpRequest request) {
        AppUser user = new AppUser();
        user.setEmail(request.identifier());
        user.setPassword(passwordEncoder.encode(request.password()));
        repository.save(user);
        updateStatus(user.getId(), AppUserStatus.CREATED);
        return getUserStatus(request.identifier());
    }

    @Override
    public void updateStatus(UUID userId, AppUserStatus appUserStatus) {

        int rowsUpdated = repository.updateUserStatusById(userId, appUserStatus);
        if (rowsUpdated == 0) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

    }

}
