package vn.iotstar.service;

import vn.iotstar.entity.ThanhToan;
import java.util.List;
import java.util.Optional;

public interface ThanhToanService {
    ThanhToan save(ThanhToan thanhToan);
    Optional<ThanhToan> findById(Integer id);
    List<ThanhToan> findByDatHang(Integer maDatHang);
    ThanhToan updateTrangThai(Integer maThanhToan, String trangThai);
    
 // Thêm phương thức mới
    void updateTrangThaiByDatHang(Integer maDatHang, String trangThai);
}