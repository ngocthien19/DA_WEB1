package vn.iotstar.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.entity.CuaHang;
import vn.iotstar.entity.DanhGiaCuaHang;
import vn.iotstar.entity.DatHang;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.repository.CuaHangRepository;
import vn.iotstar.repository.DanhGiaCuaHangRepository;

import java.util.List;
import java.util.Optional;

@Service
public class DanhGiaCuaHangService {
    
    @Autowired
    private DanhGiaCuaHangRepository danhGiaCuaHangRepository;
    
    @Autowired
    private CuaHangRepository cuaHangRepository;
    
    public List<DanhGiaCuaHang> findByCuaHang(CuaHang cuaHang) {
        return danhGiaCuaHangRepository.findByCuaHangOrderByNgayDanhGiaDesc(cuaHang);
    }
    
    public Double getAverageRating(CuaHang cuaHang) {
        return danhGiaCuaHangRepository.findAverageRatingByCuaHang(cuaHang);
    }
    
    public Long countReviews(CuaHang cuaHang) {
        return danhGiaCuaHangRepository.countByCuaHang(cuaHang);
    }
    
    public Optional<DanhGiaCuaHang> findByDatHangAndNguoiDung(DatHang datHang, NguoiDung nguoiDung) {
        return danhGiaCuaHangRepository.findByDatHangAndNguoiDung(datHang, nguoiDung);
    }
    
    public boolean existsByDatHang(DatHang datHang) {
        return danhGiaCuaHangRepository.existsByDatHang(datHang);
    }
    
    @Transactional
    public DanhGiaCuaHang save(DanhGiaCuaHang danhGiaCuaHang) {
        DanhGiaCuaHang savedReview = danhGiaCuaHangRepository.save(danhGiaCuaHang);
        
        // Cập nhật điểm đánh giá trung bình và số lượng đánh giá cho cửa hàng
        CuaHang cuaHang = danhGiaCuaHang.getCuaHang();
        Double avgRating = getAverageRating(cuaHang);
        Long reviewCount = countReviews(cuaHang);
        
        cuaHang.setDanhGiaTrungBinh(avgRating);
        cuaHang.setSoLuongDanhGia(reviewCount.intValue());
        cuaHangRepository.save(cuaHang);
        
        return savedReview;
    }
}
