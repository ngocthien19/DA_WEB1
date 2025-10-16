package vn.iotstar.service;

import vn.iotstar.model.ApiResponse;
import vn.iotstar.model.OtpRequest;

public interface OtpService {
    ApiResponse<String> generateAndSendOtp(OtpRequest otpRequest);
    ApiResponse<String> verifyOtpAndRegister(String email, String otpCode);
    void cleanupExpiredTokens();
    
    // Thêm methods cho chức năng quên mật khẩu
    ApiResponse<String> sendForgotPasswordOtp(String email);
    ApiResponse<String> verifyForgotPasswordOtp(String email, String otpCode);
    ApiResponse<String> resetPassword(String email, String otpCode, String newPassword);
    ApiResponse<String> verifyForgotPasswordOtpAndRedirect(String email, String otpCode);
}