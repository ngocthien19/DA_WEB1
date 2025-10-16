package vn.iotstar.controller.admin;

import java.io.File;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.entity.VaiTro;
import vn.iotstar.model.NguoiDungModel;
import vn.iotstar.service.NguoiDungService;
import vn.iotstar.service.VaiTroService;
import vn.iotstar.service.impl.ExcelExportService;
import vn.iotstar.service.impl.UserDetailsServiceImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminNguoiDungController {

    @Autowired
    private NguoiDungService nguoiDungService;
    
    @Autowired
    private UserDetailsServiceImpl userDetailsService;
    
    @Autowired
    private VaiTroService vaiTroService;
    
    @Autowired
    private ExcelExportService excelExportService;

    /**
     * Hiển thị danh sách người dùng với phân trang, tìm kiếm và lọc theo vai trò
     */
    @GetMapping
    public String listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            Authentication authentication,
            Model model) {
        
        // Lấy thông tin user đang đăng nhập
        NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
        
        // Tạo Pageable với sắp xếp theo maNguoiDung giảm dần
        Pageable pageable = PageRequest.of(page, size, Sort.by("maNguoiDung").descending());
        
        // Tìm kiếm và lọc
        Page<NguoiDung> userPage = nguoiDungService.searchAndFilterUsers(keyword, role, pageable);
        
        // Lấy danh sách vai trò để hiển thị trong filter
        List<VaiTro> vaiTroList = vaiTroService.getAllVaiTro();
        
        long totalVendors = nguoiDungService.countUsersByRole("VENDOR");
        long totalShippers = nguoiDungService.countUsersByRole("SHIPPER");
        long totalUsers = nguoiDungService.countUsers(); // Sử dụng method mới
        long totalActive = nguoiDungService.countAllActiveUsers(); // Sử dụng method mới
        long totalInactive = nguoiDungService.countAllInactiveUsers(); // Sử dụng method mới

        model.addAttribute("user", currentUser);
        model.addAttribute("userPage", userPage);
        model.addAttribute("vaiTroList", vaiTroList);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("selectedRole", role != null ? role : "");

        // ================== TRUYỀN STATS MỚI VÀO MODEL ==================
        model.addAttribute("totalVendors", totalVendors);
        model.addAttribute("totalShippers", totalShippers);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalActive", totalActive);
        model.addAttribute("totalInactive", totalInactive);
        
        return "admin/users/users";
    }

    /**
     * Hiển thị form thêm người dùng mới
     */
    @GetMapping("/add")
    public String showAddForm(Authentication authentication, Model model) {
        NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
        List<VaiTro> vaiTroList = vaiTroService.getAllVaiTro();
        
        model.addAttribute("user", currentUser);
        model.addAttribute("nguoiDungModel", new NguoiDungModel());
        model.addAttribute("vaiTroList", vaiTroList);
        model.addAttribute("isEdit", false);
        
        return "admin/users/user-form";
    }

    /**
     * Xử lý thêm người dùng mới (AJAX)
     */
    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<?> addUser(
            @Validated(NguoiDungModel.Create.class) @RequestBody NguoiDungModel model,
            BindingResult result,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (result.hasErrors()) {
            response.put("success", false);
            response.put("message", "Vui lòng kiểm tra lại thông tin!");
            
            Map<String, String> errors = new HashMap<>();
            for (FieldError error : result.getFieldErrors()) {
                errors.put(error.getField(), error.getDefaultMessage());
            }
            response.put("errors", errors);
            
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            nguoiDungService.createUser(model);
            response.put("success", true);
            response.put("message", "Thêm người dùng thành công!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Xử lý thêm người dùng mới với upload ảnh (Form data) - FIXED VERSION
     */
    @PostMapping("/add-with-image")
    @ResponseBody
    public ResponseEntity<?> addUserWithImage(
            @RequestParam("tenNguoiDung") String tenNguoiDung,
            @RequestParam("email") String email,
            @RequestParam("matKhau") String matKhau,
            @RequestParam(value = "sdt", required = false) String sdt,
            @RequestParam(value = "diaChi", required = false) String diaChi,
            @RequestParam("maVaiTro") String maVaiTro,
            @RequestParam(value = "trangThai", defaultValue = "Hoạt động") String trangThai,
            @RequestParam(value = "hinhAnh", required = false) MultipartFile hinhAnh,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate input
            if (tenNguoiDung == null || tenNguoiDung.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Tên người dùng không được để trống");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (email == null || email.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Email không được để trống");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (matKhau == null || matKhau.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Mật khẩu không được để trống");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (maVaiTro == null || maVaiTro.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Vai trò không được để trống");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Validate image file if provided
            String fileName = null;
            if (hinhAnh != null && !hinhAnh.isEmpty()) {
                String contentType = hinhAnh.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    response.put("success", false);
                    response.put("message", "File phải là ảnh (jpg, png, gif)");
                    return ResponseEntity.badRequest().body(response);
                }
                
                // Validate file size (10MB)
                if (hinhAnh.getSize() > 10 * 1024 * 1024) {
                    response.put("success", false);
                    response.put("message", "Kích thước file không được vượt quá 10MB");
                    return ResponseEntity.badRequest().body(response);
                }
                
                // Save image file
                fileName = saveUserImage(hinhAnh);
            }
            
            // Create NguoiDungModel
            NguoiDungModel model = NguoiDungModel.builder()
                    .tenNguoiDung(tenNguoiDung.trim())
                    .email(email.trim())
                    .matKhau(matKhau)
                    .sdt(sdt != null ? sdt.trim() : null)
                    .diaChi(diaChi != null ? diaChi.trim() : null)
                    .maVaiTro(maVaiTro.trim())
                    .trangThai(trangThai)
                    .hinhAnh(fileName)
                    .build();
            
            nguoiDungService.createUser(model);
            
            response.put("success", true);
            response.put("message", "Thêm người dùng thành công!");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Hiển thị form chỉnh sửa người dùng
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(
            @PathVariable Integer id,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
        
        try {
            NguoiDung nguoiDung = nguoiDungService.getUserById(id)
                    .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
            
            List<VaiTro> vaiTroList = vaiTroService.getAllVaiTro();
            
            // Chuyển đổi entity sang model
            NguoiDungModel nguoiDungModel = NguoiDungModel.builder()
                    .maNguoiDung(nguoiDung.getMaNguoiDung())
                    .tenNguoiDung(nguoiDung.getTenNguoiDung())
                    .email(nguoiDung.getEmail())
                    .sdt(nguoiDung.getSdt())
                    .diaChi(nguoiDung.getDiaChi())
                    .maVaiTro(nguoiDung.getVaiTro().getMaVaiTro())
                    .tenVaiTro(nguoiDung.getVaiTro().getTenVaiTro())
                    .trangThai(nguoiDung.getTrangThai())
                    .hinhAnh(nguoiDung.getHinhAnh())
                    .build();
            
            model.addAttribute("user", currentUser);
            model.addAttribute("nguoiDung", nguoiDung);
            model.addAttribute("nguoiDungModel", nguoiDungModel);
            model.addAttribute("vaiTroList", vaiTroList);
            model.addAttribute("isEdit", true);
            
            return "admin/users/user-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/users/users";
        }
    }

    /**
     * Xử lý cập nhật người dùng với ảnh (Form data)
     */
    @PostMapping("/edit-with-image/{id}")
    @ResponseBody
    public ResponseEntity<?> updateUserWithImage(
            @PathVariable Integer id,
            @RequestParam("tenNguoiDung") String tenNguoiDung,
            @RequestParam("email") String email,
            @RequestParam(value = "matKhau", required = false) String matKhau,
            @RequestParam(value = "sdt", required = false) String sdt,
            @RequestParam(value = "diaChi", required = false) String diaChi,
            @RequestParam("maVaiTro") String maVaiTro,
            @RequestParam("trangThai") String trangThai,
            @RequestParam(value = "hinhAnh", required = false) MultipartFile hinhAnh,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate input
            if (tenNguoiDung == null || tenNguoiDung.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Tên người dùng không được để trống");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (email == null || email.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Email không được để trống");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (maVaiTro == null || maVaiTro.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Vai trò không được để trống");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Validate image file if provided
            String fileName = null;
            if (hinhAnh != null && !hinhAnh.isEmpty()) {
                String contentType = hinhAnh.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    response.put("success", false);
                    response.put("message", "File phải là ảnh (jpg, png, gif)");
                    return ResponseEntity.badRequest().body(response);
                }
                
                // Validate file size (10MB)
                if (hinhAnh.getSize() > 10 * 1024 * 1024) {
                    response.put("success", false);
                    response.put("message", "Kích thước file không được vượt quá 10MB");
                    return ResponseEntity.badRequest().body(response);
                }
                
                // Save image file
                fileName = saveUserImage(hinhAnh);
            }
            
            // Create NguoiDungModel
            NguoiDungModel model = NguoiDungModel.builder()
                    .tenNguoiDung(tenNguoiDung.trim())
                    .email(email.trim())
                    .matKhau(matKhau)
                    .sdt(sdt != null ? sdt.trim() : null)
                    .diaChi(diaChi != null ? diaChi.trim() : null)
                    .maVaiTro(maVaiTro.trim())
                    .trangThai(trangThai)
                    .hinhAnh(fileName)
                    .build();
            
            nguoiDungService.updateUser(id, model);
            
            response.put("success", true);
            response.put("message", "Cập nhật người dùng thành công!");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Xử lý cập nhật người dùng (AJAX)
     */
    @PostMapping("/edit/{id}")
    @ResponseBody
    public ResponseEntity<?> updateUser(
            @PathVariable Integer id,
            @Validated(NguoiDungModel.Update.class) @RequestBody NguoiDungModel model,
            BindingResult result,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (result.hasErrors()) {
            response.put("success", false);
            response.put("message", "Vui lòng kiểm tra lại thông tin!");
            
            Map<String, String> errors = new HashMap<>();
            for (FieldError error : result.getFieldErrors()) {
                errors.put(error.getField(), error.getDefaultMessage());
            }
            response.put("errors", errors);
            
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            nguoiDungService.updateUser(id, model);
            response.put("success", true);
            response.put("message", "Cập nhật người dùng thành công!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Hiển thị chi tiết người dùng
     */
    @GetMapping("/view/{id}")
    public String viewUser(
            @PathVariable Integer id,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
        
        try {
            NguoiDung nguoiDung = nguoiDungService.getUserById(id)
                    .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
            
            model.addAttribute("user", currentUser);
            model.addAttribute("nguoiDung", nguoiDung);
            
            return "admin/users/user-view";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/users/users";
        }
    }

    /**
     * Xóa người dùng (REST API)
     */
    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteUser(@PathVariable Integer id, Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Không cho phép xóa chính mình
            Integer currentUserId = userDetailsService.getCurrentUserId(authentication);
            if (id.equals(currentUserId)) {
                response.put("success", false);
                response.put("message", "Không thể xóa tài khoản của chính mình!");
                return ResponseEntity.badRequest().body(response);
            }
            
            nguoiDungService.deleteUser(id);
            response.put("success", true);
            response.put("message", "Xóa người dùng thành công");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Thay đổi trạng thái người dùng với lý do
     */
    @PostMapping("/change-status/{id}")
    @ResponseBody
    public ResponseEntity<?> changeStatus(
            @PathVariable Integer id,
            @RequestParam String status,
            @RequestParam(required = false) String lyDoKhoa,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Không cho phép thay đổi trạng thái chính mình
            Integer currentUserId = userDetailsService.getCurrentUserId(authentication);
            if (id.equals(currentUserId)) {
                response.put("success", false);
                response.put("message", "Không thể thay đổi trạng thái của chính mình!");
                return ResponseEntity.badRequest().body(response);
            }
            
            NguoiDung nguoiDung;
            if ("Khóa".equals(status)) {
                // Nếu khóa tài khoản, cần có lý do
                if (lyDoKhoa == null || lyDoKhoa.trim().isEmpty()) {
                    response.put("success", false);
                    response.put("message", "Vui lòng nhập lý do khóa tài khoản");
                    return ResponseEntity.badRequest().body(response);
                }
                nguoiDung = nguoiDungService.lockUser(id, lyDoKhoa.trim());
            } else {
                // Nếu mở khóa, không cần lý do
                nguoiDung = nguoiDungService.unlockUser(id);
            }
            
            response.put("success", true);
            response.put("message", "Thay đổi trạng thái thành công!");
            response.put("trangThai", nguoiDung.getTrangThai());
            response.put("lyDoKhoa", nguoiDung.getLyDoKhoa());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Thay đổi trạng thái hàng loạt người dùng với lý do
     */
    @PostMapping("/bulk-change-status")
    @ResponseBody
    public ResponseEntity<?> bulkChangeStatus(
            @RequestParam("ids") List<Integer> ids,
            @RequestParam String status,
            @RequestParam(required = false) String lyDoKhoa,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Không cho phép thay đổi trạng thái chính mình
            Integer currentUserId = userDetailsService.getCurrentUserId(authentication);
            
            if (ids == null || ids.isEmpty()) {
                response.put("success", false);
                response.put("message", "Vui lòng chọn ít nhất một người dùng để thay đổi trạng thái");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Nếu khóa tài khoản, cần có lý do
            if ("Khóa".equals(status) && (lyDoKhoa == null || lyDoKhoa.trim().isEmpty())) {
                response.put("success", false);
                response.put("message", "Vui lòng nhập lý do khóa tài khoản");
                return ResponseEntity.badRequest().body(response);
            }
            
            int successCount = 0;
            int errorCount = 0;
            List<String> errorMessages = new ArrayList<>();
            
            for (Integer id : ids) {
                try {
                    // Kiểm tra không cho phép thay đổi chính mình
                    if (id.equals(currentUserId)) {
                        errorCount++;
                        errorMessages.add("Không thể thay đổi trạng thái của chính mình (ID: " + id + ")");
                        continue;
                    }
                    
                    NguoiDung nguoiDung;
                    if ("Khóa".equals(status)) {
                        nguoiDung = nguoiDungService.lockUser(id, lyDoKhoa.trim());
                    } else {
                        nguoiDung = nguoiDungService.unlockUser(id);
                    }
                    successCount++;
                } catch (Exception e) {
                    errorCount++;
                    errorMessages.add("Lỗi khi cập nhật người dùng ID " + id + ": " + e.getMessage());
                }
            }
            
            response.put("success", true);
            response.put("message", "Đã cập nhật trạng thái cho " + successCount + " người dùng thành công");
            response.put("successCount", successCount);
            response.put("errorCount", errorCount);
            response.put("errors", errorMessages);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Xóa hàng loạt người dùng - ĐÃ SỬA LỖI
     */
    @PostMapping("/bulk-delete")
    @ResponseBody
    public ResponseEntity<?> bulkDelete(
            @RequestParam("ids") List<Integer> ids,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Không cho phép xóa chính mình
            Integer currentUserId = userDetailsService.getCurrentUserId(authentication);
            
            if (ids == null || ids.isEmpty()) {
                response.put("success", false);
                response.put("message", "Vui lòng chọn ít nhất một người dùng để xóa");
                return ResponseEntity.badRequest().body(response);
            }
            
            int successCount = 0;
            int errorCount = 0;
            List<String> errorMessages = new ArrayList<>(); // ĐÃ SỬA LỖI Ở ĐÂY
            
            for (Integer id : ids) {
                try {
                    // Kiểm tra không cho phép xóa chính mình
                    if (id.equals(currentUserId)) {
                        errorCount++;
                        errorMessages.add("Không thể xóa tài khoản của chính mình (ID: " + id + ")");
                        continue;
                    }
                    
                    nguoiDungService.deleteUser(id);
                    successCount++;
                } catch (Exception e) {
                    errorCount++;
                    errorMessages.add("Lỗi khi xóa người dùng ID " + id + ": " + e.getMessage());
                }
            }
            
            response.put("success", true);
            response.put("message", "Đã xóa " + successCount + " người dùng thành công");
            response.put("successCount", successCount);
            response.put("errorCount", errorCount);
            response.put("errors", errorMessages);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Export tất cả người dùng ra Excel
     */
    @PostMapping("/export-all")
    public void exportAllUsers(
            Authentication authentication,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        
        try {
            List<NguoiDung> users = nguoiDungService.getAllUsers(Pageable.unpaged()).getContent();
            
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=all_users.xlsx");
            
            byte[] excelBytes = excelExportService.exportCustomersToExcel(users);
            response.getOutputStream().write(excelBytes);
            response.getOutputStream().flush();
            
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xuất người dùng: " + e.getMessage());
        }
    }

    /**
     * Export người dùng được chọn ra Excel
     */
    @PostMapping("/export-selected")
    public void exportSelectedUsers(
            @RequestParam("userIds") List<Integer> userIds,
            Authentication authentication,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        
        try {
            List<NguoiDung> selectedUsers = new ArrayList<>();
            for (Integer id : userIds) {
                nguoiDungService.getUserById(id).ifPresent(selectedUsers::add);
            }
            
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=selected_users.xlsx");
            
            byte[] excelBytes = excelExportService.exportCustomersToExcel(selectedUsers);
            response.getOutputStream().write(excelBytes);
            response.getOutputStream().flush();
            
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xuất người dùng: " + e.getMessage());
        }
    }
    
    /**
     * Helper method to save user images to uploads/users folder
     */
    private String saveUserImage(MultipartFile file) throws Exception {
        System.out.println("=== SAVE USER IMAGE START ===");
        
        try {
            // Use absolute path to project uploads folder
            String projectPath = System.getProperty("user.dir");
            String uploadDir = projectPath + File.separator + "uploads" + File.separator + "users" + File.separator;
            
            System.out.println("Project path: " + projectPath);
            System.out.println("Upload directory: " + uploadDir);
            
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                System.out.println("Directory doesn't exist, creating: " + uploadDir);
                boolean created = dir.mkdirs();
                if (!created) {
                    System.err.println("Failed to create directory: " + uploadDir);
                    throw new Exception("Không thể tạo thư mục uploads/users: " + uploadDir);
                }
                System.out.println("Directory created successfully");
            } else {
                System.out.println("Directory exists: " + uploadDir);
            }
            
            // Validate file
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                throw new Exception("Tên file không hợp lệ");
            }
            
            System.out.println("Original filename: " + originalFilename);
            System.out.println("File size: " + file.getSize() + " bytes");
            System.out.println("Content type: " + file.getContentType());
            
            // Get file extension
            String extension = "";
            if (originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            } else {
                // Default to .jpg if no extension
                extension = ".jpg";
            }
            
            // Create unique filename with timestamp
            String fileName = "user_" + System.currentTimeMillis() + extension;
            System.out.println("Generated filename: " + fileName);
            
            File dest = new File(dir, fileName);
            System.out.println("Destination path: " + dest.getAbsolutePath());
            
            // Save file
            file.transferTo(dest);
            
            // Verify file was saved successfully
            if (dest.exists() && dest.length() > 0) {
                System.out.println("File saved successfully - Size: " + dest.length() + " bytes");
                System.out.println("File path: " + dest.getAbsolutePath());
                System.out.println("=== SAVE USER IMAGE SUCCESS ===");
                return fileName;
            } else {
                System.err.println("File was not saved or has 0 bytes");
                throw new Exception("File không được lưu hoặc có kích thước 0 bytes");
            }
            
        } catch (Exception e) {
            System.err.println("=== SAVE USER IMAGE ERROR ===");
            System.err.println("Error saving user image: " + e.getMessage());
            e.printStackTrace();
            System.err.println("=== END ERROR ===");
            throw new Exception("Lỗi khi lưu ảnh người dùng: " + e.getMessage());
        }
    }
}
