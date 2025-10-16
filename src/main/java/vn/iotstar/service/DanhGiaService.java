package vn.iotstar.service;

import vn.iotstar.entity.DanhGia;
import vn.iotstar.entity.SanPham;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DanhGiaService {
    
    List<DanhGia> findBySanPhamOrderByNgayDanhGiaDesc(SanPham sanPham);
    
    List<DanhGia> findTop5BySanPhamOrderByNgayDanhGiaDesc(SanPham sanPham);
    
    Double getAverageRatingBySanPham(SanPham sanPham);
    
    Long getCountBySanPham(SanPham sanPham);
    
    // Phân phối số sao (số lượng đánh giá theo từng mức sao)
    List<Object[]> getRatingDistributionBySanPham(SanPham sanPham);
    
    DanhGia save(DanhGia danhGia);
    
    Page<DanhGia> findBySanPham(SanPham sanPham, Pageable pageable);
    
    // THÊM METHOD MỚI SỬ DỤNG NATIVE QUERY
    List<DanhGia> findDanhGiasWithUserBySanPham(Integer maSanPham, int page, int size);
    
    // THÊM METHOD ĐẾM
    Long countDanhGiasBySanPham(Integer maSanPham);
    
    Optional<DanhGia> findById(Integer id);
    
    void delete(Integer id);
    
    List<DanhGia> findAll();
    Page<DanhGia> findAll(Pageable pageable);
}