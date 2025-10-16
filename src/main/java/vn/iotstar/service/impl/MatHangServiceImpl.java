package vn.iotstar.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.entity.GioHang;
import vn.iotstar.entity.MatHang;
import vn.iotstar.entity.SanPham;
import vn.iotstar.repository.MatHangRepository;
import vn.iotstar.service.MatHangService;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MatHangServiceImpl implements MatHangService {

    @Autowired
    private MatHangRepository matHangRepository;

    @Override
    public List<MatHang> findByGioHang(GioHang gioHang) {
        return matHangRepository.findByGioHang(gioHang);
    }

    @Override
    public MatHang findById(Integer maMatHang) {
        return matHangRepository.findById(maMatHang).orElse(null);
    }

    @Override
    public MatHang findByGioHangAndSanPham(GioHang gioHang, SanPham sanPham) {
        Optional<MatHang> matHang = matHangRepository.findByGioHangAndSanPham(gioHang, sanPham);
        return matHang.orElse(null);
    }

    @Override
    public MatHang addOrUpdateMatHang(GioHang gioHang, SanPham sanPham, Integer soLuong) {
        MatHang matHang = findByGioHangAndSanPham(gioHang, sanPham);
        
        if (matHang != null) {
            // Sản phẩm đã có trong giỏ, cập nhật số lượng
            matHang.setSoLuongDat(matHang.getSoLuongDat() + soLuong);
        } else {
            // Sản phẩm chưa có, tạo mới
            matHang = MatHang.builder()
                    .gioHang(gioHang)
                    .sanPham(sanPham)
                    .soLuongDat(soLuong)
                    .build();
        }
        
        return matHangRepository.save(matHang);
    }

    @Override
    public MatHang updateQuantity(Integer maMatHang, Integer soLuong) {
        MatHang matHang = findById(maMatHang);
        if (matHang != null) {
            matHang.setSoLuongDat(soLuong);
            return matHangRepository.save(matHang);
        }
        return null;
    }

    @Override
    public MatHang save(MatHang matHang) {
        return matHangRepository.save(matHang);
    }

    @Override
    public void delete(MatHang matHang) {
        matHangRepository.delete(matHang);
    }

    @Override
    public void deleteById(Integer maMatHang) {
        matHangRepository.deleteById(maMatHang);
    }

    @Override
    public int countTotalItems(GioHang gioHang) {
        List<MatHang> matHangs = findByGioHang(gioHang);
        return matHangs.stream()
                .mapToInt(MatHang::getSoLuongDat)
                .sum();
    }

    @Override
    public int countDistinctItems(GioHang gioHang) {
        List<MatHang> matHangs = findByGioHang(gioHang);
        return matHangs.size();
    }

    @Override
    public void clearCart(GioHang gioHang) {
        List<MatHang> matHangs = findByGioHang(gioHang);
        matHangRepository.deleteAll(matHangs);
    }

    @Override
    public boolean isProductInCart(GioHang gioHang, SanPham sanPham) {
        MatHang matHang = findByGioHangAndSanPham(gioHang, sanPham);
        return matHang != null;
    }
}