package vn.iotstar.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.entity.GioHang;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.repository.GioHangRepository;
import vn.iotstar.service.GioHangService;

import java.time.LocalDate;
import java.util.Optional;

@Service 
@Transactional
public class GioHangServiceImpl implements GioHangService {

    @Autowired
    private GioHangRepository gioHangRepository;

    @Override
    public GioHang findByNguoiDung(NguoiDung nguoiDung) {
        Optional<GioHang> gioHang = gioHangRepository.findByNguoiDung(nguoiDung);
        return gioHang.orElse(null);
    }

    @Override
    public GioHang createGioHang(NguoiDung nguoiDung) {
        GioHang gioHang = GioHang.builder()
                .nguoiDung(nguoiDung)
                .ngayTao(LocalDate.now())
                .build();
        return gioHangRepository.save(gioHang);
    }

    @Override
    public GioHang save(GioHang gioHang) {
        return gioHangRepository.save(gioHang);
    }

    @Override
    public Optional<GioHang> findById(Integer maGioHang) {
        return gioHangRepository.findById(maGioHang);
    }

    @Override
    public void delete(GioHang gioHang) {
        gioHangRepository.delete(gioHang);
    }

    @Override
    public GioHang getOrCreateGioHang(NguoiDung nguoiDung) {
        GioHang gioHang = findByNguoiDung(nguoiDung);
        if (gioHang == null) {
            gioHang = createGioHang(nguoiDung);
        }
        return gioHang;
    }
}