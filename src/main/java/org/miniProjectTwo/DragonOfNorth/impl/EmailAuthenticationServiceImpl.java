package org.miniProjectTwo.DragonOfNorth.impl;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserSignUpRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.enums.OtpPurpose;
import org.miniProjectTwo.DragonOfNorth.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.services.AuthenticationService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus.*;
import static org.miniProjectTwo.DragonOfNorth.enums.OtpPurpose.*;
import static org.miniProjectTwo.DragonOfNorth.exception.ErrorCode.*;

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
                .orElseGet(() -> new AppUserStatusFinderResponse(NOT_EXIST));
    }

    @Override
    public AppUserStatusFinderResponse signUpUser(AppUserSignUpRequest request) {
        AppUser user = new AppUser();
        user.setEmail(request.identifier());
        user.setPassword(passwordEncoder.encode(request.password()));
        repository.save(user);
        updateStatusById(user.getId(), CREATED);
        return getUserStatus(request.identifier());
    }

    @Override
    public void updateStatusById(UUID userId, AppUserStatus appUserStatus) {

        int rowsUpdated = repository.updateUserStatusById(userId, appUserStatus);
        if (rowsUpdated == 0) {
            throw new BusinessException(USER_NOT_FOUND);
        }
    }

    @Transactional
    @Override
    public void updateStatusByIdentifier(String email, OtpPurpose otpPurpose) {
        AppUser appUser = repository.findByEmail(email).orElseThrow(()->new BusinessException(USER_NOT_FOUND));
        if (otpPurpose == SIGNUP){
            appUser.setAppUserStatus(VERIFIED);
        }
    }


}
