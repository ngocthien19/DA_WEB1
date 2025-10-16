package vn.iotstar.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.service.NguoiDungService;
import vn.iotstar.service.impl.ExcelExportService;
import vn.iotstar.service.impl.UserDetailsServiceImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin/customers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminKhachHangController {

    @Autowired
    private NguoiDungService nguoiDungService;
    
    @Autowired
    private UserDetailsServiceImpl userDetailsService;
    
    @Autowired
    private ExcelExportService excelExportService;

    /**
     * Hiển thị danh sách khách hàng (chỉ những người có vai trò USER)
     */
    @GetMapping
    public String listCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String keyword,
            Authentication authentication,
            Model model,
            HttpServletRequest request) {  // Thêm HttpServletRequest
        
        // Lấy thông tin user đang đăng nhập
        NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
        
        // Tạo Pageable với sắp xếp theo maNguoiDung giảm dần
        Pageable pageable = PageRequest.of(page, size, Sort.by("maNguoiDung").descending());
        
        // Tìm kiếm và lọc chỉ lấy người dùng có vai trò USER
        Page<NguoiDung> customerPage = nguoiDungService.searchAndFilterUsers(keyword, "USER", pageable);
        
        // Thêm CSRF token vào model
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            model.addAttribute("_csrf", csrfToken);
        }
        
        model.addAttribute("user", currentUser);
        model.addAttribute("customerPage", customerPage);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        
        return "admin/users/customers";
    }

    /**
     * Xóa nhiều khách hàng
     */
    @DeleteMapping("/delete-selected")
    @ResponseBody
    public ResponseEntity<?> deleteSelectedCustomers(@RequestBody Map<String, List<Integer>> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Integer> customerIds = request.get("customerIds");
            if (customerIds == null || customerIds.isEmpty()) {
                response.put("success", false);
                response.put("message", "Không có khách hàng nào được chọn");
                return ResponseEntity.badRequest().body(response);
            }
            
            int successCount = 0;
            int failCount = 0;
            
            for (Integer customerId : customerIds) {
                try {
                    nguoiDungService.deleteUser(customerId);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    System.err.println("Lỗi khi xóa khách hàng ID " + customerId + ": " + e.getMessage());
                }
            }
            
            response.put("success", true);
            response.put("message", "Đã xóa " + successCount + " khách hàng thành công" + 
                (failCount > 0 ? " (" + failCount + " khách hàng xóa thất bại)" : ""));
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Xuất Excel danh sách khách hàng - TẠM THỜI VÔ HIỆU HÓA
     */
    /**
     * Xuất Excel danh sách khách hàng
     */
    @PostMapping("/export-excel")
    @ResponseBody
    public ResponseEntity<?> exportCustomersToExcel(@RequestBody Map<String, Object> request) {
        try {
            List<Integer> customerIds = null;
            Object customerIdsObj = request.get("customerIds");
            
            // Xử lý customerIds an toàn
            if (customerIdsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> rawList = (List<Object>) customerIdsObj;
                customerIds = rawList.stream()
                        .map(item -> {
                            if (item instanceof Integer) {
                                return (Integer) item;
                            } else if (item instanceof String) {
                                try {
                                    return Integer.parseInt((String) item);
                                } catch (NumberFormatException e) {
                                    return null;
                                }
                            }
                            return null;
                        })
                        .filter(item -> item != null)
                        .collect(Collectors.toList());
            }
            
            List<NguoiDung> customers;
            
            if (customerIds != null && !customerIds.isEmpty()) {
                // Export khách hàng đã chọn
                customers = customerIds.stream()
                        .map(id -> nguoiDungService.getUserById(id))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList());
            } else {
                // Export tất cả khách hàng
                String keyword = (String) request.get("keyword");
                customers = nguoiDungService.searchAndFilterUsers(
                    keyword, 
                    "USER", 
                    Pageable.unpaged()
                ).getContent();
            }
            
            byte[] excelBytes = excelExportService.exportCustomersToExcel(customers);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename("customers_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx")
                    .build());
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);
                    
        } catch (Exception e) {
            e.printStackTrace(); // In stack trace để debug
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi xuất Excel: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * API xuất Excel cho GET request
     */
    @GetMapping("/export-excel")
    @ResponseBody
    public ResponseEntity<?> exportAllCustomersToExcel(
            @RequestParam(required = false) String keyword,
            Authentication authentication) {
        try {
            List<NguoiDung> customers = nguoiDungService.searchAndFilterUsers(
                keyword, 
                "USER", 
                Pageable.unpaged()
            ).getContent();
            
            byte[] excelBytes = excelExportService.exportCustomersToExcel(customers);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename("all_customers_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx")
                    .build());
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);
                    
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi xuất Excel: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}