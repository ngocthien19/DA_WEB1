package vn.iotstar.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.iotstar.entity.VaiTro;
import vn.iotstar.repository.VaiTroRepository;
import vn.iotstar.service.VaiTroService;

import java.util.List;
import java.util.Optional;

@Service
public class VaiTroServiceImpl implements VaiTroService {

    @Autowired
    private VaiTroRepository vaiTroRepository;

    @Override
    public List<VaiTro> getAllVaiTro() {
        return vaiTroRepository.findAll();
    }

    @Override
    public Optional<VaiTro> getVaiTroById(String maVaiTro) {
        return vaiTroRepository.findById(maVaiTro);
    }

    @Override
    public Optional<VaiTro> getVaiTroByTen(String tenVaiTro) {
        return vaiTroRepository.findByTenVaiTro(tenVaiTro);
    }
}