package org.miniProjectTwo.DragonOfNorth.modules.auth.service;

import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;

public interface TotpService {

    String generateSecret();

    String generateQrCode(String secret, AppUser appUser);

    boolean isValidCode(String secret, String code);
}
