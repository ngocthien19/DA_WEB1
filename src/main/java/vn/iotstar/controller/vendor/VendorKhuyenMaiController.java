package vn.iotstar.controller.vendor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.iotstar.entity.CuaHang;
import vn.iotstar.entity.KhuyenMai;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.model.KhuyenMaiImportResult;
import vn.iotstar.model.KhuyenMaiModel;
import vn.iotstar.service.CuaHangService;
import vn.iotstar.service.KhuyenMaiService;
import vn.iotstar.service.impl.ExcelExportService;
import vn.iotstar.service.impl.ExcelImportService; 
import vn.iotstar.service.impl.UserDetailsServiceImpl; 

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Controller
@RequestMapping("/vendor/promotions")
@PreAuthorize("hasRole('VENDOR')")
public class VendorKhuyenMaiController {

    @Autowired
    private KhuyenMaiService khuyenMaiService;
    
    @Autowired
    private CuaHangService cuaHangService;
    
    @Autowired
    private ExcelImportService excelImportService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;
    
    @Autowired
    private ExcelExportService excelExportService;

    private static final String STORE_LOCKED_MESSAGE = "Cửa hàng của bạn đang bị Admin khóa (Ngừng hoạt động). Bạn không thể thực hiện các thao tác quản lý khuyến mãi.";

    private CuaHang getCurrentStore(Authentication authentication) {
        NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
        if (currentUser == null) return null;
        
        List<CuaHang> stores = cuaHangService.findByNguoiDung(currentUser);
        return stores.isEmpty() ? null : stores.get(0);
    }
    
    private NguoiDung getCurrentUser(Authentication authentication) {
        return userDetailsService.getCurrentUser(authentication);
    }
    
    // ============= PAGE DISPLAY METHODS =============

