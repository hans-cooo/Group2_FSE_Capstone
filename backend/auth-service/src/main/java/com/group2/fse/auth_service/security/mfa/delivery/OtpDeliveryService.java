package com.group2.fse.auth_service.security.mfa.delivery;

/**
 * Service for delivering multi-factor one-time passwords across multiple
 * channels (SMS, EMAIL, TOTP) with resilient fallback logging.
 */
public interface OtpDeliveryService {

    /**
     * Dispatches the OTP to the specified channel and destination.
     *
     * @param username The authenticated username
     * @param destination Phone number, email address, or Authenticator App label
     * @param channel SMS, EMAIL, or TOTP
     * @param code The 6-digit one-time password
     * @param totpUri Optional otpauth:// URI if TOTP channel
     */
    void deliverOtp(String username, String destination, String channel, String code, String totpUri);
}
