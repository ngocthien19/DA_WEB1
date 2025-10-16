package vn.iotstar.controller.admin;

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
import vn.iotstar.service.impl.ExcelExportService;
import vn.iotstar.service.impl.UserDetailsServiceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/sales-history")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSalesHistoryController {

    @Autowired
    private DatHangService datHangService;
    
    @Autowired
    private CuaHangService cuaHangService;
    
    @Autowired
    private ExcelExportService excelExportService;
    
    @Autowired
    private UserDetailsServiceImpl userDetailsService;

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
            @RequestParam(required = false) Integer maCuaHang,
            Authentication authentication,
            Model model) {
        
        // THÊM DÒNG NÀY: Lấy thông tin user hiện tại
        NguoiDung currentUser = getCurrentUser(authentication);
        model.addAttribute("user", currentUser);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "datHang.maDatHang"));
        
        Page<DatHangChiTiet> salesHistoryPage = datHangService.findCompletedOrderDetails(
            maCuaHang, keyword, startDate, endDate, productName, pageable);
        
        // Thống kê tổng quan
        Map<String, Object> salesStats = calculateSalesStats(maCuaHang, startDate, endDate);
        
        // Lấy danh sách cửa hàng để hiển thị trong dropdown
        List<CuaHang> stores = cuaHangService.findActiveStores();
        
        // Format dates for display
        if (startDate != null) {
            model.addAttribute("formattedStartDate", startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }
        if (endDate != null) {
            model.addAttribute("formattedEndDate", endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }
        
        model.addAttribute("salesHistoryPage", salesHistoryPage);
        model.addAttribute("stores", stores);
        model.addAttribute("selectedStoreId", maCuaHang);
        model.addAttribute("keyword", keyword);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("productName", productName);
        model.addAttribute("currentPage", page);
        model.addAttribute("salesStats", salesStats);
        
        return "admin/sales-history/sales-history";
    }

    private Map<String, Object> calculateSalesStats(Integer maCuaHang, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> stats = new HashMap<>();
        
        CuaHang selectedStore = maCuaHang != null ? cuaHangService.findByMaCuaHang(maCuaHang) : null;
        
        // Tổng doanh thu (chỉ từ đơn hàng Hoàn thành)
        Double totalRevenue = getTotalRevenue(selectedStore, startDate, endDate);
        stats.put("totalRevenue", totalRevenue != null ? totalRevenue : 0.0);
        
        // Tổng số đơn hàng đã hoàn thành
        Long totalCompletedOrders = getTotalCompletedOrders(selectedStore, startDate, endDate);
        stats.put("totalCompletedOrders", totalCompletedOrders);
        
        // Tổng số đơn hàng đã hủy
        Long totalCancelledOrders = getTotalCancelledOrders(selectedStore, startDate, endDate);
        stats.put("totalCancelledOrders", totalCancelledOrders);
        
        // Tổng số sản phẩm đã bán (chỉ từ đơn hàng Hoàn thành)
        Long totalProductsSold = getTotalProductsSold(selectedStore, startDate, endDate);
        stats.put("totalProductsSold", totalProductsSold != null ? totalProductsSold : 0L);
        
        // Doanh thu trung bình mỗi đơn (chỉ từ đơn hàng Hoàn thành)
        Double averageOrderValue = totalCompletedOrders > 0 ? totalRevenue / totalCompletedOrders : 0.0;
        stats.put("averageOrderValue", averageOrderValue);
        
        // Số lượng cửa hàng có doanh thu
        Long storeCount = getStoreCountWithRevenue(startDate, endDate);
        stats.put("storeCount", storeCount);
        
        return stats;
    }
    
    private Long getTotalCancelledOrders(CuaHang store, LocalDate startDate, LocalDate endDate) {
        if (store != null) {
            return datHangService.countByCuaHangAndTrangThai(store, "Hủy");
        } else {
            // Tính tổng đơn hàng hủy của tất cả cửa hàng
            return calculateTotalCancelledOrdersForAllStores(startDate, endDate);
        }
    }
    
    private Long calculateTotalCancelledOrdersForAllStores(LocalDate startDate, LocalDate endDate) {
        List<CuaHang> stores = cuaHangService.findActiveStores();
        return stores.stream()
                .mapToLong(store -> datHangService.countByCuaHangAndTrangThai(store, "Hủy"))
                .sum();
    }

    private Double getTotalRevenue(CuaHang store, LocalDate startDate, LocalDate endDate) {
        if (store != null) {
            return datHangService.getTotalRevenueByCuaHangAndDateRange(store, "Hoàn thành", startDate, endDate);
        } else {
            // Tính tổng doanh thu của tất cả cửa hàng
            return calculateTotalRevenueForAllStores(startDate, endDate);
        }
    }
    
    private Long getTotalCompletedOrders(CuaHang store, LocalDate startDate, LocalDate endDate) {
        if (store != null) {
            return datHangService.countByCuaHangAndTrangThai(store, "Hoàn thành");
        } else {
            // Tính tổng đơn hàng hoàn thành của tất cả cửa hàng
            return calculateTotalCompletedOrdersForAllStores(startDate, endDate);
        }
    }
    
    private Long getTotalProductsSold(CuaHang store, LocalDate startDate, LocalDate endDate) {
        if (store != null) {
            return datHangService.getTotalProductsSoldByCuaHang(store, "Hoàn thành", startDate, endDate);
        } else {
            // Tính tổng sản phẩm đã bán của tất cả cửa hàng
            return calculateTotalProductsSoldForAllStores(startDate, endDate);
        }
    }
    
    private Long getStoreCountWithRevenue(LocalDate startDate, LocalDate endDate) {
        List<CuaHang> stores = cuaHangService.findActiveStores();
        return stores.stream()
                .filter(store -> {
                    Double revenue = datHangService.getTotalRevenueByCuaHangAndDateRange(
                        store, "Hoàn thành", startDate, endDate);
                    return revenue != null && revenue > 0;
                })
                .count();
    }
    
    // Các phương thức tính toán cho tất cả cửa hàng
    private Double calculateTotalRevenueForAllStores(LocalDate startDate, LocalDate endDate) {
        List<CuaHang> stores = cuaHangService.findActiveStores();
        return stores.stream()
                .mapToDouble(store -> {
                    Double revenue = datHangService.getTotalRevenueByCuaHangAndDateRange(
                        store, "Hoàn thành", startDate, endDate);
                    return revenue != null ? revenue : 0.0;
                })
                .sum();
    }
    
    private Long calculateTotalCompletedOrdersForAllStores(LocalDate startDate, LocalDate endDate) {
        List<CuaHang> stores = cuaHangService.findActiveStores();
        return stores.stream()
                .mapToLong(store -> datHangService.countByCuaHangAndTrangThai(store, "Hoàn thành"))
                .sum();
    }
    
    private Long calculateTotalProductsSoldForAllStores(LocalDate startDate, LocalDate endDate) {
        List<CuaHang> stores = cuaHangService.findActiveStores();
        return stores.stream()
                .mapToLong(store -> {
                    Long productsSold = datHangService.getTotalProductsSoldByCuaHang(
                        store, "Hoàn thành", startDate, endDate);
                    return productsSold != null ? productsSold : 0L;
                })
                .sum();
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportSalesHistory(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) Integer maCuaHang) {
        
        try {
            CuaHang selectedStore = maCuaHang != null ? cuaHangService.findByMaCuaHang(maCuaHang) : null;
            
            Page<DatHangChiTiet> salesHistoryPage = datHangService.findCompletedOrderDetails(
                maCuaHang, keyword, startDate, endDate, productName, Pageable.unpaged());
            
            List<DatHangChiTiet> salesHistory = salesHistoryPage.getContent();
            
            // Tính toán thống kê
            Map<String, Object> salesStats = calculateSalesStats(maCuaHang, startDate, endDate);
            
            byte[] excelData = excelExportService.exportAdminSalesHistoryToExcel(
                salesHistory, selectedStore, salesStats, maCuaHang != null);
            
            String storeName = selectedStore != null ? 
                selectedStore.getTenCuaHang().replaceAll("\\s+", "_") : "TatCaCuaHang";
            String filename = "LichSuBanHang_Admin_" + storeName + 
                    "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMyyyy_HHmmss")) + ".xlsx";
            
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
	        @RequestParam("orderIds") String orderIdsStr) {
	    
	    try {
	        // Parse order IDs
	        List<Integer> orderIds = Arrays.stream(orderIdsStr.split(","))
	                .map(String::trim)
	                .map(Integer::parseInt)
	                .collect(Collectors.toList());
	        
	        System.out.println("=== EXPORTING SELECTED ORDERS ===");
	        System.out.println("Order IDs: " + orderIds);
	        
	        // Lấy các chi tiết đơn hàng đã chọn - BỎ FILTER TRẠNG THÁI
	        List<DatHangChiTiet> salesHistory = new ArrayList<>();
	        for (Integer orderId : orderIds) {
	            DatHang order = datHangService.findByMaDatHang(orderId);
	            if (order != null) {
	                System.out.println("Order " + orderId + " - Status: " + order.getTrangThai());
	                
	                // Eager load các quan hệ cần thiết
	                order.getDatHangChiTiets().size(); // Force load
	                if (order.getVanChuyens() != null) {
	                    order.getVanChuyens().size(); // Force load
	                }
	                if (order.getThanhToans() != null) {
	                    order.getThanhToans().size(); // Force load
	                }
	                
	                // THÊM TẤT CẢ đơn hàng, không filter theo trạng thái
	                salesHistory.addAll(order.getDatHangChiTiets());
	                
	                System.out.println("Added " + order.getDatHangChiTiets().size() + " items from order " + orderId);
	            } else {
	                System.out.println("Order " + orderId + " not found");
	            }
	        }
	        
	        System.out.println("Total items to export: " + salesHistory.size());
	        
	        if (salesHistory.isEmpty()) {
	            System.out.println("WARNING: No data to export!");
	        }
	        
	        // Tính toán thống kê cho đơn hàng đã chọn
	        Map<String, Object> salesStats = calculateStatsForSelectedOrders(salesHistory);
	        
	        byte[] excelData = excelExportService.exportAdminSalesHistoryToExcel(
	            salesHistory, null, salesStats, false);
	        
	        String filename = "LichSuBanHang_DaChon_Admin_" + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMyyyy_HHmmss")) + ".xlsx";
	        
	        HttpHeaders headers = new HttpHeaders();
	        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
	        headers.setContentDispositionFormData("attachment", filename);
	        
	        System.out.println("=== EXPORT COMPLETED ===");
	        
	        return ResponseEntity.ok()
	                .headers(headers)
	                .body(excelData);
	                
	    } catch (Exception e) {
	        System.err.println("ERROR during export: " + e.getMessage());
	        e.printStackTrace();
	        return ResponseEntity.internalServerError().build();
	    }
	}
    
    private Map<String, Object> calculateStatsForSelectedOrders(List<DatHangChiTiet> salesHistory) {
        Map<String, Object> stats = new HashMap<>();
        
        // 1. Tính Tổng Doanh thu
        stats.put("totalRevenue", salesHistory.stream()
            .mapToDouble(ct -> ct.getThanhTien().doubleValue())
            .sum());
            
        // 2. Tổng số đơn hàng đã hoàn thành
        long distinctCompletedOrders = salesHistory.stream()
            .map(ct -> ct.getDatHang().getMaDatHang())
            .distinct()
            .count();
        stats.put("totalCompletedOrders", distinctCompletedOrders);
        
        // 3. Tổng số sản phẩm đã bán
        Long totalProductsSold = salesHistory.stream()
            .mapToLong(DatHangChiTiet::getSoLuong)
            .sum();
        stats.put("totalProductsSold", totalProductsSold);
        
        // 4. Doanh thu trung bình mỗi đơn
        Double totalRevenue = (Double) stats.get("totalRevenue");
        Double averageOrderValue = distinctCompletedOrders > 0 ? totalRevenue / distinctCompletedOrders : 0.0;
        stats.put("averageOrderValue", averageOrderValue);
        
        // 5. Số lượng cửa hàng
        long storeCount = salesHistory.stream()
            .map(ct -> ct.getSanPham().getCuaHang().getMaCuaHang())
            .distinct()
            .count();
        stats.put("storeCount", storeCount);
        
        return stats;
    }
}