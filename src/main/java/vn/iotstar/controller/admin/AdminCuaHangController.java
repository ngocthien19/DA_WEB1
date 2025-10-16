package vn.iotstar.controller.admin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import io.jsonwebtoken.io.IOException;
import vn.iotstar.entity.CuaHang;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.entity.SanPham;
import vn.iotstar.model.CuaHangModel;
import vn.iotstar.model.ExcelImportResult;
import vn.iotstar.service.CuaHangService;
import vn.iotstar.service.NguoiDungService;
import vn.iotstar.service.SanPhamService;
import vn.iotstar.service.impl.ExcelExportService;
import vn.iotstar.service.impl.ExcelImportService;
import vn.iotstar.service.impl.UserDetailsServiceImpl;

@Controller
@RequestMapping("/admin/stores")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCuaHangController {

    @Autowired
    private CuaHangService cuaHangService;
    
    @Autowired
    private NguoiDungService nguoiDungService;
    
    @Autowired
    private SanPhamService sanPhamService;
    
    @Autowired
    private UserDetailsServiceImpl userDetailsService;
    
    @Autowired
    private ExcelExportService excelExportService;
    
    @Autowired
    private ExcelImportService excelImportService;

    /**
     * Lấy danh sách người dùng có vai trò VENDOR và chưa có cửa hàng nào
     */
    private List<NguoiDung> getAvailableVendors() {
        List<NguoiDung> allVendors = nguoiDungService.getUsersByRole("VENDOR");
        List<NguoiDung> availableVendors = new ArrayList<>();
        
        for (NguoiDung vendor : allVendors) {
            List<CuaHang> stores = cuaHangService.findByNguoiDung(vendor);
            if (stores == null || stores.isEmpty()) {
                availableVendors.add(vendor);
            }
        }
        
        return availableVendors;
    }

    @GetMapping
    public String listStores(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "maCuaHang") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication,
            Model model) {
        
        // Lấy thông tin user đang đăng nhập
        NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
        
        // Tạo Pageable với sắp xếp theo lựa chọn của người dùng
        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
            Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Tìm kiếm và phân trang
        Page<CuaHang> storePage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            storePage = cuaHangService.searchStores(keyword, pageable);
        } else {
            storePage = cuaHangService.findAll(pageable);
        }
        
        // Lấy danh sách chủ cửa hàng để hiển thị
        List<NguoiDung> vendors = nguoiDungService.getUsersByRole("VENDOR");
        
        // LẤY THỐNG KÊ - THÊM DEBUG LOG
        long totalStores = cuaHangService.countTotalStores();
        long totalActiveStores = cuaHangService.countActiveStores();
        long totalInactiveStores = cuaHangService.countInactiveStores();
        double averageRating = cuaHangService.getAverageRating();
        
        // Debug log để kiểm tra giá trị
        System.out.println("=== STORE STATISTICS ===");
        System.out.println("Total Stores: " + totalStores);
        System.out.println("Active Stores: " + totalActiveStores);
        System.out.println("Inactive Stores: " + totalInactiveStores);
        System.out.println("Average Rating: " + averageRating);
        System.out.println("========================");
        
        // Thêm thống kê vào model - ĐẢM BẢO TÊN ATTRIBUTE ĐÚNG
        model.addAttribute("totalStores", totalStores);
        model.addAttribute("totalActiveStores", totalActiveStores);
        model.addAttribute("totalInactiveStores", totalInactiveStores);
        model.addAttribute("averageRating", averageRating);
        
        model.addAttribute("user", currentUser);
        model.addAttribute("storePage", storePage);
        model.addAttribute("vendors", vendors);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        
        return "admin/stores/stores";
    }

    /**
     * Hiển thị form thêm cửa hàng mới
     */
    @GetMapping("/add")
    public String showAddForm(Authentication authentication, Model model) {
        NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
        
        // Thay đổi: chỉ lấy những vendor chưa có cửa hàng
        List<NguoiDung> availableVendors = getAvailableVendors();
        int currentYear = java.time.Year.now().getValue();
        model.addAttribute("maxYear", currentYear + 1);
        model.addAttribute("user", currentUser);
        model.addAttribute("cuaHangModel", new CuaHangModel());
        model.addAttribute("vendors", availableVendors); // Sử dụng availableVendors thay vì all vendors
        model.addAttribute("isEdit", false);
        
        return "admin/stores/store-form";
    }

    /**
     * Xử lý thêm cửa hàng mới với upload ảnh
     */
    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<?> addStore(
            @RequestParam("tenCuaHang") String tenCuaHang,
            @RequestParam("maNguoiDung") Integer maNguoiDung,
            @RequestParam(value = "moTa", required = false) String moTa,
            @RequestParam("diaChi") String diaChi,
            @RequestParam("soDienThoai") String soDienThoai,
            @RequestParam("email") String email,
            @RequestParam(value = "namThanhLap", required = false) Integer namThanhLap,
            @RequestParam(value = "trangThai", defaultValue = "true") Boolean trangThai,
            @RequestParam(value = "hinhAnh", required = false) MultipartFile hinhAnh,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate input
            if (tenCuaHang == null || tenCuaHang.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Tên cửa hàng không được để trống");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (maNguoiDung == null) {
                response.put("success", false);
                response.put("message", "Chủ cửa hàng không được để trống");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (diaChi == null || diaChi.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Địa chỉ không được để trống");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (soDienThoai == null || soDienThoai.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Số điện thoại không được để trống");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (email == null || email.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Email không được để trống");
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
                fileName = saveStoreImage(hinhAnh);
            }
            
            // Create CuaHangModel
            CuaHangModel model = CuaHangModel.builder()
                    .tenCuaHang(tenCuaHang.trim())
                    .maNguoiDung(maNguoiDung)
                    .moTa(moTa != null ? moTa.trim() : null)
                    .diaChi(diaChi.trim())
                    .soDienThoai(soDienThoai.trim())
                    .email(email.trim())
                    .namThanhLap(namThanhLap)
                    .trangThai(trangThai)
                    .hinhAnh(fileName)
                    .build();
            
            // Save store
            cuaHangService.createStore(model);
            
            response.put("success", true);
            response.put("message", "Thêm cửa hàng thành công!");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Hiển thị form chỉnh sửa cửa hàng
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(
            @PathVariable Integer id,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
        
        try {
            CuaHang cuaHang = cuaHangService.findByMaCuaHang(id);
            if (cuaHang == null) {
                throw new RuntimeException("Cửa hàng không tồn tại");
            }
            
        // Trong edit mode, lấy tất cả vendors nhưng đánh dấu vendor hiện tại
        List<NguoiDung> allVendors = nguoiDungService.getUsersByRole("VENDOR");
        List<NguoiDung> availableVendors = getAvailableVendors();
        
        // Kết hợp: vendor hiện tại + các vendor chưa có cửa hàng
        List<NguoiDung> combinedVendors = new ArrayList<>();
        combinedVendors.add(cuaHang.getNguoiDung()); // Thêm vendor hiện tại
        for (NguoiDung vendor : availableVendors) {
            if (!vendor.getMaNguoiDung().equals(cuaHang.getNguoiDung().getMaNguoiDung())) {
                combinedVendors.add(vendor);
            }
        }
            
            // Chuyển đổi entity sang model
            CuaHangModel cuaHangModel = CuaHangModel.builder()
                    .maCuaHang(cuaHang.getMaCuaHang())
                    .tenCuaHang(cuaHang.getTenCuaHang())
                    .maNguoiDung(cuaHang.getNguoiDung().getMaNguoiDung())
                    .moTa(cuaHang.getMoTa())
                    .diaChi(cuaHang.getDiaChi())
                    .soDienThoai(cuaHang.getSoDienThoai())
                    .email(cuaHang.getEmail())
                    .namThanhLap(cuaHang.getNamThanhLap())
                    .trangThai(cuaHang.getTrangThai())
                    .hinhAnh(cuaHang.getHinhAnh())
                    .danhGiaTrungBinh(cuaHang.getDanhGiaTrungBinh())
                    .soLuongDanhGia(cuaHang.getSoLuongDanhGia())
                    .ngayTao(cuaHang.getNgayTao())
                    .tenNguoiDung(cuaHang.getNguoiDung().getTenNguoiDung())
                    .emailNguoiDung(cuaHang.getNguoiDung().getEmail())
                    .sdtNguoiDung(cuaHang.getNguoiDung().getSdt())
                    .build();
            
            model.addAttribute("user", currentUser);
            model.addAttribute("cuaHang", cuaHang);
            model.addAttribute("cuaHangModel", cuaHangModel);
            model.addAttribute("vendors", combinedVendors); // Sử dụng combinedVendors
            model.addAttribute("isEdit", true);
            
            return "admin/stores/store-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/stores";
        }
    }

    /**
     * Xử lý cập nhật cửa hàng với ảnh
     */
    @PostMapping("/edit/{id}")
    @ResponseBody
    public ResponseEntity<?> updateStore(
            @PathVariable Integer id,
            @RequestParam("tenCuaHang") String tenCuaHang,
            @RequestParam("maNguoiDung") Integer maNguoiDung,
            @RequestParam(value = "moTa", required = false) String moTa,
            @RequestParam("diaChi") String diaChi,
            @RequestParam("soDienThoai") String soDienThoai,
            @RequestParam("email") String email,
            @RequestParam(value = "namThanhLap", required = false) Integer namThanhLap,
            @RequestParam("trangThai") Boolean trangThai,
            @RequestParam(value = "hinhAnh", required = false) MultipartFile hinhAnh,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate input
            if (tenCuaHang == null || tenCuaHang.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Tên cửa hàng không được để trống");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (maNguoiDung == null) {
                response.put("success", false);
                response.put("message", "Chủ cửa hàng không được để trống");
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
                fileName = saveStoreImage(hinhAnh);
            }
            
            // Create CuaHangModel
            CuaHangModel model = CuaHangModel.builder()
                    .tenCuaHang(tenCuaHang.trim())
                    .maNguoiDung(maNguoiDung)
                    .moTa(moTa != null ? moTa.trim() : null)
                    .diaChi(diaChi.trim())
                    .soDienThoai(soDienThoai.trim())
                    .email(email.trim())
                    .namThanhLap(namThanhLap)
                    .trangThai(trangThai)
                    .hinhAnh(fileName)
                    .build();
            
            // Update store
            cuaHangService.updateStore(id, model);
            
            response.put("success", true);
            response.put("message", "Cập nhật cửa hàng thành công!");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Hiển thị chi tiết cửa hàng
     */
    @GetMapping("/view/{id}")
    public String viewStore(
            @PathVariable Integer id,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
        
        try {
            CuaHang cuaHang = cuaHangService.findByMaCuaHang(id);
            if (cuaHang == null) {
                throw new RuntimeException("Cửa hàng không tồn tại");
            }
            
            model.addAttribute("user", currentUser);
            model.addAttribute("cuaHang", cuaHang);
            
            return "admin/stores/store-view";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/stores";
        }
    }

    /**
     * Xóa cửa hàng
     */
    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteStore(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            CuaHang cuaHang = cuaHangService.findByMaCuaHang(id);
            if (cuaHang == null) {
                response.put("success", false);
                response.put("message", "Cửa hàng không tồn tại");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Delete store
            cuaHangService.deleteStore(id);
            
            response.put("success", true);
            response.put("message", "Xóa cửa hàng thành công");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Thay đổi trạng thái cửa hàng - FIXED VERSION
     */
    @PostMapping("/change-status/{id}")
    @ResponseBody
    public ResponseEntity<?> changeStatus(
            @PathVariable Integer id,
            @RequestParam Boolean status,
            @RequestParam(value = "lyDoKhoa", required = false) String lyDoKhoa) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            CuaHang cuaHang = cuaHangService.findByMaCuaHang(id);
            if (cuaHang == null) {
                response.put("success", false);
                response.put("message", "Cửa hàng không tồn tại");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Lưu trạng thái cũ để so sánh
            Boolean oldStatus = cuaHang.getTrangThai();
            
            // Cập nhật trạng thái mới
            cuaHang.setTrangThai(status);
            
            if (!status && lyDoKhoa != null && !lyDoKhoa.trim().isEmpty()) {
                // Khi khóa cửa hàng, lưu lý do
                cuaHang.setLyDoKhoa(lyDoKhoa.trim());
            } else if (status) {
                // Khi mở khóa, xóa lý do
                cuaHang.setLyDoKhoa(null);
            }
            
            // Save store - phương thức này sẽ tự động đồng bộ sản phẩm
            CuaHang updatedStore = cuaHangService.save(cuaHang);
            
            // Kiểm tra xem có cần đồng bộ sản phẩm không
            if (!oldStatus.equals(status)) {
                // Gọi phương thức đồng bộ trạng thái sản phẩm
                try {
                    // Lấy danh sách sản phẩm của cửa hàng
                    List<SanPham> products = sanPhamService.findByCuaHang(updatedStore);
                    int updatedCount = 0;
                    
                    for (SanPham product : products) {
                        // Chỉ cập nhật nếu trạng thái khác
                        if (!product.getTrangThai().equals(status)) {
                            // Sử dụng phương thức đặc biệt cho hệ thống (bỏ qua validation)
                            sanPhamService.updateProductStatusForSystem(product.getMaSanPham(), status);
                            updatedCount++;
                        }
                    }
                    
                    System.out.println("Đã đồng bộ trạng thái cho " + updatedCount + " sản phẩm của cửa hàng " + updatedStore.getTenCuaHang());
                    
                } catch (Exception e) {
                    System.err.println("Lỗi khi đồng bộ trạng thái sản phẩm: " + e.getMessage());
                    // Không throw exception ở đây để không ảnh hưởng đến việc cập nhật cửa hàng
                }
            }
            
            response.put("success", true);
            response.put("message", "Thay đổi trạng thái thành công! Đã đồng bộ " + 
                         (oldStatus.equals(status) ? "0" : "tất cả") + " sản phẩm.");
            response.put("trangThai", updatedStore.getTrangThai());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Xóa hàng loạt cửa hàng
     */
    @PostMapping("/bulk-delete")
    @ResponseBody
    public ResponseEntity<?> bulkDelete(
            @RequestParam("ids") List<Integer> ids) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (ids == null || ids.isEmpty()) {
            response.put("success", false);
            response.put("message", "Vui lòng chọn ít nhất một cửa hàng để xóa");
            return ResponseEntity.badRequest().body(response);
        }
        
        int successCount = 0;
        int errorCount = 0;
        List<String> errorMessages = new ArrayList<>();
        
        for (Integer id : ids) {
            try {
                cuaHangService.deleteStore(id);
                successCount++;
            } catch (Exception e) {
                errorCount++;
                errorMessages.add("Lỗi khi xóa cửa hàng ID " + id + ": " + e.getMessage());
            }
        }
        
        response.put("success", true);
        response.put("message", "Đã xóa " + successCount + " cửa hàng thành công");
        response.put("successCount", successCount);
        response.put("errorCount", errorCount);
        response.put("errors", errorMessages);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Thay đổi trạng thái hàng loạt cửa hàng - UPDATED VERSION
     */
    @PostMapping("/bulk-change-status")
    @ResponseBody
    public ResponseEntity<?> bulkChangeStatus(
            @RequestParam("ids") List<Integer> ids,
            @RequestParam Boolean status,
            @RequestParam(value = "lyDoKhoa", required = false) String lyDoKhoa) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (ids == null || ids.isEmpty()) {
            response.put("success", false);
            response.put("message", "Vui lòng chọn ít nhất một cửa hàng để thay đổi trạng thái");
            return ResponseEntity.badRequest().body(response);
        }
        
        int successCount = 0;
        int errorCount = 0;
        List<String> errorMessages = new ArrayList<>();
        
        for (Integer id : ids) {
            try {
                CuaHang cuaHang = cuaHangService.findByMaCuaHang(id);
                if (cuaHang == null) {
                    throw new RuntimeException("Cửa hàng không tồn tại");
                }
                
                // Lưu trạng thái cũ để so sánh
                Boolean oldStatus = cuaHang.getTrangThai();
                
                // Cập nhật trạng thái và lý do khóa
                cuaHang.setTrangThai(status);
                
                if (!status && lyDoKhoa != null && !lyDoKhoa.trim().isEmpty()) {
                    // Khi khóa cửa hàng, lưu lý do
                    cuaHang.setLyDoKhoa(lyDoKhoa.trim());
                } else if (status) {
                    // Khi mở khóa, xóa lý do
                    cuaHang.setLyDoKhoa(null);
                }
                
                // Save store - phương thức này sẽ tự động đồng bộ sản phẩm
                CuaHang updatedStore = cuaHangService.save(cuaHang);
                
                // Đồng bộ trạng thái sản phẩm nếu trạng thái thay đổi
                if (!oldStatus.equals(status)) {
                    try {
                        List<SanPham> products = sanPhamService.findByCuaHang(updatedStore);
                        int updatedProductCount = 0;
                        
                        for (SanPham product : products) {
                            if (!product.getTrangThai().equals(status)) {
                                sanPhamService.updateProductStatusForSystem(product.getMaSanPham(), status);
                                updatedProductCount++;
                            }
                        }
                        
                        System.out.println("Đã đồng bộ " + updatedProductCount + " sản phẩm cho cửa hàng " + updatedStore.getTenCuaHang());
                        
                    } catch (Exception e) {
                        System.err.println("Lỗi khi đồng bộ sản phẩm cho cửa hàng " + updatedStore.getTenCuaHang() + ": " + e.getMessage());
                    }
                }
                
                successCount++;
            } catch (Exception e) {
                errorCount++;
                errorMessages.add("Lỗi khi cập nhật cửa hàng ID " + id + ": " + e.getMessage());
            }
        }
        
        response.put("success", true);
        response.put("message", "Đã cập nhật trạng thái cho " + successCount + " cửa hàng thành công");
        response.put("successCount", successCount);
        response.put("errorCount", errorCount);
        response.put("errors", errorMessages);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Hiển thị trang import cửa hàng từ Excel
     */
    @GetMapping("/import")
    public String showImportExcelPage(Authentication authentication, Model model) {
        NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
        model.addAttribute("user", currentUser);
        return "admin/stores/import-excel";
    }

    /**
     * Tải template Excel mẫu cho cửa hàng
     */
    @GetMapping("/download-template")
    public void downloadTemplate(jakarta.servlet.http.HttpServletResponse response) throws IOException {
        try {
            // Tạo template Excel (cần implement phương thức này trong ExcelImportService)
            byte[] excelBytes = excelImportService.generateStoresTemplate();
            
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=stores_template.xlsx");
            response.getOutputStream().write(excelBytes);
            response.getOutputStream().flush();
            
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo template: " + e.getMessage());
        }
    }

    /**
     * Import cửa hàng từ Excel files
     */
    @PostMapping("/import")
    @ResponseBody
    public ResponseEntity<?> importStoresFromExcel(@RequestParam("excelFiles") List<MultipartFile> excelFiles) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Gọi service để import (cần implement phương thức này trong ExcelImportService)
            ExcelImportResult importResult = excelImportService.importAndSaveStoresFromMultipleExcel(excelFiles);
            
            response.put("success", importResult.isSuccess());
            response.put("message", importResult.getMessage());
            response.put("totalRecords", importResult.getTotalRecords());
            response.put("successCount", importResult.getSuccessCount());
            response.put("errorCount", importResult.getErrorCount());
            response.put("errors", importResult.getErrors());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
 // Sửa phương thức exportAllStores
    @PostMapping("/export-all")
    public void exportAllStores(
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        
        try {
            List<CuaHang> stores = cuaHangService.findAll();
            
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=all_stores.xlsx");
            
            // Sử dụng ExcelExportService thực tế
            byte[] excelBytes = excelExportService.exportStoresToExcel(stores);
            response.getOutputStream().write(excelBytes);
            
            response.getOutputStream().flush();
            
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xuất cửa hàng: " + e.getMessage());
        }
    }

    // Sửa phương thức exportSelectedStores
    @PostMapping("/export-selected")
    public void exportSelectedStores(
            @RequestParam("storeIds") List<Integer> storeIds,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        
        try {
            List<CuaHang> selectedStores = new ArrayList<>();
            for (Integer id : storeIds) {
                CuaHang store = cuaHangService.findByMaCuaHang(id);
                if(store != null) {
                    selectedStores.add(store);
                }
            }
            
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=selected_stores.xlsx");
            
            // Sử dụng ExcelExportService thực tế
            byte[] excelBytes = excelExportService.exportStoresToExcel(selectedStores);
            response.getOutputStream().write(excelBytes);
            
            response.getOutputStream().flush();
            
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xuất cửa hàng: " + e.getMessage());
        }
    }
    
    /**
     * Helper method to save store images to uploads/stores folder
     */
    private String saveStoreImage(MultipartFile file) throws Exception {
        System.out.println("=== SAVE STORE IMAGE START ===");
        
        try {
            // Use absolute path to project uploads folder
            String projectPath = System.getProperty("user.dir");
            String uploadDir = projectPath + File.separator + "uploads" + File.separator + "stores" + File.separator;
            
            System.out.println("Project path: " + projectPath);
            System.out.println("Upload directory: " + uploadDir);
            
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                System.out.println("Directory doesn't exist, creating: " + uploadDir);
                boolean created = dir.mkdirs();
                if (!created) {
                    System.err.println("Failed to create directory: " + uploadDir);
                    throw new Exception("Không thể tạo thư mục uploads/stores: " + uploadDir);
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
            String fileName = "store_" + System.currentTimeMillis() + extension;
            System.out.println("Generated filename: " + fileName);
            
            File dest = new File(dir, fileName);
            System.out.println("Destination path: " + dest.getAbsolutePath());
            
            // Save file
            file.transferTo(dest);
            
            // Verify file was saved successfully
            if (dest.exists() && dest.length() > 0) {
                System.out.println("File saved successfully - Size: " + dest.length() + " bytes");
                System.out.println("File path: " + dest.getAbsolutePath());
                System.out.println("=== SAVE STORE IMAGE SUCCESS ===");
                return fileName;
            } else {
                System.err.println("File was not saved or has 0 bytes");
                throw new Exception("File không được lưu hoặc có kích thước 0 bytes");
            }
            
        } catch (Exception e) {
            System.err.println("=== SAVE STORE IMAGE ERROR ===");
            System.err.println("Error saving store image: " + e.getMessage());
            e.printStackTrace();
            System.err.println("=== END ERROR ===");
            throw new Exception("Lỗi khi lưu ảnh cửa hàng: " + e.getMessage());
        }
    }
}