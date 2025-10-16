package vn.iotstar.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.iotstar.entity.CuaHang;
import vn.iotstar.entity.DanhMuc;
import vn.iotstar.entity.SanPham;

import java.util.List;

@Repository
public interface SanPhamRepository extends JpaRepository<SanPham, Integer>, 
                                           JpaSpecificationExecutor<SanPham> {
    
    @Query("SELECT s FROM SanPham s WHERE s.danhMuc = :danhMuc ORDER BY s.ngayNhap DESC")
    List<SanPham> findTop4ByDanhMucOrderByNgayNhapDesc(DanhMuc danhMuc, Pageable pageable);
    
    SanPham findByMaSanPham(Integer maSanPham);
    
    @Query("SELECT s FROM SanPham s WHERE s.danhMuc = :danhMuc AND s.maSanPham != :maSanPham")
    List<SanPham> findByDanhMucAndNotMaSanPham(DanhMuc danhMuc, Integer maSanPham);
    
    // THÊM METHOD MỚI: Lấy sản phẩm cùng LoaiSanPham (trừ sản phẩm hiện tại)
    @Query("SELECT s FROM SanPham s WHERE s.loaiSanPham = :loaiSanPham AND s.maSanPham != :maSanPham")
    List<SanPham> findByLoaiSanPhamAndNotMaSanPham(String loaiSanPham, Integer maSanPham);
    
    @Query("SELECT s FROM SanPham s WHERE s.danhMuc = :danhMuc")
    List<SanPham> findByDanhMuc(DanhMuc danhMuc);
    
    Page<SanPham> findByDanhMuc(DanhMuc danhMuc, Pageable pageable);
    
    @Query("SELECT s FROM SanPham s WHERE s.cuaHang = :cuaHang")
    Page<SanPham> findByCuaHang(CuaHang cuaHang, Pageable pageable);

    @Query("SELECT s FROM SanPham s WHERE s.cuaHang = :cuaHang")
    List<SanPham> findByCuaHang(CuaHang cuaHang);
    
    List<SanPham> findTop5ByOrderByNgayNhapDesc();

    // THÊM METHOD MỚI
    long count(); // Đếm tổng số sản phẩm
    
    // Lấy tất cả sản phẩm (cho admin)
    List<SanPham> findAll();
    
    Page<SanPham> findAll(Pageable pageable);
    
    @Query("SELECT s FROM SanPham s WHERE s.tenSanPham LIKE %:keyword% OR s.moTaSanPham LIKE %:keyword%")
    Page<SanPham> searchProducts(@Param("keyword") String keyword, Pageable pageable);
    
    Page<SanPham> findByTenSanPhamContainingIgnoreCaseOrMoTaSanPhamContainingIgnoreCase(
        String tenSanPham, String moTaSanPham, Pageable pageable);
        
    // Thêm method mới để lấy danh sách LoaiSanPham duy nhất theo DanhMuc
    @Query("SELECT DISTINCT s.loaiSanPham FROM SanPham s WHERE s.danhMuc = :danhMuc AND s.loaiSanPham IS NOT NULL AND s.loaiSanPham != ''")
    List<String> findDistinctLoaiSanPhamByDanhMuc(DanhMuc danhMuc);
    
    // THÊM METHOD MỚI: Lấy sản phẩm theo danh sách mã sản phẩm
    @Query("SELECT s FROM SanPham s WHERE s.maSanPham IN :maSanPhamList")
    List<SanPham> findByMaSanPhamIn(List<Integer> maSanPhamList);
    
 // Lấy sản phẩm theo cửa hàng với trạng thái
    @Query("SELECT s FROM SanPham s WHERE s.cuaHang = :cuaHang AND s.trangThai = :trangThai")
    List<SanPham> findByCuaHangAndTrangThai(CuaHang cuaHang, Boolean trangThai);
    
    // Lấy sản phẩm theo danh mục với trạng thái
    @Query("SELECT s FROM SanPham s WHERE s.danhMuc = :danhMuc AND s.trangThai = true")
    List<SanPham> findByDanhMucAndTrangThaiTrue(DanhMuc danhMuc);
    
    // Lấy top sản phẩm theo danh mục với trạng thái
    @Query("SELECT s FROM SanPham s WHERE s.danhMuc = :danhMuc AND s.trangThai = true ORDER BY s.ngayNhap DESC")
    List<SanPham> findTop4ByDanhMucAndTrangThaiTrueOrderByNgayNhapDesc(DanhMuc danhMuc, Pageable pageable);
    
    // Tìm kiếm sản phẩm với trạng thái
    @Query("SELECT s FROM SanPham s WHERE (s.tenSanPham LIKE %:keyword% OR s.moTaSanPham LIKE %:keyword%) AND s.trangThai = true")
    Page<SanPham> searchActiveProducts(@Param("keyword") String keyword, Pageable pageable);

    // THÊM METHOD MỚI CHO VENDOR: Tìm kiếm sản phẩm theo tên/mô tả VÀ Cửa hàng
    @Query("SELECT s FROM SanPham s WHERE s.cuaHang = :cuaHang AND (s.tenSanPham LIKE %:tenSanPham% OR s.moTaSanPham LIKE %:moTaSanPham%)")
    Page<SanPham> findByCuaHangAndTenSanPhamContainingIgnoreCaseOrMoTaSanPhamContainingIgnoreCase(
        @Param("cuaHang") CuaHang cuaHang,
        @Param("tenSanPham") String tenSanPham, 
        @Param("moTaSanPham") String moTaSanPham, 
        Pageable pageable);

    // THÊM METHOD MỚI CHO VENDOR: Lấy distinct loại sản phẩm theo Cửa hàng và Danh mục
    @Query("SELECT DISTINCT s.loaiSanPham FROM SanPham s WHERE s.cuaHang = :cuaHang AND s.danhMuc = :danhMuc AND s.loaiSanPham IS NOT NULL AND s.loaiSanPham != ''")
    List<String> findDistinctLoaiSanPhamByCuaHangAndDanhMuc(CuaHang cuaHang, DanhMuc danhMuc);
}