package vn.iotstar.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.iotstar.entity.DatHang;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.entity.CuaHang;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DatHangRepository extends JpaRepository<DatHang, Integer> {
    
    // Lấy 5 đơn hàng mới nhất
    List<DatHang> findTop5ByOrderByNgayDatDesc();
    
    // Đếm tổng số đơn hàng
    long count();
    
    // Tìm đơn hàng theo trạng thái
    List<DatHang> findByTrangThai(String trangThai);
    
    // Tìm đơn hàng theo người dùng
    List<DatHang> findByNguoiDung_MaNguoiDung(Integer maNguoiDung);
    
    Page<DatHang> findByNguoiDung_MaNguoiDung(Integer maNguoiDung, Pageable pageable);
    
    List<DatHang> findByNguoiDungOrderByNgayDatDesc(NguoiDung nguoiDung);
    
    Page<DatHang> findByNguoiDung(NguoiDung nguoiDung, Pageable pageable);
    
    // Thống kê đơn hàng theo tháng
    @Query("SELECT MONTH(d.ngayDat), COUNT(d) FROM DatHang d WHERE YEAR(d.ngayDat) = YEAR(CURRENT_DATE) GROUP BY MONTH(d.ngayDat) ORDER BY MONTH(d.ngayDat)")
    List<Object[]> getOrderStatsByMonth();
    
    // ================== CÁC PHƯƠNG THỨC CHO VENDOR ==================

    @Query("SELECT COUNT(DISTINCT dh) FROM DatHang dh JOIN DatHangChiTiet dhct ON dhct.datHang = dh WHERE dhct.sanPham.cuaHang = :cuaHang AND dh.trangThai = :trangThai")
    long countByCuaHangAndTrangThai(@Param("cuaHang") CuaHang cuaHang, @Param("trangThai") String trangThai);
    
    @Query("SELECT DISTINCT dh FROM DatHang dh JOIN DatHangChiTiet dhct ON dhct.datHang = dh WHERE dhct.sanPham.cuaHang = :cuaHang AND dh.trangThai = :trangThai")
    List<DatHang> findByCuaHangAndTrangThai(@Param("cuaHang") CuaHang cuaHang, @Param("trangThai") String trangThai, Pageable pageable);
    
    @Query("SELECT COUNT(DISTINCT dh) FROM DatHang dh JOIN DatHangChiTiet dhct ON dhct.datHang = dh WHERE dhct.sanPham.cuaHang = :cuaHang")
    long countByCuaHang(@Param("cuaHang") CuaHang cuaHang);
    
 // Tìm đơn hàng theo cửa hàng với bộ lọc (ĐÃ SỬA LỖI: THAY dh.nguoiDung.hoTen BẰNG dh.nguoiDung.email)
    @Query("SELECT DISTINCT dh FROM DatHang dh JOIN dh.datHangChiTiets dhct " +
           "WHERE dhct.sanPham.cuaHang = :cuaHang " +
           "AND (:keyword IS NULL OR LOWER(dh.nguoiDung.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR CAST(dh.maDatHang AS string) LIKE CONCAT('%', :keyword, '%')) " +
           "AND (:startDate IS NULL OR dh.ngayDat >= :startDate) " +
           "AND (:endDate IS NULL OR dh.ngayDat <= :endDate) " +
           "AND (:trangThai IS NULL OR dh.trangThai = :trangThai)")
    Page<DatHang> findByCuaHangAndFilters(
        @Param("cuaHang") CuaHang cuaHang,
        @Param("keyword") String keyword,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("trangThai") String trangThai,
        Pageable pageable);
    
    // Tìm tất cả đơn hàng theo cửa hàng
    @Query("SELECT DISTINCT dh FROM DatHang dh JOIN dh.datHangChiTiets dhct " +
           "WHERE dhct.sanPham.cuaHang = :cuaHang")
    List<DatHang> findByCuaHang(@Param("cuaHang") CuaHang cuaHang);
    
    @Query("SELECT DISTINCT dh FROM DatHang dh " +
    	       "LEFT JOIN dh.datHangChiTiets dhct " +
    	       "LEFT JOIN dhct.sanPham sp " +
    	       "LEFT JOIN sp.cuaHang ch " +
    	       "WHERE (:keyword IS NULL OR LOWER(dh.nguoiDung.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
    	       "     OR CAST(dh.maDatHang AS string) LIKE CONCAT('%', :keyword, '%')) " +
    	       "AND (:startDate IS NULL OR dh.ngayDat >= :startDate) " +
    	       "AND (:endDate IS NULL OR dh.ngayDat <= :endDate) " +
    	       "AND (:trangThai IS NULL OR dh.trangThai = :trangThai) " +
    	       "AND (:maCuaHang IS NULL OR ch.maCuaHang = :maCuaHang)")
    	Page<DatHang> findAllOrdersWithFilters(
    	    @Param("keyword") String keyword,
    	    @Param("startDate") LocalDate startDate,
    	    @Param("endDate") LocalDate endDate,
    	    @Param("trangThai") String trangThai,
    	    @Param("maCuaHang") Integer maCuaHang,
    	    Pageable pageable);
}