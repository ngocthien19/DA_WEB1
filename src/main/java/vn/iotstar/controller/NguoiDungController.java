package vn.iotstar.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.model.ApiResponse;
import vn.iotstar.repository.NguoiDungRepository;
import vn.iotstar.service.UserDetailsImpl;
import vn.iotstar.util.JwtUtil;

@Controller
@RequestMapping("/api/user")
public class NguoiDungController {
    
    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private JwtUtil jwtUtil;

    // API để lấy thông tin profile (cho AJAX calls)
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<NguoiDung>> getProfile(HttpServletRequest request) {
        try {
            String token = getJwtFromRequest(request);
            if (token == null || !jwtUtil.validateJwtToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized"));
            }

            String email = jwtUtil.getUserNameFromJwtToken(token);
            NguoiDung user = nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

            return ResponseEntity.ok(ApiResponse.success(user));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    // Thêm endpoint để hiển thị trang profile HTML
    @GetMapping("/profile-page")
    public String showProfilePage(HttpServletRequest request, Model model) {
        try {
            String token = getJwtFromRequest(request);
            if (token != null && jwtUtil.validateJwtToken(token)) {
                String email = jwtUtil.getUserNameFromJwtToken(token);
                NguoiDung user = nguoiDungRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
                
                model.addAttribute("user", user);
                model.addAttribute("activeMenu", "user");
            }
        } catch (Exception e) {
            // Nếu có lỗi, vẫn trả về trang profile nhưng không có user data
            System.err.println("Error loading user profile: " + e.getMessage());
        }
        
        return "user/profile"; // Trả về template profile.html
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        // Thử lấy token từ localStorage thông qua parameter (cho web)
        String tokenParam = request.getParameter("token");
        if (StringUtils.hasText(tokenParam)) {
            return tokenParam;
        }
        
        return null;
    }
}