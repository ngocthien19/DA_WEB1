package vn.iotstar.controller.admin;

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
@RequestMapping("/admin/products")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSanPhamController {

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

    // ============= PAGE DISPLAY METHODS =============

    @GetMapping
    public String listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "maSanPham") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String loaiSanPhamFilter,
            Authentication authentication,
            Model model) {
        
        NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
        
        // FIXED: Xử lý đúng cách với các giá trị sortBy từ dropdown
        String[] sortParams = parseSortParametersFromDropdown(sortBy);
        Sort sort = createSort(sortParams[0], sortParams[1]);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Get products with filtering
        Page<SanPham> productPage = getFilteredProducts(keyword, loaiSanPhamFilter, pageable);
        
        // Prepare model attributes
        prepareModelAttributes(model, currentUser, productPage, keyword, sortParams[0], sortParams[1], loaiSanPhamFilter);
        
        return "admin/products/products";
    }

    // ============= PRIVATE HELPER METHODS - FIXED SORTING =============

    /**
     * Tạo Sort object từ tham số sắp xếp
     */
    private Sort createSort(String sortBy, String sortDir) {
        // Validate sort direction
        String actualSortDir = "desc".equalsIgnoreCase(sortDir) ? "desc" : "asc";
        
        // Xử lý các trường hợp đặc biệt cho giá trị mặc định
        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = "ngayNhap"; // Mặc định sort theo ngày nhập mới nhất
            actualSortDir = "desc";
        }
        
        return "desc".equalsIgnoreCase(actualSortDir) ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
    }

    /**
     * Validate và chuẩn hóa tham số sortBy
     */
    private String validateAndNormalizeSortBy(String sortBy) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            return "maSanPham"; // Giá trị mặc định
        }
        
        // Danh sách các trường được phép sắp xếp
        Set<String> allowedSortFields = new HashSet<>(Arrays.asList(
            "maSanPham", "tenSanPham", "giaBan", "soLuongConLai", 
            "soLuongDaBan", "ngayNhap", "trangThai", "loaiSanPham"
        ));
        
        // Kiểm tra nếu sortBy chứa hậu tố _asc hoặc _desc (từ frontend)
        String cleanSortBy = sortBy;
        if (sortBy.contains("_desc")) {
            cleanSortBy = sortBy.replace("_desc", "");
        } else if (sortBy.contains("_asc")) {
            cleanSortBy = sortBy.replace("_asc", "");
        }
        
        // Kiểm tra trường sắp xếp có hợp lệ không
        if (!allowedSortFields.contains(cleanSortBy)) {
            return "maSanPham"; // Fallback về giá trị mặc định
        }
        
        return cleanSortBy;
    }

    /**
     * Validate và chuẩn hóa tham số sortDir
     */
    private String validateAndNormalizeSortDir(String sortDir) {
        if (sortDir == null || sortDir.trim().isEmpty()) {
            return "desc"; // Giá trị mặc định
        }
        
        // Chỉ cho phép "asc" hoặc "desc"
        if (sortDir.equalsIgnoreCase("asc") || sortDir.equalsIgnoreCase("desc")) {
            return sortDir.toLowerCase();
        }
        
        return "desc"; // Fallback về giá trị mặc định
    }
    
    /**
     * Phân tích tham số sắp xếp từ dropdown (có thể chứa _desc, _asc)
     */
    private String[] parseSortParametersFromDropdown(String sortBy) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            return new String[]{"maSanPham", "desc"};
        }
        
        // Nếu sortBy chứa _desc hoặc _asc
        if (sortBy.contains("_desc")) {
            String field = sortBy.replace("_desc", "");
            return new String[]{field, "desc"};
        } else if (sortBy.contains("_asc")) {
            String field = sortBy.replace("_asc", "");
            return new String[]{field, "asc"};
        } else {
            // Mặc định cho các trường không có hậu tố
            switch (sortBy) {
                case "ngayNhap":
                    return new String[]{"ngayNhap", "desc"}; // Mới nhất đầu tiên
                case "giaBan":
                    return new String[]{"giaBan", "asc"}; // Giá thấp → cao
                case "soLuongConLai":
                    return new String[]{"soLuongConLai", "asc"}; // Số lượng ít → nhiều
                case "soLuongDaBan":
                    return new String[]{"soLuongDaBan", "asc"}; // Đã bán ít → nhiều
                case "luotThich":
                    return new String[]{"luotThich", "asc"}; // Lượt thích ít → nhiều
                case "saoDanhGia":
                    return new String[]{"saoDanhGia", "asc"}; // Đánh giá thấp → cao
                case "maSanPham":
                    return new String[]{"maSanPham", "asc"}; // Mã sản phẩm cũ → mới
                default:
                    // FIXED: Trả về chính xác field được chọn thay vì mặc định
                    return new String[]{sortBy, "asc"};
            }
        }
    }

    private Page<SanPham> getFilteredProducts(String keyword, String loaiSanPhamFilter, Pageable pageable) {
        Page<SanPham> productPage;
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            productPage = sanPhamService.searchProducts(keyword, pageable);
        } else {
            productPage = sanPhamService.findAll(pageable);
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

    private void prepareModelAttributes(Model model, NguoiDung user, Page<SanPham> productPage, 
                                      String keyword, String sortBy, String sortDir, String loaiSanPhamFilter) {
        List<CuaHang> stores = cuaHangService.findAll();
        List<DanhMuc> categories = danhMucService.findAll();
        List<String> allLoaiSanPham = getAllDistinctLoaiSanPham();
        
        // FIXED: Thêm thông tin sắp xếp cho view
        String currentSortBy = validateAndNormalizeSortBy(sortBy);
        String currentSortDir = validateAndNormalizeSortDir(sortDir);
        String oppositeSortDir = currentSortDir.equals("asc") ? "desc" : "asc";
        
        model.addAttribute("user", user);
        model.addAttribute("productPage", productPage);
        model.addAttribute("stores", stores);
        model.addAttribute("categories", categories);
        model.addAttribute("allLoaiSanPham", allLoaiSanPham);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("sortBy", currentSortBy);
        model.addAttribute("sortDir", currentSortDir);
        model.addAttribute("oppositeSortDir", oppositeSortDir); // Cho nút toggle
        model.addAttribute("loaiSanPhamFilter", loaiSanPhamFilter);
        
        // FIXED: Thêm thông tin về trường sắp xếp hiện tại cho từng cột
        model.addAttribute("currentSortField", currentSortBy);
        model.addAttribute("currentSortDirection", currentSortDir);
    }

    // Các phương thức khác giữ nguyên...
    @GetMapping("/add")
    public String showAddForm(Authentication authentication, Model model) {
        NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
        prepareProductFormModel(model, currentUser, null, false);
        return "admin/products/product-form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(
            @PathVariable Integer id,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        try {
            NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
            SanPham sanPham = getProductById(id);
            SanPhamModel sanPhamModel = convertToSanPhamModel(sanPham);
            prepareProductFormModel(model, currentUser, sanPhamModel, true);
            return "admin/products/product-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/products";
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
            SanPham sanPham = getProductById(id);
            model.addAttribute("user", currentUser);
            model.addAttribute("sanPham", sanPham);
            return "admin/products/product-view";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/products";
        }
    }

    @GetMapping("/import")
    public String showImportPage(Authentication authentication, Model model) {
        NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
        model.addAttribute("user", currentUser);
        return "admin/products/import-excel";
    }

    // ============= CRUD OPERATIONS =============

    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<?> addProduct(
            @RequestParam("tenSanPham") String tenSanPham,
            @RequestParam("maCuaHang") Integer maCuaHang,
            @RequestParam("maDanhMuc") Integer maDanhMuc,
            @RequestParam(value = "moTaSanPham", required = false) String moTaSanPham,
            @RequestParam("giaBan") BigDecimal giaBan,
            @RequestParam("soLuongConLai") Integer soLuongConLai,
            @RequestParam("loaiSanPham") String loaiSanPham,
            @RequestParam(value = "trangThai", defaultValue = "true") Boolean trangThai,
            @RequestParam(value = "hinhAnh", required = false) MultipartFile hinhAnh,
            Authentication authentication) {
        
        return processProductOperation(null, tenSanPham, maCuaHang, maDanhMuc, moTaSanPham, 
                                     giaBan, soLuongConLai, loaiSanPham, trangThai, hinhAnh, false);
    }

    @PostMapping("/edit/{id}")
    @ResponseBody
    public ResponseEntity<?> updateProduct(
            @PathVariable Integer id,
            @RequestParam("tenSanPham") String tenSanPham,
            @RequestParam("maCuaHang") Integer maCuaHang,
            @RequestParam("maDanhMuc") Integer maDanhMuc,
            @RequestParam(value = "moTaSanPham", required = false) String moTaSanPham,
            @RequestParam("giaBan") BigDecimal giaBan,
            @RequestParam("soLuongConLai") Integer soLuongConLai,
            @RequestParam("loaiSanPham") String loaiSanPham,
            @RequestParam("trangThai") Boolean trangThai,
            @RequestParam(value = "hinhAnh", required = false) MultipartFile hinhAnh,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // ========== KIỂM TRA CÓ THỂ CẬP NHẬT KHÔNG ==========
            if (!sanPhamService.canUpdateProduct(id)) {
                response.put("success", false);
                response.put("message", "Không thể cập nhật sản phẩm vì cửa hàng đang bị khóa");
                return ResponseEntity.badRequest().body(response);
            }
            
            return processProductOperation(id, tenSanPham, maCuaHang, maDanhMuc, moTaSanPham, 
                                         giaBan, soLuongConLai, loaiSanPham, trangThai, hinhAnh, true);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteProduct(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            SanPham sanPham = getProductById(id);
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
    public ResponseEntity<?> bulkDeleteProducts(@RequestBody Map<String, List<Integer>> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
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
                    SanPham sanPham = getProductById(productId);
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
            @RequestParam Boolean status) {
        Map<String, Object> response = new HashMap<>();
        
        try {
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
            
            SanPham sanPham = getProductById(id);
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
    
    @PostMapping("/bulk-change-status")
    @ResponseBody
    public ResponseEntity<?> bulkChangeStatus(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            @SuppressWarnings("unchecked")
            List<Integer> productIds = (List<Integer>) request.get("productIds");
            Boolean status = (Boolean) request.get("status");
            
            if (productIds == null || productIds.isEmpty()) {
                response.put("success", false);
                response.put("message", "Không có sản phẩm nào được chọn để thay đổi trạng thái");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (status == null) {
                response.put("success", false);
                response.put("message", "Trạng thái không hợp lệ");
                return ResponseEntity.badRequest().body(response);
            }
            
            int updatedCount = 0;
            int failedCount = 0;
            List<String> errorMessages = new ArrayList<>();
            
            for (Integer productId : productIds) {
                try {
                    // Kiểm tra có thể thay đổi trạng thái không
                    if (sanPhamService.canChangeProductStatus(productId, status)) {
                        SanPham sanPham = getProductById(productId);
                        sanPham.setTrangThai(status);
                        sanPhamService.save(sanPham);
                        updatedCount++;
                    } else {
                        failedCount++;
                        SanPham sanPham = getProductById(productId);
                        errorMessages.add("Sản phẩm '" + sanPham.getTenSanPham() + "': Không thể kích hoạt vì danh mục đang ngừng hoạt động");
                    }
                } catch (Exception e) {
                    failedCount++;
                    errorMessages.add("Sản phẩm ID " + productId + ": " + e.getMessage());
                }
            }
            
            if (updatedCount > 0) {
                response.put("success", true);
                response.put("message", "Đã cập nhật trạng thái thành công cho " + updatedCount + " sản phẩm");
                response.put("updatedCount", updatedCount);
                
                if (failedCount > 0) {
                    response.put("failedCount", failedCount);
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
            response.put("message", "Lỗi khi thay đổi trạng thái nhiều sản phẩm: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ============= EXCEL OPERATIONS =============

    @PostMapping("/import")
    @ResponseBody
    public ResponseEntity<?> importProductsFromExcel(
            @RequestParam("excelFiles") List<MultipartFile> excelFiles,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate files
            validateExcelFiles(excelFiles);
            
            // Import and save products
            ExcelImportResult importResult = excelImportService.importAndSaveProductsFromMultipleExcel(excelFiles);
            
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

    @GetMapping("/export-excel")
    public void exportProductsToExcel(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String loaiSanPhamFilter,
            @RequestParam(defaultValue = "ngayNhap") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) List<Integer> selectedProducts,
            HttpServletResponse response) {
        
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=san_pham_export.xlsx");
            
            List<SanPham> products = getProductsForExport(keyword, loaiSanPhamFilter, sortBy, sortDir, selectedProducts);
            Workbook workbook = createExportWorkbook(products);
            workbook.write(response.getOutputStream());
            workbook.close();
            
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xuất Excel: " + e.getMessage());
        }
    }

    @GetMapping("/download-template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=template_san_pham.xlsx");
            
            byte[] templateBytes = excelImportService.generateExcelTemplate();
            response.getOutputStream().write(templateBytes);
            response.getOutputStream().flush();
            
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tải template: " + e.getMessage());
        }
    }

    // ============= API ENDPOINTS =============

    @GetMapping("/loai-san-pham/{maDanhMuc}")
    @ResponseBody
    public ResponseEntity<List<String>> getLoaiSanPhamByDanhMuc(@PathVariable Integer maDanhMuc) {
        try {
            List<String> loaiSanPhamList = getLoaiSanPhamByDanhMucId(maDanhMuc);
            return ResponseEntity.ok(loaiSanPhamList);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }
    }

    // ============= PRIVATE HELPER METHODS =============

    private void prepareProductFormModel(Model model, NguoiDung user, SanPhamModel sanPhamModel, boolean isEdit) {
        List<CuaHang> stores = cuaHangService.findAll();
        List<DanhMuc> categories = danhMucService.findAll();
        
        if (sanPhamModel == null) {
            List<String> availableLoaiSanPham = new ArrayList<>();
            if (!categories.isEmpty()) {
                availableLoaiSanPham = getLoaiSanPhamByDanhMucId(categories.get(0).getMaDanhMuc());
            }
            
            sanPhamModel = SanPhamModel.builder()
                    .availableLoaiSanPham(availableLoaiSanPham)
                    .build();
        }
        
        model.addAttribute("user", user);
        model.addAttribute("sanPhamModel", sanPhamModel);
        model.addAttribute("stores", stores);
        model.addAttribute("categories", categories);
        model.addAttribute("isEdit", isEdit);
    }

    private ResponseEntity<?> processProductOperation(Integer id, String tenSanPham, Integer maCuaHang, 
                                                    Integer maDanhMuc, String moTaSanPham, BigDecimal giaBan,
                                                    Integer soLuongConLai, String loaiSanPham, Boolean trangThai,
                                                    MultipartFile hinhAnh, boolean isUpdate) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate input
            validateProductInput(tenSanPham, maCuaHang, maDanhMuc, giaBan, loaiSanPham);
            
            // Handle image upload
            String fileName = handleImageUpload(hinhAnh);
            
            // Create product model
            SanPhamModel model = buildSanPhamModel(id, tenSanPham, maCuaHang, maDanhMuc, moTaSanPham,
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

    private void validateProductInput(String tenSanPham, Integer maCuaHang, Integer maDanhMuc, 
                                    BigDecimal giaBan, String loaiSanPham) {
        if (tenSanPham == null || tenSanPham.trim().isEmpty()) {
            throw new RuntimeException("Tên sản phẩm không được để trống");
        }
        if (maCuaHang == null) {
            throw new RuntimeException("Cửa hàng không được để trống");
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

    private SanPhamModel buildSanPhamModel(Integer id, String tenSanPham, Integer maCuaHang, Integer maDanhMuc,
                                         String moTaSanPham, BigDecimal giaBan, Integer soLuongConLai,
                                         String loaiSanPham, Boolean trangThai, String fileName, boolean isUpdate) {
        SanPhamModel.SanPhamModelBuilder builder = SanPhamModel.builder()
                .tenSanPham(tenSanPham.trim())
                .maCuaHang(maCuaHang)
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

    private List<SanPham> getProductsForExport(String keyword, String loaiSanPhamFilter, 
            String sortBy, String sortDir, List<Integer> selectedProducts) {
        if (selectedProducts != null && !selectedProducts.isEmpty()) {
            return sanPhamService.findAllById(selectedProducts);
        }
        
        // FIXED: Sử dụng cùng logic xử lý sort như trong listProducts
        String[] sortParams = parseSortParametersFromDropdown(sortBy);
        Sort sort = createSort(sortParams[0], sortParams[1]);
        Pageable pageable = PageRequest.of(0, 1000, sort);
        
        Page<SanPham> productPage = getFilteredProducts(keyword, loaiSanPhamFilter, pageable);
        return productPage.getContent();
    }

    // ============= UTILITY METHODS =============

    private SanPham getProductById(Integer id) {
        SanPham sanPham = sanPhamService.findByMaSanPham(id);
        if (sanPham == null) {
            throw new RuntimeException("Sản phẩm không tồn tại");
        }
        return sanPham;
    }

    private SanPhamModel convertToSanPhamModel(SanPham sanPham) {
        List<String> availableLoaiSanPham = getLoaiSanPhamByDanhMucId(sanPham.getDanhMuc().getMaDanhMuc());
        
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

    private List<String> getLoaiSanPhamByDanhMucId(Integer maDanhMuc) {
        try {
            DanhMuc danhMuc = danhMucService.findByMaDanhMuc(maDanhMuc);
            List<String> loaiSanPhamList = new ArrayList<>();
            
            if (danhMuc != null) {
                List<String> existingTypes = sanPhamService.findDistinctLoaiSanPhamByDanhMuc(danhMuc);
                loaiSanPhamList.addAll(existingTypes);
                addDefaultLoaiSanPhamByCategory(danhMuc.getTenDanhMuc(), loaiSanPhamList);
            }
            
            return loaiSanPhamList;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<String> getAllDistinctLoaiSanPham() {
        try {
            List<SanPham> allProducts = sanPhamService.findAll();
            return allProducts.stream()
                    .map(SanPham::getLoaiSanPham)
                    .filter(loai -> loai != null && !loai.trim().isEmpty())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void addDefaultLoaiSanPhamByCategory(String tenDanhMuc, List<String> loaiSanPhamList) {
        if (tenDanhMuc == null) return;
        
        String categoryName = tenDanhMuc.toLowerCase();
        List<String> defaultTypes = new ArrayList<>();
        
        if (categoryName.contains("chó cảnh") || categoryName.contains("cho canh")) {
            defaultTypes.addAll(Arrays.asList("Chó Poodle", "Chó Alaska", "Chó Phú Quốc", "Chó Corgi", "Chó Husky", "Chó Golden"));
        } else if (categoryName.contains("mèo cảnh") || categoryName.contains("meo canh")) {
            defaultTypes.addAll(Arrays.asList("Mèo Ba Tư", "Mèo Anh", "Mèo Xiêm", "Mèo Ragdoll", "Mèo Maine Coon", "Mèo Sphynx"));
        } else if (categoryName.contains("phụ kiện") || categoryName.contains("phu kien")) {
            defaultTypes.addAll(Arrays.asList("Thức ăn", "Võng", "Cặp", "Dây dắt", "Bát ăn", "Chuồng", "Bàn chải", "Lược", "Đồ chơi"));
        }
        
        for (String type : defaultTypes) {
            if (!loaiSanPhamList.contains(type)) {
                loaiSanPhamList.add(type);
            }
        }
    }

    private String saveProductImage(MultipartFile file) throws Exception {
        String projectPath = System.getProperty("user.dir");
        String uploadDir = projectPath + File.separator + "uploads" + File.separator + "products" + File.separator;
        
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                throw new Exception("Không thể tạo thư mục uploads/products: " + uploadDir);
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
        
        String fileName = "product_" + System.currentTimeMillis() + extension;
        File dest = new File(dir, fileName);
        
        file.transferTo(dest);
        
        if (dest.exists() && dest.length() > 0) {
            return fileName;
        } else {
            throw new Exception("File không được lưu hoặc có kích thước 0 bytes");
        }
    }

    private Workbook createExportWorkbook(List<SanPham> products) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sản phẩm");
        
        // Create styles
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        
        // Create header row
        createHeaderRow(sheet, headerStyle);
        
        // Add data rows
        createDataRows(sheet, products, dataStyle);
        
        // Auto-size columns
        for (int i = 0; i < 13; i++) {
            sheet.autoSizeColumn(i);
        }
        
        return workbook;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        return headerStyle;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        return dataStyle;
    }

    private void createHeaderRow(Sheet sheet, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "Mã SP", "Tên sản phẩm", "Cửa hàng", "Danh mục", "Loại sản phẩm",
            "Giá bán", "Số lượng tồn", "Đã bán", "Lượt thích", "Đánh giá",
            "Trạng thái", "Ngày nhập", "Mô tả"
        };
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void createDataRows(Sheet sheet, List<SanPham> products, CellStyle dataStyle) {
        int rowNum = 1;
        for (SanPham product : products) {
            Row row = sheet.createRow(rowNum++);
            
            createSafeCell(row, 0, product.getMaSanPham(), dataStyle);
            createSafeCell(row, 1, product.getTenSanPham(), dataStyle);
            createSafeCell(row, 2, product.getCuaHang().getTenCuaHang(), dataStyle);
            createSafeCell(row, 3, product.getDanhMuc().getTenDanhMuc(), dataStyle);
            createSafeCell(row, 4, product.getLoaiSanPham(), dataStyle);
            createSafeCell(row, 5, product.getGiaBan(), dataStyle);
            createSafeCell(row, 6, product.getSoLuongConLai(), dataStyle);
            createSafeCell(row, 7, product.getSoLuongDaBan(), dataStyle);
            createSafeCell(row, 8, product.getLuotThich(), dataStyle);
            createSafeCell(row, 9, product.getSaoDanhGia(), dataStyle);
            createSafeCell(row, 10, product.getTrangThai() ? "Đang bán" : "Ngừng bán", dataStyle);
            
            if (product.getNgayNhap() != null) {
                createSafeCell(row, 11, new SimpleDateFormat("dd/MM/yyyy").format(product.getNgayNhap()), dataStyle);
            } else {
                createSafeCell(row, 11, "", dataStyle);
            }
            
            createSafeCell(row, 12, product.getMoTaSanPham(), dataStyle);
        }
    }

    private void createSafeCell(Row row, int column, Object value, CellStyle style) {
        Cell cell = row.createCell(column);
        
        try {
            if (value == null) {
                cell.setCellValue("");
            } else if (value instanceof Integer) {
                cell.setCellValue((Integer) value);
            } else if (value instanceof Long) {
                cell.setCellValue((Long) value);
            } else if (value instanceof Double) {
                cell.setCellValue((Double) value);
            } else if (value instanceof BigDecimal) {
                cell.setCellValue(((BigDecimal) value).doubleValue());
            } else if (value instanceof Boolean) {
                cell.setCellValue((Boolean) value);
            } else if (value instanceof String) {
                cell.setCellValue((String) value);
            } else {
                cell.setCellValue(value.toString());
            }
        } catch (Exception e) {
            cell.setCellValue(value != null ? value.toString() : "");
        }
        
        if (style != null) {
            cell.setCellStyle(style);
        }
    }
}