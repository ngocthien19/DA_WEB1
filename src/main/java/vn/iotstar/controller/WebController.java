package vn.iotstar.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.iotstar.service.AuthService;

@Controller
public class WebController {

    @Autowired
    private AuthService authService;

    @GetMapping("/login")
    public String loginPage() {
        return "web/login";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam String email, 
                            @RequestParam String matKhau, 
                            Model model) {
        try {
            // Thử đăng nhập
            boolean isValid = authService.validateUser(email, matKhau);
            if (isValid) {
                // Redirect to home page after successful login
                return "redirect:/";
            } else {
                model.addAttribute("error", "Email hoặc mật khẩu không đúng");
                return "web/login";
            }
        } catch (Exception e) {
            model.addAttribute("error", "Đã xảy ra lỗi khi đăng nhập");
            return "web/login";
        }
    }

    @GetMapping("/register")
    public String registerPage() {
        return "web/register";
    }

    @GetMapping("/verify-otp")
    public String verifyOtpPage() {
        return "web/verify-otp";
    }
    
    // === ROUTES CHO CHỨC NĂNG QUÊN MẬT KHẨU ===
    
    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "web/forgot-password";
    }
    
    @GetMapping("/verify-forgot-password-otp")
    public String verifyForgotPasswordOtpPage() {
        return "web/verify-forgot-password-otp";
    }
    
    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam String email, 
                                   @RequestParam String otp, 
                                   Model model) {
        model.addAttribute("email", email);
        model.addAttribute("otp", otp);
        return "web/reset-password";
    }
}