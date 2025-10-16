package vn.iotstar.controller.vendor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.iotstar.entity.*;
import vn.iotstar.repository.*;
import vn.iotstar.service.DatHangService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/vendor/api")
public class VendorRevenueApiController {

    @Autowired
    private CuaHangRepository cuaHangRepository;

    @Autowired
    private DatHangService datHangService;

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private DatHangChiTietRepository datHangChiTietRepository;

    /**
     * API để lấy dữ liệu doanh thu thực tế cho biểu đồ
     */
    @GetMapping("/revenue-data")
    public ResponseEntity<Map<String, Object>> getRevenueData(
            @RequestParam(defaultValue = "7") int days) {
        try {
            // Lấy thông tin vendor đang đăng nhập
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            NguoiDung user = nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

            // Lấy cửa hàng của vendor
            List<CuaHang> cuaHangList = cuaHangRepository.findByNguoiDung(user);
            if (cuaHangList.isEmpty()) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Store not found"));
            }

            CuaHang cuaHang = cuaHangList.get(0);

            // Tạo dữ liệu doanh thu thực tế
            Map<String, Object> revenueData = getActualRevenueData(cuaHang, days);
            
            return ResponseEntity.ok(revenueData);

        } catch (Exception e) {
            System.err.println("Error generating revenue data: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(Collections.singletonMap("error", "Failed to generate revenue data"));
        }
    }

    private Map<String, Object> getActualRevenueData(CuaHang cuaHang, int days) {
        Map<String, Object> result = new HashMap<>();
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        
        // Lấy dữ liệu thống kê từ service
        Map<LocalDate, DatHangService.DailyRevenueStats> statsMap = 
            datHangService.getDailyRevenueStatsByDateRange(cuaHang, startDate, endDate);
        
        // Tạo labels và data
        List<String> labels = new ArrayList<>();
        List<Double> revenue = new ArrayList<>();
        List<Long> orderCounts = new ArrayList<>();
        List<Long> productCounts = new ArrayList<>();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
        LocalDate current = startDate;
        
        double totalRevenue = 0;
        long totalOrders = 0;
        long totalProducts = 0;
        
        while (!current.isAfter(endDate)) {
            labels.add(current.format(formatter));
            
            DatHangService.DailyRevenueStats stats = statsMap.get(current);
            if (stats != null) {
                revenue.add(stats.getRevenue());
                orderCounts.add(stats.getOrderCount());
                productCounts.add(stats.getProductCount());
                
                totalRevenue += stats.getRevenue();
                totalOrders += stats.getOrderCount();
                totalProducts += stats.getProductCount();
            } else {
                revenue.add(0.0);
                orderCounts.add(0L);
                productCounts.add(0L);
            }
            
            current = current.plusDays(1);
        }
        
        result.put("labels", labels);
        result.put("data", revenue);
        result.put("orderCounts", orderCounts);
        result.put("productCounts", productCounts);
        result.put("totalRevenue", totalRevenue);
        result.put("totalOrders", totalOrders);
        result.put("totalProducts", totalProducts);
        result.put("averageRevenue", totalRevenue / days);
        result.put("storeName", cuaHang.getTenCuaHang());
        result.put("period", startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + 
                           " - " + endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        
        return result;
    }

    private DailyRevenueStats getDailyRevenueStats(CuaHang cuaHang, LocalDate date) {
        // Lấy tất cả chi tiết đơn hàng đã hoàn thành trong ngày
        List<DatHangChiTiet> orderDetails = getCompletedOrderDetailsByDate(cuaHang, date);
        
        double dailyRevenue = 0.0;
        long orderCount = 0;
        long productCount = 0;
        
        if (!orderDetails.isEmpty()) {
            // Tính tổng doanh thu
            dailyRevenue = orderDetails.stream()
                .mapToDouble(dhct -> dhct.getThanhTien().doubleValue())
                .sum();
            
            // Đếm số đơn hàng duy nhất
            orderCount = orderDetails.stream()
                .map(dhct -> dhct.getDatHang().getMaDatHang())
                .distinct()
                .count();
            
            // Đếm tổng số sản phẩm đã bán
            productCount = orderDetails.stream()
                .mapToLong(DatHangChiTiet::getSoLuong)
                .sum();
        }
        
        return new DailyRevenueStats(dailyRevenue, orderCount, productCount);
    }

    private List<DatHangChiTiet> getCompletedOrderDetailsByDate(CuaHang cuaHang, LocalDate date) {
        // Query để lấy tất cả chi tiết đơn hàng đã hoàn thành trong ngày cụ thể
        return datHangChiTietRepository.findAll().stream()
            .filter(dhct -> {
                DatHang datHang = dhct.getDatHang();
                SanPham sanPham = dhct.getSanPham();
                
                return sanPham.getCuaHang() != null && 
                       sanPham.getCuaHang().getMaCuaHang().equals(cuaHang.getMaCuaHang()) &&
                       "Hoàn thành".equals(datHang.getTrangThai()) &&
                       date.equals(datHang.getNgayDat());
            })
            .collect(Collectors.toList());
    }

    // Helper class để lưu trữ thống kê hàng ngày
    private static class DailyRevenueStats {
        private final double revenue;
        private final long orderCount;
        private final long productCount;
        
        public DailyRevenueStats(double revenue, long orderCount, long productCount) {
            this.revenue = revenue;
            this.orderCount = orderCount;
            this.productCount = productCount;
        }
        
        public double getRevenue() { return revenue; }
        public long getOrderCount() { return orderCount; }
        public long getProductCount() { return productCount; }
    }
}