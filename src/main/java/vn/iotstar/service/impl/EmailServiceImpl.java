package vn.iotstar.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import vn.iotstar.service.EmailService;

import java.io.UnsupportedEncodingException;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendOtpEmail(String toEmail, String otpCode, String userName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "PetStore");
            helper.setTo(toEmail);
            helper.setSubject("Mã xác thực đăng ký tài khoản - PetStore");

            String htmlContent = buildOtpEmailContent(otpCode, userName);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException("Không thể gửi email: " + e.getMessage());
        }
    }

    @Override
    public void sendWelcomeEmail(String toEmail, String userName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "PetStore");
            helper.setTo(toEmail);
            helper.setSubject("Chào mừng bạn đến với PetStore!");

            String htmlContent = buildWelcomeEmailContent(userName);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException("Không thể gửi email: " + e.getMessage());
        }
    }

    @Override
    public void sendForgotPasswordOtp(String toEmail, String otpCode, String userName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "PetStore");
            helper.setTo(toEmail);
            helper.setSubject("Mã xác thực đặt lại mật khẩu - PetStore");

            String htmlContent = buildForgotPasswordEmailContent(otpCode, userName);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException("Không thể gửi email: " + e.getMessage());
        }
    }

    private String buildOtpEmailContent(String otpCode, String userName) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }" +
                ".container { max-width: 600px; margin: 30px auto; background-color: #ffffff; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }" +
                ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; }" +
                ".header h1 { margin: 0; font-size: 28px; }" +
                ".content { padding: 40px 30px; }" +
                ".otp-box { background-color: #f8f9fa; border: 2px dashed #667eea; border-radius: 8px; padding: 20px; text-align: center; margin: 30px 0; }" +
                ".otp-code { font-size: 36px; font-weight: bold; color: #667eea; letter-spacing: 8px; margin: 10px 0; }" +
                ".info-box { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }" +
                ".footer { background-color: #f8f9fa; padding: 20px; text-align: center; color: #666; font-size: 12px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h1>🐾 PetStore</h1>" +
                "<p>Xác thực tài khoản của bạn</p>" +
                "</div>" +
                "<div class='content'>" +
                "<h2>Xin chào " + userName + "!</h2>" +
                "<p>Cảm ơn bạn đã đăng ký tài khoản tại PetStore. Để hoàn tất quá trình đăng ký, vui lòng sử dụng mã OTP bên dưới:</p>" +
                "<div class='otp-box'>" +
                "<p style='margin: 0; color: #666;'>Mã xác thực của bạn là:</p>" +
                "<div class='otp-code'>" + otpCode + "</div>" +
                "</div>" +
                "<div class='info-box'>" +
                "<p style='margin: 0;'><strong>⚠️ Lưu ý quan trọng:</strong></p>" +
                "<ul style='margin: 10px 0 0 0; padding-left: 20px;'>" +
                "<li>Mã OTP này có hiệu lực trong <strong>5 phút</strong></li>" +
                "<li>Không chia sẻ mã này với bất kỳ ai</li>" +
                "<li>Nếu bạn không yêu cầu đăng ký, vui lòng bỏ qua email này</li>" +
                "</ul>" +
                "</div>" +
                "<p>Nếu bạn gặp bất kỳ vấn đề nào, vui lòng liên hệ với chúng tôi qua email hỗ trợ.</p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>© 2025 PetStore. All rights reserved.</p>" +
                "<p>Email này được gửi tự động, vui lòng không phản hồi.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private String buildWelcomeEmailContent(String userName) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }" +
                ".container { max-width: 600px; margin: 30px auto; background-color: #ffffff; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }" +
                ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 40px; text-align: center; }" +
                ".header h1 { margin: 0; font-size: 32px; }" +
                ".content { padding: 40px 30px; }" +
                ".welcome-icon { font-size: 48px; text-align: center; margin: 20px 0; }" +
                ".btn { display: inline-block; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; }" +
                ".footer { background-color: #f8f9fa; padding: 20px; text-align: center; color: #666; font-size: 12px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h1>🎉 Chào mừng đến với PetStore!</h1>" +
                "</div>" +
                "<div class='content'>" +
                "<div class='welcome-icon'>🐾</div>" +
                "<h2>Xin chào " + userName + "!</h2>" +
                "<p>Tài khoản của bạn đã được kích hoạt thành công! Chúng tôi rất vui mừng chào đón bạn tham gia cộng đồng yêu thú cưng của PetStore.</p>" +
                "<p><strong>Với tài khoản của mình, bạn có thể:</strong></p>" +
                "<ul>" +
                "<li>🛍️ Mua sắm hàng ngàn sản phẩm chất lượng cho thú cưng</li>" +
                "<li>💝 Nhận ưu đãi và khuyến mãi độc quyền</li>" +
                "<li>📦 Theo dõi đơn hàng dễ dàng</li>" +
                "<li>⭐ Đánh giá và chia sẻ trải nghiệm</li>" +
                "</ul>" +
                "<div style='text-align: center;'>" +
                "<a href='#' class='btn'>Khám phá ngay</a>" +
                "</div>" +
                "<p style='margin-top: 30px;'>Nếu bạn có bất kỳ câu hỏi nào, đừng ngần ngại liên hệ với chúng tôi.</p>" +
                "<p>Chúc bạn có trải nghiệm mua sắm tuyệt vời!</p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>© 2025 PetStore. All rights reserved.</p>" +
                "<p>Email: support@petstore.com | Hotline: 1900-xxxx</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private String buildForgotPasswordEmailContent(String otpCode, String userName) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }" +
                ".container { max-width: 600px; margin: 30px auto; background-color: #ffffff; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }" +
                ".header { background: linear-gradient(135deg, #e74c3c 0%, #c0392b 100%); color: white; padding: 30px; text-align: center; }" +
                ".header h1 { margin: 0; font-size: 28px; }" +
                ".content { padding: 40px 30px; }" +
                ".otp-box { background-color: #f8f9fa; border: 2px dashed #e74c3c; border-radius: 8px; padding: 20px; text-align: center; margin: 30px 0; }" +
                ".otp-code { font-size: 36px; font-weight: bold; color: #e74c3c; letter-spacing: 8px; margin: 10px 0; }" +
                ".warning-box { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }" +
                ".security-box { background-color: #f8d7da; border-left: 4px solid #dc3545; padding: 15px; margin: 20px 0; }" +
                ".footer { background-color: #f8f9fa; padding: 20px; text-align: center; color: #666; font-size: 12px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h1>🔐 PetStore</h1>" +
                "<p>Đặt lại mật khẩu</p>" +
                "</div>" +
                "<div class='content'>" +
                "<h2>Xin chào " + userName + "!</h2>" +
                "<p>Chúng tôi đã nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn. Để tiếp tục, vui lòng sử dụng mã OTP bên dưới:</p>" +
                "<div class='otp-box'>" +
                "<p style='margin: 0; color: #666;'>Mã xác thực đặt lại mật khẩu:</p>" +
                "<div class='otp-code'>" + otpCode + "</div>" +
                "</div>" +
                "<div class='warning-box'>" +
                "<p style='margin: 0;'><strong>⚠️ Lưu ý quan trọng:</strong></p>" +
                "<ul style='margin: 10px 0 0 0; padding-left: 20px;'>" +
                "<li>Mã OTP này có hiệu lực trong <strong>5 phút</strong></li>" +
                "<li>Chỉ sử dụng mã này để đặt lại mật khẩu</li>" +
                "<li>Không chia sẻ mã này với bất kỳ ai</li>" +
                "</ul>" +
                "</div>" +
                "<div class='security-box'>" +
                "<p style='margin: 0;'><strong>🛡️ Bảo mật:</strong></p>" +
                "<ul style='margin: 10px 0 0 0; padding-left: 20px;'>" +
                "<li>Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này</li>" +
                "<li>Nếu có hoạt động đáng ngờ, hãy liên hệ với chúng tôi ngay lập tức</li>" +
                "</ul>" +
                "</div>" +
                "<p>Sau khi xác thực thành công, bạn sẽ có thể tạo mật khẩu mới cho tài khoản của mình.</p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>© 2025 PetStore. All rights reserved.</p>" +
                "<p>Email này được gửi tự động, vui lòng không phản hồi.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}