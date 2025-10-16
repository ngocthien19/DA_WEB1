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
@RequestMapping("/vendor/reviews")
@PreAuthorize("hasRole('VENDOR')")
public class VendorDanhGiaController {

    @Autowired
    private DanhGiaService danhGiaService;
    
    @Autowired
    private CuaHangService cuaHangService;
    
    @Autowired
    private SanPhamService sanPhamService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    private static final String STORE_LOCKED_MESSAGE = "Cửa hàng của bạn đang bị Admin khóa (Ngừng hoạt động). Bạn không thể thực hiện các thao tác quản lý đánh giá.";

    private CuaHang getCurrentStore(Authentication authentication) {
        NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
        if (currentUser == null) return null;
        
        List<CuaHang> stores = cuaHangService.findByNguoiDung(currentUser);
        return stores.isEmpty() ? null : stores.get(0);
    }
    
    private NguoiDung getCurrentUser(Authentication authentication) {
        return userDetailsService.getCurrentUser(authentication);
    }

    @GetMapping
    public String listDanhGia(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer soSao,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
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

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "ngayDanhGia"));
        Page<DanhGia> danhGiaPage;
        
        // Lấy tất cả sản phẩm của cửa hàng
        List<SanPham> storeProducts = sanPhamService.findByCuaHang(cuaHang);
        
        // XỬ LÝ KHI CHỌN "TẤT CẢ" - chuyển giá trị rỗng thành null
        if ("".equals(keyword)) keyword = null;
        
        // Áp dụng bộ lọc
        if ((keyword != null && !keyword.isEmpty()) || soSao != null || startDate != null || endDate != null) {
            danhGiaPage = filterDanhGia(storeProducts, keyword, soSao, startDate, endDate, pageable);
        } else {
            // Lấy tất cả đánh giá của các sản phẩm trong cửa hàng
            danhGiaPage = getDanhGiaByStoreProducts(storeProducts, pageable);
        }
        
        // LẤY THỐNG KÊ SỐ SAO
        Map<Integer, Long> ratingStats = getRatingStatistics(storeProducts);
        
        model.addAttribute("danhGiaPage", danhGiaPage);
        model.addAttribute("cuaHang", cuaHang);
        model.addAttribute("keyword", keyword);
        model.addAttribute("soSao", soSao);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("currentPage", page);
        
        // THÊM THỐNG KÊ VÀO MODEL
        model.addAttribute("ratingStats", ratingStats);
        model.addAttribute("totalReviews", danhGiaPage.getTotalElements());
        
        return "vendor/reviews/reviews";
    }

    private Page<DanhGia> filterDanhGia(List<SanPham> storeProducts, String keyword, Integer soSao, 
                                       LocalDate startDate, LocalDate endDate, Pageable pageable) {
        // Tạm thời lấy tất cả đánh giá rồi filter thủ công
        // Trong thực tế nên triển khai Specification hoặc custom query
        List<DanhGia> allReviews = new ArrayList<>();
        for (SanPham product : storeProducts) {
            Page<DanhGia> productReviews = danhGiaService.findBySanPham(product, Pageable.unpaged());
            allReviews.addAll(productReviews.getContent());
        }
        
        // Áp dụng filter
        List<DanhGia> filteredReviews = allReviews.stream()
            .filter(review -> {
                boolean matches = true;
                
                // Filter theo keyword
                if (keyword != null && !keyword.isEmpty()) {
                    String lowerKeyword = keyword.toLowerCase();
                    matches = review.getSanPham().getTenSanPham().toLowerCase().contains(lowerKeyword) ||
                             review.getNguoiDung().getTenNguoiDung().toLowerCase().contains(lowerKeyword) ||
                             review.getBinhLuan().toLowerCase().contains(lowerKeyword);
                }
                
                // Filter theo số sao
                if (matches && soSao != null) {
                    matches = review.getSoSao().equals(soSao);
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

    private Page<DanhGia> getDanhGiaByStoreProducts(List<SanPham> storeProducts, Pageable pageable) {
        List<DanhGia> allReviews = new ArrayList<>();
        for (SanPham product : storeProducts) {
            Page<DanhGia> productReviews = danhGiaService.findBySanPham(product, Pageable.unpaged());
            allReviews.addAll(productReviews.getContent());
        }
        
        // Sắp xếp theo ngày đánh giá giảm dần
        allReviews.sort((r1, r2) -> r2.getNgayDanhGia().compareTo(r1.getNgayDanhGia()));
        
        // Phân trang thủ công
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allReviews.size());
        List<DanhGia> pageContent = allReviews.subList(start, end);
        
        return new org.springframework.data.domain.PageImpl<>(
            pageContent, pageable, allReviews.size()
        );
    }

    private Map<Integer, Long> getRatingStatistics(List<SanPham> storeProducts) {
        Map<Integer, Long> stats = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            stats.put(i, 0L);
        }
        
        for (SanPham product : storeProducts) {
            Page<DanhGia> reviews = danhGiaService.findBySanPham(product, Pageable.unpaged());
            for (DanhGia review : reviews.getContent()) {
                int rating = review.getSoSao();
                stats.put(rating, stats.get(rating) + 1);
            }
        }
        
        return stats;
    }

    @GetMapping("/view/{id}")
    public String viewDanhGia(@PathVariable Integer id, Authentication authentication, Model model) {
        NguoiDung currentUser = getCurrentUser(authentication);
        CuaHang cuaHang = getCurrentStore(authentication);
        
        model.addAttribute("user", currentUser);

        if (cuaHang == null) {
            return "redirect:/vendor/dashboard";
        }
        
        DanhGia danhGia = danhGiaService.findById(id)
                .orElseThrow(() -> new RuntimeException("Đánh giá không tồn tại"));
        
        // Kiểm tra đánh giá có thuộc về sản phẩm của cửa hàng không
        if (!danhGia.getSanPham().getCuaHang().getMaCuaHang().equals(cuaHang.getMaCuaHang())) {
            return "redirect:/vendor/reviews";
        }
        
        model.addAttribute("danhGia", danhGia);
        model.addAttribute("cuaHang", cuaHang);
        return "vendor/reviews/review-view";
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteDanhGia(@PathVariable Integer id, Authentication authentication) {
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

            DanhGia danhGia = danhGiaService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Đánh giá không tồn tại"));
            
            // Kiểm tra đánh giá có thuộc về sản phẩm của cửa hàng không
            if (!danhGia.getSanPham().getCuaHang().getMaCuaHang().equals(cuaHang.getMaCuaHang())) {
                response.put("success", false);
                response.put("message", "Đánh giá không thuộc về cửa hàng của bạn");
                return ResponseEntity.badRequest().body(response);
            }
            
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
                response.put("message", "Vui lòng chọn ít nhất một đánh giá để xóa");
                return ResponseEntity.badRequest().body(response);
            }
            
            int successCount = 0;
            int errorCount = 0;
            List<String> errorMessages = new ArrayList<>();
            
            for (Integer id : ids) {
                try {
                    DanhGia danhGia = danhGiaService.findById(id).orElse(null);
                    if (danhGia != null && 
                        danhGia.getSanPham().getCuaHang().getMaCuaHang().equals(cuaHang.getMaCuaHang())) {
                        danhGiaService.delete(id);
                        successCount++;
                    } else {
                        errorCount++;
                        errorMessages.add("Đánh giá ID " + id + " không tồn tại hoặc không thuộc về cửa hàng của bạn");
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

    // Thêm method findById vào DanhGiaService
    private DanhGiaService danhGiaService() {
        return danhGiaService;
    }
}