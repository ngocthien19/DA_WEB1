package vn.iotstar.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import vn.iotstar.entity.CuaHang;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.model.CuaHangModel;

public interface CuaHangService {
    // Lấy tất cả các cửa hàng
    List<CuaHang> findAll();
    
    // Lấy cửa hàng theo mã
    CuaHang findByMaCuaHang(Integer maCuaHang);
    
    // Lấy cửa hàng theo tên
    CuaHang findByTenCuaHang(String tenCuaHang);
     
    List<CuaHang> findTop3NewestStores();
    
    Page<CuaHang> findAll(Pageable pageable);
    
    // Tìm kiếm cửa hàng
    Page<CuaHang> searchStores(String keyword, Pageable pageable);
    
    // Lấy cửa hàng đang hoạt động
    Page<CuaHang> findActiveStores(Pageable pageable);
    
    // Thêm các phương thức mới
    CuaHang createStore(CuaHangModel model);
    CuaHang updateStore(Integer id, CuaHangModel model);
    void deleteStore(Integer id);
    CuaHang save(CuaHang cuaHang);
    
 // Lấy cửa hàng đang hoạt động
    List<CuaHang> findActiveStores();
    
    // Lấy top cửa hàng mới nhất đang hoạt động
    List<CuaHang> findTop3ActiveNewestStores();
    
 // Tìm kiếm cửa hàng đang hoạt động
    Page<CuaHang> searchActiveStores(String keyword, Pageable pageable);
    
 // Tìm cửa hàng theo người dùng
    List<CuaHang> findByNguoiDung(NguoiDung nguoiDung);
    
 // Thống kê
    long countTotalStores();
    long countActiveStores();
    long countInactiveStores();
    double getAverageRating();
}