package vn.iotstar.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import vn.iotstar.entity.DanhGia;
import vn.iotstar.entity.SanPham;
import vn.iotstar.repository.DanhGiaRepository;
import vn.iotstar.service.DanhGiaService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DanhGiaServiceImpl implements DanhGiaService {

    @Autowired
    private DanhGiaRepository danhGiaRepository;

    @Override
    public List<DanhGia> findBySanPhamOrderByNgayDanhGiaDesc(SanPham sanPham) {
        return danhGiaRepository.findBySanPhamOrderByNgayDanhGiaDesc(sanPham);
    }

    @Override
    public List<DanhGia> findTop5BySanPhamOrderByNgayDanhGiaDesc(SanPham sanPham) {
        return danhGiaRepository.findTop5BySanPhamOrderByNgayDanhGiaDesc(sanPham);
    }

    @Override
    public Double getAverageRatingBySanPham(SanPham sanPham) {
        return danhGiaRepository.findAverageRatingBySanPham(sanPham);
    }

    @Override
    public Long getCountBySanPham(SanPham sanPham) {
        return danhGiaRepository.countBySanPham(sanPham);
    }

    @Override
    public List<Object[]> getRatingDistributionBySanPham(SanPham sanPham) {
        return danhGiaRepository.getRatingDistribution(sanPham);
    }

    @Override
    public DanhGia save(DanhGia danhGia) {
        return danhGiaRepository.save(danhGia);
    }
    
    @Override
    public Page<DanhGia> findBySanPham(SanPham sanPham, Pageable pageable) {
        return danhGiaRepository.findBySanPham(sanPham, pageable);
    }
    
    @Override
    public List<DanhGia> findDanhGiasWithUserBySanPham(Integer maSanPham, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "ngayDanhGia"));
        return danhGiaRepository.findBySanPhamMaSanPhamWithUser(maSanPham, pageable);
    }
    
    // THÊM METHOD ĐẾM
    @Override
    public Long countDanhGiasBySanPham(Integer maSanPham) {
        return danhGiaRepository.countDanhGiasBySanPham(maSanPham);
    }
    
    @Override
    public Optional<DanhGia> findById(Integer id) {
        return danhGiaRepository.findById(id);
    }
    
    @Override
    public void delete(Integer id) {
        danhGiaRepository.deleteById(id);
    }
    
    @Override
    public List<DanhGia> findAll() {
        return danhGiaRepository.findAll();
    }

    @Override
    public Page<DanhGia> findAll(Pageable pageable) {
        return danhGiaRepository.findAll(pageable);
    }
}