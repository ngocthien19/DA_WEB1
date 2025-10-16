package vn.iotstar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.iotstar.entity.GioHang;
import vn.iotstar.entity.NguoiDung;

import java.util.Optional;

@Repository
public interface GioHangRepository extends JpaRepository<GioHang, Integer> {
    Optional<GioHang> findByNguoiDung(NguoiDung nguoiDung);
}