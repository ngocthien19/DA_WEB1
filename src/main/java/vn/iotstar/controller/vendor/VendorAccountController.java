package vn.iotstar.controller.vendor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import vn.iotstar.entity.CuaHang;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.model.ApiResponse;
import vn.iotstar.model.UserProfileDTO;
import vn.iotstar.model.VendorProfileDTO;
import vn.iotstar.repository.CuaHangRepository;
import vn.iotstar.repository.NguoiDungRepository;
import vn.iotstar.service.CuaHangService;
import vn.iotstar.util.JwtUtil;

import java.io.File;
import java.util.List;

@Controller
@RequestMapping("/vendor")
public class VendorAccountController {

    @Autowired
    private NguoiDungRepository nguoiDungRepository;
    
    @Autowired
    private CuaHangRepository cuaHangRepository;
    
    @Autowired
    private CuaHangService cuaHangService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/account")
    public String showVendorAccount(HttpServletRequest request, Model model) {
        try {
            String token = extractJwtFromRequest(request);
            
            if (token == null) {
                return "redirect:/login?error=unauthorized";
            }
            
            if (!jwtUtil.validateJwtToken(token)) {
                return "redirect:/login?error=invalid_token";
            }
            
            String email = jwtUtil.getUserNameFromJwtToken(token);
            NguoiDung user = nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Lấy thông tin cửa hàng của vendor
            List<CuaHang> cuaHangs = cuaHangRepository.findByNguoiDung(user);
            CuaHang cuaHang = cuaHangs != null && !cuaHangs.isEmpty() ? cuaHangs.get(0) : null;
            
            model.addAttribute("user", user);
            model.addAttribute("cuaHang", cuaHang);
            model.addAttribute("activeMenu", "account");
            
        } catch (Exception e) {
            System.err.println("Error loading vendor account: " + e.getMessage());
            return "redirect:/login?error=system_error";
        }
        
        return "vendor/account";
    }

