package vn.iotstar.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.entity.SanPham;
import vn.iotstar.model.SanPhamModel;
import vn.iotstar.entity.CuaHang;
import vn.iotstar.entity.DanhMuc;
import vn.iotstar.repository.SanPhamRepository;
import vn.iotstar.service.CuaHangService;
import vn.iotstar.service.DanhMucService;
import vn.iotstar.service.SanPhamService;

@Service
public class SanPhamServiceImpl implements SanPhamService {

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private CuaHangService cuaHangService;

    @Autowired
    private DanhMucService danhMucService;

    @Override
    public List<SanPham> findTop4ByDanhMucOrderByNgayNhapDesc(DanhMuc danhMuc) {
        Pageable pageable = PageRequest.of(0, 4);  
        return sanPhamRepository.findTop4ByDanhMucOrderByNgayNhapDesc(danhMuc, pageable);
    }

    @Override
    public SanPham findByMaSanPham(Integer maSanPham) {
        return sanPhamRepository.findByMaSanPham(maSanPham); 
    }

    @Override
    public List<SanPham> findRelatedProductsByCategoryExcludingCurrent(DanhMuc danhMuc, Integer maSanPham) {
        return sanPhamRepository.findByDanhMucAndNotMaSanPham(danhMuc, maSanPham);
    }
    
    // THÊM METHOD MỚI: Lấy sản phẩm cùng LoaiSanPham
    @Override
    public List<SanPham> findRelatedProductsByLoaiSanPhamExcludingCurrent(String loaiSanPham, Integer maSanPham) {
        return sanPhamRepository.findByLoaiSanPhamAndNotMaSanPham(loaiSanPham, maSanPham);
    }
    
    @Override
    public List<SanPham> findByDanhMuc(DanhMuc danhMuc) {
        return sanPhamRepository.findByDanhMuc(danhMuc);
    }

    @Override
    public Page<SanPham> findByDanhMuc(DanhMuc danhMuc, Pageable pageable) {
        return sanPhamRepository.findByDanhMuc(danhMuc, pageable);
    }

    @Override
    public Page<SanPham> findAll(Specification<SanPham> spec, Pageable pageable) {
        return sanPhamRepository.findAll(spec, pageable);
    }
    
    @Override
    public List<SanPham> findAll() {
        return sanPhamRepository.findAll();
    }
    
    @Override
    public Page<SanPham> findByCuaHang(CuaHang cuaHang, Pageable pageable) {
        return sanPhamRepository.findByCuaHang(cuaHang, pageable);
    }

    @Override
    public List<SanPham> findByCuaHang(CuaHang cuaHang) {
        return sanPhamRepository.findByCuaHang(cuaHang);
    }

    @Override
    public Page<SanPham> findAll(Pageable pageable) {
        return sanPhamRepository.findAll(pageable);
    }

    @Override
    public Page<SanPham> searchProducts(String keyword, Pageable pageable) {
        return sanPhamRepository.findByTenSanPhamContainingIgnoreCaseOrMoTaSanPhamContainingIgnoreCase(
            keyword, keyword, pageable);
    }

    @Override
    @Transactional
    public SanPham createProduct(SanPhamModel model) {
        try {
            // Tìm cửa hàng và danh mục
            CuaHang cuaHang = cuaHangService.findByMaCuaHang(model.getMaCuaHang());
            DanhMuc danhMuc = danhMucService.findByMaDanhMuc(model.getMaDanhMuc());
            
            if (cuaHang == null) {
                throw new RuntimeException("Cửa hàng không tồn tại");
            }
            if (danhMuc == null) {
                throw new RuntimeException("Danh mục không tồn tại");
            }
            
            // Tạo sản phẩm mới
            SanPham sanPham = SanPham.builder()
                    .tenSanPham(model.getTenSanPham())
                    .cuaHang(cuaHang)
                    .danhMuc(danhMuc)
                    .moTaSanPham(model.getMoTaSanPham())
                    .giaBan(model.getGiaBan())
                    .soLuongConLai(model.getSoLuongConLai() != null ? model.getSoLuongConLai() : 0)
                    .soLuongDaBan(model.getSoLuongDaBan() != null ? model.getSoLuongDaBan() : 0)
                    .luotThich(model.getLuotThich() != null ? model.getLuotThich() : BigDecimal.ZERO)
                    .loaiSanPham(model.getLoaiSanPham())
                    .trangThai(model.getTrangThai() != null ? model.getTrangThai() : true)
                    .hinhAnh(model.getHinhAnh())
                    .ngayNhap(model.getNgayNhap() != null ? model.getNgayNhap() : new Date())
                    .saoDanhGia(0)
                    .build();
            
            return sanPhamRepository.save(sanPham);
            
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo sản phẩm: " + e.getMessage(), e);
        }
    }
    
