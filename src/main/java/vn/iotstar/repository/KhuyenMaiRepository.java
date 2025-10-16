package vn.iotstar.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.iotstar.entity.KhuyenMai;
import vn.iotstar.entity.CuaHang;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Repository
public interface KhuyenMaiRepository extends JpaRepository<KhuyenMai, Integer> {
    
    // Tìm khuyến mãi theo cửa hàng
    List<KhuyenMai> findByCuaHang(CuaHang cuaHang);
    
    // Tìm khuyến mãi theo cửa hàng với phân trang
    Page<KhuyenMai> findByCuaHang(CuaHang cuaHang, Pageable pageable);
    
    // Tìm kiếm theo mã giảm giá và cửa hàng
    Page<KhuyenMai> findByCuaHangAndMaGiamGiaContainingIgnoreCase(
        CuaHang cuaHang, String maGiamGia, Pageable pageable);
    
    // Tìm kiếm theo thời gian bắt đầu/kết thúc và cửa hàng
    @Query("SELECT km FROM KhuyenMai km WHERE km.cuaHang = :cuaHang " +
           "AND (:startDate IS NULL OR km.ngayBatDau >= :startDate) " +
           "AND (:endDate IS NULL OR km.ngayKetThuc <= :endDate)")
    Page<KhuyenMai> findByCuaHangAndDateRange(
        @Param("cuaHang") CuaHang cuaHang,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        Pageable pageable);
    
 // TÌM KIẾM ĐẦY ĐỦ: mã giảm giá + thời gian + trạng thái kích hoạt + tình trạng hạn
    @Query("SELECT km FROM KhuyenMai km WHERE km.cuaHang = :cuaHang " +
           "AND (:maGiamGia IS NULL OR LOWER(km.maGiamGia) LIKE LOWER(CONCAT('%', :maGiamGia, '%'))) " +
           "AND (:startDate IS NULL OR km.ngayBatDau >= :startDate) " +
           "AND (:endDate IS NULL OR km.ngayKetThuc <= :endDate) " +
           "AND (:activationStatus IS NULL OR km.trangThai = :activationStatus) " +
           // Validity Status: active = còn hạn, expired = hết hạn, upcoming = sắp diễn ra
           "AND (:validityStatus IS NULL OR " +
           "  (:validityStatus = 'active' AND km.trangThai = true AND km.ngayBatDau <= :currentDate AND km.ngayKetThuc >= :currentDate) OR " +
           "  (:validityStatus = 'expired' AND km.ngayKetThuc < :currentDate) OR " +
           "  (:validityStatus = 'upcoming' AND km.ngayBatDau > :currentDate)" +
           ")")
    Page<KhuyenMai> findByCuaHangAndFilters(
        @Param("cuaHang") CuaHang cuaHang,
        @Param("maGiamGia") String maGiamGia,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("activationStatus") Boolean activationStatus,
        @Param("validityStatus") String validityStatus,
        @Param("currentDate") LocalDate currentDate,
        Pageable pageable);
    
    // Kiểm tra mã giảm giá đã tồn tại trong cửa hàng chưa
    boolean existsByCuaHangAndMaGiamGia(CuaHang cuaHang, String maGiamGia);
    
    // Kiểm tra mã giảm giá đã tồn tại (trừ khuyến mãi hiện tại khi update)
    boolean existsByCuaHangAndMaGiamGiaAndMaKhuyenMaiNot(CuaHang cuaHang, String maGiamGia, Integer maKhuyenMai);
    
 // Thêm vào KhuyenMaiRepository.java
    @Query("SELECT COUNT(km) FROM KhuyenMai km WHERE km.cuaHang = :cuaHang " +
           "AND km.trangThai = true " +
           "AND km.ngayBatDau <= :currentDate " +
           "AND km.ngayKetThuc >= :currentDate")
    Long countActiveByCuaHang(@Param("cuaHang") CuaHang cuaHang, @Param("currentDate") LocalDate currentDate);

    @Query("SELECT COUNT(km) FROM KhuyenMai km WHERE km.cuaHang = :cuaHang " +
           "AND km.ngayBatDau > :currentDate")
    Long countUpcomingByCuaHang(@Param("cuaHang") CuaHang cuaHang, @Param("currentDate") LocalDate currentDate);

    @Query("SELECT COUNT(km) FROM KhuyenMai km WHERE km.cuaHang = :cuaHang " +
           "AND km.ngayKetThuc < :currentDate")
    Long countExpiredByCuaHang(@Param("cuaHang") CuaHang cuaHang, @Param("currentDate") LocalDate currentDate);

    @Query("SELECT COUNT(km) FROM KhuyenMai km WHERE km.cuaHang = :cuaHang " +
           "AND km.trangThai = false")
    Long countInactiveByCuaHang(@Param("cuaHang") CuaHang cuaHang);
}