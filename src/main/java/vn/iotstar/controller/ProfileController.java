package vn.iotstar.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import vn.iotstar.entity.DanhMuc;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.model.ApiResponse;
import vn.iotstar.model.UserProfileDTO;
import vn.iotstar.repository.NguoiDungRepository;
import vn.iotstar.service.DanhMucService;
import vn.iotstar.util.JwtUtil;

import java.io.File;
import java.util.List;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private NguoiDungRepository nguoiDungRepository;
    
    @Autowired
    private DanhMucService danhMucService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public String showProfile(HttpServletRequest request, Model model) {
    	List<DanhMuc> danhMucs = danhMucService.findAllActiveCategories();
        model.addAttribute("danhMucs", danhMucs);
        try {
            // Lấy token từ request
            String token = extractJwtFromRequest(request);
            
            if (token == null) {
                // Nếu không có token, chuyển hướng đến login
                return "redirect:/login?error=unauthorized";
            }
            
            if (!jwtUtil.validateJwtToken(token)) {
                // Token không hợp lệ
                return "redirect:/login?error=invalid_token";
            }
            
            String email = jwtUtil.getUserNameFromJwtToken(token);
            NguoiDung user = nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            model.addAttribute("user", user);
            model.addAttribute("activeMenu", "user");
            
        } catch (Exception e) {
            System.err.println("Error loading profile: " + e.getMessage());
            return "redirect:/login?error=system_error";
        }
        
        return "web/profile";
    }

    // API endpoint to get user profile data
    @GetMapping("/api/user/profile")
    @ResponseBody
    public ResponseEntity<ApiResponse<UserProfileDTO>> getUserProfile(HttpServletRequest request) {
        try {
            String token = extractJwtFromRequest(request);
            
            if (token == null || !jwtUtil.validateJwtToken(token)) {
                return ResponseEntity.status(401)
                    .body(ApiResponse.error("Unauthorized: Token không hợp lệ"));
            }
            
            String email = jwtUtil.getUserNameFromJwtToken(token);
            NguoiDung user = nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Convert entity to DTO to avoid JSON serialization issues
            UserProfileDTO userDTO = convertToDTO(user);
            
            return ResponseEntity.ok(ApiResponse.success(userDTO));
            
        } catch (Exception e) {
            System.err.println("Error getting user profile: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body(ApiResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    // API endpoint to update user profile
    @PutMapping("/api/user/update")
    @ResponseBody
    public ResponseEntity<ApiResponse<UserProfileDTO>> updateProfile(
            @RequestBody UpdateProfileRequest request,
            HttpServletRequest httpRequest) {
        try {
            String token = extractJwtFromRequest(httpRequest);
            
            if (token == null || !jwtUtil.validateJwtToken(token)) {
                return ResponseEntity.status(401)
                    .body(ApiResponse.error("Unauthorized: Token không hợp lệ"));
            }
            
            String email = jwtUtil.getUserNameFromJwtToken(token);
            NguoiDung user = nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Validate số điện thoại nếu có
            if (request.getSdt() != null && !request.getSdt().trim().isEmpty()) {
                String sdt = request.getSdt().trim();
                if (!sdt.matches("^0[0-9]{9}$")) {
                    return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Số điện thoại phải bắt đầu bằng số 0 và có đúng 10 chữ số"));
                }
                user.setSdt(sdt);
            } else {
                user.setSdt(null);
            }
            
            // Cập nhật thông tin khác
            if (request.getTenNguoiDung() != null && !request.getTenNguoiDung().trim().isEmpty()) {
                user.setTenNguoiDung(request.getTenNguoiDung().trim());
            }
            
            if (request.getDiaChi() != null) {
                user.setDiaChi(request.getDiaChi().trim());
            }
            
            NguoiDung savedUser = nguoiDungRepository.save(user);
            UserProfileDTO userDTO = convertToDTO(savedUser);
            
            return ResponseEntity.ok(ApiResponse.success(userDTO));
            
        } catch (Exception e) {
            System.err.println("Error updating profile: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body(ApiResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    // API endpoint to change password
    @PutMapping("/api/user/change-password")
    @ResponseBody
    public ResponseEntity<ApiResponse<String>> changePassword(
            @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {
        try {
            String token = extractJwtFromRequest(httpRequest);
            
            if (token == null || !jwtUtil.validateJwtToken(token)) {
                return ResponseEntity.status(401)
                    .body(ApiResponse.error("Unauthorized: Token không hợp lệ"));
            }
            
            String email = jwtUtil.getUserNameFromJwtToken(token);
            NguoiDung user = nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Kiểm tra mật khẩu hiện tại
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getMatKhau())) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Mật khẩu hiện tại không đúng"));
            }
            
            // Cập nhật mật khẩu mới
            user.setMatKhau(passwordEncoder.encode(request.getNewPassword()));
            nguoiDungRepository.save(user);
            
            return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công"));
            
        } catch (Exception e) {
            System.err.println("Error changing password: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body(ApiResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    // API endpoint to upload avatar
    @PostMapping("/api/user/upload-avatar")
    @ResponseBody
    public ResponseEntity<ApiResponse<String>> uploadAvatar(
            @RequestParam("avatar") MultipartFile file,
            HttpServletRequest httpRequest) {
        try {
            System.out.println("=== UPLOAD AVATAR START ===");
            
            String token = extractJwtFromRequest(httpRequest);
            
            if (token == null || !jwtUtil.validateJwtToken(token)) {
                System.out.println("Token validation failed");
                return ResponseEntity.status(401)
                    .body(ApiResponse.error("Unauthorized: Token không hợp lệ"));
            }
            
            String email = jwtUtil.getUserNameFromJwtToken(token);
            System.out.println("User email from token: " + email);
            
            NguoiDung user = nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            System.out.println("Found user: " + user.getMaNguoiDung() + " - " + user.getTenNguoiDung());
            System.out.println("Current avatar: " + user.getHinhAnh());
            
            // Validate file
            if (file.isEmpty()) {
                System.out.println("File is empty");
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Vui lòng chọn file ảnh"));
            }
            
            System.out.println("File info - Name: " + file.getOriginalFilename() + 
                             ", Size: " + file.getSize() + " bytes" + 
                             ", ContentType: " + file.getContentType());
            
            // Check file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                System.out.println("Invalid file type: " + contentType);
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File phải là ảnh (jpg, png, gif)"));
            }
            
            // Save file
            String fileName = saveUploadedFile(file, user.getMaNguoiDung());
            System.out.println("File saved with name: " + fileName);
            
            // Update user avatar in database
            String oldAvatar = user.getHinhAnh();
            user.setHinhAnh(fileName);
            NguoiDung savedUser = nguoiDungRepository.save(user);
            
            System.out.println("Database updated - Old avatar: " + oldAvatar + ", New avatar: " + savedUser.getHinhAnh());
            System.out.println("=== UPLOAD AVATAR SUCCESS ===");
            
            return ResponseEntity.ok(ApiResponse.success("Upload ảnh thành công"));
            
        } catch (Exception e) {
            System.err.println("=== UPLOAD AVATAR ERROR ===");
            System.err.println("Error uploading avatar: " + e.getMessage());
            e.printStackTrace();
            System.err.println("=== END ERROR ===");
            return ResponseEntity.status(500)
                .body(ApiResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    // Helper method to convert entity to DTO
    private UserProfileDTO convertToDTO(NguoiDung user) {
        UserProfileDTO.VaiTroDTO vaiTroDTO = null;
        if (user.getVaiTro() != null) {
            vaiTroDTO = UserProfileDTO.VaiTroDTO.builder()
                .maVaiTro(user.getVaiTro().getMaVaiTro())
                .tenVaiTro(user.getVaiTro().getTenVaiTro())
                .build();
        }
        
        return UserProfileDTO.builder()
            .maNguoiDung(user.getMaNguoiDung())
            .tenNguoiDung(user.getTenNguoiDung())
            .email(user.getEmail())
            .sdt(user.getSdt())
            .diaChi(user.getDiaChi())
            .hinhAnh(user.getHinhAnh())
            .trangThai(user.getTrangThai())
            .vaiTro(vaiTroDTO)
            .build();
    }

    // Helper method to extract JWT token from request
    private String extractJwtFromRequest(HttpServletRequest request) {
        // Try to get from Authorization header first
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        // Try to get from cookie
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("jwtToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        return null;
    }

    // Helper method to save uploaded file
    private String saveUploadedFile(MultipartFile file, Integer userId) throws Exception {
        System.out.println("=== SAVE FILE START ===");
        
        // Use absolute path to project uploads folder
        String projectPath = System.getProperty("user.dir");
        String uploadDir = projectPath + File.separator + "uploads" + File.separator + "users" + File.separator;
        
        System.out.println("Project path: " + projectPath);
        System.out.println("Upload directory: " + uploadDir);
        
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            System.out.println("Directory does not exist, creating: " + uploadDir);
            boolean created = dir.mkdirs();
            if (!created) {
                System.err.println("Failed to create directory: " + uploadDir);
                throw new Exception("Không thể tạo thư mục uploads/users: " + uploadDir);
            }
            System.out.println("Directory created successfully");
        } else {
            System.out.println("Directory already exists: " + uploadDir);
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new Exception("Tên file không hợp lệ");
        }
        
        // Check if file has extension
        if (!originalFilename.contains(".")) {
            throw new Exception("File phải có phần mở rộng");
        }
        
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = "user_" + userId + "_" + System.currentTimeMillis() + extension;
        
        System.out.println("Original filename: " + originalFilename);
        System.out.println("Generated filename: " + fileName);
        
        File dest = new File(dir, fileName);
        System.out.println("Destination file path: " + dest.getAbsolutePath());
        
        try {
            file.transferTo(dest);
            
            // Verify file was saved
            if (dest.exists() && dest.length() > 0) {
                System.out.println("File saved successfully - Size: " + dest.length() + " bytes");
                System.out.println("=== SAVE FILE SUCCESS ===");
                return fileName;
            } else {
                throw new Exception("File không được lưu hoặc có kích thước 0 bytes");
            }
        } catch (Exception e) {
            System.err.println("Error saving file: " + e.getMessage());
            throw new Exception("Lỗi khi lưu file: " + e.getMessage());
        }
    }

    // Request DTOs
    public static class UpdateProfileRequest {
        private String tenNguoiDung;
        private String sdt;
        private String diaChi;
        
        // Getters and Setters
        public String getTenNguoiDung() { return tenNguoiDung; }
        public void setTenNguoiDung(String tenNguoiDung) { this.tenNguoiDung = tenNguoiDung; }
        public String getSdt() { return sdt; }
        public void setSdt(String sdt) { this.sdt = sdt; }
        public String getDiaChi() { return diaChi; }
        public void setDiaChi(String diaChi) { this.diaChi = diaChi; }
    }

    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;
        
        // Getters and Setters
        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}