 // Trong SanPhamServiceImpl.java
    @Override
    public boolean canUpdateProduct(Integer productId) {
        try {
            SanPham sanPham = sanPhamRepository.findByMaSanPham(productId);
            if (sanPham == null) {
                return false;
            }
            
            CuaHang cuaHang = sanPham.getCuaHang();
            return cuaHang != null && cuaHang.getTrangThai();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional
    public SanPham updateProduct(Integer id, SanPhamModel model) {
        try {
            SanPham sanPham = sanPhamRepository.findByMaSanPham(id);
            if (sanPham == null) {
                throw new RuntimeException("Sản phẩm không tồn tại");
            }
            
            // ========== KIỂM TRA TRẠNG THÁI CỬA HÀNG ==========
            CuaHang cuaHang = sanPham.getCuaHang();
            if (cuaHang != null && !cuaHang.getTrangThai()) {
                throw new RuntimeException("Không thể cập nhật sản phẩm vì cửa hàng đang bị khóa");
            }
            
            // Tìm cửa hàng và danh mục
            CuaHang newCuaHang = cuaHangService.findByMaCuaHang(model.getMaCuaHang());
            DanhMuc danhMuc = danhMucService.findByMaDanhMuc(model.getMaDanhMuc());
            
            if (newCuaHang == null) {
                throw new RuntimeException("Cửa hàng không tồn tại");
            }
            if (danhMuc == null) {
                throw new RuntimeException("Danh mục không tồn tại");
            }
            
            // ========== KIỂM TRA TRẠNG THÁI CỬA HÀNG MỚI ==========
            if (!newCuaHang.getTrangThai()) {
                throw new RuntimeException("Không thể chuyển sản phẩm sang cửa hàng đang bị khóa");
            }
            
            // Cập nhật thông tin
            sanPham.setTenSanPham(model.getTenSanPham());
            sanPham.setCuaHang(newCuaHang);
            sanPham.setDanhMuc(danhMuc);
            sanPham.setMoTaSanPham(model.getMoTaSanPham());
            sanPham.setGiaBan(model.getGiaBan());
            sanPham.setSoLuongConLai(model.getSoLuongConLai());
            sanPham.setLoaiSanPham(model.getLoaiSanPham());
            sanPham.setTrangThai(model.getTrangThai());
            
            // Cập nhật hình ảnh nếu có
            if (model.getHinhAnh() != null && !model.getHinhAnh().trim().isEmpty()) {
                sanPham.setHinhAnh(model.getHinhAnh());
            }
            
            return sanPhamRepository.save(sanPham);
            
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi cập nhật sản phẩm: " + e.getMessage(), e);
        }
    }

    @Override
    public SanPham save(SanPham sanPham) {
        // ========== KIỂM TRA TRẠNG THÁI CỬA HÀNG KHI LƯU ==========
        if (sanPham.getCuaHang() != null && !sanPham.getCuaHang().getTrangThai()) {
            throw new RuntimeException("Không thể cập nhật sản phẩm vì cửa hàng đang bị khóa");
        }
        return sanPhamRepository.save(sanPham);
    }

    @Override
    public void deleteProduct(Integer id) {
        SanPham sanPham = sanPhamRepository.findByMaSanPham(id);
        if (sanPham == null) {
            throw new RuntimeException("Sản phẩm không tồn tại");
        }
        sanPhamRepository.delete(sanPham);
    }

    @Override
    public List<String> findDistinctLoaiSanPhamByDanhMuc(DanhMuc danhMuc) {
        return sanPhamRepository.findDistinctLoaiSanPhamByDanhMuc(danhMuc);
    }
    @Override
    public List<SanPham> findAllById(List<Integer> ids) {
        return sanPhamRepository.findAllById(ids);
    }
    
    @Override
    public List<SanPham> findTop4ByDanhMucAndTrangThaiTrueOrderByNgayNhapDesc(DanhMuc danhMuc) {
        Pageable pageable = PageRequest.of(0, 4);  
        return sanPhamRepository.findTop4ByDanhMucAndTrangThaiTrueOrderByNgayNhapDesc(danhMuc, pageable);
    }
    
    @Override
    public List<SanPham> findByCuaHangAndTrangThai(CuaHang cuaHang, Boolean trangThai) {
        return sanPhamRepository.findByCuaHangAndTrangThai(cuaHang, trangThai);
    }
    
    @Override
    public Page<SanPham> searchActiveProducts(String keyword, Pageable pageable) {
        return sanPhamRepository.searchActiveProducts(keyword, pageable);
    }	
    
    @Override
    @Transactional
    public SanPham updateProductStatusForSystem(Integer productId, Boolean status) {
        try {
            SanPham sanPham = sanPhamRepository.findByMaSanPham(productId);
            if (sanPham == null) {
                throw new RuntimeException("Sản phẩm không tồn tại");
            }
            
            // BỎ QUA VALIDATION cho trường hợp hệ thống tự động cập nhật
            sanPham.setTrangThai(status);
            return sanPhamRepository.save(sanPham);
            
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi cập nhật trạng thái sản phẩm từ hệ thống: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean canChangeProductStatus(Integer productId, Boolean newStatus) {
        try {
            SanPham sanPham = sanPhamRepository.findByMaSanPham(productId);
            if (sanPham == null) {
                return false;
            }
            
            // Nếu muốn chuyển sang trạng thái "Đang bán" (true)
            // thì phải kiểm tra danh mục có đang hoạt động không
            if (newStatus && sanPham.getDanhMuc() != null) {
                return sanPham.getDanhMuc().getTrangThai();
            }
            
            // Cho phép chuyển sang "Ngừng bán" (false) bất kỳ lúc nào
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public Page<SanPham> findByCuaHangAndTenSanPhamContainingIgnoreCaseOrMoTaSanPhamContainingIgnoreCase(
        CuaHang cuaHang, String tenSanPham, String moTaSanPham, Pageable pageable) {
        return sanPhamRepository.findByCuaHangAndTenSanPhamContainingIgnoreCaseOrMoTaSanPhamContainingIgnoreCase(
            cuaHang, tenSanPham, moTaSanPham, pageable);
    }
}