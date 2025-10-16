package vn.iotstar.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.iotstar.entity.CuaHang;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.entity.SanPham;
import vn.iotstar.model.CuaHangModel;
import vn.iotstar.repository.CuaHangRepository;
import vn.iotstar.repository.NguoiDungRepository;
import vn.iotstar.service.CuaHangService;
import vn.iotstar.service.SanPhamService;

@Service
@Transactional
public class CuaHangServiceImpl implements CuaHangService {

    @Autowired
    private CuaHangRepository cuaHangRepository;
    
    @Autowired
    private NguoiDungRepository nguoiDungRepository;
    
    @Autowired 
    @Lazy
    private SanPhamService sanPhamService;

    @Override
    public List<CuaHang> findAll() {
        return cuaHangRepository.findAll();
    }

    @Override
    public CuaHang findByMaCuaHang(Integer maCuaHang) {
        return cuaHangRepository.findByMaCuaHang(maCuaHang);
    }

    @Override
    public CuaHang findByTenCuaHang(String tenCuaHang) {
        return cuaHangRepository.findByTenCuaHang(tenCuaHang);
    }
    
    @Override
    public List<CuaHang> findTop3NewestStores() {
        return cuaHangRepository.findTop3ByOrderByNgayTaoDesc();
    }
    
    @Override
    public Page<CuaHang> findAll(Pageable pageable) {
        return cuaHangRepository.findAll(pageable);
    }
    
    @Override
    public Page<CuaHang> searchStores(String keyword, Pageable pageable) {
        return cuaHangRepository.findByTenCuaHangContainingIgnoreCaseOrDiaChiContainingIgnoreCase(
            keyword, keyword, pageable);
    }
    
    @Override
    public Page<CuaHang> findActiveStores(Pageable pageable) {
        return cuaHangRepository.findByTrangThaiTrue(pageable);
    }
    
