package vn.iotstar.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import vn.iotstar.entity.SanPham;
import vn.iotstar.model.SanPhamModel;
import vn.iotstar.entity.CuaHang;
import vn.iotstar.entity.DanhMuc;

public interface SanPhamService {
    List<SanPham> findTop4ByDanhMucOrderByNgayNhapDesc(DanhMuc danhMuc);
    SanPham findByMaSanPham(Integer maSanPham);
    List<SanPham> findRelatedProductsByCategoryExcludingCurrent(DanhMuc danhMuc, Integer maSanPham);
    
    // THÊM METHOD MỚI: Lấy sản phẩm cùng LoaiSanPham
    List<SanPham> findRelatedProductsByLoaiSanPhamExcludingCurrent(String loaiSanPham, Integer maSanPham);
    
    List<SanPham> findByDanhMuc(DanhMuc danhMuc);
    Page<SanPham> findByDanhMuc(DanhMuc danhMuc, Pageable pageable);
    
    // Method cho Specification
    Page<SanPham> findAll(Specification<SanPham> spec, Pageable pageable);
    
    List<SanPham> findAll();
    
    Page<SanPham> findByCuaHang(CuaHang cuaHang, Pageable pageable);
    List<SanPham> findByCuaHang(CuaHang cuaHang);
    
    // Thêm các phương thức sau vào SanPhamService.java
    Page<SanPham> findAll(Pageable pageable);
    Page<SanPham> searchProducts(String keyword, Pageable pageable);
    SanPham createProduct(SanPhamModel model);
    SanPham updateProduct(Integer id, SanPhamModel model);
    void deleteProduct(Integer id);
    SanPham save(SanPham sanPham);
    
    // Thêm phương thức mới để lấy danh sách LoaiSanPham theo DanhMuc
    List<String> findDistinctLoaiSanPhamByDanhMuc(DanhMuc danhMuc);
    
    // THÊM METHOD MỚI: Lấy sản phẩm theo danh sách ID
    List<SanPham> findAllById(List<Integer> ids);
    
 // Lấy sản phẩm theo danh mục với trạng thái hoạt động
    List<SanPham> findTop4ByDanhMucAndTrangThaiTrueOrderByNgayNhapDesc(DanhMuc danhMuc);
    
    // Lấy sản phẩm theo cửa hàng với trạng thái
    List<SanPham> findByCuaHangAndTrangThai(CuaHang cuaHang, Boolean trangThai);
    
    // Tìm kiếm sản phẩm đang hoạt động
    Page<SanPham> searchActiveProducts(String keyword, Pageable pageable);
    
    boolean canUpdateProduct(Integer productId);
    
 // Thêm phương thức đặc biệt cho hệ thống (bỏ qua validation)
    SanPham updateProductStatusForSystem(Integer productId, Boolean status);
    
    boolean canChangeProductStatus(Integer productId, Boolean newStatus);
    
 // Tìm kiếm sản phẩm trong cửa hàng
    Page<SanPham> findByCuaHangAndTenSanPhamContainingIgnoreCaseOrMoTaSanPhamContainingIgnoreCase(
        CuaHang cuaHang, String tenSanPham, String moTaSanPham, Pageable pageable);
}