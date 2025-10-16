package vn.iotstar.controller.admin;

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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.entity.DanhMuc;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.model.DanhMucModel;
import vn.iotstar.service.DanhMucService;
import vn.iotstar.service.impl.UserDetailsServiceImpl;

import java.io.File;
import java.time.LocalDateTime; 
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDanhMucController {

    @Autowired
    private DanhMucService danhMucService;
    
    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    // ============= PAGE DISPLAY METHODS =============

    @GetMapping
    public String listCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            Authentication authentication,
            Model model) {
        
        NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("maDanhMuc").descending());
        Page<DanhMuc> categoryPage;
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            categoryPage = danhMucService.searchCategories(keyword, pageable);
        } else {
            categoryPage = danhMucService.findAll(pageable);
        }
        
        model.addAttribute("user", currentUser);
        model.addAttribute("categoryPage", categoryPage);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        
        return "admin/categories/categories";
    }

    @GetMapping("/add")
    public String showAddForm(Authentication authentication, Model model) {
        NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
        model.addAttribute("user", currentUser);
        model.addAttribute("danhMucModel", new DanhMucModel());
        model.addAttribute("isEdit", false);
        return "admin/categories/category-form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(
            @PathVariable Integer id,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        try {
            NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
            DanhMuc danhMuc = danhMucService.findByMaDanhMuc(id);
            if (danhMuc == null) {
                throw new RuntimeException("Danh mục không tồn tại");
            }
            
            DanhMucModel danhMucModel = convertToDanhMucModel(danhMuc);
            model.addAttribute("user", currentUser);
            model.addAttribute("danhMucModel", danhMucModel);
            model.addAttribute("isEdit", true);
            return "admin/categories/category-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/categories";
        }
    }

    @GetMapping("/view/{id}")
    public String viewCategory(
            @PathVariable Integer id,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        try {
            NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
            DanhMuc danhMuc = danhMucService.findByMaDanhMuc(id);
            if (danhMuc == null) {
                throw new RuntimeException("Danh mục không tồn tại");
            }
            
            model.addAttribute("user", currentUser);
            model.addAttribute("danhMuc", danhMuc);
            return "admin/categories/category-view";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/categories";
        }
    }

    // ============= CRUD OPERATIONS =============

    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<?> addCategory(
            @RequestParam("tenDanhMuc") String tenDanhMuc,
            @RequestParam(value = "moTa", required = false) String moTa,
            @RequestParam(value = "trangThai", defaultValue = "true") Boolean trangThai,
            @RequestParam(value = "hinhAnh", required = false) MultipartFile hinhAnh) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate input
            if (tenDanhMuc == null || tenDanhMuc.trim().isEmpty()) {
                throw new RuntimeException("Tên danh mục không được để trống");
            }
            
            // Handle image upload
            String fileName = handleImageUpload(hinhAnh);
            
            // Create category
            DanhMuc danhMuc = DanhMuc.builder()
                    .tenDanhMuc(tenDanhMuc.trim())
                    .moTa(moTa != null ? moTa.trim() : null)
                    .hinhAnh(fileName)
                    .trangThai(trangThai)
                    .ngayTao(LocalDateTime.now())
                    .build();
            
            DanhMuc savedCategory = danhMucService.createCategory(danhMuc);
            
            response.put("success", true);
            response.put("message", "Thêm danh mục thành công!");
            response.put("categoryId", savedCategory.getMaDanhMuc());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/edit/{id}")
    @ResponseBody
    public ResponseEntity<?> updateCategory(
            @PathVariable Integer id,
            @RequestParam("tenDanhMuc") String tenDanhMuc,
            @RequestParam(value = "moTa", required = false) String moTa,
            @RequestParam("trangThai") Boolean trangThai,
            @RequestParam(value = "hinhAnh", required = false) MultipartFile hinhAnh) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate input
            if (tenDanhMuc == null || tenDanhMuc.trim().isEmpty()) {
                throw new RuntimeException("Tên danh mục không được để trống");
            }
            
            // Get existing category
            DanhMuc existingCategory = danhMucService.findByMaDanhMuc(id);
            if (existingCategory == null) {
                throw new RuntimeException("Danh mục không tồn tại");
            }
            
            // Handle image upload
            String fileName = handleImageUpload(hinhAnh);
            if (fileName == null) {
                // Keep existing image if no new image uploaded
                fileName = existingCategory.getHinhAnh();
            }
            
            // Update category
            DanhMuc danhMuc = DanhMuc.builder()
                    .tenDanhMuc(tenDanhMuc.trim())
                    .moTa(moTa != null ? moTa.trim() : null)
                    .hinhAnh(fileName)
                    .trangThai(trangThai)
                    .build();
            
            DanhMuc updatedCategory = danhMucService.updateCategory(id, danhMuc);
            
            response.put("success", true);
            response.put("message", "Cập nhật danh mục thành công!");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteCategory(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            danhMucService.deleteCategory(id);
            response.put("success", true);
            response.put("message", "Xóa danh mục thành công");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/change-status/{id}")
    @ResponseBody
    public ResponseEntity<?> changeStatus(
            @PathVariable Integer id,
            @RequestParam Boolean status) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            DanhMuc danhMuc = danhMucService.findByMaDanhMuc(id);
            if (danhMuc == null) {
                throw new RuntimeException("Danh mục không tồn tại");
            }
            
            // Lưu trạng thái cũ để so sánh
            Boolean oldStatus = danhMuc.getTrangThai();
            
            // QUAN TRỌNG: Dùng updateCategory thay vì set trực tiếp và save
            // Tạo đối tượng DanhMuc mới với thông tin cập nhật
            DanhMuc updatedDanhMuc = DanhMuc.builder()
                    .tenDanhMuc(danhMuc.getTenDanhMuc())
                    .moTa(danhMuc.getMoTa())
                    .hinhAnh(danhMuc.getHinhAnh())
                    .trangThai(status)
                    .ngayTao(danhMuc.getNgayTao())
                    .build();
            
            // Gọi updateCategory - phương thức này đã có logic cập nhật sản phẩm
            DanhMuc result = danhMucService.updateCategory(id, updatedDanhMuc);
            
            response.put("success", true);
            response.put("message", "Thay đổi trạng thái thành công!");
            response.put("trangThai", result.getTrangThai());
            
            // THÊM: Thông báo về số lượng sản phẩm đã cập nhật
            if (!status) { // Nếu chuyển sang Ngừng hoạt động
                response.put("productUpdate", "Tất cả sản phẩm trong danh mục đã được chuyển sang Ngừng bán.");
            } else { // Nếu chuyển sang Hoạt động
                response.put("productUpdate", "Các sản phẩm trong danh mục đã được kích hoạt trở lại.");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Cập nhật trạng thái của tất cả sản phẩm trong danh mục
     */
    private void updateProductsStatusByCategory(Integer categoryId, Boolean status) {
        try {
            DanhMuc danhMuc = danhMucService.findByMaDanhMuc(categoryId);
            if (danhMuc != null) {
                // Gọi service để cập nhật sản phẩm
                // Service sẽ tự động xử lý thông qua phương thức updateProductsStatusByCategory
                // Đảm bảo phương thức này đã được triển khai trong DanhMucServiceImpl
                System.out.println("Đang cập nhật trạng thái sản phẩm cho danh mục: " + danhMuc.getTenDanhMuc());
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi cập nhật trạng thái sản phẩm: " + e.getMessage());
        }
    }

    // ============= PRIVATE HELPER METHODS =============

    private String handleImageUpload(MultipartFile hinhAnh) throws Exception {
        if (hinhAnh != null && !hinhAnh.isEmpty()) {
            String contentType = hinhAnh.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new RuntimeException("File phải là ảnh (jpg, png, gif)");
            }
            
            if (hinhAnh.getSize() > 5 * 1024 * 1024) { // 5MB
                throw new RuntimeException("Kích thước file không được vượt quá 5MB");
            }
            
            return saveCategoryImage(hinhAnh);
        }
        return null;
    }

    private String saveCategoryImage(MultipartFile file) throws Exception {
        String projectPath = System.getProperty("user.dir");
        String uploadDir = projectPath + File.separator + "uploads" + File.separator + "categories" + File.separator;
        
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                throw new Exception("Không thể tạo thư mục uploads/categories: " + uploadDir);
            }
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new Exception("Tên file không hợp lệ");
        }
        
        String extension = "";
        if (originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        } else {
            extension = ".jpg";
        }
        
        String fileName = "category_" + System.currentTimeMillis() + extension;
        File dest = new File(dir, fileName);
        
        file.transferTo(dest);
        
        if (dest.exists() && dest.length() > 0) {
            return fileName;
        } else {
            throw new Exception("File không được lưu hoặc có kích thước 0 bytes");
        }
    }

    private DanhMucModel convertToDanhMucModel(DanhMuc danhMuc) {
        return DanhMucModel.builder()
                .maDanhMuc(danhMuc.getMaDanhMuc())
                .tenDanhMuc(danhMuc.getTenDanhMuc())
                .moTa(danhMuc.getMoTa())
                .hinhAnh(danhMuc.getHinhAnh())
                .ngayTao(danhMuc.getNgayTao())
                .trangThai(danhMuc.getTrangThai())
                .build();
    }
}