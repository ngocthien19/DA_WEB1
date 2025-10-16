package vn.iotstar.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.entity.OtpToken;
import vn.iotstar.entity.VaiTro;
import vn.iotstar.model.ApiResponse;
import vn.iotstar.model.OtpRequest;
import vn.iotstar.repository.NguoiDungRepository;
import vn.iotstar.repository.OtpTokenRepository;
import vn.iotstar.repository.VaiTroRepository;
import vn.iotstar.service.EmailService;
import vn.iotstar.service.OtpService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OtpServiceImpl implements OtpService {

    @Autowired
    private OtpTokenRepository otpTokenRepository;

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private VaiTroRepository vaiTroRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;

    @Override
    @Transactional
    public ApiResponse<String> generateAndSendOtp(OtpRequest otpRequest) {
        // Kiểm tra email đã tồn tại chưa
        if (nguoiDungRepository.existsByEmail(otpRequest.getEmail())) {
            return ApiResponse.error("Email đã được sử dụng");
        }

        try {
            // Xóa OTP cũ nếu có (chỉ cho đăng ký)
            otpTokenRepository.deleteByEmailAndOtpType(otpRequest.getEmail(), "REGISTER");

            // Tạo mã OTP ngẫu nhiên
            String otpCode = generateOtpCode();

            // Tạo token mới
            OtpToken otpToken = OtpToken.builder()
                    .email(otpRequest.getEmail())
                    .otpCode(otpCode)
                    .expiryTime(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                    .createdAt(LocalDateTime.now())
                    .tenNguoiDung(otpRequest.getTenNguoiDung())
                    .matKhau(passwordEncoder.encode(otpRequest.getMatKhau()))
                    .sdt(otpRequest.getSdt())
                    .diaChi(otpRequest.getDiaChi())
                    .otpType("REGISTER")
                    .isUsed(false)
                    .attemptCount(0)
                    .build();

            otpTokenRepository.save(otpToken);

            // Gửi email
            emailService.sendOtpEmail(
                    otpRequest.getEmail(),
                    otpCode,
                    otpRequest.getTenNguoiDung()
            );

            return ApiResponse.success("Mã OTP đã được gửi đến email của bạn. Vui lòng kiểm tra hộp thư.");

        } catch (Exception e) {
            return ApiResponse.error("Không thể gửi mã OTP. Vui lòng thử lại sau.");
        }
    }

    @Override
    @Transactional
    public ApiResponse<String> verifyOtpAndRegister(String email, String otpCode) {
        // Tìm OTP token
        Optional<OtpToken> optionalToken = otpTokenRepository
                .findByEmailAndIsUsedFalse(email);

        if (optionalToken.isEmpty()) {
            return ApiResponse.error("Không tìm thấy mã OTP. Vui lòng yêu cầu gửi lại.");
        }

        OtpToken otpToken = optionalToken.get();

        // Kiểm tra số lần thử
        if (!otpToken.canAttempt()) {
            otpTokenRepository.delete(otpToken);
            return ApiResponse.error("Bạn đã nhập sai quá nhiều lần. Vui lòng yêu cầu gửi lại mã OTP.");
        }

        // Kiểm tra OTP hết hạn
        if (otpToken.isExpired()) {
            otpTokenRepository.delete(otpToken);
            return ApiResponse.error("Mã OTP đã hết hạn. Vui lòng yêu cầu gửi lại.");
        }

        // Kiểm tra mã OTP
        if (!otpToken.getOtpCode().equals(otpCode)) {
            otpToken.incrementAttempt();
            otpTokenRepository.save(otpToken);
            int remainingAttempts = 5 - otpToken.getAttemptCount();
            return ApiResponse.error("Mã OTP không chính xác. Bạn còn " + remainingAttempts + " lần thử.");
        }

        try {
            // Tạo người dùng mới
            NguoiDung nguoiDung = new NguoiDung();
            nguoiDung.setTenNguoiDung(otpToken.getTenNguoiDung());
            nguoiDung.setEmail(otpToken.getEmail());
            nguoiDung.setMatKhau(otpToken.getMatKhau()); // Đã được mã hóa
            nguoiDung.setSdt(otpToken.getSdt());
            nguoiDung.setDiaChi(otpToken.getDiaChi());
            nguoiDung.setTrangThai("Hoạt động");

            // Gán vai trò USER
            Optional<VaiTro> userRole = vaiTroRepository.findById("USER");
            if (userRole.isEmpty()) {
                return ApiResponse.error("Vai trò mặc định không tồn tại");
            }
            nguoiDung.setVaiTro(userRole.get());

            // Lưu người dùng
            nguoiDungRepository.save(nguoiDung);

            // Đánh dấu OTP đã sử dụng
            otpToken.setIsUsed(true);
            otpTokenRepository.save(otpToken);

            // Gửi email chào mừng
            try {
                emailService.sendWelcomeEmail(nguoiDung.getEmail(), nguoiDung.getTenNguoiDung());
            } catch (Exception e) {
                // Không làm gián đoạn quá trình đăng ký nếu email chào mừng thất bại
            }

            return ApiResponse.success("Đăng ký tài khoản thành công!");

        } catch (Exception e) {
            return ApiResponse.error("Có lỗi xảy ra trong quá trình đăng ký. Vui lòng thử lại.");
        }
    }

    @Override
    @Scheduled(cron = "0 0 * * * *") // Chạy mỗi giờ
    @Transactional
    public void cleanupExpiredTokens() {
        otpTokenRepository.deleteExpiredAndUsedTokens(LocalDateTime.now());
    }

    @Override
    public ApiResponse<String> sendForgotPasswordOtp(String email) {
        // Kiểm tra email có tồn tại trong hệ thống không
        Optional<NguoiDung> nguoiDungOpt = nguoiDungRepository.findByEmail(email);
        if (nguoiDungOpt.isEmpty()) {
            return ApiResponse.error("Email không tồn tại trong hệ thống");
        }

        NguoiDung nguoiDung = nguoiDungOpt.get();
        
        // Tạo mã OTP ngẫu nhiên
        String otpCode = generateOtpCode();
        
        // Tạo và lưu OTP token trong method riêng có transaction
        boolean tokenCreated = createOtpToken(email, otpCode, nguoiDung.getTenNguoiDung());
        
        if (!tokenCreated) {
            return ApiResponse.error("Có lỗi xảy ra khi tạo mã OTP. Vui lòng thử lại sau.");
        }

        // Gửi email NGOÀI transaction để tránh rollback
        return sendEmailSafely(email, otpCode, nguoiDung.getTenNguoiDung());
    }
    
    /**
     * Method riêng để tạo OTP token với transaction
     */
    @Transactional
    private boolean createOtpToken(String email, String otpCode, String tenNguoiDung) {
        try {
            // Xóa OTP cũ nếu có (cho forgot password)
            otpTokenRepository.deleteByEmailAndOtpType(email, "FORGOT_PASSWORD");

            // Tạo token mới cho forgot password - set giá trị mặc định cho các trường bắt buộc
            OtpToken otpToken = OtpToken.builder()
                    .email(email)
                    .otpCode(otpCode)
                    .expiryTime(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                    .createdAt(LocalDateTime.now())
                    .tenNguoiDung(tenNguoiDung)
                    .otpType("FORGOT_PASSWORD") // Phân biệt loại OTP
                    .isUsed(false)
                    .attemptCount(0)
                    // Set giá trị mặc định cho các trường có constraint NOT NULL
                    .matKhau("") // Empty string thay vì NULL
                    .sdt("") // Empty string thay vì NULL
                    .diaChi("") // Empty string thay vì NULL
                    .build();

            // Lưu OTP token
            otpTokenRepository.save(otpToken);
            return true;
            
        } catch (Exception e) {
            System.err.println("Failed to create OTP token: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Method riêng để gửi email ngoài transaction
     */
    private ApiResponse<String> sendEmailSafely(String email, String otpCode, String userName) {
        try {
            emailService.sendForgotPasswordOtp(email, otpCode, userName);
            return ApiResponse.success("Mã OTP đã được gửi đến email của bạn. Vui lòng kiểm tra hộp thư.");
            
        } catch (Exception emailException) {
            // Log lỗi chi tiết
            System.err.println("Email sending failed for " + email + ": " + emailException.getMessage());
            emailException.printStackTrace();
            
            // Nếu email gửi thất bại, xóa OTP token đã tạo trong transaction riêng
            cleanupOtpToken(email);
            
            return ApiResponse.error("Không thể gửi email. Vui lòng kiểm tra cấu hình email và thử lại sau.");
        }
    }
    
    /**
     * Method cleanup OTP token trong transaction riêng
     */
    @Transactional
    private void cleanupOtpToken(String email) {
        try {
            otpTokenRepository.deleteByEmailAndOtpType(email, "FORGOT_PASSWORD");
        } catch (Exception cleanupException) {
            System.err.println("Failed to cleanup OTP token after email failure: " + cleanupException.getMessage());
        }
    }

    @Override
    @Transactional
    public ApiResponse<String> verifyForgotPasswordOtp(String email, String otpCode) {
        // Tìm OTP token cho forgot password
        Optional<OtpToken> optionalToken = otpTokenRepository
                .findByEmailAndOtpTypeAndIsUsedFalse(email, "FORGOT_PASSWORD");

        if (optionalToken.isEmpty()) {
            return ApiResponse.error("Không tìm thấy mã OTP. Vui lòng yêu cầu gửi lại.");
        }

        OtpToken otpToken = optionalToken.get();

        // Kiểm tra số lần thử
        if (!otpToken.canAttempt()) {
            otpTokenRepository.delete(otpToken);
            return ApiResponse.error("Bạn đã nhập sai quá nhiều lần. Vui lòng yêu cầu gửi lại mã OTP.");
        }

        // Kiểm tra OTP hết hạn
        if (otpToken.isExpired()) {
            otpTokenRepository.delete(otpToken);
            return ApiResponse.error("Mã OTP đã hết hạn. Vui lòng yêu cầu gửi lại.");
        }

        // Kiểm tra mã OTP
        if (!otpToken.getOtpCode().equals(otpCode)) {
            otpToken.incrementAttempt();
            otpTokenRepository.save(otpToken);
            int remainingAttempts = 5 - otpToken.getAttemptCount();
            return ApiResponse.error("Mã OTP không chính xác. Bạn còn " + remainingAttempts + " lần thử.");
        }

        return ApiResponse.success("Mã OTP hợp lệ. Bạn có thể đặt lại mật khẩu mới.");
    }

    @Override
    @Transactional
    public ApiResponse<String> resetPassword(String email, String otpCode, String newPassword) {
        // Tìm OTP token cho forgot password
        Optional<OtpToken> optionalToken = otpTokenRepository
                .findByEmailAndOtpTypeAndIsUsedFalse(email, "FORGOT_PASSWORD");

        if (optionalToken.isEmpty()) {
            return ApiResponse.error("Không tìm thấy mã OTP hợp lệ.");
        }

        OtpToken otpToken = optionalToken.get();

        // Kiểm tra OTP hết hạn
        if (otpToken.isExpired()) {
            otpTokenRepository.delete(otpToken);
            return ApiResponse.error("Mã OTP đã hết hạn. Vui lòng yêu cầu gửi lại.");
        }

        // Kiểm tra mã OTP
        if (!otpToken.getOtpCode().equals(otpCode)) {
            return ApiResponse.error("Mã OTP không chính xác.");
        }

        try {
            // Tìm người dùng và cập nhật mật khẩu
            Optional<NguoiDung> nguoiDungOpt = nguoiDungRepository.findByEmail(email);
            if (nguoiDungOpt.isEmpty()) {
                return ApiResponse.error("Không tìm thấy người dùng.");
            }

            NguoiDung nguoiDung = nguoiDungOpt.get();
            nguoiDung.setMatKhau(passwordEncoder.encode(newPassword));
            nguoiDungRepository.save(nguoiDung);

            // Đánh dấu OTP đã sử dụng
            otpToken.setIsUsed(true);
            otpTokenRepository.save(otpToken);

            return ApiResponse.success("Đặt lại mật khẩu thành công!");

        } catch (Exception e) {
            return ApiResponse.error("Có lỗi xảy ra khi đặt lại mật khẩu. Vui lòng thử lại.");
        }
    }

    private String generateOtpCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder();
        
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        
        return otp.toString();
    }
    
    @Override
    public ApiResponse<String> verifyForgotPasswordOtpAndRedirect(String email, String otpCode) {
        try {
            // Xác minh OTP
            ApiResponse<String> verifyResult = verifyForgotPasswordOtp(email, otpCode);
            
            if (verifyResult.isSuccess()) {
                // Nếu OTP hợp lệ, trả về success với thông tin để redirect
                String redirectUrl = "/reset-password?email=" + URLEncoder.encode(email, StandardCharsets.UTF_8) 
                        + "&otp=" + otpCode;
                return ApiResponse.success(redirectUrl);
            } else {
                return verifyResult;
            }
        } catch (Exception e) {
            return ApiResponse.error("Có lỗi xảy ra khi xác minh OTP");
        }
    }
}