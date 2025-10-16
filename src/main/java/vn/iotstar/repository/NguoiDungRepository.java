package vn.iotstar.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.iotstar.entity.NguoiDung;

import java.util.List;
import java.util.Optional;

@Repository
public interface NguoiDungRepository extends JpaRepository<NguoiDung, Integer>, JpaSpecificationExecutor<NguoiDung> {
    Optional<NguoiDung> findByEmail(String email);
    Boolean existsByEmail(String email);
    List<NguoiDung> findTop5ByOrderByMaNguoiDungDesc();
    
    // Đếm tổng số người dùng
    long count();
    
    // Tìm người dùng theo vai trò
    List<NguoiDung> findByVaiTro_MaVaiTro(String maVaiTro);
    
    // Tìm người dùng theo mã vai trò và trạng thái
    List<NguoiDung> findByVaiTro_MaVaiTroAndTrangThai(String maVaiTro, String trangThai);
    
    // Tìm kiếm người dùng theo tên hoặc email
    @Query("SELECT n FROM NguoiDung n WHERE LOWER(n.tenNguoiDung) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(n.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<NguoiDung> searchByTenNguoiDungOrEmail(String keyword);
    
    @Query("SELECT n FROM NguoiDung n WHERE LOWER(n.tenNguoiDung) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(n.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<NguoiDung> searchByTenNguoiDungOrEmail(@Param("keyword") String keyword, Pageable pageable);
    
    // 1. Đếm người dùng theo vai trò (chỉ cần VENDOR và SHIPPER)
    @Query("SELECT COUNT(n) FROM NguoiDung n WHERE n.vaiTro.maVaiTro = :maVaiTro")
    long countByVaiTroMaVaiTro(@Param("maVaiTro") String maVaiTro);
    
 // Đếm tổng người dùng thông thường (USER)
    @Query("SELECT COUNT(n) FROM NguoiDung n WHERE n.vaiTro.maVaiTro = 'USER'")
    long countUsers();

    // Đếm tổng người dùng đang hoạt động (bao gồm cả ADMIN)
    @Query("SELECT COUNT(n) FROM NguoiDung n WHERE n.trangThai = 'Hoạt động'")
    long countAllActiveUsers();

    // Đếm tổng người dùng bị khóa (bao gồm cả ADMIN)
    @Query("SELECT COUNT(n) FROM NguoiDung n WHERE n.trangThai = 'Khóa'")
    long countAllInactiveUsers();
    
    NguoiDung findByMaNguoiDung(Integer maNguoiDung);
    
 // Tìm người dùng theo trạng thái
    List<NguoiDung> findByTrangThai(String trangThai);
    
    // Đếm số người dùng theo trạng thái
    long countByTrangThai(String trangThai);
}