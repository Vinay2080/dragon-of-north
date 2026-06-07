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

    /**
     * Generates a new TOTP secret using the default secret generator provided by the TOTP library. The generated secret is a Base32-encoded string that can be used to set up TOTP-based MFA for a user. This method does not associate the generated secret with any user or persist it; it simply returns the raw secret string for use in the MFA setup process.
     *
     * @return A newly generated TOTP secret as a Base32-encoded string.
     */
    @Override
    public String generateSecret() {
        return new DefaultSecretGenerator().generate();
    }

    /**
     * Generates a QR code for setting up TOTP-based MFA for a user. The QR code contains the necessary information for the user's authenticator app to register the account.
     *
     * @param secret  the TOTP secret for the user
     * @param appUser the user for whom to generate the QR code
     * @return A data URL containing the QR code image, or null if an error occurs
     */
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

    /**
     * Validates a TOTP code against a secret. Checks if the provided code is valid for the given secret at the current time.
     *
     * @param secret the TOTP secret for the user
     * @param code   the TOTP code to validate
     * @return true if the code is valid, false otherwise
     */
    @Override
    public boolean isValidCode(String secret, String code) {
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        return codeVerifier.isValidCode(secret, code);
    }
}
