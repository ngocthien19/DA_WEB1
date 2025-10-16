package vn.iotstar.controller.vendor;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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

import jakarta.servlet.http.HttpServletResponse;
import vn.iotstar.entity.SanPham;
import vn.iotstar.entity.CuaHang;
import vn.iotstar.entity.DanhMuc;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.model.ExcelImportResult;
import vn.iotstar.model.SanPhamModel;
import vn.iotstar.service.SanPhamService;
import vn.iotstar.service.CuaHangService;
import vn.iotstar.service.DanhMucService;
import vn.iotstar.service.impl.ExcelImportService;
import vn.iotstar.service.impl.UserDetailsServiceImpl;

@Controller
@RequestMapping("/vendor/products")
@PreAuthorize("hasRole('VENDOR')")
public class VendorSanPhamController {

    @Autowired
    private SanPhamService sanPhamService;
    
    @Autowired
    private CuaHangService cuaHangService;
    
    @Autowired
    private DanhMucService danhMucService;
    
    @Autowired
    private UserDetailsServiceImpl userDetailsService;
    
    @Autowired
    private ExcelImportService excelImportService;

    private static final String STORE_LOCKED_MESSAGE = "Cửa hàng của bạn đang bị Admin khóa (Ngừng hoạt động). Bạn không thể thực hiện các thao tác quản lý sản phẩm.";

    // ============= PAGE DISPLAY METHODS =============

