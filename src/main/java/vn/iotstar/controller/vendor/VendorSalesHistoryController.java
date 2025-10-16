package vn.iotstar.controller.vendor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.entity.CuaHang;
import vn.iotstar.entity.DatHang;
import vn.iotstar.entity.DatHangChiTiet;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.service.CuaHangService;
import vn.iotstar.service.DatHangService;
import vn.iotstar.service.impl.UserDetailsServiceImpl;
import vn.iotstar.service.impl.ExcelExportService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/vendor/sales-history")
@PreAuthorize("hasRole('VENDOR')")
public class VendorSalesHistoryController {

    @Autowired
    private DatHangService datHangService;
    
    @Autowired
    private CuaHangService cuaHangService;
    
    @Autowired
    private UserDetailsServiceImpl userDetailsService;
    
    @Autowired
    private ExcelExportService excelExportService;

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
    public String salesHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) String productName,
            Authentication authentication,
            Model model) {
        
        NguoiDung currentUser = getCurrentUser(authentication);
        CuaHang cuaHang = getCurrentStore(authentication);
        
        model.addAttribute("user", currentUser);

        if (cuaHang == null) {
            return "redirect:/vendor/dashboard";
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "datHang.maDatHang"));
        
        // Lấy tất cả đơn hàng đã hoàn thành VÀ đã hủy của cửa hàng
        Page<DatHangChiTiet> salesHistoryPage = datHangService.findCompletedOrderDetailsByCuaHang(
            cuaHang, keyword, startDate, endDate, productName, pageable);
        
        // Thống kê doanh thu (chỉ tính đơn hoàn thành)
        Map<String, Object> salesStats = calculateSalesStats(cuaHang, startDate, endDate);
        
        model.addAttribute("salesHistoryPage", salesHistoryPage);
        model.addAttribute("cuaHang", cuaHang);
        model.addAttribute("keyword", keyword);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("productName", productName);
        model.addAttribute("currentPage", page);
        model.addAttribute("salesStats", salesStats);
        
        return "vendor/sales-history/sales-history";
    }

    private Map<String, Object> calculateSalesStats(CuaHang cuaHang, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> stats = new HashMap<>();
        
        // Tổng doanh thu (chỉ tính đơn Hoàn thành)
        Double totalRevenue = datHangService.getTotalRevenueByCuaHangAndDateRange(cuaHang, "Hoàn thành", startDate, endDate);
        stats.put("totalRevenue", totalRevenue != null ? totalRevenue : 0.0);
        
        // Tổng số đơn hàng đã hoàn thành
        Long totalCompletedOrders = datHangService.countByCuaHangAndTrangThai(cuaHang, "Hoàn thành");
        stats.put("totalCompletedOrders", totalCompletedOrders);
        
        // Tổng số đơn hàng đã hủy
        Long totalCancelledOrders = datHangService.countByCuaHangAndTrangThai(cuaHang, "Hủy");
        stats.put("totalCancelledOrders", totalCancelledOrders);
        
        // Tổng số sản phẩm đã bán (chỉ tính đơn Hoàn thành)
        Long totalProductsSold = datHangService.getTotalProductsSoldByCuaHang(cuaHang, "Hoàn thành", startDate, endDate);
        stats.put("totalProductsSold", totalProductsSold != null ? totalProductsSold : 0L);
        
        // Doanh thu trung bình mỗi đơn (chỉ tính đơn Hoàn thành)
        Double averageOrderValue = totalCompletedOrders > 0 ? totalRevenue / totalCompletedOrders : 0.0;
        stats.put("averageOrderValue", averageOrderValue);
        
        return stats;
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportSalesHistory(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) String productName,
            Authentication authentication) {
        
        try {
            CuaHang cuaHang = getCurrentStore(authentication);
            if (cuaHang == null) {
                return ResponseEntity.badRequest().build();
            }
            
            // FIXED: Lấy tất cả lịch sử bán hàng theo filter (Pageable.unpaged() là đúng cho xuất tổng)
            Page<DatHangChiTiet> salesHistoryPage = datHangService.findCompletedOrderDetailsByCuaHang(
                cuaHang, keyword, startDate, endDate, productName, Pageable.unpaged());
            
            List<DatHangChiTiet> salesHistory = salesHistoryPage.getContent();
            
            // Tính toán thống kê
            Map<String, Object> salesStats = calculateSalesStats(cuaHang, startDate, endDate);
            
            byte[] excelData = excelExportService.exportSalesHistoryToExcel(salesHistory, cuaHang, salesStats);
            
            String filename = "LichSuBanHang_" + cuaHang.getTenCuaHang().replaceAll("\\s+", "_") + 
                            "_" + LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy")) + ".xlsx";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
                    
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/export-selected")
    public ResponseEntity<byte[]> exportSelectedSalesHistory(
            @RequestParam("orderIds") String orderIdsStr,
            Authentication authentication) {
        
        try {
            CuaHang cuaHang = getCurrentStore(authentication);
            if (cuaHang == null) {
                return ResponseEntity.badRequest().build();
            }
            
            // Parse order IDs
            List<Integer> orderIds = Arrays.stream(orderIdsStr.split(","))
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
            
            // Lấy các chi tiết đơn hàng đã chọn
            List<DatHangChiTiet> salesHistory = new ArrayList<>();
            for (Integer orderId : orderIds) {
                DatHang order = datHangService.findByMaDatHang(orderId);
                if (order != null && "Hoàn thành".equals(order.getTrangThai())) {
                    // Ép load các quan hệ cần thiết
                    order.getDatHangChiTiets().size(); // Force load
                    if (order.getVanChuyens() != null) {
                        order.getVanChuyens().size(); // Force load
                    }
                    if (order.getThanhToans() != null) {
                        order.getThanhToans().size(); // Force load
                    }
                    
                    // Lấy chi tiết đơn hàng thuộc cửa hàng
                    order.getDatHangChiTiets().stream()
                        .filter(ct -> ct.getSanPham().getCuaHang().getMaCuaHang().equals(cuaHang.getMaCuaHang()))
                        .forEach(salesHistory::add);
                }
            }
            
            // Tính toán thống kê cho đơn hàng đã chọn
            Map<String, Object> salesStats = new HashMap<>();
            
            // 1. Tính Tổng Doanh thu (Double)
            salesStats.put("totalRevenue", salesHistory.stream()
                .mapToDouble(ct -> ct.getThanhTien().doubleValue())
                .sum());
                
            // 2. Tổng số đơn hàng đã hoàn thành (Cần phải là Long)
            // Lấy số lượng mã đơn hàng duy nhất từ danh sách chi tiết (vì mỗi chi tiết có thể thuộc cùng một đơn hàng)
            long distinctCompletedOrders = salesHistory.stream()
                .map(ct -> ct.getDatHang().getMaDatHang())
                .distinct()
                .count();
                
            salesStats.put("totalCompletedOrders", distinctCompletedOrders); // Đảm bảo là Long (sử dụng long)
            
            // 3. Tổng số sản phẩm đã bán (Cần phải là Long)
            Long totalProductsSold = salesHistory.stream()
                .mapToLong(DatHangChiTiet::getSoLuong) // Dùng mapToLong để đảm bảo kết quả là long
                .sum();
            salesStats.put("totalProductsSold", totalProductsSold); // Đảm bảo là Long
            
            // 4. Doanh thu trung bình mỗi đơn
            Double totalRevenue = (Double) salesStats.get("totalRevenue");
            Double averageOrderValue = distinctCompletedOrders > 0 ? totalRevenue / distinctCompletedOrders : 0.0;
            salesStats.put("averageOrderValue", averageOrderValue);

            
            byte[] excelData = excelExportService.exportSalesHistoryToExcel(salesHistory, cuaHang, salesStats);
            
            String filename = "LichSuBanHang_DaChon_" + 
                            LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy")) + ".xlsx";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
                    
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}