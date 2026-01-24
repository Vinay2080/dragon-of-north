package org.miniProjectTwo.DragonOfNorth.impl.auth;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserSignUpRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.services.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.services.AuthenticationService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus.CREATED;
import static org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus.NOT_EXIST;
import static org.miniProjectTwo.DragonOfNorth.enums.IdentifierType.EMAIL;
import static org.miniProjectTwo.DragonOfNorth.exception.ErrorCode.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class EmailAuthenticationServiceImpl implements AuthenticationService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthCommonServices authCommonServices;


    @Override
    public IdentifierType supports() {
        return EMAIL;
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
        user.setAppUserStatus(CREATED);
        appUserRepository.save(user);
        return getUserStatus(request.identifier());
    }



    @Transactional
    @Override
    public AppUserStatusFinderResponse completeSignUp(String identifier) {
        AppUser appUser = appUserRepository.findByEmail(identifier).orElseThrow(() -> new BusinessException(USER_NOT_FOUND));
        authCommonServices.updateUserStatus(appUser.getAppUserStatus(), appUser);
        authCommonServices.assignDefaultRole(appUser);
        appUser.setEmailVerified(true);
        appUserRepository.save(appUser);
        return getUserStatus(identifier);
    }

}

