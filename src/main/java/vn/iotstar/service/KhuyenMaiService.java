package vn.iotstar.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.iotstar.entity.KhuyenMai;
import vn.iotstar.entity.CuaHang;
import vn.iotstar.model.KhuyenMaiModel;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface KhuyenMaiService {
    
    // Lấy tất cả khuyến mãi theo cửa hàng
    List<KhuyenMai> findByCuaHang(CuaHang cuaHang);
    
    // Lấy khuyến mãi theo cửa hàng với phân trang
    Page<KhuyenMai> findByCuaHang(CuaHang cuaHang, Pageable pageable);
    
    // Tìm kiếm và lọc khuyến mãi với đầy đủ bộ lọc
    Page<KhuyenMai> findByCuaHangAndFilters(
        CuaHang cuaHang, 
        String maGiamGia, 
        LocalDate startDate, 
        LocalDate endDate,
        Boolean activationStatus,
        String validityStatus,
        Pageable pageable);
    
    // Lấy khuyến mãi theo mã
    KhuyenMai findByMaKhuyenMai(Integer maKhuyenMai);
    
    // Tạo khuyến mãi mới
    KhuyenMai createKhuyenMai(KhuyenMaiModel model);
    
    // Cập nhật khuyến mãi
    KhuyenMai updateKhuyenMai(Integer id, KhuyenMaiModel model);
    
    // Xóa khuyến mãi
    void deleteKhuyenMai(Integer id);
    
    // Thay đổi trạng thái khuyến mãi
    KhuyenMai changeStatus(Integer id, Boolean status);
    
    // Kiểm tra mã giảm giá đã tồn tại
    boolean isMaGiamGiaExists(CuaHang cuaHang, String maGiamGia);
    
    // Kiểm tra mã giảm giá đã tồn tại (trừ khuyến mãi hiện tại)
    boolean isMaGiamGiaExistsForUpdate(CuaHang cuaHang, String maGiamGia, Integer maKhuyenMai);
    
    Map<String, Long> getPromotionStatusCounts(CuaHang cuaHang);
}