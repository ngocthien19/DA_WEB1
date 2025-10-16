package vn.iotstar.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.iotstar.entity.DatHangChiTiet;
import vn.iotstar.entity.CuaHang;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DatHangChiTietRepository extends JpaRepository<DatHangChiTiet, Integer> {
    
    // Thêm các phương thức mới cho lịch sử bán hàng
    
	@Query("SELECT dhct FROM DatHangChiTiet dhct " +
		       "JOIN dhct.datHang dh " +
		       "JOIN dhct.sanPham sp " +
		       "WHERE sp.cuaHang = :cuaHang " +
		       "AND dh.trangThai IN ('Hoàn thành', :trangThaiHuy) " + // Dùng parameter
		       "AND (:keyword IS NULL OR :keyword = '' OR " +
		       "     LOWER(dh.nguoiDung.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
		       "     CAST(dh.maDatHang AS string) LIKE CONCAT('%', :keyword, '%')) " +
		       "AND (:startDate IS NULL OR dh.ngayDat >= :startDate) " +
		       "AND (:endDate IS NULL OR dh.ngayDat <= :endDate) " +
		       "AND (:productName IS NULL OR :productName = '' OR " +
		       "     LOWER(sp.tenSanPham) LIKE LOWER(CONCAT('%', :productName, '%')))")
		Page<DatHangChiTiet> findCompletedOrderDetailsByCuaHang(
		    @Param("cuaHang") CuaHang cuaHang,
		    @Param("keyword") String keyword,
		    @Param("startDate") LocalDate startDate,
		    @Param("endDate") LocalDate endDate,
		    @Param("productName") String productName,
		    @Param("trangThaiHuy") String trangThaiHuy, // Thêm parameter
		    Pageable pageable);
    
    @Query("SELECT COALESCE(SUM(dhct.thanhTien), 0) FROM DatHangChiTiet dhct " +
           "JOIN dhct.datHang dh " +
           "JOIN dhct.sanPham sp " +
           "WHERE sp.cuaHang = :cuaHang " +
           "AND dh.trangThai = :trangThai " +
           "AND (:startDate IS NULL OR dh.ngayDat >= :startDate) " +
           "AND (:endDate IS NULL OR dh.ngayDat <= :endDate)")
    Double getTotalRevenueByCuaHangAndDateRange(
        @Param("cuaHang") CuaHang cuaHang,
        @Param("trangThai") String trangThai,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    @Query("SELECT COALESCE(SUM(dhct.soLuong), 0) FROM DatHangChiTiet dhct " +
           "JOIN dhct.datHang dh " +
           "JOIN dhct.sanPham sp " +
           "WHERE sp.cuaHang = :cuaHang " +
           "AND dh.trangThai = :trangThai " +
           "AND (:startDate IS NULL OR dh.ngayDat >= :startDate) " +
           "AND (:endDate IS NULL OR dh.ngayDat <= :endDate)")
    Long getTotalProductsSoldByCuaHang(
        @Param("cuaHang") CuaHang cuaHang,
        @Param("trangThai") String trangThai,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    @Query("SELECT dhct FROM DatHangChiTiet dhct " +
    	       "JOIN dhct.datHang dh " +
    	       "JOIN dhct.sanPham sp " +
    	       "WHERE sp.cuaHang = :cuaHang " +
    	       "AND dh.trangThai = 'Hoàn thành' " +
    	       "AND dh.ngayDat = :date")
    	List<DatHangChiTiet> findCompletedOrderDetailsByCuaHangAndDate(
    	    @Param("cuaHang") CuaHang cuaHang,
    	    @Param("date") LocalDate date);

    	@Query("SELECT dh.ngayDat, SUM(dhct.thanhTien), COUNT(DISTINCT dh), SUM(dhct.soLuong) " +
    	       "FROM DatHangChiTiet dhct " +
    	       "JOIN dhct.datHang dh " +
    	       "JOIN dhct.sanPham sp " +
    	       "WHERE sp.cuaHang = :cuaHang " +
    	       "AND dh.trangThai = 'Hoàn thành' " +
    	       "AND dh.ngayDat BETWEEN :startDate AND :endDate " +
    	       "GROUP BY dh.ngayDat " +
    	       "ORDER BY dh.ngayDat")
    	List<Object[]> getDailyRevenueStatsByCuaHangAndDateRange(
    	    @Param("cuaHang") CuaHang cuaHang,
    	    @Param("startDate") LocalDate startDate,
    	    @Param("endDate") LocalDate endDate);
    	
    	@Query("SELECT dhct FROM DatHangChiTiet dhct " +
    	           "JOIN dhct.datHang dh " +
    	           "JOIN dhct.sanPham sp " +
    	           "JOIN sp.cuaHang ch " +
    	           "WHERE dh.trangThai IN ('Hoàn thành', :trangThaiHuy) " +
    	           "AND (:keyword IS NULL OR :keyword = '' OR " +
    	           "     LOWER(dh.nguoiDung.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
    	           "     CAST(dh.maDatHang AS string) LIKE CONCAT('%', :keyword, '%') OR " +
    	           "     LOWER(ch.tenCuaHang) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
    	           "AND (:startDate IS NULL OR dh.ngayDat >= :startDate) " +
    	           "AND (:endDate IS NULL OR dh.ngayDat <= :endDate) " +
    	           "AND (:productName IS NULL OR :productName = '' OR " +
    	           "     LOWER(sp.tenSanPham) LIKE LOWER(CONCAT('%', :productName, '%')))")
    	    Page<DatHangChiTiet> findCompletedOrderDetails(
    	        @Param("keyword") String keyword,
    	        @Param("startDate") LocalDate startDate,
    	        @Param("endDate") LocalDate endDate,
    	        @Param("productName") String productName,
    	        @Param("trangThaiHuy") String trangThaiHuy,
    	        Pageable pageable);
}