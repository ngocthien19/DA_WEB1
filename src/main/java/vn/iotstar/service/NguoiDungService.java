package vn.iotstar.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.model.NguoiDungModel;

import java.util.List;
import java.util.Optional;

public interface NguoiDungService {
    
    // Lấy tất cả người dùng với phân trang
    Page<NguoiDung> getAllUsers(Pageable pageable);
    
    // Tìm kiếm người dùng với phân trang
    Page<NguoiDung> searchUsers(String keyword, Pageable pageable);
    
    // Tìm kiếm và lọc theo vai trò với phân trang
    Page<NguoiDung> searchAndFilterUsers(String keyword, String maVaiTro, Pageable pageable);
    
    // Lấy người dùng theo ID
    Optional<NguoiDung> getUserById(Integer id);
    
    // Lấy người dùng theo email
    Optional<NguoiDung> getUserByEmail(String email);
    
    // Tạo mới người dùng
    NguoiDung createUser(NguoiDungModel model);
    
    // Cập nhật người dùng
    NguoiDung updateUser(Integer id, NguoiDungModel model);
    
    // Xóa người dùng
    void deleteUser(Integer id);
    
    // Kiểm tra email tồn tại
    boolean existsByEmail(String email);
    
    // Lấy top người dùng mới nhất
    List<NguoiDung> getRecentUsers(int limit);
    
    // Đếm tổng số người dùng
    long countTotalUsers();
    
    // Lấy người dùng theo vai trò
    List<NguoiDung> getUsersByRole(String maVaiTro);
    
    // Thay đổi trạng thái người dùng
    NguoiDung changeUserStatus(Integer id, String status);
    List<NguoiDung> findByVaiTro(String maVaiTro);
    
    // Tìm người dùng theo mã
    NguoiDung findByMaNguoiDung(Integer maNguoiDung);
    
    // Đếm số lượng người dùng theo vai trò
    long countUsersByRole(String maVaiTro);
    
    // Đếm tổng số người dùng
    long countUsers();
    
    // Đếm số người dùng đang hoạt động
    long countAllActiveUsers();
    
    // Đếm số người dùng không hoạt động
    long countAllInactiveUsers();
    
 // Khóa/mở khóa người dùng với lý do
    NguoiDung lockUser(Integer id, String lyDoKhoa);
    NguoiDung unlockUser(Integer id);
    
    // Kiểm tra người dùng có bị khóa không
    boolean isUserLocked(Integer id);
    boolean isUserLockedByEmail(String email);
}