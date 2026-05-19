package org.miniProjectTwo.DragonOfNorth.modules.auth.service.impl;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.extern.slf4j.Slf4j;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.TotpService;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.springframework.stereotype.Service;

import java.util.Base64;

import static dev.samstevens.totp.code.HashingAlgorithm.SHA1;

@Service
@Slf4j
class TotpServiceImpl implements TotpService {

    @Override
    public String generateSecret() {
        return new DefaultSecretGenerator().generate();
    }

    @Override
    public String generateQrCode(String secret, AppUser appUser) {
        QrData qrData = new QrData.Builder()
                .label("Dragon-of-North:" + appUser.getEmail())
                .secret(secret)
                .issuer("Dragon-of-North")
                .algorithm(SHA1)
                .digits(6)
                .period(30)
                .build();
        QrGenerator qrGenerator = new ZxingPngQrGenerator();

        try {
            byte[] imageData = qrGenerator.generate(qrData);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageData);
        } catch (QrGenerationException e) {
            log.error("Error generating MFA QR code: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isValidCode(String secret, String code) {
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        return codeVerifier.isValidCode(secret, code);
    }
}
