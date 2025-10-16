package vn.iotstar.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.entity.KhuyenMai;
import vn.iotstar.entity.CuaHang;
import vn.iotstar.model.KhuyenMaiModel;
import vn.iotstar.repository.KhuyenMaiRepository;
import vn.iotstar.repository.CuaHangRepository;
import vn.iotstar.service.KhuyenMaiService;

import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class KhuyenMaiServiceImpl implements KhuyenMaiService {

    @Autowired
    private KhuyenMaiRepository khuyenMaiRepository;
    
    @Autowired
    private CuaHangRepository cuaHangRepository;

    @Override
    public List<KhuyenMai> findByCuaHang(CuaHang cuaHang) {
        return khuyenMaiRepository.findByCuaHang(cuaHang);
    }

    @Override
    public Page<KhuyenMai> findByCuaHang(CuaHang cuaHang, Pageable pageable) {
        return khuyenMaiRepository.findByCuaHang(cuaHang, pageable);
    }

    @Override
    public Page<KhuyenMai> findByCuaHangAndFilters(
            CuaHang cuaHang, 
            String maGiamGia, 
            LocalDate startDate, 
            LocalDate endDate,
            Boolean activationStatus,
            String validityStatus,
            Pageable pageable) {
        
        LocalDate currentDate = LocalDate.now();
        
        return khuyenMaiRepository.findByCuaHangAndFilters(
            cuaHang, 
            maGiamGia, 
            startDate, 
            endDate,
            activationStatus,
            validityStatus,
            currentDate,
            pageable);
    }

    @Override
    public KhuyenMai findByMaKhuyenMai(Integer maKhuyenMai) {
        return khuyenMaiRepository.findById(maKhuyenMai).orElse(null);
    }

    @Override
    @Transactional
    public KhuyenMai createKhuyenMai(KhuyenMaiModel model) {
        // Tìm cửa hàng
        Optional<CuaHang> cuaHang = cuaHangRepository.findById(model.getMaCuaHang());
        if (!cuaHang.isPresent()) {
            throw new RuntimeException("Cửa hàng không tồn tại");
        }
        
        // Kiểm tra mã giảm giá đã tồn tại
        if (khuyenMaiRepository.existsByCuaHangAndMaGiamGia(cuaHang.get(), model.getMaGiamGia())) {
            throw new RuntimeException("Mã giảm giá '" + model.getMaGiamGia() + "' đã tồn tại trong cửa hàng");
        }
        
        // VALIDATE DATES MANUALLY - CHỈ ÁP DỤNG KHI TẠO MỚI
        LocalDate today = LocalDate.now();
        if (model.getNgayBatDau().isBefore(today)) {
            throw new RuntimeException("Ngày bắt đầu phải là hiện tại hoặc tương lai");
        }
        if (model.getNgayKetThuc().isBefore(model.getNgayBatDau())) {
            throw new RuntimeException("Ngày kết thúc phải sau ngày bắt đầu");
        }
        if (model.getNgayKetThuc().isBefore(today)) {
            throw new RuntimeException("Ngày kết thúc phải là tương lai");
        }
        
        // Tạo khuyến mãi mới
        KhuyenMai khuyenMai = KhuyenMai.builder()
                .cuaHang(cuaHang.get())
                .maGiamGia(model.getMaGiamGia())
                .discount(model.getDiscount())
                .ngayBatDau(model.getNgayBatDau())
                .ngayKetThuc(model.getNgayKetThuc())
                .soLuongMaGiamGia(model.getSoLuongMaGiamGia())
                .soLuongDaSuDung(model.getSoLuongDaSuDung() != null ? model.getSoLuongDaSuDung() : 0)
                .trangThai(model.getTrangThai() != null ? model.getTrangThai() : true)
                .build();
        
        return khuyenMaiRepository.save(khuyenMai);
    }

    @Override
    @Transactional
    public KhuyenMai updateKhuyenMai(Integer id, KhuyenMaiModel model) {
        KhuyenMai khuyenMai = khuyenMaiRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khuyến mãi không tồn tại"));
        
        // Tìm cửa hàng
        Optional<CuaHang> cuaHang = cuaHangRepository.findById(model.getMaCuaHang());
        if (!cuaHang.isPresent()) {
            throw new RuntimeException("Cửa hàng không tồn tại");
        }
        
        // Kiểm tra mã giảm giá đã tồn tại (trừ khuyến mãi hiện tại)
        if (khuyenMaiRepository.existsByCuaHangAndMaGiamGiaAndMaKhuyenMaiNot(
                cuaHang.get(), model.getMaGiamGia(), id)) {
            throw new RuntimeException("Mã giảm giá '" + model.getMaGiamGia() + "' đã tồn tại trong cửa hàng");
        }
        
        // VALIDATE DATES CƠ BẢN - KHÔNG KIỂM TRA NGÀY BẮT ĐẦU PHẢI LÀ TƯƠNG LAI
        if (model.getNgayBatDau() == null) {
            throw new RuntimeException("Ngày bắt đầu không được để trống");
        }
        
        if (model.getNgayKetThuc() == null) {
            throw new RuntimeException("Ngày kết thúc không được để trống");
        }
        
        if (model.getNgayKetThuc().isBefore(model.getNgayBatDau())) {
            throw new RuntimeException("Ngày kết thúc phải sau ngày bắt đầu");
        }
        
        // Cập nhật thông tin
        khuyenMai.setCuaHang(cuaHang.get());
        khuyenMai.setMaGiamGia(model.getMaGiamGia());
        khuyenMai.setDiscount(model.getDiscount());
        khuyenMai.setNgayBatDau(model.getNgayBatDau());
        khuyenMai.setNgayKetThuc(model.getNgayKetThuc());
        khuyenMai.setSoLuongMaGiamGia(model.getSoLuongMaGiamGia());
        
        if (model.getSoLuongDaSuDung() != null) {
            khuyenMai.setSoLuongDaSuDung(model.getSoLuongDaSuDung());
        }
         
        if (model.getTrangThai() != null) {
            khuyenMai.setTrangThai(model.getTrangThai());
        }
        
        return khuyenMaiRepository.save(khuyenMai);
    }

    @Override
    @Transactional
    public void deleteKhuyenMai(Integer id) {
        KhuyenMai khuyenMai = khuyenMaiRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khuyến mãi không tồn tại"));
        khuyenMaiRepository.delete(khuyenMai);
    }

    @Override
    @Transactional
    public KhuyenMai changeStatus(Integer id, Boolean status) {
        try {
            KhuyenMai khuyenMai = khuyenMaiRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Khuyến mãi không tồn tại"));
            
            // Đảm bảo các field không null
            if (khuyenMai.getSoLuongDaSuDung() == null) {
                khuyenMai.setSoLuongDaSuDung(0);
            }
            
            khuyenMai.setTrangThai(status);
            return khuyenMaiRepository.save(khuyenMai);
            
        } catch (Exception e) {
            // Transaction sẽ tự động rollback khi có exception
            throw new RuntimeException("Lỗi khi thay đổi trạng thái: " + e.getMessage());
        }
    }

    @Override
    public boolean isMaGiamGiaExists(CuaHang cuaHang, String maGiamGia) {
        return khuyenMaiRepository.existsByCuaHangAndMaGiamGia(cuaHang, maGiamGia);
    }

    @Override
    public boolean isMaGiamGiaExistsForUpdate(CuaHang cuaHang, String maGiamGia, Integer maKhuyenMai) {
        return khuyenMaiRepository.existsByCuaHangAndMaGiamGiaAndMaKhuyenMaiNot(cuaHang, maGiamGia, maKhuyenMai);
    }
    
    @Override
    public Map<String, Long> getPromotionStatusCounts(CuaHang cuaHang) {
        LocalDate currentDate = LocalDate.now();
        
        Map<String, Long> statusCounts = new HashMap<>();
        statusCounts.put("active", khuyenMaiRepository.countActiveByCuaHang(cuaHang, currentDate));
        statusCounts.put("upcoming", khuyenMaiRepository.countUpcomingByCuaHang(cuaHang, currentDate));
        statusCounts.put("expired", khuyenMaiRepository.countExpiredByCuaHang(cuaHang, currentDate));
        statusCounts.put("inactive", khuyenMaiRepository.countInactiveByCuaHang(cuaHang));
        
        return statusCounts;
    }
}