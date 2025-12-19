package org.miniProjectTwo.DragonOfNorth.impl;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserStatusFinderRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.services.AuthenticationService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailAuthenticationServiceImpl implements AuthenticationService {

    private final AppUserRepository repository;

    /**
     * Finds user status or returns nonexistence status
     */
    @Override
    public AppUserStatusFinderResponse statusFinder(AppUserStatusFinderRequest request) {
        return repository
                .findAppUserStatusByEmail(request.email()).map(AppUserStatusFinderResponse::new)
                .orElseGet(() -> new AppUserStatusFinderResponse(AppUserStatus.NOT_EXIST));
    }
}
