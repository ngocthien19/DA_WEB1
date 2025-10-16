package vn.iotstar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.iotstar.entity.GioHang;
import vn.iotstar.entity.MatHang;
import vn.iotstar.entity.SanPham;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatHangRepository extends JpaRepository<MatHang, Integer> {
    List<MatHang> findByGioHang(GioHang gioHang);
    Optional<MatHang> findByGioHangAndSanPham(GioHang gioHang, SanPham sanPham);
}