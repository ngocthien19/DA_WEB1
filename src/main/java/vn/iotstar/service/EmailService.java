package vn.iotstar.service;

public interface EmailService {
    void sendOtpEmail(String toEmail, String otpCode, String userName);
    void sendWelcomeEmail(String toEmail, String userName);
    void sendForgotPasswordOtp(String toEmail, String otpCode, String userName);
}