package vn.iotstar.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import vn.iotstar.entity.DanhMuc;
import vn.iotstar.entity.SanPham;
import vn.iotstar.repository.DanhMucRepository;
import vn.iotstar.service.DanhMucService;
import vn.iotstar.service.SanPhamService;

import java.util.List;

@Service
public class DanhMucServiceImpl implements DanhMucService {

    @Autowired
    private DanhMucRepository danhMucRepository;
    
    @Autowired
    @Lazy
    private SanPhamService sanPhamService;

    @Override
    public List<DanhMuc> findAll() {
        return danhMucRepository.findAll();
    }

    @Override
    public Page<DanhMuc> findAll(Pageable pageable) {
        return danhMucRepository.findAll(pageable);
    }

    @Override
    public Page<DanhMuc> searchCategories(String keyword, Pageable pageable) {
        return danhMucRepository.findByTenDanhMucContainingIgnoreCase(keyword, pageable);
    }

    @Override
    public DanhMuc findByMaDanhMuc(Integer maDanhMuc) {
        return danhMucRepository.findById(maDanhMuc).orElse(null);
    }

    @Override
    public DanhMuc findByTenDanhMuc(String tenDanhMuc) {
        return danhMucRepository.findByTenDanhMuc(tenDanhMuc);
    }

    @Override
    public DanhMuc createCategory(DanhMuc danhMuc) {
        // Check if category name already exists
        DanhMuc existingCategory = danhMucRepository.findByTenDanhMuc(danhMuc.getTenDanhMuc());
        if (existingCategory != null) {
            throw new RuntimeException("Tên danh mục đã tồn tại");
        }
        
        if (danhMuc.getTrangThai() == null) {
            danhMuc.setTrangThai(true);
        }
        
        return danhMucRepository.save(danhMuc);
    }

    @Override
    @Transactional
    public DanhMuc updateCategory(Integer id, DanhMuc danhMuc) {
        DanhMuc existingCategory = findByMaDanhMuc(id);
        if (existingCategory == null) {
            throw new RuntimeException("Danh mục không tồn tại");
        }
        
        // Check if category name already exists (excluding current category)
        DanhMuc categoryWithSameName = danhMucRepository.findByTenDanhMuc(danhMuc.getTenDanhMuc());
        if (categoryWithSameName != null && !categoryWithSameName.getMaDanhMuc().equals(id)) {
            throw new RuntimeException("Tên danh mục đã tồn tại");
        }
        
        // Lưu trạng thái cũ để so sánh
        Boolean oldStatus = existingCategory.getTrangThai();
        Boolean newStatus = danhMuc.getTrangThai();
        
        // Cập nhật thông tin danh mục
        existingCategory.setTenDanhMuc(danhMuc.getTenDanhMuc());
        existingCategory.setMoTa(danhMuc.getMoTa());
        existingCategory.setHinhAnh(danhMuc.getHinhAnh());
        existingCategory.setTrangThai(newStatus);
        
        DanhMuc updatedCategory = danhMucRepository.save(existingCategory);
        
        // ========== TỰ ĐỘNG CẬP NHẬT TRẠNG THÁI SẢN PHẨM ==========
        // Nếu trạng thái danh mục thay đổi
        if (oldStatus != null && !oldStatus.equals(newStatus)) {
            updateProductsStatusByCategory(id, newStatus);
        }
        
        return updatedCategory;
    }

    @Override
    @Transactional
    public void deleteCategory(Integer id) {
        DanhMuc danhMuc = findByMaDanhMuc(id);
        if (danhMuc == null) {
            throw new RuntimeException("Danh mục không tồn tại");
        }
        
        // Cập nhật tất cả sản phẩm trong danh mục về trạng thái ngừng bán trước khi xóa
        updateProductsStatusByCategory(id, false);
        
        danhMucRepository.delete(danhMuc);
    }

    /**
     * Cập nhật trạng thái của tất cả sản phẩm trong danh mục
     */
    private void updateProductsStatusByCategory(Integer categoryId, Boolean status) {
        try {
            DanhMuc danhMuc = findByMaDanhMuc(categoryId);
            if (danhMuc != null) {
                List<SanPham> products = sanPhamService.findByDanhMuc(danhMuc);
                int updatedCount = 0;
                
                for (SanPham product : products) {
                    try {
                        // Sử dụng phương thức đặc biệt để bỏ qua validation
                        sanPhamService.updateProductStatusForSystem(product.getMaSanPham(), status);
                        updatedCount++;
                        
                        System.out.println("Đã cập nhật trạng thái sản phẩm " + product.getMaSanPham() + 
                                         " - " + product.getTenSanPham() + 
                                         " thành: " + (status ? "Đang bán" : "Ngừng bán"));
                    } catch (Exception e) {
                        System.err.println("Lỗi khi cập nhật sản phẩm " + product.getMaSanPham() + ": " + e.getMessage());
                    }
                }
                
                System.out.println("ĐÃ HOÀN TẤT: Cập nhật trạng thái cho " + updatedCount + 
                                 "/" + products.size() + " sản phẩm trong danh mục " + danhMuc.getTenDanhMuc());
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi cập nhật trạng thái sản phẩm theo danh mục: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    @Transactional
    public DanhMuc save(DanhMuc danhMuc) {
        // Lấy danh mục hiện tại từ database để so sánh trạng thái
        DanhMuc existingCategory = null;
        if (danhMuc.getMaDanhMuc() != null) {
            existingCategory = danhMucRepository.findById(danhMuc.getMaDanhMuc()).orElse(null);
        }
        
        // Lưu danh mục
        DanhMuc savedCategory = danhMucRepository.save(danhMuc);
        
        // TỰ ĐỘNG CẬP NHẬT SẢN PHẨM KHI TRẠNG THÁI THAY ĐỔI
        if (existingCategory != null && !existingCategory.getTrangThai().equals(danhMuc.getTrangThai())) {
            updateProductsStatusByCategory(danhMuc.getMaDanhMuc(), danhMuc.getTrangThai());
        }
        
        return savedCategory;
    }

    @Override
    public List<DanhMuc> findAllActiveCategories() {
        return danhMucRepository.findAllActiveCategories();
    }

    @Override
    public long countActiveCategories() {
        return danhMucRepository.countActiveCategories();
    }
}