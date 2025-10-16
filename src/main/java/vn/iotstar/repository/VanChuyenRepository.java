package vn.iotstar.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.iotstar.entity.VanChuyen;
import vn.iotstar.entity.CuaHang;
import vn.iotstar.entity.DatHang;

import java.util.List;
import java.util.Optional;
@Repository
public interface VanChuyenRepository extends JpaRepository<VanChuyen, Integer> {
    
	@Query("SELECT DISTINCT dh FROM DatHang dh " +
		       "JOIN dh.datHangChiTiets dhct " +
		       "JOIN dhct.sanPham sp " +
		       "WHERE sp.cuaHang = :cuaHang " +
		       "AND dh.trangThai = :trangThai " +
		       "AND dh.trangThai NOT IN ('Hủy', 'Hoàn thành') " + // THÊM ĐIỀU KIỆN NÀY
		       "AND NOT EXISTS (SELECT 1 FROM VanChuyen vc WHERE vc.datHang.maDatHang = dh.maDatHang) " +
		       "AND (:keyword IS NULL OR :keyword = '' OR " +
		       "     LOWER(dh.nguoiDung.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
		       "     CAST(dh.maDatHang AS string) LIKE CONCAT('%', :keyword, '%'))")
		Page<DatHang> findUnassignedConfirmedOrdersByCuaHang(
		    @Param("cuaHang") CuaHang cuaHang,
		    @Param("keyword") String keyword,
		    @Param("trangThai") String trangThai,
		    Pageable pageable);
    
    @Query("SELECT DISTINCT dh FROM DatHang dh " +
           "JOIN dh.datHangChiTiets dhct " +
           "JOIN dhct.sanPham sp " +
           "WHERE sp.cuaHang = :cuaHang " +
           "AND dh.trangThai = :trangThai")
    List<DatHang> findAllConfirmedOrdersByCuaHang(
        @Param("cuaHang") CuaHang cuaHang,
        @Param("trangThai") String trangThai);
    
    @Query("SELECT COUNT(vc) FROM VanChuyen vc WHERE vc.datHang.maDatHang = :orderId")
    long countVanChuyenByOrderId(@Param("orderId") Integer orderId);
    
    @Query("SELECT vc FROM VanChuyen vc " +
    	       "JOIN vc.datHang dh " +
    	       "JOIN dh.datHangChiTiets dhct " +
    	       "JOIN dhct.sanPham sp " +
    	       "WHERE sp.cuaHang = :cuaHang " +
    	       "AND dh.trangThai NOT IN ('Hoàn thành', 'Hủy') " + // QUAN TRỌNG: loại bỏ đơn Hủy
    	       "AND vc.trangThai NOT IN ('Hoàn thành', 'Hủy') " + // QUAN TRỌNG: loại bỏ vận chuyển Hủy
    	       "AND (:keyword IS NULL OR :keyword = '' OR " +
    	       "     LOWER(dh.nguoiDung.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
    	       "     CAST(dh.maDatHang AS string) LIKE CONCAT('%', :keyword, '%')) ")
    	Page<VanChuyen> findAssignedOrdersByCuaHang(
    	    @Param("cuaHang") CuaHang cuaHang,
    	    @Param("keyword") String keyword,
    	    Pageable pageable);
    
    @Query("SELECT COUNT(DISTINCT dh) FROM DatHang dh " +
    	       "JOIN dh.datHangChiTiets dhct " +
    	       "JOIN dhct.sanPham sp " +
    	       "WHERE sp.cuaHang = :cuaHang " +
    	       "AND dh.trangThai = :trangThai " +
    	       "AND dh.trangThai NOT IN ('Hủy', 'Hoàn thành') " + // THÊM ĐIỀU KIỆN NÀY
    	       "AND NOT EXISTS (SELECT 1 FROM VanChuyen vc WHERE vc.datHang.maDatHang = dh.maDatHang)")
    	long countUnassignedConfirmedOrdersByCuaHang(
    	    @Param("cuaHang") CuaHang cuaHang,
    	    @Param("trangThai") String trangThai);
    
    @Query("SELECT COUNT(DISTINCT vc.datHang.maDatHang) FROM VanChuyen vc " +
    	       "JOIN vc.datHang dh " +
    	       "JOIN dh.datHangChiTiets dhct " +
    	       "JOIN dhct.sanPham sp " +
    	       "WHERE sp.cuaHang = :cuaHang " +
    	       "AND dh.trangThai NOT IN ('Hoàn thành', 'Hủy') " + // THÊM 'Hủy' vào đây
    	       "AND vc.trangThai NOT IN ('Hoàn thành', 'Hủy')") // THÊM 'Hủy' vào đây
    	long countAssignedOrdersByCuaHang(@Param("cuaHang") CuaHang cuaHang);
    
    @Query("SELECT vc FROM VanChuyen vc WHERE vc.datHang.maDatHang = :orderId")
    Optional<VanChuyen> findByOrderId(@Param("orderId") Integer orderId);
    
    @Query("SELECT vc FROM VanChuyen vc WHERE vc.nguoiDung.maNguoiDung = :shipperId")
    List<VanChuyen> findByShipperId(@Param("shipperId") Integer shipperId);
    
 // Phương thức mới cho Shipper
    @Query("SELECT vc FROM VanChuyen vc WHERE vc.nguoiDung.maNguoiDung = :shipperId AND vc.trangThai = :status")
    Page<VanChuyen> findByShipperIdAndStatus(@Param("shipperId") Integer shipperId, 
                                           @Param("status") String status, 
                                           Pageable pageable);

    @Query("SELECT COUNT(vc) FROM VanChuyen vc WHERE vc.nguoiDung.maNguoiDung = :shipperId AND vc.trangThai = :status")
    long countByShipperIdAndStatus(@Param("shipperId") Integer shipperId, 
                                 @Param("status") String status);
}