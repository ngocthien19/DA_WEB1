package vn.iotstar.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.iotstar.entity.DanhGia;
import vn.iotstar.entity.SanPham;

import java.util.List;

@Repository
public interface DanhGiaRepository extends JpaRepository<DanhGia, Integer> {
    
    // Tìm tất cả đánh giá theo sản phẩm, sắp xếp theo ngày mới nhất
    List<DanhGia> findBySanPhamOrderByNgayDanhGiaDesc(SanPham sanPham);
    
    // Tìm đánh giá theo sản phẩm với phân trang
    @Query("SELECT d FROM DanhGia d WHERE d.sanPham = :sanPham ORDER BY d.ngayDanhGia DESC")
    List<DanhGia> findBySanPhamWithPagination(@Param("sanPham") SanPham sanPham);
    
    // Tính điểm đánh giá trung bình theo sản phẩm
    @Query("SELECT COALESCE(AVG(d.soSao), 0) FROM DanhGia d WHERE d.sanPham = :sanPham")
    Double findAverageRatingBySanPham(@Param("sanPham") SanPham sanPham);
    
    // Đếm số lượng đánh giá theo sản phẩm
    @Query("SELECT COUNT(d) FROM DanhGia d WHERE d.sanPham = :sanPham")
    Long countBySanPham(@Param("sanPham") SanPham sanPham);
    
    // Lấy top 5 đánh giá mới nhất theo sản phẩm
    @Query("SELECT d FROM DanhGia d WHERE d.sanPham = :sanPham ORDER BY d.ngayDanhGia DESC LIMIT 5")
    List<DanhGia> findTop5BySanPhamOrderByNgayDanhGiaDesc(@Param("sanPham") SanPham sanPham);
    
    // Thêm method này vào DanhGiaRepository
    @Query("SELECT d.soSao, COUNT(d) FROM DanhGia d WHERE d.sanPham = :sanPham GROUP BY d.soSao ORDER BY d.soSao DESC")
    List<Object[]> getRatingDistribution(@Param("sanPham") SanPham sanPham);
    
    @EntityGraph(attributePaths = {"nguoiDung"})
    @Query("SELECT d FROM DanhGia d WHERE d.sanPham.maSanPham = :maSanPham")
    List<DanhGia> findBySanPhamMaSanPhamWithUser(
            @Param("maSanPham") Integer maSanPham,
            Pageable pageable);
    
    // Đếm tổng số đánh giá cho phân trang
    @Query(value = "SELECT COUNT(*) FROM DanhGia dg WHERE dg.MaSanPham = :maSanPham", 
           nativeQuery = true)
    Long countDanhGiasBySanPham(@Param("maSanPham") Integer maSanPham);
    
    // Thêm method phân trang (giữ nguyên)
    Page<DanhGia> findBySanPham(SanPham sanPham, Pageable pageable);
    
    Page<DanhGia> findAll(Pageable pageable);
}