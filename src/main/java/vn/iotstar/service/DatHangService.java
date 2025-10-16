package vn.iotstar.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import vn.iotstar.entity.DatHang;
import vn.iotstar.entity.DatHangChiTiet;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.model.DatHangRequest;
import vn.iotstar.entity.CuaHang;

public interface DatHangService {
    
    // Các phương thức cơ bản (giả định)
    List<DatHang> findAll();
    DatHang findByMaDatHang(Integer maDatHang);
    DatHang save(DatHang datHang);

    // Phương thức cho Admin Dashboard
    List<DatHang> findRecentOrders(int limit);
    long countAllOrders();

    // ================== CÁC PHƯƠNG THỨC MỚI CHO VENDOR ==================
    
    long countByCuaHangAndTrangThai(CuaHang cuaHang, String trangThai);

    List<DatHang> findRecentOrdersByCuaHangAndTrangThai(CuaHang cuaHang, String trangThai, int limit);
    
    /**
     * Xử lý đặt hàng từ giỏ hàng
     */
    DatHang datHang(NguoiDung nguoiDung, DatHangRequest datHangRequest);
    
    /**
     * Tìm đơn hàng theo người dùng
     */
    List<DatHang> findByNguoiDung(Integer maNguoiDung);
    
 // Các phương thức mới cho vendor
    Page<DatHang> findByCuaHangAndFilters(
        CuaHang cuaHang, 
        String keyword, 
        LocalDate startDate, 
        LocalDate endDate,
        String trangThai,
        Pageable pageable);
    
    List<DatHang> findByCuaHang(CuaHang cuaHang);
    
    void deleteDatHang(Integer maDatHang);
    
    /**
     * Lấy chi tiết đơn hàng đã hoàn thành theo cửa hàng với bộ lọc
     */
    Page<DatHangChiTiet> findCompletedOrderDetailsByCuaHang(
        CuaHang cuaHang, 
        String keyword, 
        LocalDate startDate, 
        LocalDate endDate,
        String productName,
        Pageable pageable);

    /**
     * Tính tổng doanh thu theo cửa hàng và khoảng thời gian
     */
    Double getTotalRevenueByCuaHangAndDateRange(
        CuaHang cuaHang, 
        String trangThai, 
        LocalDate startDate, 
        LocalDate endDate);

    /**
     * Tính tổng số sản phẩm đã bán theo cửa hàng
     */
    Long getTotalProductsSoldByCuaHang(
        CuaHang cuaHang, 
        String trangThai, 
        LocalDate startDate, 
        LocalDate endDate);
    

    /**
     * Lấy thống kê doanh thu hàng ngày theo khoảng thời gian
     */
    Map<LocalDate, DailyRevenueStats> getDailyRevenueStatsByDateRange(
        CuaHang cuaHang, 
        LocalDate startDate, 
        LocalDate endDate); 

    // Inner class cho thống kê doanh thu hàng ngày
    class DailyRevenueStats {
        private double revenue;
        private long orderCount;
        private long productCount;
        
        // constructor, getters, setters
        public DailyRevenueStats(double revenue, long orderCount, long productCount) {
            this.revenue = revenue;
            this.orderCount = orderCount;
            this.productCount = productCount;
        }
        
        // Getters
        public double getRevenue() { return revenue; }
        public long getOrderCount() { return orderCount; }
        public long getProductCount() { return productCount; }
    }
    
    List<DatHang> findByCuaHangAndTrangThai(CuaHang cuaHang, String trangThai, Pageable pageable);
    
    Page<DatHang> findAllOrdersWithFilters(
            String keyword, 
            LocalDate startDate, 
            LocalDate endDate,
            String trangThai,
            Integer maCuaHang,
            Pageable pageable);
    
    
    DatHang updateOrderStatus(Integer maDatHang, String newStatus);
    
    Page<DatHangChiTiet> findCompletedOrderDetails(
            Integer maCuaHang, 
            String keyword, 
            LocalDate startDate, 
            LocalDate endDate,
            String productName,
            Pageable pageable);
    
    Page<DatHang> findByNguoiDungWithPagination(Integer maNguoiDung, Pageable pageable);
    List<DatHang> findByNguoiDung(NguoiDung nguoiDung);
    Page<DatHang> findByNguoiDung(NguoiDung nguoiDung, Pageable pageable);
    
    void syncDeliveryStatusWhenCancelled(Integer maDatHang, String lyDoHuy);
}