    // API endpoint to get vendor profile data
    @GetMapping("/api/account/profile")
    @ResponseBody
    public ResponseEntity<ApiResponse<VendorProfileDTO>> getVendorProfile(HttpServletRequest request) {
        try {
            String token = extractJwtFromRequest(request);
            
            if (token == null || !jwtUtil.validateJwtToken(token)) {
                return ResponseEntity.status(401)
                    .body(ApiResponse.error("Unauthorized: Token không hợp lệ"));
            }
            
            String email = jwtUtil.getUserNameFromJwtToken(token);
            NguoiDung user = nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Lấy thông tin cửa hàng
            List<CuaHang> cuaHangs = cuaHangRepository.findByNguoiDung(user);
            CuaHang cuaHang = cuaHangs != null && !cuaHangs.isEmpty() ? cuaHangs.get(0) : null;
            
            VendorProfileDTO vendorDTO = convertToVendorDTO(user, cuaHang);
            
            return ResponseEntity.ok(ApiResponse.success(vendorDTO));
            
        } catch (Exception e) {
            System.err.println("Error getting vendor profile: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body(ApiResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    // API endpoint to update vendor profile
    @PutMapping("/api/account/update-profile")
    @ResponseBody
    public ResponseEntity<ApiResponse<VendorProfileDTO>> updateVendorProfile(
            @RequestBody UpdateVendorProfileRequest request,
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
            
            // Cập nhật thông tin cá nhân
            if (request.getTenNguoiDung() != null && !request.getTenNguoiDung().trim().isEmpty()) {
                user.setTenNguoiDung(request.getTenNguoiDung().trim());
            }
            
            if (request.getDiaChi() != null) {
                user.setDiaChi(request.getDiaChi().trim());
            }
            
            NguoiDung savedUser = nguoiDungRepository.save(user);
            
            // Lấy lại thông tin cửa hàng sau khi cập nhật
            List<CuaHang> cuaHangs = cuaHangRepository.findByNguoiDung(savedUser);
            CuaHang cuaHang = cuaHangs != null && !cuaHangs.isEmpty() ? cuaHangs.get(0) : null;
            
            VendorProfileDTO vendorDTO = convertToVendorDTO(savedUser, cuaHang);
            
            return ResponseEntity.ok(ApiResponse.success(vendorDTO));
            
        } catch (Exception e) {
            System.err.println("Error updating vendor profile: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body(ApiResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    // API endpoint to update store information
    @PutMapping("/api/account/update-store")
    @ResponseBody
    public ResponseEntity<ApiResponse<VendorProfileDTO>> updateStoreInfo(
            @RequestBody UpdateStoreRequest request,
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
            
            // Lấy thông tin cửa hàng
            List<CuaHang> cuaHangs = cuaHangRepository.findByNguoiDung(user);
            if (cuaHangs == null || cuaHangs.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Bạn chưa có cửa hàng"));
            }
            
            CuaHang cuaHang = cuaHangs.get(0);
            
            // Cập nhật thông tin cửa hàng
            if (request.getTenCuaHang() != null && !request.getTenCuaHang().trim().isEmpty()) {
                cuaHang.setTenCuaHang(request.getTenCuaHang().trim());
            }
            
            if (request.getMoTa() != null) {
                cuaHang.setMoTa(request.getMoTa().trim());
            }
            
            if (request.getDiaChi() != null && !request.getDiaChi().trim().isEmpty()) {
                cuaHang.setDiaChi(request.getDiaChi().trim());
            }
            
            if (request.getSoDienThoai() != null && !request.getSoDienThoai().trim().isEmpty()) {
                String sdt = request.getSoDienThoai().trim();
                if (!sdt.matches("(84|0[3|5|7|8|9])+([0-9]{8})\\b")) {
                    return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Số điện thoại cửa hàng không hợp lệ"));
                }
                cuaHang.setSoDienThoai(sdt);
            }
            
            if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
                cuaHang.setEmail(request.getEmail().trim());
            }
            
            if (request.getNamThanhLap() != null) {
                cuaHang.setNamThanhLap(request.getNamThanhLap());
            }
            
            CuaHang savedStore = cuaHangRepository.save(cuaHang);
            VendorProfileDTO vendorDTO = convertToVendorDTO(user, savedStore);
            
            return ResponseEntity.ok(ApiResponse.success(vendorDTO));
            
        } catch (Exception e) {
            System.err.println("Error updating store info: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body(ApiResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    // API endpoint to change password
    @PutMapping("/api/account/change-password")
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
    @PostMapping("/api/account/upload-avatar")
    @ResponseBody
    public ResponseEntity<ApiResponse<String>> uploadAvatar(
            @RequestParam("avatar") MultipartFile file,
            HttpServletRequest httpRequest) {
        try {
            System.out.println("=== VENDOR UPLOAD AVATAR START ===");
            
            String token = extractJwtFromRequest(httpRequest);
            
            if (token == null || !jwtUtil.validateJwtToken(token)) {
                System.out.println("Token validation failed");
                return ResponseEntity.status(401)
                    .body(ApiResponse.error("Unauthorized: Token không hợp lệ"));
            }
            
            String email = jwtUtil.getUserNameFromJwtToken(token);
            System.out.println("Vendor email from token: " + email);
            
            NguoiDung user = nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            System.out.println("Found vendor: " + user.getMaNguoiDung() + " - " + user.getTenNguoiDung());
            
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
            user.setHinhAnh(fileName);
            NguoiDung savedUser = nguoiDungRepository.save(user);
            
            System.out.println("Database updated - New avatar: " + savedUser.getHinhAnh());
            System.out.println("=== VENDOR UPLOAD AVATAR SUCCESS ===");
            
            return ResponseEntity.ok(ApiResponse.success("Upload ảnh thành công"));
            
        } catch (Exception e) {
            System.err.println("=== VENDOR UPLOAD AVATAR ERROR ===");
            System.err.println("Error uploading avatar: " + e.getMessage());
            e.printStackTrace();
            System.err.println("=== END ERROR ===");
            return ResponseEntity.status(500)
                .body(ApiResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    // API endpoint to upload store image
    @PostMapping("/api/account/upload-store-image")
    @ResponseBody
    public ResponseEntity<ApiResponse<String>> uploadStoreImage(
            @RequestParam("storeImage") MultipartFile file,
            HttpServletRequest httpRequest) {
        try {
            System.out.println("=== VENDOR UPLOAD STORE IMAGE START ===");
            
            String token = extractJwtFromRequest(httpRequest);
            
            if (token == null || !jwtUtil.validateJwtToken(token)) {
                return ResponseEntity.status(401)
                    .body(ApiResponse.error("Unauthorized: Token không hợp lệ"));
            }
            
            String email = jwtUtil.getUserNameFromJwtToken(token);
            NguoiDung user = nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Lấy thông tin cửa hàng
            List<CuaHang> cuaHangs = cuaHangRepository.findByNguoiDung(user);
            if (cuaHangs == null || cuaHangs.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Bạn chưa có cửa hàng"));
            }
            
            CuaHang cuaHang = cuaHangs.get(0);
            
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Vui lòng chọn file ảnh"));
            }
            
            // Check file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File phải là ảnh (jpg, png, gif)"));
            }
            
            // Save file
            String fileName = saveStoreImageFile(file, cuaHang.getMaCuaHang());
            
            // Update store image in database
            cuaHang.setHinhAnh(fileName);
            cuaHangRepository.save(cuaHang);
            
            System.out.println("=== VENDOR UPLOAD STORE IMAGE SUCCESS ===");
            
            return ResponseEntity.ok(ApiResponse.success("Upload ảnh cửa hàng thành công"));
            
        } catch (Exception e) {
            System.err.println("Error uploading store image: " + e.getMessage());
            return ResponseEntity.status(500)
                .body(ApiResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    // Helper method to convert to Vendor DTO
    private VendorProfileDTO convertToVendorDTO(NguoiDung user, CuaHang cuaHang) {
        UserProfileDTO.VaiTroDTO vaiTroDTO = null;
        if (user.getVaiTro() != null) {
            vaiTroDTO = UserProfileDTO.VaiTroDTO.builder()
                .maVaiTro(user.getVaiTro().getMaVaiTro())
                .tenVaiTro(user.getVaiTro().getTenVaiTro())
                .build();
        }
        
        VendorProfileDTO.StoreInfoDTO storeInfoDTO = null;
        if (cuaHang != null) {
            storeInfoDTO = VendorProfileDTO.StoreInfoDTO.builder()
                .maCuaHang(cuaHang.getMaCuaHang())
                .tenCuaHang(cuaHang.getTenCuaHang())
                .moTa(cuaHang.getMoTa())
                .diaChi(cuaHang.getDiaChi())
                .soDienThoai(cuaHang.getSoDienThoai())
                .email(cuaHang.getEmail())
                .hinhAnh(cuaHang.getHinhAnh())
                .namThanhLap(cuaHang.getNamThanhLap())
                .danhGiaTrungBinh(cuaHang.getDanhGiaTrungBinh())
                .soLuongDanhGia(cuaHang.getSoLuongDanhGia())
                .trangThai(cuaHang.getTrangThai())
                .ngayTao(cuaHang.getNgayTao())
                .build();
        }
        
        return VendorProfileDTO.builder()
            .maNguoiDung(user.getMaNguoiDung())
            .tenNguoiDung(user.getTenNguoiDung())
            .email(user.getEmail())
            .sdt(user.getSdt())
            .diaChi(user.getDiaChi())
            .hinhAnh(user.getHinhAnh())
            .trangThai(user.getTrangThai())
            .vaiTro(vaiTroDTO)
            .storeInfo(storeInfoDTO)
            .build();
    }

    // Helper method to extract JWT token from request
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("jwtToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        return null;
    }

    // Helper method to save uploaded avatar file
    private String saveUploadedFile(MultipartFile file, Integer userId) throws Exception {
        String projectPath = System.getProperty("user.dir");
        String uploadDir = projectPath + File.separator + "uploads" + File.separator + "users" + File.separator;
        
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                throw new Exception("Không thể tạo thư mục uploads/users: " + uploadDir);
            }
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new Exception("Tên file không hợp lệ");
        }
        
        if (!originalFilename.contains(".")) {
            throw new Exception("File phải có phần mở rộng");
        }
        
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = "vendor_" + userId + "_" + System.currentTimeMillis() + extension;
        
        File dest = new File(dir, fileName);
        
        try {
            file.transferTo(dest);
            
            if (dest.exists() && dest.length() > 0) {
                return fileName;
            } else {
                throw new Exception("File không được lưu hoặc có kích thước 0 bytes");
            }
        } catch (Exception e) {
            throw new Exception("Lỗi khi lưu file: " + e.getMessage());
        }
    }

    // Helper method to save store image file
    private String saveStoreImageFile(MultipartFile file, Integer storeId) throws Exception {
        String projectPath = System.getProperty("user.dir");
        String uploadDir = projectPath + File.separator + "uploads" + File.separator + "stores" + File.separator;
        
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                throw new Exception("Không thể tạo thư mục uploads/stores: " + uploadDir);
            }
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new Exception("Tên file không hợp lệ");
        }
        
        if (!originalFilename.contains(".")) {
            throw new Exception("File phải có phần mở rộng");
        }
        
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = "store_" + storeId + "_" + System.currentTimeMillis() + extension;
        
        File dest = new File(dir, fileName);
        
        try {
            file.transferTo(dest);
            
            if (dest.exists() && dest.length() > 0) {
                return fileName;
            } else {
                throw new Exception("File không được lưu hoặc có kích thước 0 bytes");
            }
        } catch (Exception e) {
            throw new Exception("Lỗi khi lưu file: " + e.getMessage());
        }
    }

    // Request DTOs
    public static class UpdateVendorProfileRequest {
        private String tenNguoiDung;
        private String sdt;
        private String diaChi;
        
        public String getTenNguoiDung() { return tenNguoiDung; }
        public void setTenNguoiDung(String tenNguoiDung) { this.tenNguoiDung = tenNguoiDung; }
        public String getSdt() { return sdt; }
        public void setSdt(String sdt) { this.sdt = sdt; }
        public String getDiaChi() { return diaChi; }
        public void setDiaChi(String diaChi) { this.diaChi = diaChi; }
    }

    public static class UpdateStoreRequest {
        private String tenCuaHang;
        private String moTa;
        private String diaChi;
        private String soDienThoai;
        private String email;
        private Integer namThanhLap;
        
        public String getTenCuaHang() { return tenCuaHang; }
        public void setTenCuaHang(String tenCuaHang) { this.tenCuaHang = tenCuaHang; }
        public String getMoTa() { return moTa; }
        public void setMoTa(String moTa) { this.moTa = moTa; }
        public String getDiaChi() { return diaChi; }
        public void setDiaChi(String diaChi) { this.diaChi = diaChi; }
        public String getSoDienThoai() { return soDienThoai; }
        public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public Integer getNamThanhLap() { return namThanhLap; }
        public void setNamThanhLap(Integer namThanhLap) { this.namThanhLap = namThanhLap; }
    }

    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;
        
        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}