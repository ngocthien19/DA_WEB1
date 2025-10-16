package vn.iotstar.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.iotstar.entity.ThanhToan;
import vn.iotstar.repository.ThanhToanRepository;
import vn.iotstar.service.ThanhToanService;

import java.util.List;
import java.util.Optional;

@Service
public class ThanhToanServiceImpl implements ThanhToanService {

    @Autowired
    private ThanhToanRepository thanhToanRepository;

    @Override
    public ThanhToan save(ThanhToan thanhToan) {
        return thanhToanRepository.save(thanhToan);
    }

    @Override
    public Optional<ThanhToan> findById(Integer id) {
        return thanhToanRepository.findById(id);
    }

    @Override
    public List<ThanhToan> findByDatHang(Integer maDatHang) {
        System.out.println("Finding payments for order: " + maDatHang);
        List<ThanhToan> payments = thanhToanRepository.findByDatHang_MaDatHang(maDatHang);
        System.out.println("Found " + payments.size() + " payments");
        return payments;
    }

    @Override
    public ThanhToan updateTrangThai(Integer maThanhToan, String trangThai) {
        ThanhToan thanhToan = thanhToanRepository.findById(maThanhToan)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thanh toán"));
        thanhToan.setTrangThai(trangThai);
        return thanhToanRepository.save(thanhToan);
    }
    
    @Override
    public void updateTrangThaiByDatHang(Integer maDatHang, String trangThai) {
        List<ThanhToan> thanhToans = thanhToanRepository.findByDatHang_MaDatHang(maDatHang);
        for (ThanhToan thanhToan : thanhToans) {
            thanhToan.setTrangThai(trangThai);
            thanhToanRepository.save(thanhToan);
        }
    }
}