package vn.iotstar.service;

import vn.iotstar.entity.GioHang;
import vn.iotstar.entity.MatHang;
import vn.iotstar.entity.SanPham;

import java.util.List;
import java.util.Optional;

public interface MatHangService {
    
    List<MatHang> findByGioHang(GioHang gioHang);

    MatHang findById(Integer maMatHang);

    MatHang findByGioHangAndSanPham(GioHang gioHang, SanPham sanPham);

    MatHang addOrUpdateMatHang(GioHang gioHang, SanPham sanPham, Integer soLuong);

    MatHang updateQuantity(Integer maMatHang, Integer soLuong);

    MatHang save(MatHang matHang);

    void delete(MatHang matHang);

    void deleteById(Integer maMatHang);

    int countTotalItems(GioHang gioHang);
    
    int countDistinctItems(GioHang gioHang);
    void clearCart(GioHang gioHang);
    
    boolean isProductInCart(GioHang gioHang, SanPham sanPham);
}