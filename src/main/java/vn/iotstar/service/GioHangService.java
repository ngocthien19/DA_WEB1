package vn.iotstar.service;

import vn.iotstar.entity.GioHang;
import vn.iotstar.entity.NguoiDung;

import java.util.Optional;

public interface GioHangService {
    
    GioHang findByNguoiDung(NguoiDung nguoiDung);

    GioHang createGioHang(NguoiDung nguoiDung);

    GioHang save(GioHang gioHang);
       
    Optional<GioHang> findById(Integer maGioHang);
   
    void delete(GioHang gioHang);
    
    GioHang getOrCreateGioHang(NguoiDung nguoiDung);
}