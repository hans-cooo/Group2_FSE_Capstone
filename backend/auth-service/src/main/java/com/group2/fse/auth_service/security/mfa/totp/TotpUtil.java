package com.group2.fse.auth_service.security.mfa.totp;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Standard RFC 6238 Time-Based One-Time Password (TOTP) utility.
 * Generates Base32 secrets and calculates/verifies 6-digit rolling codes
 * compatible with Google Authenticator, Microsoft Authenticator, Authy, etc.
 */
public final class TotpUtil {

    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;
    private static final int MOD_DIVISOR = 1_000_000;
    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private TotpUtil() {
        // Utility class
    }

    /**
     * Generates a secure random 16-byte (128-bit) Base32 secret key.
     */
    public static String generateSecret() {
        byte[] buffer = new byte[16];
        SECURE_RANDOM.nextBytes(buffer);
        return encodeBase32(buffer);
    }

    /**
     * Generates the current 6-digit TOTP code for the given Base32 secret.
     */
    public static String generateCurrentCode(String base32Secret) {
        long currentWindow = System.currentTimeMillis() / 1000 / TIME_STEP_SECONDS;
        return generateCodeForWindow(base32Secret, currentWindow);
    }

    /**
     * Verifies a 6-digit TOTP code against the Base32 secret, allowing a +/- 1 window
     * drift (previous 30s, current 30s, next 30s) to account for client/server clock skew.
     */
    public static boolean verifyCode(String base32Secret, String inputCode) {
        if (base32Secret == null || inputCode == null || inputCode.length() != CODE_DIGITS) {
            return false;
        }

        long currentWindow = System.currentTimeMillis() / 1000 / TIME_STEP_SECONDS;

        // Check window: current, -1 (previous 30s), +1 (next 30s)
        for (long window = currentWindow - 1; window <= currentWindow + 1; window++) {
            String expectedCode = generateCodeForWindow(base32Secret, window);
            if (expectedCode.equals(inputCode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generates an otpauth:// URI suitable for rendering as a QR code for authenticator apps.
     */
    public static String getOtpAuthUri(String issuer, String accountName, String base32Secret) {
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
                issuer, accountName, base32Secret, issuer, CODE_DIGITS, TIME_STEP_SECONDS);
    }

    private static String generateCodeForWindow(String base32Secret, long window) {
        try {
            byte[] keyBytes = decodeBase32(base32Secret);
            byte[] data = ByteBuffer.allocate(8).putLong(window).array();

            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(keyBytes, HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(data);

            // Dynamic truncation (RFC 4226)
            int offset = hash[hash.length - 1] & 0x0F;
            long truncatedHash = 0;
            for (int i = 0; i < 4; ++i) {
                truncatedHash <<= 8;
                truncatedHash |= (hash[offset + i] & 0xFF);
            }
            truncatedHash &= 0x7FFFFFFF;
            truncatedHash %= MOD_DIVISOR;

            return String.format("%0" + CODE_DIGITS + "d", truncatedHash);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to calculate TOTP HMAC-SHA1 hash", e);
        }
    }

    private static String encodeBase32(byte[] data) {
        StringBuilder result = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                bitsLeft -= 5;
                result.append(BASE32_CHARS.charAt((buffer >> bitsLeft) & 0x1F));
            }
        }
        if (bitsLeft > 0) {
            buffer <<= (5 - bitsLeft);
            result.append(BASE32_CHARS.charAt(buffer & 0x1F));
        }
        return result.toString();
    }

    private static byte[] decodeBase32(String base32) {
        String clean = base32.toUpperCase().replaceAll("[^A-Z2-7]", "");
        byte[] bytes = new byte[clean.length() * 5 / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int index = 0;
        for (char c : clean.toCharArray()) {
            int val = BASE32_CHARS.indexOf(c);
            if (val < 0) {
                continue;
            }
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bitsLeft -= 8;
                bytes[index++] = (byte) ((buffer >> bitsLeft) & 0xFF);
            }
        }
        return Arrays.copyOf(bytes, index);
    }
}
