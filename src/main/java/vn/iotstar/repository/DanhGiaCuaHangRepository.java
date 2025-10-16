package vn.iotstar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.iotstar.entity.CuaHang;
import vn.iotstar.entity.DanhGiaCuaHang;
import vn.iotstar.entity.DatHang;
import vn.iotstar.entity.NguoiDung;

import java.util.List;
import java.util.Optional;

@Repository
public interface DanhGiaCuaHangRepository extends JpaRepository<DanhGiaCuaHang, Integer> {
    
    // Tìm tất cả đánh giá theo cửa hàng, sắp xếp theo ngày mới nhất
    List<DanhGiaCuaHang> findByCuaHangOrderByNgayDanhGiaDesc(CuaHang cuaHang);
    
    // Tính điểm đánh giá trung bình theo cửa hàng
    @Query("SELECT COALESCE(AVG(d.soSao), 0) FROM DanhGiaCuaHang d WHERE d.cuaHang = :cuaHang")
    Double findAverageRatingByCuaHang(@Param("cuaHang") CuaHang cuaHang);
    
    // Đếm số lượng đánh giá theo cửa hàng
    @Query("SELECT COUNT(d) FROM DanhGiaCuaHang d WHERE d.cuaHang = :cuaHang")
    Long countByCuaHang(@Param("cuaHang") CuaHang cuaHang);
    
    // Kiểm tra người dùng đã đánh giá cửa hàng cho đơn hàng này chưa
    Optional<DanhGiaCuaHang> findByDatHangAndNguoiDung(DatHang datHang, NguoiDung nguoiDung);
    
    // Kiểm tra đơn hàng đã được đánh giá chưa
    boolean existsByDatHang(DatHang datHang);
}