    @GetMapping
    public String listKhuyenMai(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) String validityStatus,
            @RequestParam(required = false) String activationStatus,
            Authentication authentication,
            Model model) {
        
        NguoiDung currentUser = getCurrentUser(authentication);
        CuaHang cuaHang = getCurrentStore(authentication);
        
        model.addAttribute("user", currentUser);

        if (cuaHang == null) {
            return "redirect:/vendor/dashboard"; 
        }

        if (Boolean.FALSE.equals(cuaHang.getTrangThai())) {
            model.addAttribute("errorMessage", STORE_LOCKED_MESSAGE);
            model.addAttribute("cuaHang", cuaHang);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "maKhuyenMai"));
        Page<KhuyenMai> khuyenMaiPage;
        
        // XỬ LÝ KHI CHỌN "TẤT CẢ" - chuyển giá trị rỗng thành null
        if ("".equals(keyword)) keyword = null;
        if ("".equals(validityStatus)) validityStatus = null;
        
        // XỬ LÝ activationStatus: chuyển chuỗi thành Boolean
        Boolean activationStatusBoolean = null;
        if (activationStatus != null && !activationStatus.isEmpty()) {
            activationStatusBoolean = Boolean.valueOf(activationStatus);
        }
        
        // Áp dụng bộ lọc với validityStatus và activationStatus
        if ((keyword != null && !keyword.isEmpty()) || startDate != null || endDate != null 
                || validityStatus != null || activationStatusBoolean != null) {
            khuyenMaiPage = khuyenMaiService.findByCuaHangAndFilters(
                cuaHang, keyword, startDate, endDate, activationStatusBoolean, validityStatus, pageable);
        } else {
            khuyenMaiPage = khuyenMaiService.findByCuaHang(cuaHang, pageable);
        }
        
        // LẤY TỔNG SỐ LƯỢNG THEO TRẠNG THÁI TỪ TOÀN BỘ DATABASE
        Map<String, Long> statusCounts = khuyenMaiService.getPromotionStatusCounts(cuaHang);
        
        model.addAttribute("khuyenMaiPage", khuyenMaiPage);
        model.addAttribute("cuaHang", cuaHang);
        model.addAttribute("keyword", keyword);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("validityStatus", validityStatus);
        model.addAttribute("activationStatus", activationStatus);
        model.addAttribute("currentPage", page);
        
        // THÊM SỐ LƯỢNG TỔNG VÀO MODEL
        model.addAttribute("totalActive", statusCounts.get("active"));
        model.addAttribute("totalUpcoming", statusCounts.get("upcoming"));
        model.addAttribute("totalExpired", statusCounts.get("expired"));
        model.addAttribute("totalInactive", statusCounts.get("inactive"));
        
        return "vendor/promotions/promotions";
    }

    @GetMapping("/add")
    public String showAddForm(Authentication authentication, Model model) {
        NguoiDung currentUser = getCurrentUser(authentication);
        CuaHang cuaHang = getCurrentStore(authentication);
        
        model.addAttribute("user", currentUser); 
        
        if (cuaHang == null) {
            return "redirect:/vendor/dashboard";
        }
        
        if (Boolean.FALSE.equals(cuaHang.getTrangThai())) {
            model.addAttribute("errorMessage", STORE_LOCKED_MESSAGE);
            return "redirect:/vendor/promotions";
        }
        
        KhuyenMaiModel khuyenMaiModel = new KhuyenMaiModel();
        khuyenMaiModel.setMaCuaHang(cuaHang.getMaCuaHang());
        khuyenMaiModel.setTrangThai(true);
        // Set default dates
        khuyenMaiModel.setNgayBatDau(LocalDate.now());
        khuyenMaiModel.setNgayKetThuc(LocalDate.now().plusDays(30));
        
        model.addAttribute("khuyenMaiModel", khuyenMaiModel);
        model.addAttribute("cuaHang", cuaHang);
        model.addAttribute("isEdit", false);
        return "vendor/promotions/promotion-form";
    }

    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<?> addKhuyenMai(
            @RequestBody KhuyenMaiModel khuyenMaiModel,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            CuaHang cuaHang = getCurrentStore(authentication);
            if (cuaHang == null) {
                response.put("success", false);
                response.put("message", "Bạn chưa đăng nhập hoặc chưa có cửa hàng.");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (Boolean.FALSE.equals(cuaHang.getTrangThai())) {
                response.put("success", false);
                response.put("message", STORE_LOCKED_MESSAGE);
                return ResponseEntity.badRequest().body(response);
            }
            
            // Validate dates
            LocalDate today = LocalDate.now();
            if (khuyenMaiModel.getNgayBatDau() == null) {
                response.put("success", false);
                response.put("message", "Ngày bắt đầu không được để trống");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (khuyenMaiModel.getNgayKetThuc() == null) {
                response.put("success", false);
                response.put("message", "Ngày kết thúc không được để trống");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (khuyenMaiModel.getNgayBatDau().isBefore(today)) {
                response.put("success", false);
                response.put("message", "Ngày bắt đầu phải là hiện tại hoặc tương lai");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (khuyenMaiModel.getNgayKetThuc().isBefore(khuyenMaiModel.getNgayBatDau()) || 
                khuyenMaiModel.getNgayKetThuc().isEqual(khuyenMaiModel.getNgayBatDau())) {
                response.put("success", false);
                response.put("message", "Ngày kết thúc phải sau ngày bắt đầu");
                return ResponseEntity.badRequest().body(response);
            }
            
            khuyenMaiModel.setMaCuaHang(cuaHang.getMaCuaHang());
            khuyenMaiService.createKhuyenMai(khuyenMaiModel);
            
            response.put("success", true);
            response.put("message", "Thêm khuyến mãi thành công!");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Authentication authentication, Model model) {
        NguoiDung currentUser = getCurrentUser(authentication);
        CuaHang cuaHang = getCurrentStore(authentication);
        
        model.addAttribute("user", currentUser); 

        if (cuaHang == null) {
            return "redirect:/vendor/dashboard";
        }
        
        if (Boolean.FALSE.equals(cuaHang.getTrangThai())) {
            model.addAttribute("errorMessage", STORE_LOCKED_MESSAGE);
            return "redirect:/vendor/promotions";
        }
        
        KhuyenMai khuyenMai = khuyenMaiService.findByMaKhuyenMai(id);
        if (khuyenMai == null || !khuyenMai.getCuaHang().getMaCuaHang().equals(cuaHang.getMaCuaHang())) {
            return "redirect:/vendor/promotions";
        }
        
        KhuyenMaiModel khuyenMaiModel = KhuyenMaiModel.builder()
                .maKhuyenMai(khuyenMai.getMaKhuyenMai())
                .maCuaHang(khuyenMai.getCuaHang().getMaCuaHang())
                .maGiamGia(khuyenMai.getMaGiamGia())
                .discount(khuyenMai.getDiscount())
                .ngayBatDau(khuyenMai.getNgayBatDau())
                .ngayKetThuc(khuyenMai.getNgayKetThuc())
                .soLuongMaGiamGia(khuyenMai.getSoLuongMaGiamGia())
                .soLuongDaSuDung(khuyenMai.getSoLuongDaSuDung())
                .trangThai(khuyenMai.getTrangThai())
                .build();
        
        model.addAttribute("khuyenMaiModel", khuyenMaiModel);
        model.addAttribute("cuaHang", cuaHang);
        model.addAttribute("isEdit", true);
        return "vendor/promotions/promotion-form";
    }

    @PostMapping("/edit/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> updateKhuyenMai(
            @PathVariable Integer id,
            @RequestBody KhuyenMaiModel khuyenMaiModel,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            CuaHang cuaHang = getCurrentStore(authentication);
            if (cuaHang == null) {
                response.put("success", false);
                response.put("message", "Bạn chưa đăng nhập hoặc chưa có cửa hàng.");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (Boolean.FALSE.equals(cuaHang.getTrangThai())) {
                response.put("success", false);
                response.put("message", STORE_LOCKED_MESSAGE);
                return ResponseEntity.badRequest().body(response);
            }
            
            // Validate dates - CHỈ VALIDATE CƠ BẢN, KHÔNG KIỂM TRA NGÀY BẮT ĐẦU PHẢI LÀ TƯƠNG LAI
            LocalDate today = LocalDate.now();
            if (khuyenMaiModel.getNgayBatDau() == null) {
                response.put("success", false);
                response.put("message", "Ngày bắt đầu không được để trống");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (khuyenMaiModel.getNgayKetThuc() == null) {
                response.put("success", false);
                response.put("message", "Ngày kết thúc không được để trống");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (khuyenMaiModel.getNgayKetThuc().isBefore(khuyenMaiModel.getNgayBatDau()) || 
                khuyenMaiModel.getNgayKetThuc().isEqual(khuyenMaiModel.getNgayBatDau())) {
                response.put("success", false);
                response.put("message", "Ngày kết thúc phải sau ngày bắt đầu");
                return ResponseEntity.badRequest().body(response);
            }
            
            khuyenMaiModel.setMaCuaHang(cuaHang.getMaCuaHang());
            khuyenMaiService.updateKhuyenMai(id, khuyenMaiModel);
            
            response.put("success", true);
            response.put("message", "Cập nhật khuyến mãi thành công!");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/view/{id}")
    public String viewKhuyenMai(@PathVariable Integer id, Authentication authentication, Model model) {
        NguoiDung currentUser = getCurrentUser(authentication);
        CuaHang cuaHang = getCurrentStore(authentication);
        
        model.addAttribute("user", currentUser); 

        if (cuaHang == null) {
            return "redirect:/vendor/dashboard";
        }
        
        KhuyenMai khuyenMai = khuyenMaiService.findByMaKhuyenMai(id);
        if (khuyenMai == null || !khuyenMai.getCuaHang().getMaCuaHang().equals(cuaHang.getMaCuaHang())) {
            return "redirect:/vendor/promotions";
        }
        
        model.addAttribute("khuyenMai", khuyenMai);
        model.addAttribute("cuaHang", cuaHang);
        return "vendor/promotions/promotion-view";
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteKhuyenMai(@PathVariable Integer id, Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            CuaHang cuaHang = getCurrentStore(authentication);
            if (cuaHang == null) {
                response.put("success", false);
                response.put("message", "Bạn chưa đăng nhập hoặc chưa có cửa hàng.");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (Boolean.FALSE.equals(cuaHang.getTrangThai())) {
                response.put("success", false);
                response.put("message", STORE_LOCKED_MESSAGE);
                return ResponseEntity.badRequest().body(response);
            }

            KhuyenMai khuyenMai = khuyenMaiService.findByMaKhuyenMai(id);
            if (khuyenMai == null || !khuyenMai.getCuaHang().getMaCuaHang().equals(cuaHang.getMaCuaHang())) {
                response.put("success", false);
                response.put("message", "Khuyến mãi không tồn tại hoặc không thuộc về cửa hàng của bạn");
                return ResponseEntity.badRequest().body(response);
            }
            
            khuyenMaiService.deleteKhuyenMai(id);
            
            response.put("success", true);
            response.put("message", "Xóa khuyến mãi thành công");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/change-status/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> changeStatus(
            @PathVariable Integer id,
            @RequestParam Boolean status,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            CuaHang cuaHang = getCurrentStore(authentication);
            if (cuaHang == null) {
                response.put("success", false);
                response.put("message", "Bạn chưa đăng nhập hoặc chưa có cửa hàng.");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (Boolean.FALSE.equals(cuaHang.getTrangThai())) {
                response.put("success", false);
                response.put("message", STORE_LOCKED_MESSAGE);
                return ResponseEntity.badRequest().body(response);
            }
            
            KhuyenMai khuyenMai = khuyenMaiService.findByMaKhuyenMai(id);
            if (khuyenMai == null || !khuyenMai.getCuaHang().getMaCuaHang().equals(cuaHang.getMaCuaHang())) {
                response.put("success", false);
                response.put("message", "Khuyến mãi không tồn tại hoặc không thuộc về cửa hàng của bạn");
                return ResponseEntity.badRequest().body(response);
            }
            
            KhuyenMai updatedKhuyenMai = khuyenMaiService.changeStatus(id, status);
            
            response.put("success", true);
            response.put("message", status ? "Kích hoạt khuyến mãi thành công!" : "Ngừng kích hoạt khuyến mãi thành công!");
            response.put("trangThai", updatedKhuyenMai.getTrangThai());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ============= BULK OPERATIONS =============

    @PostMapping("/bulk-change-status")
    @ResponseBody
    public ResponseEntity<?> bulkChangeStatus(
            @RequestParam("ids") List<Integer> ids,
            @RequestParam Boolean status,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            CuaHang cuaHang = getCurrentStore(authentication);
            if (cuaHang == null) {
                response.put("success", false);
                response.put("message", "Bạn chưa đăng nhập hoặc chưa có cửa hàng.");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (Boolean.FALSE.equals(cuaHang.getTrangThai())) {
                response.put("success", false);
                response.put("message", STORE_LOCKED_MESSAGE);
                return ResponseEntity.badRequest().body(response);
            }
            
            if (ids == null || ids.isEmpty()) {
                response.put("success", false);
                response.put("message", "Vui lòng chọn ít nhất một khuyến mãi để thay đổi trạng thái");
                return ResponseEntity.badRequest().body(response);
            }
            
            int successCount = 0;
            int errorCount = 0;
            List<String> errorMessages = new ArrayList<>();
            
            for (Integer id : ids) {
                try {
                    KhuyenMai khuyenMai = khuyenMaiService.findByMaKhuyenMai(id);
                    if (khuyenMai != null && khuyenMai.getCuaHang().getMaCuaHang().equals(cuaHang.getMaCuaHang())) {
                        khuyenMaiService.changeStatus(id, status);
                        successCount++;
                    } else {
                        errorCount++;
                        errorMessages.add("Khuyến mãi ID " + id + " không tồn tại hoặc không thuộc về cửa hàng của bạn");
                    }
                } catch (Exception e) {
                    errorCount++;
                    errorMessages.add("Lỗi khi cập nhật khuyến mãi ID " + id + ": " + e.getMessage());
                }
            }
            
            response.put("success", true);
            response.put("message", "Đã cập nhật trạng thái cho " + successCount + " khuyến mãi thành công");
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

    @PostMapping("/bulk-delete")
    @ResponseBody
    public ResponseEntity<?> bulkDelete(
            @RequestParam("ids") List<Integer> ids,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            CuaHang cuaHang = getCurrentStore(authentication);
            if (cuaHang == null) {
                response.put("success", false);
                response.put("message", "Bạn chưa đăng nhập hoặc chưa có cửa hàng.");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (Boolean.FALSE.equals(cuaHang.getTrangThai())) {
                response.put("success", false);
                response.put("message", STORE_LOCKED_MESSAGE);
                return ResponseEntity.badRequest().body(response);
            }
            
            if (ids == null || ids.isEmpty()) {
                response.put("success", false);
                response.put("message", "Vui lòng chọn ít nhất một khuyến mãi để xóa");
                return ResponseEntity.badRequest().body(response);
            }
            
            int successCount = 0;
            int errorCount = 0;
            List<String> errorMessages = new ArrayList<>();
            
            for (Integer id : ids) {
                try {
                    KhuyenMai khuyenMai = khuyenMaiService.findByMaKhuyenMai(id);
                    if (khuyenMai != null && khuyenMai.getCuaHang().getMaCuaHang().equals(cuaHang.getMaCuaHang())) {
                        khuyenMaiService.deleteKhuyenMai(id);
                        successCount++;
                    } else {
                        errorCount++;
                        errorMessages.add("Khuyến mãi ID " + id + " không tồn tại hoặc không thuộc về cửa hàng của bạn");
                    }
                } catch (Exception e) {
                    errorCount++;
                    errorMessages.add("Lỗi khi xóa khuyến mãi ID " + id + ": " + e.getMessage());
                }
            }
            
            response.put("success", true);
            response.put("message", "Đã xóa " + successCount + " khuyến mãi thành công");
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

    @GetMapping("/import")
    public String showImportPage(Authentication authentication, Model model) {
        NguoiDung currentUser = getCurrentUser(authentication);
        CuaHang cuaHang = getCurrentStore(authentication);

        model.addAttribute("user", currentUser); 
        
        if (cuaHang == null) {
            return "redirect:/vendor/dashboard";
        }
        
        if (Boolean.FALSE.equals(cuaHang.getTrangThai())) {
            model.addAttribute("errorMessage", STORE_LOCKED_MESSAGE);
            return "redirect:/vendor/promotions";
        }
        
        model.addAttribute("cuaHang", cuaHang);
        return "vendor/promotions/import-excel";
    }

    @PostMapping("/import")
    @ResponseBody
    public ResponseEntity<?> importKhuyenMaiFromExcel(
            @RequestParam("excelFiles") List<MultipartFile> excelFiles,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            CuaHang cuaHang = getCurrentStore(authentication);
            if (cuaHang == null) {
                response.put("success", false);
                response.put("message", "Bạn chưa đăng nhập hoặc chưa có cửa hàng.");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (Boolean.FALSE.equals(cuaHang.getTrangThai())) {
                response.put("success", false);
                response.put("message", STORE_LOCKED_MESSAGE);
                return ResponseEntity.badRequest().body(response);
            }
            
            KhuyenMaiImportResult importResult = excelImportService.importAndSaveKhuyenMaiFromMultipleExcelForVendor(excelFiles, cuaHang);
            
            if (importResult.isSuccess()) {
                response.put("success", true);
                response.put("message", importResult.getMessage());
                response.put("totalFiles", excelFiles.size());
                response.put("totalRecords", importResult.getTotalRecords());
                response.put("successCount", importResult.getSuccessCount());
                response.put("errorCount", importResult.getErrorCount());
                response.put("errors", importResult.getErrors());
            } else {
                response.put("success", false);
                response.put("message", importResult.getMessage());
                response.put("errors", importResult.getErrors());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi import: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/download-template")
    public void downloadTemplate(Authentication authentication, HttpServletResponse response) throws IOException {
        try {
            CuaHang cuaHang = getCurrentStore(authentication);
            if (cuaHang == null) {
                throw new RuntimeException("Bạn chưa đăng nhập hoặc chưa có cửa hàng.");
            }
            
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=template_khuyen_mai_vendor.xlsx");
            
            byte[] templateBytes = excelImportService.generateKhuyenMaiTemplateForVendor(cuaHang);
            response.getOutputStream().write(templateBytes);
            response.getOutputStream().flush();
            
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tải template: " + e.getMessage());
        }
    }

    // ============= EXPORT METHODS =============

    @PostMapping("/export-all")
    public void exportAllKhuyenMai(
            Authentication authentication,
            HttpServletResponse response) throws IOException {
        
        try {
            CuaHang cuaHang = getCurrentStore(authentication);
            if (cuaHang == null) {
                throw new RuntimeException("Bạn chưa đăng nhập hoặc chưa có cửa hàng.");
            }
            
            List<KhuyenMai> khuyenMaiList = khuyenMaiService.findByCuaHang(cuaHang);
            
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=all_promotions_" + cuaHang.getMaCuaHang() + ".xlsx");
            
            byte[] excelBytes = excelExportService.exportPromotionsToExcel(khuyenMaiList, cuaHang);
            response.getOutputStream().write(excelBytes);
            response.getOutputStream().flush();
            
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xuất khuyến mãi: " + e.getMessage());
        }
    }

    @PostMapping("/export-selected")
    public void exportSelectedKhuyenMai(
            @RequestParam("promotionIds") List<Integer> promotionIds,
            Authentication authentication,
            HttpServletResponse response) throws IOException {
        
        try {
            CuaHang cuaHang = getCurrentStore(authentication);
            if (cuaHang == null) {
                throw new RuntimeException("Bạn chưa đăng nhập hoặc chưa có cửa hàng.");
            }
            
            List<KhuyenMai> khuyenMaiList = new ArrayList<>();
            for (Integer id : promotionIds) {
                KhuyenMai khuyenMai = khuyenMaiService.findByMaKhuyenMai(id);
                if (khuyenMai != null && khuyenMai.getCuaHang().getMaCuaHang().equals(cuaHang.getMaCuaHang())) {
                    khuyenMaiList.add(khuyenMai);
                }
            }
            
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=selected_promotions_" + cuaHang.getMaCuaHang() + ".xlsx");
            
            byte[] excelBytes = excelExportService.exportPromotionsToExcel(khuyenMaiList, cuaHang);
            response.getOutputStream().write(excelBytes);
            response.getOutputStream().flush();
            
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xuất khuyến mãi: " + e.getMessage());
        }
    }

    @GetMapping("/export-detail/{id}")
    public void exportPromotionDetail(
            @PathVariable Integer id,
            Authentication authentication,
            HttpServletResponse response) throws IOException {
        
        try {
            CuaHang cuaHang = getCurrentStore(authentication);
            if (cuaHang == null) {
                throw new RuntimeException("Bạn chưa đăng nhập hoặc chưa có cửa hàng.");
            }
            
            KhuyenMai khuyenMai = khuyenMaiService.findByMaKhuyenMai(id);
            if (khuyenMai == null || !khuyenMai.getCuaHang().getMaCuaHang().equals(cuaHang.getMaCuaHang())) {
                throw new RuntimeException("Khuyến mãi không tồn tại hoặc không thuộc về cửa hàng của bạn");
            }
            
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=promotion_detail_" + khuyenMai.getMaGiamGia() + ".xlsx");
            
            byte[] excelBytes = excelExportService.exportPromotionDetailToExcel(khuyenMai);
            response.getOutputStream().write(excelBytes);
            response.getOutputStream().flush();
            
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xuất chi tiết khuyến mãi: " + e.getMessage());
        }
    }
}