    @Override
    @Transactional
    public CuaHang createStore(CuaHangModel model) {
        System.out.println("=== CREATE STORE START ===");
        System.out.println("Model info - TenCuaHang: " + model.getTenCuaHang() + ", HinhAnh: " + model.getHinhAnh());
        
        try {
            // Tìm người dùng (chủ cửa hàng)
            Optional<NguoiDung> nguoiDung = nguoiDungRepository.findById(model.getMaNguoiDung());
            if (!nguoiDung.isPresent()) {
                throw new RuntimeException("Chủ cửa hàng không tồn tại");
            }
            
            // ========== VALIDATION MỚI: Kiểm tra xem vendor đã có cửa hàng chưa ==========
            List<CuaHang> existingStores = cuaHangRepository.findByNguoiDung(nguoiDung.get());
            if (existingStores != null && !existingStores.isEmpty()) {
                throw new RuntimeException("Người dùng " + nguoiDung.get().getTenNguoiDung() + " đã có cửa hàng. Mỗi người dùng chỉ được sở hữu 1 cửa hàng.");
            }
            // ========== END VALIDATION ==========
            
            // Tạo cửa hàng mới với giá trị mặc định đúng
            CuaHang cuaHang = CuaHang.builder()
                    .tenCuaHang(model.getTenCuaHang())
                    .nguoiDung(nguoiDung.get())
                    .moTa(model.getMoTa())
                    .diaChi(model.getDiaChi())
                    .soDienThoai(model.getSoDienThoai())
                    .email(model.getEmail())
                    .namThanhLap(model.getNamThanhLap())
                    .trangThai(model.getTrangThai() != null ? model.getTrangThai() : true)
                    .hinhAnh(model.getHinhAnh())
                    .danhGiaTrungBinh(0.0)  // Đặt giá trị mặc định 0.0 thay vì null
                    .soLuongDanhGia(0)      // Đặt giá trị mặc định 0 thay vì null
                    .ngayTao(new Date())    // Đảm bảo có ngày tạo
                    .build();
            
            // Validate trước khi save
            System.out.println("Validating store: DanhGia=" + cuaHang.getDanhGiaTrungBinh() + ", SoLuong=" + cuaHang.getSoLuongDanhGia());
            
            CuaHang savedStore = cuaHangRepository.save(cuaHang);
            System.out.println("Store created - ID: " + savedStore.getMaCuaHang() + ", HinhAnh: " + savedStore.getHinhAnh());
            System.out.println("=== CREATE STORE SUCCESS ===");
            
            return savedStore;
            
        } catch (Exception e) {
            System.err.println("=== CREATE STORE ERROR ===");
            System.err.println("Error creating store: " + e.getMessage());
            e.printStackTrace();
            System.err.println("=== END ERROR ===");
            throw new RuntimeException("Lỗi khi tạo cửa hàng: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public CuaHang updateStore(Integer id, CuaHangModel model) {
        System.out.println("=== UPDATE STORE START ===");
        System.out.println("Updating store ID: " + id + ", Model HinhAnh: " + model.getHinhAnh());
        
        try {
            CuaHang cuaHang = cuaHangRepository.findByMaCuaHang(id);
            if (cuaHang == null) { 
                throw new RuntimeException("Cửa hàng không tồn tại");
            }
            System.out.println("Current store HinhAnh: " + cuaHang.getHinhAnh());
            System.out.println("Current ratings: DanhGia=" + cuaHang.getDanhGiaTrungBinh() + ", SoLuong=" + cuaHang.getSoLuongDanhGia());
            
            // Lưu trạng thái cũ để so sánh
            Boolean oldStatus = cuaHang.getTrangThai();
            
            // Tìm người dùng (chủ cửa hàng)
            Optional<NguoiDung> nguoiDung = nguoiDungRepository.findById(model.getMaNguoiDung());
            if (!nguoiDung.isPresent()) {
                throw new RuntimeException("Chủ cửa hàng không tồn tại");
            }
            
            // ========== VALIDATION MỚI: Kiểm tra xem vendor mới đã có cửa hàng chưa (nếu thay đổi chủ cửa hàng) ==========
            if (!cuaHang.getNguoiDung().getMaNguoiDung().equals(model.getMaNguoiDung())) {
                List<CuaHang> existingStores = cuaHangRepository.findByNguoiDung(nguoiDung.get());
                if (existingStores != null && !existingStores.isEmpty()) {
                    // Kiểm tra xem cửa hàng tìm thấy có phải là cửa hàng hiện tại không
                    boolean isSameStore = existingStores.stream()
                            .anyMatch(store -> store.getMaCuaHang().equals(id));
                    
                    if (!isSameStore) {
                        throw new RuntimeException("Người dùng " + nguoiDung.get().getTenNguoiDung() + " đã có cửa hàng khác. Mỗi người dùng chỉ được sở hữu 1 cửa hàng.");
                    }
                }
            }
            // ========== END VALIDATION ==========
            
            // Cập nhật thông tin - XỬ LÝ NULL AN TOÀN
            cuaHang.setTenCuaHang(model.getTenCuaHang());
            cuaHang.setNguoiDung(nguoiDung.get());
            cuaHang.setMoTa(model.getMoTa());
            cuaHang.setDiaChi(model.getDiaChi());
            cuaHang.setSoDienThoai(model.getSoDienThoai());
            cuaHang.setEmail(model.getEmail());
            cuaHang.setNamThanhLap(model.getNamThanhLap());
            
         // Cập nhật lý do khóa (chỉ khi chuyển sang trạng thái khóa)
            if (model.getTrangThai() != null && !model.getTrangThai() && model.getLyDoKhoa() != null) {
                cuaHang.setLyDoKhoa(model.getLyDoKhoa());
            } else if (model.getTrangThai() != null && model.getTrangThai()) {
                // Khi mở khóa, xóa lý do
                cuaHang.setLyDoKhoa(null);
            }
            
            // XỬ LÝ TRẠNG THÁI NULL
            if (model.getTrangThai() != null) {
                cuaHang.setTrangThai(model.getTrangThai());
            } else {
                cuaHang.setTrangThai(true); // giá trị mặc định
            }
            
            // Đảm bảo các giá trị đánh giá không null
            if (cuaHang.getDanhGiaTrungBinh() == null) {
                cuaHang.setDanhGiaTrungBinh(0.0);
            }
            if (cuaHang.getSoLuongDanhGia() == null) {
                cuaHang.setSoLuongDanhGia(0);
            }
            
            // Cập nhật hình ảnh nếu có
            if (model.getHinhAnh() != null && !model.getHinhAnh().trim().isEmpty()) {
                cuaHang.setHinhAnh(model.getHinhAnh());
                System.out.println("Updated HinhAnh to: " + model.getHinhAnh());
            }
            
            // Validate trước khi save
            System.out.println("Validating updated store: DanhGia=" + cuaHang.getDanhGiaTrungBinh() + ", SoLuong=" + cuaHang.getSoLuongDanhGia());
            
            CuaHang savedStore = cuaHangRepository.save(cuaHang);
            
            // ========== ĐỒNG BỘ TRẠNG THÁI SẢN PHẨM ==========
            Boolean newStatus = cuaHang.getTrangThai(); // Lấy từ entity đã được xử lý null
            
            // Sử dụng Boolean.TRUE.equals() để so sánh an toàn
            if (Boolean.TRUE.equals(oldStatus) && !Boolean.TRUE.equals(newStatus)) {
                updateProductsStatusByStore(savedStore, false);
            }
            else if (!Boolean.TRUE.equals(oldStatus) && Boolean.TRUE.equals(newStatus)) {
                updateProductsStatusByStore(savedStore, true);
            }
            
            System.out.println("Store updated - ID: " + savedStore.getMaCuaHang() + ", Final HinhAnh: " + savedStore.getHinhAnh());
            System.out.println("=== UPDATE STORE SUCCESS ===");
            
            return savedStore;
            
        } catch (Exception e) {
            System.err.println("=== UPDATE STORE ERROR ===");
            System.err.println("Error updating store: " + e.getMessage());
            e.printStackTrace();
            System.err.println("=== END ERROR ===");
            throw new RuntimeException("Lỗi khi cập nhật cửa hàng: " + e.getMessage(), e);
        }
    }

    /**
     * Cập nhật trạng thái tất cả sản phẩm thuộc cửa hàng
     */
    private void updateProductsStatusByStore(CuaHang cuaHang, Boolean newStatus) {
        try {
            List<SanPham> products = sanPhamService.findByCuaHang(cuaHang);
            int updatedCount = 0;
            
            for (SanPham product : products) {
                // Chỉ cập nhật nếu trạng thái khác
                if (!product.getTrangThai().equals(newStatus)) {
                    // SỬA: Sử dụng phương thức đặc biệt cho hệ thống
                    sanPhamService.updateProductStatusForSystem(product.getMaSanPham(), newStatus);
                    updatedCount++;
                }
            }
            
            System.out.println("Đã cập nhật trạng thái cho " + updatedCount + " sản phẩm của cửa hàng " + cuaHang.getTenCuaHang());
            
        } catch (Exception e) {
            System.err.println("Lỗi khi cập nhật trạng thái sản phẩm: " + e.getMessage());
            throw new RuntimeException("Lỗi khi đồng bộ trạng thái sản phẩm: " + e.getMessage());
        }
    }
    
    @Override
    public void deleteStore(Integer id) {
        CuaHang cuaHang = cuaHangRepository.findByMaCuaHang(id);
        if (cuaHang == null) {
            throw new RuntimeException("Cửa hàng không tồn tại");
        }
        cuaHangRepository.delete(cuaHang);
    }
    
    @Override
    public CuaHang save(CuaHang cuaHang) {
        // Lưu trạng thái cũ trước khi save
        CuaHang existingStore = null;
        if (cuaHang.getMaCuaHang() != null) {
            existingStore = cuaHangRepository.findByMaCuaHang(cuaHang.getMaCuaHang());
        }
        
        // Save cửa hàng
        CuaHang savedStore = cuaHangRepository.save(cuaHang);
        
        // Đồng bộ trạng thái sản phẩm nếu trạng thái thay đổi
        if (existingStore != null && !existingStore.getTrangThai().equals(savedStore.getTrangThai())) {
            updateProductsStatusByStore(savedStore, savedStore.getTrangThai());
        }
        
        return savedStore;
    }
    
    @Override
    public List<CuaHang> findActiveStores() {
        return cuaHangRepository.findByTrangThaiTrue();
    }
    
    @Override
    public List<CuaHang> findTop3ActiveNewestStores() {
        return cuaHangRepository.findTop3ByTrangThaiTrueOrderByNgayTaoDesc();
    }
    
    @Override
    public Page<CuaHang> searchActiveStores(String keyword, Pageable pageable) {
        return cuaHangRepository.findByTenCuaHangContainingIgnoreCaseOrDiaChiContainingIgnoreCaseAndTrangThaiTrue(
            keyword, keyword, pageable);
    }
    
    @Override
    public List<CuaHang> findByNguoiDung(NguoiDung nguoiDung) {
        return cuaHangRepository.findByNguoiDung(nguoiDung);
    }
    
    @Override
    public long countTotalStores() {
        return cuaHangRepository.count();
    }

    @Override
    public long countActiveStores() {
        return cuaHangRepository.countByTrangThaiTrue();
    }

    @Override
    public long countInactiveStores() {
        return cuaHangRepository.countByTrangThaiFalse();
    }

    @Override
    public double getAverageRating() {
        List<CuaHang> stores = cuaHangRepository.findAll();
        if (stores.isEmpty()) {
            return 0.0;
        }
        
        double totalRating = 0.0;
        int count = 0;
        
        for (CuaHang store : stores) {
            if (store.getDanhGiaTrungBinh() != null && store.getDanhGiaTrungBinh() > 0) {
                totalRating += store.getDanhGiaTrungBinh();
                count++;
            }
        }
        
        return count > 0 ? Math.round((totalRating / count) * 10.0) / 10.0 : 0.0;
    }
}