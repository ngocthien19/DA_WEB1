package vn.iotstar.controller.admin;

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
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.entity.CuaHang;
import vn.iotstar.entity.DanhGia;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.entity.SanPham;
import vn.iotstar.service.CuaHangService;
import vn.iotstar.service.DanhGiaService;
import vn.iotstar.service.SanPhamService;
import vn.iotstar.service.impl.UserDetailsServiceImpl;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Controller
@RequestMapping("/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDanhGiaController {

    @Autowired
    private DanhGiaService danhGiaService;
    
    @Autowired
    private CuaHangService cuaHangService;
    
    @Autowired
    private SanPhamService sanPhamService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    private NguoiDung getCurrentUser(Authentication authentication) {
        return userDetailsService.getCurrentUser(authentication);
    }

    @GetMapping
    public String listDanhGia(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer soSao,
            @RequestParam(required = false) Integer maCuaHang,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            Authentication authentication,
            Model model) {
        
        NguoiDung currentUser = getCurrentUser(authentication);
        model.addAttribute("user", currentUser);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "ngayDanhGia"));
        Page<DanhGia> danhGiaPage;
        
        // Lấy tất cả cửa hàng cho filter
        List<CuaHang> allStores = cuaHangService.findAll();
        model.addAttribute("allStores", allStores);
        
        // XỬ LÝ KHI CHỌN "TẤT CẢ" - chuyển giá trị rỗng thành null
        if ("".equals(keyword)) keyword = null;
        
        // Áp dụng bộ lọc
        if ((keyword != null && !keyword.isEmpty()) || soSao != null || maCuaHang != null || startDate != null || endDate != null) {
            danhGiaPage = filterDanhGia(keyword, soSao, maCuaHang, startDate, endDate, pageable);
        } else {
            // Lấy tất cả đánh giá
            danhGiaPage = danhGiaService.findAll(pageable);
        }
        
        // LẤY THỐNG KÊ SỐ SAO
        Map<Integer, Long> ratingStats = getRatingStatistics();
        
        model.addAttribute("danhGiaPage", danhGiaPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("soSao", soSao);
        model.addAttribute("maCuaHang", maCuaHang);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("currentPage", page);
        
        // THÊM THỐNG KÊ VÀO MODEL
        model.addAttribute("ratingStats", ratingStats);
        model.addAttribute("totalReviews", danhGiaPage.getTotalElements());
        
        return "admin/reviews/reviews";
    }

    private Page<DanhGia> filterDanhGia(String keyword, Integer soSao, Integer maCuaHang, 
                                       LocalDate startDate, LocalDate endDate, Pageable pageable) {
        // Tạm thời lấy tất cả đánh giá rồi filter thủ công
        // Trong thực tế nên triển khai Specification hoặc custom query
        List<DanhGia> allReviews = danhGiaService.findAll();
        
        // Áp dụng filter
        List<DanhGia> filteredReviews = allReviews.stream()
            .filter(review -> {
                boolean matches = true;
                
                // Filter theo keyword
                if (keyword != null && !keyword.isEmpty()) {
                    String lowerKeyword = keyword.toLowerCase();
                    matches = review.getSanPham().getTenSanPham().toLowerCase().contains(lowerKeyword) ||
                             review.getNguoiDung().getTenNguoiDung().toLowerCase().contains(lowerKeyword) ||
                             review.getBinhLuan().toLowerCase().contains(lowerKeyword) ||
                             review.getSanPham().getCuaHang().getTenCuaHang().toLowerCase().contains(lowerKeyword);
                }
                
                // Filter theo số sao
                if (matches && soSao != null) {
                    matches = review.getSoSao().equals(soSao);
                }
                
                // Filter theo cửa hàng
                if (matches && maCuaHang != null) {
                    matches = review.getSanPham().getCuaHang().getMaCuaHang().equals(maCuaHang);
                }
                
                // Filter theo ngày
                if (matches && startDate != null) {
                    LocalDate reviewDate = review.getNgayDanhGia().toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate();
                    matches = !reviewDate.isBefore(startDate);
                }
                
                if (matches && endDate != null) {
                    LocalDate reviewDate = review.getNgayDanhGia().toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate();
                    matches = !reviewDate.isAfter(endDate);
                }
                
                return matches;
            })
            .toList();
        
        // Phân trang thủ công
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredReviews.size());
        List<DanhGia> pageContent = filteredReviews.subList(start, end);
        
        return new org.springframework.data.domain.PageImpl<>(
            pageContent, pageable, filteredReviews.size()
        );
    }

    private Map<Integer, Long> getRatingStatistics() {
        Map<Integer, Long> stats = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            stats.put(i, 0L);
        }
        
        List<DanhGia> allReviews = danhGiaService.findAll();
        for (DanhGia review : allReviews) {
            int rating = review.getSoSao();
            stats.put(rating, stats.get(rating) + 1);
        }
        
        return stats;
    }

    @GetMapping("/view/{id}")
    public String viewDanhGia(@PathVariable Integer id, Authentication authentication, Model model) {
        NguoiDung currentUser = getCurrentUser(authentication);
        model.addAttribute("user", currentUser);
        
        DanhGia danhGia = danhGiaService.findById(id)
                .orElseThrow(() -> new RuntimeException("Đánh giá không tồn tại"));
        
        model.addAttribute("danhGia", danhGia);
        return "admin/reviews/review-view";
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteDanhGia(@PathVariable Integer id, Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            DanhGia danhGia = danhGiaService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Đánh giá không tồn tại"));
            
            danhGiaService.delete(id);
            
            response.put("success", true);
            response.put("message", "Xóa đánh giá thành công");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
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
            if (ids == null || ids.isEmpty()) {
                response.put("success", false);
                response.put("message", "Vui lòng chọn ít nhất một đánh giá để xóa");
                return ResponseEntity.badRequest().body(response);
            }
            
            int successCount = 0;
            int errorCount = 0;
            List<String> errorMessages = new ArrayList<>();
            
            for (Integer id : ids) {
                try {
                    DanhGia danhGia = danhGiaService.findById(id).orElse(null);
                    if (danhGia != null) {
                        danhGiaService.delete(id);
                        successCount++;
                    } else {
                        errorCount++;
                        errorMessages.add("Đánh giá ID " + id + " không tồn tại");
                    }
                } catch (Exception e) {
                    errorCount++;
                    errorMessages.add("Lỗi khi xóa đánh giá ID " + id + ": " + e.getMessage());
                }
            }
            
            response.put("success", true);
            response.put("message", "Đã xóa " + successCount + " đánh giá thành công");
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

    // Thêm method findAll vào DanhGiaService
    private List<DanhGia> findAllDanhGia() {
        // Triển khai tạm thời - trong thực tế nên có method này trong service
        return new ArrayList<>(); // Cần implement properly
    }
}