    @GetMapping
    public String listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "maSanPham_desc") String sortBy, 
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String loaiSanPhamFilter,
            Authentication authentication,
            Model model) {
        
        NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
        CuaHang currentStore = getCurrentStore(currentUser);
        
        // CHECK 1: Nếu chưa có cửa hàng
        if (currentStore == null) {
            model.addAttribute("errorMessage", "Bạn chưa có cửa hàng. Vui lòng tạo cửa hàng trước khi quản lý sản phẩm.");
            model.addAttribute("cuaHang", null); 
            model.addAttribute("user", currentUser);
            return "vendor/products/products";
        }

        // CHECK 2: Nếu cửa hàng bị khóa
        if (currentStore.getTrangThai() == false) {
             model.addAttribute("errorMessage", STORE_LOCKED_MESSAGE);
             model.addAttribute("cuaHang", currentStore);
             model.addAttribute("user", currentUser);
        }

        // FIXED: Xử lý sắp xếp
        String[] sortParams = parseSortParametersFromDropdown(sortBy);
        Sort sort = createSort(sortParams[0], sortParams[1]);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Get products with filtering - LUÔN LẤY DỮ LIỆU
        Page<SanPham> productPage = getFilteredProductsForVendor(currentStore, keyword, loaiSanPhamFilter, pageable);
        
        // Thống kê sản phẩm - SỬA LẠI: Lấy tổng số sản phẩm từ database
        if (currentStore != null) {
            // Tổng số sản phẩm (không bị ảnh hưởng bởi tìm kiếm/lọc)
            long totalProducts = sanPhamService.findByCuaHang(currentStore).size();
            
            // Số sản phẩm đang hoạt động (không bị ảnh hưởng bởi tìm kiếm/lọc)
            long activeProductsCount = sanPhamService.findByCuaHangAndTrangThai(currentStore, true).size();
            
            // Số sản phẩm ngừng bán (không bị ảnh hưởng bởi tìm kiếm/lọc)
            long inactiveProductsCount = sanPhamService.findByCuaHangAndTrangThai(currentStore, false).size();
            
            model.addAttribute("totalProductsCount", totalProducts);
            model.addAttribute("activeProductsCount", activeProductsCount);
            model.addAttribute("inactiveProductsCount", inactiveProductsCount);
        }
        
        // Prepare model attributes
        prepareModelAttributesForVendor(model, currentUser, currentStore, productPage, keyword, sortParams[0], sortParams[1], loaiSanPhamFilter);
        
        return "vendor/products/products";
    }

    @GetMapping("/add")
    public String showAddForm(Authentication authentication, Model model) {
        NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
        CuaHang currentStore = getCurrentStore(currentUser);
        
        if (currentStore == null) {
            model.addAttribute("errorMessage", "Bạn chưa có cửa hàng. Vui lòng tạo cửa hàng trước khi thêm sản phẩm.");
            return "redirect:/vendor/products";
        }
        
        // CHECK: Nếu cửa hàng bị khóa
        if (currentStore.getTrangThai() == false) {
            // Thêm thông báo và chuyển hướng
            model.addAttribute("errorMessage", STORE_LOCKED_MESSAGE);
            return "redirect:/vendor/products";
        }
        
        prepareProductFormModelForVendor(model, currentUser, currentStore, null, false);
        return "vendor/products/product-form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(
            @PathVariable Integer id,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        try {
            NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
            CuaHang currentStore = getCurrentStore(currentUser);
            
            if (currentStore == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn chưa có cửa hàng.");
                return "redirect:/vendor/products";
            }
            
            // CHECK: Nếu cửa hàng bị khóa
            if (currentStore.getTrangThai() == false) {
                 redirectAttributes.addFlashAttribute("errorMessage", STORE_LOCKED_MESSAGE);
                 return "redirect:/vendor/products";
            }

            SanPham sanPham = getProductByIdAndStore(id, currentStore);
            SanPhamModel sanPhamModel = convertToSanPhamModel(sanPham);
            prepareProductFormModelForVendor(model, currentUser, currentStore, sanPhamModel, true);
            return "vendor/products/product-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/vendor/products";
        }
    }

    @GetMapping("/view/{id}")
    public String viewProduct(
            @PathVariable Integer id,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        try {
            NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
            CuaHang currentStore = getCurrentStore(currentUser);
            
            if (currentStore == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn chưa có cửa hàng.");
                return "redirect:/vendor/products";
            }
            
            // VIEW không bị chặn
            SanPham sanPham = getProductByIdAndStore(id, currentStore);
            model.addAttribute("user", currentUser);
            model.addAttribute("cuaHang", currentStore); 
            model.addAttribute("sanPham", sanPham);
            return "vendor/products/product-view";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/vendor/products";
        }
    }

    @GetMapping("/import")
    public String showImportPage(Authentication authentication, Model model) {
        NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
        CuaHang currentStore = getCurrentStore(currentUser);
        
        if (currentStore == null) {
            model.addAttribute("errorMessage", "Bạn chưa có cửa hàng. Vui lòng tạo cửa hàng trước khi import sản phẩm.");
            return "redirect:/vendor/products";
        }
        
        // CHECK: Nếu cửa hàng bị khóa
        if (currentStore.getTrangThai() == false) {
            model.addAttribute("errorMessage", STORE_LOCKED_MESSAGE);
            return "redirect:/vendor/products";
        }
        
        model.addAttribute("user", currentUser);
        model.addAttribute("cuaHang", currentStore);
        return "vendor/products/import-excel";
    }

    // ============= CRUD OPERATIONS (CẦN CHẶN) =============

    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<?> addProduct(
            @RequestParam("tenSanPham") String tenSanPham,
            @RequestParam("maDanhMuc") Integer maDanhMuc,
            @RequestParam(value = "moTaSanPham", required = false) String moTaSanPham,
            @RequestParam("giaBan") BigDecimal giaBan,
            @RequestParam("soLuongConLai") Integer soLuongConLai,
            @RequestParam("loaiSanPham") String loaiSanPham,
            @RequestParam(value = "trangThai", defaultValue = "true") Boolean trangThai,
            @RequestParam(value = "hinhAnh", required = false) MultipartFile hinhAnh,
            Authentication authentication) {
        
        NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
        CuaHang currentStore = getCurrentStore(currentUser);
        
        if (currentStore == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Bạn chưa có cửa hàng. Vui lòng tạo cửa hàng trước khi thêm sản phẩm.");
            return ResponseEntity.badRequest().body(response);
        }

        // CHECK: Nếu cửa hàng bị khóa
        if (currentStore.getTrangThai() == false) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", STORE_LOCKED_MESSAGE);
            return ResponseEntity.badRequest().body(response);
        }
        
        return processProductOperationForVendor(null, currentStore, tenSanPham, maDanhMuc, moTaSanPham, 
                                              giaBan, soLuongConLai, loaiSanPham, trangThai, hinhAnh, false);
    }

    @PostMapping("/edit/{id}")
    @ResponseBody
    public ResponseEntity<?> updateProduct(
            @PathVariable Integer id,
            @RequestParam("tenSanPham") String tenSanPham,
            @RequestParam("maDanhMuc") Integer maDanhMuc,
            @RequestParam(value = "moTaSanPham", required = false) String moTaSanPham,
            @RequestParam("giaBan") BigDecimal giaBan,
            @RequestParam("soLuongConLai") Integer soLuongConLai,
            @RequestParam("loaiSanPham") String loaiSanPham,
            @RequestParam("trangThai") Boolean trangThai,
            @RequestParam(value = "hinhAnh", required = false) MultipartFile hinhAnh,
            Authentication authentication) {
        
        NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
        CuaHang currentStore = getCurrentStore(currentUser);
        
        if (currentStore == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Bạn chưa có cửa hàng.");
            return ResponseEntity.badRequest().body(response);
        }
        
        Map<String, Object> response = new HashMap<>();
        
        try {
             // CHECK: Nếu cửa hàng bị khóa
            if (currentStore.getTrangThai() == false) {
                response.put("success", false);
                response.put("message", STORE_LOCKED_MESSAGE);
                return ResponseEntity.badRequest().body(response);
            }
            
            // ========== KIỂM TRA CÓ THỂ CẬP NHẬT KHÔNG ==========
            if (!sanPhamService.canUpdateProduct(id)) {
                response.put("success", false);
                response.put("message", "Không thể cập nhật sản phẩm vì cửa hàng đang bị khóa");
                return ResponseEntity.badRequest().body(response);
            }
            
            // ========== KIỂM TRA SẢN PHẨM THUỘC VỀ CỬA HÀNG HIỆN TẠI ==========
            SanPham existingProduct = getProductByIdAndStore(id, currentStore);
            
            return processProductOperationForVendor(id, currentStore, tenSanPham, maDanhMuc, moTaSanPham, 
                                                  giaBan, soLuongConLai, loaiSanPham, trangThai, hinhAnh, true);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteProduct(
            @PathVariable Integer id,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
            CuaHang currentStore = getCurrentStore(currentUser);
            
            if (currentStore == null) {
                response.put("success", false);
                response.put("message", "Bạn chưa có cửa hàng.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // CHECK: Nếu cửa hàng bị khóa
            if (currentStore.getTrangThai() == false) {
                response.put("success", false);
                response.put("message", STORE_LOCKED_MESSAGE);
                return ResponseEntity.badRequest().body(response);
            }

            // Kiểm tra sản phẩm thuộc về cửa hàng hiện tại
            SanPham sanPham = getProductByIdAndStore(id, currentStore);
            sanPhamService.deleteProduct(id);
            
            response.put("success", true);
            response.put("message", "Xóa sản phẩm thành công");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @DeleteMapping("/bulk-delete")
    @ResponseBody
    public ResponseEntity<?> bulkDeleteProducts(
            @RequestBody Map<String, List<Integer>> request,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
            CuaHang currentStore = getCurrentStore(currentUser);
            
            if (currentStore == null) {
                response.put("success", false);
                response.put("message", "Bạn chưa có cửa hàng.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // CHECK: Nếu cửa hàng bị khóa
            if (currentStore.getTrangThai() == false) {
                response.put("success", false);
                response.put("message", STORE_LOCKED_MESSAGE);
                return ResponseEntity.badRequest().body(response);
            }
            
            List<Integer> productIds = request.get("productIds");
            
            if (productIds == null || productIds.isEmpty()) {
                response.put("success", false);
                response.put("message", "Không có sản phẩm nào được chọn để xóa");
                return ResponseEntity.badRequest().body(response);
            }
            
            int deletedCount = 0;
            List<String> errorMessages = new ArrayList<>();
            
            for (Integer productId : productIds) {
                try {
                    // Kiểm tra sản phẩm thuộc về cửa hàng hiện tại
                    SanPham sanPham = getProductByIdAndStore(productId, currentStore);
                    sanPhamService.deleteProduct(productId);
                    deletedCount++;
                } catch (Exception e) {
                    errorMessages.add("Sản phẩm ID " + productId + ": " + e.getMessage());
                }
            }
            
            if (deletedCount > 0) {
                response.put("success", true);
                response.put("message", "Đã xóa thành công " + deletedCount + " sản phẩm");
                response.put("deletedCount", deletedCount);
                
                if (!errorMessages.isEmpty()) {
                    response.put("errors", errorMessages);
                    response.put("hasErrors", true);
                }
            } else {
                response.put("success", false);
                response.put("message", "Không thể xóa bất kỳ sản phẩm nào");
                response.put("errors", errorMessages);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi khi xóa nhiều sản phẩm: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/change-status/{id}")
    @ResponseBody
    public ResponseEntity<?> changeStatus(
            @PathVariable Integer id,
            @RequestParam Boolean status,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
            CuaHang currentStore = getCurrentStore(currentUser);
            
            if (currentStore == null) {
                response.put("success", false);
                response.put("message", "Bạn chưa có cửa hàng.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // CHECK: Nếu cửa hàng bị khóa
            if (currentStore.getTrangThai() == false) {
                response.put("success", false);
                response.put("message", STORE_LOCKED_MESSAGE);
                return ResponseEntity.badRequest().body(response);
            }
            
            // ========== KIỂM TRA CÓ THỂ THAY ĐỔI TRẠNG THÁI KHÔNG ==========
            if (!sanPhamService.canChangeProductStatus(id, status)) {
                response.put("success", false);
                response.put("message", "Không thể kích hoạt sản phẩm vì danh mục đang ngừng hoạt động");
                return ResponseEntity.badRequest().body(response);
            }
            
            // ========== KIỂM TRA CÓ THỂ CẬP NHẬT KHÔNG ==========
            if (!sanPhamService.canUpdateProduct(id)) {
                response.put("success", false);
                response.put("message", "Không thể thay đổi trạng thái sản phẩm vì cửa hàng đang bị khóa");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Kiểm tra sản phẩm thuộc về cửa hàng hiện tại
            SanPham sanPham = getProductByIdAndStore(id, currentStore);
            sanPham.setTrangThai(status);
            sanPhamService.save(sanPham);
            
            response.put("success", true);
            response.put("message", "Thay đổi trạng thái thành công!");
            response.put("trangThai", sanPham.getTrangThai());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ============= BULK STATUS CHANGE =============
    
    @PostMapping("/bulk-change-status")
    @ResponseBody
    public ResponseEntity<?> bulkChangeStatus(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
            CuaHang currentStore = getCurrentStore(currentUser);
            
            if (currentStore == null) {
                response.put("success", false);
                response.put("message", "Bạn chưa có cửa hàng.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // CHECK: Nếu cửa hàng bị khóa
            if (currentStore.getTrangThai() == false) {
                response.put("success", false);
                response.put("message", STORE_LOCKED_MESSAGE);
                return ResponseEntity.badRequest().body(response);
            }
            
            // FIX: Xử lý productIds từ List<String> sang List<Integer>
            @SuppressWarnings("unchecked")
            List<String> productIdsString = (List<String>) request.get("productIds");
            Boolean status = (Boolean) request.get("status");
            
            if (productIdsString == null || productIdsString.isEmpty()) {
                response.put("success", false);
                response.put("message", "Không có sản phẩm nào được chọn");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (status == null) {
                response.put("success", false);
                response.put("message", "Trạng thái không hợp lệ");
                return ResponseEntity.badRequest().body(response);
            }
            
            // FIX: Chuyển đổi từ String sang Integer
            List<Integer> productIds = new ArrayList<>();
            for (String productIdStr : productIdsString) {
                try {
                    productIds.add(Integer.parseInt(productIdStr));
                } catch (NumberFormatException e) {
                    // Bỏ qua ID không hợp lệ
                    System.err.println("ID sản phẩm không hợp lệ: " + productIdStr);
                }
            }
            
            if (productIds.isEmpty()) {
                response.put("success", false);
                response.put("message", "Không có ID sản phẩm hợp lệ nào được chọn");
                return ResponseEntity.badRequest().body(response);
            }
            
            int updatedCount = 0;
            List<String> errorMessages = new ArrayList<>();
            
            for (Integer productId : productIds) {
                try {
                    // Kiểm tra sản phẩm thuộc về cửa hàng hiện tại
                    SanPham sanPham = getProductByIdAndStore(productId, currentStore);
                    
                    // Kiểm tra có thể thay đổi trạng thái không
                    if (!sanPhamService.canChangeProductStatus(productId, status)) {
                        errorMessages.add("Sản phẩm '" + sanPham.getTenSanPham() + "' không thể kích hoạt vì danh mục đang ngừng hoạt động");
                        continue;
                    }
                    
                    // Kiểm tra có thể cập nhật không
                    if (!sanPhamService.canUpdateProduct(productId)) {
                        errorMessages.add("Sản phẩm '" + sanPham.getTenSanPham() + "' không thể thay đổi vì cửa hàng đang bị khóa");
                        continue;
                    }
                    
                    sanPham.setTrangThai(status);
                    sanPhamService.save(sanPham);
                    updatedCount++;
                    
                } catch (Exception e) {
                    errorMessages.add("Sản phẩm ID " + productId + ": " + e.getMessage());
                }
            }
            
            if (updatedCount > 0) {
                response.put("success", true);
                response.put("message", "Đã cập nhật trạng thái thành công cho " + updatedCount + " sản phẩm");
                response.put("updatedCount", updatedCount);
                
                if (!errorMessages.isEmpty()) {
                    response.put("errors", errorMessages);
                    response.put("hasErrors", true);
                }
            } else {
                response.put("success", false);
                response.put("message", "Không thể cập nhật trạng thái cho bất kỳ sản phẩm nào");
                response.put("errors", errorMessages);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi khi thay đổi trạng thái hàng loạt: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ============= EXCEL OPERATIONS (CẦN CHẶN IMPORT) =============

    @PostMapping("/import")
    @ResponseBody
    public ResponseEntity<?> importProductsFromExcel(
            @RequestParam("excelFiles") List<MultipartFile> excelFiles,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
            CuaHang currentStore = getCurrentStore(currentUser);
            
            if (currentStore == null) {
                response.put("success", false);
                response.put("message", "Bạn chưa có cửa hàng. Vui lòng tạo cửa hàng trước khi import sản phẩm.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // CHECK: Nếu cửa hàng bị khóa
            if (currentStore.getTrangThai() == false) {
                response.put("success", false);
                response.put("message", STORE_LOCKED_MESSAGE);
                return ResponseEntity.badRequest().body(response);
            }
            
            // Validate files
            validateExcelFiles(excelFiles);
            
            // Import and save products for vendor (không cần mã cửa hàng trong file)
            ExcelImportResult importResult = excelImportService.importAndSaveProductsFromMultipleExcelForVendor(excelFiles, currentStore);
            
            // Prepare response
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

    // EXPORT/DOWNLOAD không bị chặn

    @GetMapping("/export-excel")
    public void exportProductsToExcel(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String loaiSanPhamFilter,
            @RequestParam(defaultValue = "ngayNhap") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) List<Integer> selectedProducts,
            Authentication authentication,
            HttpServletResponse response) {
        
        try {
            NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
            CuaHang currentStore = getCurrentStore(currentUser);
            
            if (currentStore == null) {
                throw new RuntimeException("Bạn chưa có cửa hàng.");
            }
            
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=san_pham_cua_hang_" + currentStore.getMaCuaHang() + ".xlsx");
            
            List<SanPham> products = getProductsForExportForVendor(currentStore, keyword, loaiSanPhamFilter, sortBy, sortDir, selectedProducts);
            Workbook workbook = createExportWorkbook(products);
            workbook.write(response.getOutputStream());
            workbook.close();
            
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xuất Excel: " + e.getMessage());
        }
    }

    @GetMapping("/download-template")
    public void downloadTemplate(
            Authentication authentication,
            HttpServletResponse response) throws IOException {
        
        try {
            NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
            CuaHang currentStore = getCurrentStore(currentUser);
            
            if (currentStore == null) {
                throw new RuntimeException("Bạn chưa có cửa hàng.");
            }
            
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=template_san_pham_vendor.xlsx");
            
            byte[] templateBytes = excelImportService.generateExcelTemplateForVendor(currentStore);
            response.getOutputStream().write(templateBytes);
            response.getOutputStream().flush();
            
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tải template: " + e.getMessage());
        }
    }

    // ============= API ENDPOINTS =============

    @GetMapping("/loai-san-pham/{maDanhMuc}")
    @ResponseBody
    public ResponseEntity<List<String>> getLoaiSanPhamByDanhMuc(
            @PathVariable Integer maDanhMuc,
            Authentication authentication) {
        
        try {
            NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
            CuaHang currentStore = getCurrentStore(currentUser);
            
            if (currentStore == null) {
                return ResponseEntity.badRequest().body(new ArrayList<>());
            }
            
            // Lấy loại sản phẩm theo danh mục VÀ cửa hàng hiện tại
            List<String> loaiSanPhamList = getLoaiSanPhamByDanhMucAndStore(maDanhMuc, currentStore);
            return ResponseEntity.ok(loaiSanPhamList);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }
    }

    // ============= PRIVATE HELPER METHODS =============

    private CuaHang getCurrentStore(NguoiDung user) {
        if (user == null) return null;
        
        List<CuaHang> userStores = cuaHangService.findByNguoiDung(user);
        if (userStores != null && !userStores.isEmpty()) {
            return userStores.get(0); // Mỗi vendor chỉ có 1 cửa hàng
        }
        return null;
    }

    private SanPham getProductByIdAndStore(Integer id, CuaHang store) {
        SanPham sanPham = sanPhamService.findByMaSanPham(id);
        if (sanPham == null) {
            throw new RuntimeException("Sản phẩm không tồn tại");
        }
        if (!sanPham.getCuaHang().getMaCuaHang().equals(store.getMaCuaHang())) {
            throw new RuntimeException("Sản phẩm không thuộc về cửa hàng của bạn");
        }
        return sanPham;
    }

    private Page<SanPham> getFilteredProductsForVendor(CuaHang currentStore, String keyword, String loaiSanPhamFilter, Pageable pageable) {
        Page<SanPham> productPage;
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            // Tìm kiếm trong cửa hàng hiện tại
            productPage = sanPhamService.findByCuaHangAndTenSanPhamContainingIgnoreCaseOrMoTaSanPhamContainingIgnoreCase(
                currentStore, keyword, keyword, pageable);
        } else {
            // Lấy tất cả sản phẩm của cửa hàng hiện tại
            productPage = sanPhamService.findByCuaHang(currentStore, pageable);
        }
        
        // Apply LoaiSanPham filter
        if (loaiSanPhamFilter != null && !loaiSanPhamFilter.trim().isEmpty()) {
            List<SanPham> filteredProducts = productPage.getContent().stream()
                    .filter(product -> loaiSanPhamFilter.equals(product.getLoaiSanPham()))
                    .collect(Collectors.toList());
            
            productPage = new PageImpl<>(filteredProducts, pageable, filteredProducts.size());
        }
        
        return productPage;
    }

    private void prepareModelAttributesForVendor(Model model, NguoiDung user, CuaHang currentStore, 
                                               Page<SanPham> productPage, String keyword, 
                                               String sortBy, String sortDir, String loaiSanPhamFilter) {
        List<DanhMuc> categories = danhMucService.findAll();
        List<String> allLoaiSanPham = getAllDistinctLoaiSanPhamByStore(currentStore);
        
        // FIXED: Thêm thông tin sắp xếp cho view
        String currentSortBy = validateAndNormalizeSortBy(sortBy);
        String currentSortDir = validateAndNormalizeSortDir(sortDir);
        String oppositeSortDir = currentSortDir.equals("asc") ? "desc" : "asc";
        
        model.addAttribute("user", user);
        // FIX ĐỒNG BỘ: Dùng 'cuaHang' để khớp với header.html
        model.addAttribute("cuaHang", currentStore);
        model.addAttribute("productPage", productPage);
        model.addAttribute("categories", categories);
        model.addAttribute("allLoaiSanPham", allLoaiSanPham);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("sortBy", currentSortBy);
        model.addAttribute("sortDir", currentSortDir);
        model.addAttribute("oppositeSortDir", oppositeSortDir);
        model.addAttribute("loaiSanPhamFilter", loaiSanPhamFilter);
        model.addAttribute("currentSortField", currentSortBy);
        model.addAttribute("currentSortDirection", currentSortDir);
    }

    private void prepareProductFormModelForVendor(Model model, NguoiDung user, CuaHang currentStore, 
                                                 SanPhamModel sanPhamModel, boolean isEdit) {
        List<DanhMuc> categories = danhMucService.findAll();
        
        if (sanPhamModel == null) {
            List<String> availableLoaiSanPham = new ArrayList<>();
            if (!categories.isEmpty()) {
                availableLoaiSanPham = getLoaiSanPhamByDanhMucAndStore(categories.get(0).getMaDanhMuc(), currentStore);
            }
            
            sanPhamModel = SanPhamModel.builder()
                    .availableLoaiSanPham(availableLoaiSanPham)
                    .build();
        }
        
        model.addAttribute("user", user);
        // FIX ĐỒNG BỘ: Dùng 'cuaHang' để khớp với header.html
        model.addAttribute("cuaHang", currentStore);
        model.addAttribute("sanPhamModel", sanPhamModel);
        model.addAttribute("categories", categories);
        model.addAttribute("isEdit", isEdit);
    }

    private ResponseEntity<?> processProductOperationForVendor(Integer id, CuaHang currentStore, String tenSanPham, 
                                                              Integer maDanhMuc, String moTaSanPham, BigDecimal giaBan,
                                                              Integer soLuongConLai, String loaiSanPham, Boolean trangThai,
                                                              MultipartFile hinhAnh, boolean isUpdate) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate input
            validateProductInput(tenSanPham, maDanhMuc, giaBan, loaiSanPham);
            
            // Handle image upload
            String fileName = handleImageUpload(hinhAnh);
            
            // Create product model - TỰ ĐỘNG gán cửa hàng hiện tại
            SanPhamModel model = buildSanPhamModelForVendor(id, currentStore, tenSanPham, maDanhMuc, moTaSanPham,
                                                          giaBan, soLuongConLai, loaiSanPham, trangThai, fileName, isUpdate);
            
            // Save or update product
            if (isUpdate) {
                sanPhamService.updateProduct(id, model);
                response.put("message", "Cập nhật sản phẩm thành công!");
            } else {
                sanPhamService.createProduct(model);
                response.put("message", "Thêm sản phẩm thành công!");
            }
            
            response.put("success", true);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    private void validateProductInput(String tenSanPham, Integer maDanhMuc, 
                                    BigDecimal giaBan, String loaiSanPham) {
        if (tenSanPham == null || tenSanPham.trim().isEmpty()) {
            throw new RuntimeException("Tên sản phẩm không được để trống");
        }
        if (maDanhMuc == null) {
            throw new RuntimeException("Danh mục không được để trống");
        }
        if (giaBan == null || giaBan.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Giá bán phải lớn hơn 0");
        }
        if (loaiSanPham == null || loaiSanPham.trim().isEmpty()) {
            throw new RuntimeException("Loại sản phẩm không được để trống");
        }
    }

    private String handleImageUpload(MultipartFile hinhAnh) throws Exception {
        // Sử dụng cùng logic với Admin
        if (hinhAnh != null && !hinhAnh.isEmpty()) {
            String contentType = hinhAnh.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new RuntimeException("File phải là ảnh (jpg, png, gif)");
            }
            
            if (hinhAnh.getSize() > 10 * 1024 * 1024) {
                throw new RuntimeException("Kích thước file không được vượt quá 10MB");
            }
            
            return saveProductImage(hinhAnh);
        }
        return null;
    }

    private SanPhamModel buildSanPhamModelForVendor(Integer id, CuaHang currentStore, String tenSanPham, Integer maDanhMuc,
                                                   String moTaSanPham, BigDecimal giaBan, Integer soLuongConLai,
                                                   String loaiSanPham, Boolean trangThai, String fileName, boolean isUpdate) {
        SanPhamModel.SanPhamModelBuilder builder = SanPhamModel.builder()
                .tenSanPham(tenSanPham.trim())
                .maCuaHang(currentStore.getMaCuaHang()) // TỰ ĐỘNG gán cửa hàng hiện tại
                .maDanhMuc(maDanhMuc)
                .moTaSanPham(moTaSanPham != null ? moTaSanPham.trim() : null)
                .giaBan(giaBan)
                .soLuongConLai(soLuongConLai != null ? soLuongConLai : 0)
                .loaiSanPham(loaiSanPham.trim())
                .trangThai(trangThai)
                .hinhAnh(fileName);

        if (!isUpdate) {
            builder.soLuongDaBan(0)
                   .luotThich(BigDecimal.ZERO)
                   .ngayNhap(new Date());
        }

        if (id != null) {
            builder.maSanPham(id);
        }

        return builder.build();
    }

    private void validateExcelFiles(List<MultipartFile> excelFiles) {
        // Sử dụng cùng logic với Admin
        if (excelFiles == null || excelFiles.isEmpty()) {
            throw new RuntimeException("Vui lòng chọn ít nhất 1 file Excel để import");
        }
        
        if (excelFiles.size() > 5) {
            throw new RuntimeException("Chỉ được phép import tối đa 5 file");
        }
        
        for (MultipartFile file : excelFiles) {
            if (file.isEmpty()) {
                throw new RuntimeException("File không được để trống");
            }
            
            String contentType = file.getContentType();
            if (contentType == null || 
                (!contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") && 
                 !contentType.equals("application/vnd.ms-excel"))) {
                throw new RuntimeException("Chỉ hỗ trợ file Excel (.xlsx)");
            }
        }
    }

    private List<SanPham> getProductsForExportForVendor(CuaHang currentStore, String keyword, String loaiSanPhamFilter, 
                                                       String sortBy, String sortDir, List<Integer> selectedProducts) {
        if (selectedProducts != null && !selectedProducts.isEmpty()) {
            // Chỉ export các sản phẩm được chọn VÀ thuộc về cửa hàng hiện tại
            List<SanPham> allSelected = sanPhamService.findAllById(selectedProducts);
            return allSelected.stream()
                    .filter(product -> product.getCuaHang().getMaCuaHang().equals(currentStore.getMaCuaHang()))
                    .collect(Collectors.toList());
        }
        
        // FIXED: Sử dụng cùng logic xử lý sort như trong listProducts
        String[] sortParams = parseSortParametersFromDropdown(sortBy);
        Sort sort = createSort(sortParams[0], sortParams[1]);
        Pageable pageable = PageRequest.of(0, 1000, sort);
        
        Page<SanPham> productPage = getFilteredProductsForVendor(currentStore, keyword, loaiSanPhamFilter, pageable);
        return productPage.getContent();
    }

    // ============= UTILITY METHODS (giống Admin) =============

    private SanPhamModel convertToSanPhamModel(SanPham sanPham) {
        List<String> availableLoaiSanPham = getLoaiSanPhamByDanhMucAndStore(
            sanPham.getDanhMuc().getMaDanhMuc(), sanPham.getCuaHang());
        
        return SanPhamModel.builder()
                .maSanPham(sanPham.getMaSanPham())
                .tenSanPham(sanPham.getTenSanPham())
                .maCuaHang(sanPham.getCuaHang().getMaCuaHang())
                .maDanhMuc(sanPham.getDanhMuc().getMaDanhMuc())
                .moTaSanPham(sanPham.getMoTaSanPham())
                .giaBan(sanPham.getGiaBan())
                .soLuongConLai(sanPham.getSoLuongConLai())
                .soLuongDaBan(sanPham.getSoLuongDaBan())
                .luotThich(sanPham.getLuotThich())
                .loaiSanPham(sanPham.getLoaiSanPham())
                .trangThai(sanPham.getTrangThai())
                .hinhAnh(sanPham.getHinhAnh())
                .ngayNhap(sanPham.getNgayNhap())
                .tenCuaHang(sanPham.getCuaHang().getTenCuaHang())
                .tenDanhMuc(sanPham.getDanhMuc().getTenDanhMuc())
                .availableLoaiSanPham(availableLoaiSanPham)
                .build();
    }

    private List<String> getLoaiSanPhamByDanhMucAndStore(Integer maDanhMuc, CuaHang store) {
        try {
            DanhMuc danhMuc = danhMucService.findByMaDanhMuc(maDanhMuc);
            List<String> loaiSanPhamList = new ArrayList<>();
            
            if (danhMuc != null) {
                // Lấy loại sản phẩm theo danh mục VÀ cửa hàng
                List<SanPham> storeProducts = sanPhamService.findByCuaHang(store);
                List<String> existingTypes = storeProducts.stream()
                        .filter(p -> p.getDanhMuc().getMaDanhMuc().equals(maDanhMuc))
                        .map(SanPham::getLoaiSanPham)
                        .distinct()
                        .collect(Collectors.toList());
                
                loaiSanPhamList.addAll(existingTypes);
            }
            
            return loaiSanPhamList;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<String> getAllDistinctLoaiSanPhamByStore(CuaHang store) {
        List<SanPham> storeProducts = sanPhamService.findByCuaHang(store);
        return storeProducts.stream()
                .map(SanPham::getLoaiSanPham)
                .distinct()
                .collect(Collectors.toList());
    }

    private String saveProductImage(MultipartFile hinhAnh) throws Exception {
        // Lấy thư mục làm việc hiện tại của hệ thống (nơi ứng dụng được chạy)
        String rootPath = System.getProperty("user.dir");
        
        // Tạo đường dẫn upload: [Dự án Root]/uploads/products/
        String uploadDir = rootPath + File.separator + "uploads" + File.separator + "products" + File.separator;
        
        File uploadPath = new File(uploadDir);
        
        // Nếu thư mục không tồn tại, tạo nó (mkdirs đảm bảo tạo cả thư mục cha)
        if (!uploadPath.exists()) {
            if (!uploadPath.mkdirs()) {
                throw new IOException("Không thể tạo thư mục upload: " + uploadDir);
            }
        }
        
        // Tạo tên file
        String originalFileName = hinhAnh.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        
        String fileName = "product_" + System.currentTimeMillis() + fileExtension;
        File dest = new File(uploadPath, fileName);
        
        // Thực hiện lưu file
        hinhAnh.transferTo(dest);
        
        // Trả về tên file (để lưu vào database)
        return fileName;
    }

    private Workbook createExportWorkbook(List<SanPham> products) {
        // Sử dụng cùng logic với Admin
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sản phẩm");
        
        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Mã SP", "Tên SP", "Danh mục", "Giá bán", "Số lượng", "Đã bán", "Loại SP", "Trạng thái", "Ngày nhập"};
        
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Create data rows
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        int rowNum = 1;
        
        for (SanPham product : products) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(product.getMaSanPham());
            row.createCell(1).setCellValue(product.getTenSanPham());
            row.createCell(2).setCellValue(product.getDanhMuc().getTenDanhMuc());
            row.createCell(3).setCellValue(product.getGiaBan().doubleValue());
            row.createCell(4).setCellValue(product.getSoLuongConLai());
            row.createCell(5).setCellValue(product.getSoLuongDaBan());
            row.createCell(6).setCellValue(product.getLoaiSanPham());
            row.createCell(7).setCellValue(product.getTrangThai() ? "Hoạt động" : "Ngừng hoạt động");
            row.createCell(8).setCellValue(dateFormat.format(product.getNgayNhap()));
        }
        
        // Auto size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
        
        return workbook;
    }

    // ============= SORTING UTILITY METHODS =============

    private String[] parseSortParametersFromDropdown(String sortBy) {
        // FIXED: Xử lý các giá trị sortBy từ dropdown để khớp với yêu cầu và fix lỗi
        switch (sortBy) {
            case "tenSanPham":
                return new String[]{"tenSanPham", "asc"}; // Tên A-Z
            case "giaBan":
                return new String[]{"giaBan", "asc"}; // Giá thấp -> cao
            case "giaBan_desc":
                return new String[]{"giaBan", "desc"}; // Giá cao -> thấp
            case "soLuongConLai":
                return new String[]{"soLuongConLai", "asc"}; // Số lượng ít -> nhiều
            case "soLuongConLai_desc":
                return new String[]{"soLuongConLai", "desc"}; // Số lượng nhiều -> ít
            case "maSanPham_desc": // Mặc định khi không có tham số
                return new String[]{"maSanPham", "desc"}; 
            case "ngayNhap":
            default:
                return new String[]{"ngayNhap", "desc"}; // Mới nhất (Ngày nhập giảm dần)
        }
    }

    private Sort createSort(String sortBy, String sortDir) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        
        // Sắp xếp chính
        Sort primarySort = Sort.by(direction, sortBy);
        
        // FIX LỖI: Kiểm tra nếu sắp xếp chính không phải là MaSanPham thì mới thêm MaSanPham giảm dần làm sắp xếp phụ
        if (!sortBy.equals("maSanPham")) {
            // Sắp xếp phụ theo ID giảm dần để đảm bảo thứ tự ổn định khi các trường chính bằng nhau
            Sort secondarySort = Sort.by(Sort.Direction.DESC, "maSanPham");
            return primarySort.and(secondarySort); 
        }
        
        // Nếu sortBy là maSanPham, chỉ trả về primarySort (tránh trùng lặp)
        return primarySort; 
    }
    
    // START FIX: Thêm lại các hàm validation bị thiếu
    private String validateAndNormalizeSortBy(String sortBy) {
        Set<String> allowedSortFields = new HashSet<>(Arrays.asList(
            "maSanPham", "tenSanPham", "giaBan", "soLuongConLai", "soLuongDaBan", "ngayNhap"
        ));
        // Loại bỏ hậu tố _desc khi kiểm tra
        String normalizedSortBy = sortBy.replace("_desc", "");
        
        // Đảm bảo trả về một trường hợp được phép hoặc mặc định 'maSanPham'
        if (allowedSortFields.contains(normalizedSortBy)) {
            return normalizedSortBy;
        }
        return "maSanPham";
    }

    private String validateAndNormalizeSortDir(String sortDir) {
        // Xác định hướng sắ xếp từ tham số hoặc từ hậu tố _desc
        if (sortDir.equalsIgnoreCase("asc")) {
            return "asc";
        }
        if (sortDir.equalsIgnoreCase("desc")) {
            return "desc";
        }
        
        // Nếu không có sortDir, kiểm tra trong chuỗi sortBy
        // Ví dụ: sortBy = "giaBan_desc"
        String currentSortByParam = (String) org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes().getAttribute("sortBy", org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);
        if (currentSortByParam != null && currentSortByParam.endsWith("_desc")) {
             return "desc";
        }
        
        return "desc"; // Mặc định cuối cùng
    }
    // END FIX
}