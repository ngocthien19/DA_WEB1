package vn.iotstar.service;

import vn.iotstar.entity.VaiTro;

import java.util.List;
import java.util.Optional;

public interface VaiTroService {
    
    // Lấy tất cả vai trò
    List<VaiTro> getAllVaiTro();
    
    // Lấy vai trò theo mã
    Optional<VaiTro> getVaiTroById(String maVaiTro);
    
    // Lấy vai trò theo tên
    Optional<VaiTro> getVaiTroByTen(String tenVaiTro);
}