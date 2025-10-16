package vn.iotstar.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.entity.VaiTro;
import vn.iotstar.model.NguoiDungModel;
import vn.iotstar.repository.NguoiDungRepository;
import vn.iotstar.repository.VaiTroRepository;
import vn.iotstar.service.NguoiDungService;
import vn.iotstar.specification.NguoiDungSpecification;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class NguoiDungServiceImpl implements NguoiDungService {

    @Autowired
    private NguoiDungRepository nguoiDungRepository;
    
    @Autowired
    private VaiTroRepository vaiTroRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Page<NguoiDung> getAllUsers(Pageable pageable) {
        return nguoiDungRepository.findAll(pageable);
    }

    @Override
    public Page<NguoiDung> searchUsers(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return nguoiDungRepository.findAll(pageable);
        }
        return nguoiDungRepository.searchByTenNguoiDungOrEmail(keyword, pageable);
    }

    @Override
    public Page<NguoiDung> searchAndFilterUsers(String keyword, String maVaiTro, Pageable pageable) {
        Specification<NguoiDung> spec = NguoiDungSpecification.filterUsers(keyword, maVaiTro);
        return nguoiDungRepository.findAll(spec, pageable);
    }

    @Override
    public Optional<NguoiDung> getUserById(Integer id) {
        return nguoiDungRepository.findById(id);
    }

    @Override
    public Optional<NguoiDung> getUserByEmail(String email) {
        return nguoiDungRepository.findByEmail(email);
    }

    @Override
    public NguoiDung createUser(NguoiDungModel model) {
        System.out.println("=== CREATE USER START ===");
        System.out.println("Model info - Email: " + model.getEmail() + ", HinhAnh: " + model.getHinhAnh());
        
        // Kiểm tra email đã tồn tại
        if (nguoiDungRepository.existsByEmail(model.getEmail())) {
            throw new RuntimeException("Email đã tồn tại trong hệ thống");
        }
        
        // Tìm vai trò
        VaiTro vaiTro = vaiTroRepository.findById(model.getMaVaiTro())
                .orElseThrow(() -> new RuntimeException("Vai trò không tồn tại"));
        
        // Tạo người dùng mới
        NguoiDung nguoiDung = NguoiDung.builder()
                .tenNguoiDung(model.getTenNguoiDung())
                .email(model.getEmail())
                .matKhau(passwordEncoder.encode(model.getMatKhau()))
                .sdt(model.getSdt())
                .diaChi(model.getDiaChi())
                .hinhAnh(model.getHinhAnh()) // FIX: Thêm trường hinhAnh
                .vaiTro(vaiTro)
                .trangThai(model.getTrangThai() != null ? model.getTrangThai() : "Hoạt động")
                .build();
        
        NguoiDung savedUser = nguoiDungRepository.save(nguoiDung);
        System.out.println("User created - ID: " + savedUser.getMaNguoiDung() + ", HinhAnh: " + savedUser.getHinhAnh());
        System.out.println("=== CREATE USER SUCCESS ===");
        
        return savedUser;
    }

    @Override
    public NguoiDung updateUser(Integer id, NguoiDungModel model) {
        System.out.println("=== UPDATE USER START ===");
        System.out.println("Updating user ID: " + id + ", Model HinhAnh: " + model.getHinhAnh());
        
        NguoiDung nguoiDung = nguoiDungRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        
        System.out.println("Current user HinhAnh: " + nguoiDung.getHinhAnh());
        
        // Kiểm tra email nếu thay đổi
        if (!nguoiDung.getEmail().equals(model.getEmail()) && 
            nguoiDungRepository.existsByEmail(model.getEmail())) {
            throw new RuntimeException("Email đã tồn tại trong hệ thống");
        }
        
        // Cập nhật thông tin
        nguoiDung.setTenNguoiDung(model.getTenNguoiDung());
        nguoiDung.setEmail(model.getEmail());
        nguoiDung.setSdt(model.getSdt());
        nguoiDung.setDiaChi(model.getDiaChi());
        
        // FIX: Cập nhật hình ảnh nếu có
        if (model.getHinhAnh() != null) {
            nguoiDung.setHinhAnh(model.getHinhAnh());
            System.out.println("Updated HinhAnh to: " + model.getHinhAnh());
        }
        
        // Cập nhật mật khẩu nếu có
        if (model.getMatKhau() != null && !model.getMatKhau().isEmpty()) {
            nguoiDung.setMatKhau(passwordEncoder.encode(model.getMatKhau()));
        }
        
        // Cập nhật vai trò nếu có
        if (model.getMaVaiTro() != null) {
            VaiTro vaiTro = vaiTroRepository.findById(model.getMaVaiTro())
                    .orElseThrow(() -> new RuntimeException("Vai trò không tồn tại"));
            nguoiDung.setVaiTro(vaiTro);
        }
        
        // Cập nhật trạng thái
        if (model.getTrangThai() != null) {
            nguoiDung.setTrangThai(model.getTrangThai());
        }
        
        NguoiDung savedUser = nguoiDungRepository.save(nguoiDung);
        System.out.println("User updated - ID: " + savedUser.getMaNguoiDung() + ", Final HinhAnh: " + savedUser.getHinhAnh());
        System.out.println("=== UPDATE USER SUCCESS ===");
        
        return savedUser;
    }

    @Override
    public void deleteUser(Integer id) {
        NguoiDung nguoiDung = nguoiDungRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        nguoiDungRepository.delete(nguoiDung);
    }

    @Override
    public boolean existsByEmail(String email) {
        return nguoiDungRepository.existsByEmail(email);
    }

    @Override
    public List<NguoiDung> getRecentUsers(int limit) {
        return nguoiDungRepository.findTop5ByOrderByMaNguoiDungDesc();
    }

    @Override
    public long countTotalUsers() {
        return nguoiDungRepository.count();
    }

    @Override
    public List<NguoiDung> getUsersByRole(String maVaiTro) {
        return nguoiDungRepository.findByVaiTro_MaVaiTro(maVaiTro);
    }

    @Override
    public NguoiDung changeUserStatus(Integer id, String status) {
        NguoiDung nguoiDung = nguoiDungRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        nguoiDung.setTrangThai(status);
        return nguoiDungRepository.save(nguoiDung);
    }
    
    @Override
    public List<NguoiDung> findByVaiTro(String maVaiTro) {
        return nguoiDungRepository.findByVaiTro_MaVaiTro(maVaiTro);
    }
    
    @Override
    public long countUsersByRole(String maVaiTro) {
        return nguoiDungRepository.countByVaiTroMaVaiTro(maVaiTro);
    }

    @Override
    public long countUsers() {
        return nguoiDungRepository.countUsers();
    }

    @Override
    public long countAllActiveUsers() {
        return nguoiDungRepository.countAllActiveUsers();
    }

    @Override
    public long countAllInactiveUsers() {
        return nguoiDungRepository.countAllInactiveUsers();
    }
    
    @Override
    public NguoiDung findByMaNguoiDung(Integer maNguoiDung) {
        return nguoiDungRepository.findByMaNguoiDung(maNguoiDung);
    }
    
    @Override
    public NguoiDung lockUser(Integer id, String lyDoKhoa) {
        NguoiDung nguoiDung = nguoiDungRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        
        nguoiDung.setTrangThai("Khóa");
        nguoiDung.setLyDoKhoa(lyDoKhoa);
        
        return nguoiDungRepository.save(nguoiDung);
    }

    @Override
    public NguoiDung unlockUser(Integer id) {
        NguoiDung nguoiDung = nguoiDungRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        
        nguoiDung.setTrangThai("Hoạt động");
        nguoiDung.setLyDoKhoa(null);
        
        return nguoiDungRepository.save(nguoiDung);
    }

    @Override
    public boolean isUserLocked(Integer id) {
        return nguoiDungRepository.findById(id)
                .map(user -> "Khóa".equals(user.getTrangThai()))
                .orElse(false);
    }

    @Override
    public boolean isUserLockedByEmail(String email) {
        return nguoiDungRepository.findByEmail(email)
                .map(user -> "Khóa".equals(user.getTrangThai()))
                .orElse(false);
    }
}