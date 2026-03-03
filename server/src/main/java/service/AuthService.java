package service;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class AuthService {
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public static String generateNewToken() {
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }

    public static String hashPassword(String providedClearTextPassword) {
        String hashedPassword = encoder.encode(providedClearTextPassword);
        return hashedPassword;
    }

    public static boolean verifyPassword(String providedClearTextPassword, String hashedPassword) {
        return encoder.matches(providedClearTextPassword, hashedPassword);
    }
}
