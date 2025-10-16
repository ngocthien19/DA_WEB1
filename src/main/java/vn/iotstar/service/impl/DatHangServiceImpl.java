package vn.iotstar.service.impl;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import vn.iotstar.entity.*;
import vn.iotstar.model.DatHangRequest;
import vn.iotstar.repository.DatHangRepository;
import vn.iotstar.repository.DatHangChiTietRepository;
import vn.iotstar.service.*;

@Service
@Transactional
public class DatHangServiceImpl implements DatHangService {
    
    @Autowired
    private DatHangRepository datHangRepository;
    
    @Autowired
    private GioHangService gioHangService;
    
    @Autowired
    private MatHangService matHangService;
    
    @Autowired
    private CuaHangService cuaHangService;
    
    @Autowired
    private KhuyenMaiService khuyenMaiService;
    
    @Autowired
    private SanPhamService sanPhamService;
    
    @Autowired
    private DatHangChiTietRepository datHangChiTietRepository;
    
    @Autowired
    private ThanhToanService thanhToanService;
    
    @Autowired
    @Lazy
    private VanChuyenService vanChuyenService;

    @Override
    public List<DatHang> findAll() {
        return datHangRepository.findAll();
    }

    @Override
    public DatHang findByMaDatHang(Integer maDatHang) {
        return datHangRepository.findById(maDatHang).orElse(null);
    }
    
    /**
     * Đồng bộ hóa trạng thái vận chuyển khi đơn hàng bị hủy
     */
    @Override
    public void syncDeliveryStatusWhenCancelled(Integer maDatHang, String lyDoHuy) {
        // Di chuyển logic từ phương thức private lên đây
        try {
            
            // DEBUG: Kiểm tra service có null không
            System.out.println("🔴 VanChuyenService is null: " + (vanChuyenService == null));
            
            if (vanChuyenService == null) {
                System.err.println("❌ LỖI: VanChuyenService là NULL!");
                return;
            }
            
            // Tìm bản ghi vận chuyển của đơn hàng
            VanChuyen vanChuyen = null;
            try {
                vanChuyen = vanChuyenService.getDeliveryByOrderId(maDatHang);
            } catch (Exception e) {
                System.err.println("❌ Lỗi trong getDeliveryByOrderId: " + e.getMessage());
                e.printStackTrace();
                return;
            }
            
            if (vanChuyen != null) {
                
                // Chỉ cập nhật nếu trạng thái hiện tại không phải là "Hủy" hoặc "Hoàn thành"
                if (!"Hủy".equals(vanChuyen.getTrangThai()) && !"Hoàn thành".equals(vanChuyen.getTrangThai())) {
                    
                    try {
                        VanChuyen updated = vanChuyenService.updateDeliveryStatus(vanChuyen.getMaVanChuyen(), "Hủy", lyDoHuy);
                        
                        // DEBUG: Kiểm tra lại từ database
                        try {
                            VanChuyen check = vanChuyenService.getDeliveryById(vanChuyen.getMaVanChuyen());
                            System.out.println("✅ KIỂM TRA DB - Trạng thái VC: " + check.getTrangThai());
                        } catch (Exception checkEx) {
                            System.err.println("❌ Lỗi khi kiểm tra DB: " + checkEx.getMessage());
                        }
                        
                    } catch (Exception e) {
                        System.err.println("❌ Lỗi trong updateDeliveryStatus: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("ℹ️ Vận chuyển đã ở trạng thái cuối: " + vanChuyen.getTrangThai());
                }
            } else {
                System.out.println("❌ Không tìm thấy bản ghi vận chuyển cho đơn hàng: " + maDatHang);
            }
            
            System.out.println("🔴 === KẾT THÚC ĐỒNG BỘ HÓA ===");
        } catch (Exception e) {
            System.err.println("💥 LỖI KHI ĐỒNG BỘ HÓA: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    @Transactional
    public DatHang save(DatHang datHang) {
        try {
            System.out.println("🟢 === BẮT ĐẦU LƯU ĐƠN HÀNG ===");
            System.out.println("🟢 Order ID: " + datHang.getMaDatHang());
            System.out.println("🟢 Trạng thái mới: " + datHang.getTrangThai());
            
            DatHang existingDatHang = null;
            if (datHang.getMaDatHang() != null) {
                existingDatHang = datHangRepository.findById(datHang.getMaDatHang()).orElse(null);
                if (existingDatHang != null) {
                    System.out.println("🟢 Trạng thái cũ: " + existingDatHang.getTrangThai());
                }
            }
            
            DatHang savedDatHang = datHangRepository.save(datHang);
            System.out.println("✅ Đơn hàng đã lưu thành công");
            
            // XỬ LÝ ĐỒNG BỘ HÓA KHI HỦY ĐƠN HÀNG
            if (existingDatHang != null && 
                !"Hủy".equals(existingDatHang.getTrangThai()) && 
                "Hủy".equals(savedDatHang.getTrangThai())) {
                
                System.out.println("🚨 === KÍCH HOẠT ĐỒNG BỘ TỪ SAVE ===");
                // Đồng bộ hóa trạng thái vận chuyển
                syncDeliveryStatusWhenCancelled(savedDatHang.getMaDatHang(), savedDatHang.getLyDoHuy());
            }
            
            System.out.println("🟢 === KẾT THÚC LƯU ĐƠN HÀNG ===");
            return savedDatHang;
        } catch (Exception e) {
            System.err.println("💥 Lỗi khi lưu đơn hàng: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi lưu đơn hàng: " + e.getMessage(), e);
        }
    }
    
    @Transactional(rollbackFor = Exception.class)
    public DatHang updateOrderStatus(Integer maDatHang, String newStatus) {
        try {
            System.out.println("=== UPDATE ORDER STATUS METHOD STARTED ===");
            System.out.println("Order ID: " + maDatHang);
            System.out.println("New Status: " + newStatus);
            
            DatHang datHang = datHangRepository.findById(maDatHang)
                    .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));
            
            String oldStatus = datHang.getTrangThai();
            System.out.println("Old Status: " + oldStatus);
            
            datHang.setTrangThai(newStatus);
            
            DatHang savedDatHang = datHangRepository.save(datHang);
            System.out.println("Order saved with new status");
            
            // XỬ LÝ ĐỒNG BỘ HÓA KHI HỦY ĐƠN HÀNG
            if (!"Hủy".equals(oldStatus) && "Hủy".equals(newStatus)) {
                System.out.println("=== TRIGGERING SYNC FROM UPDATE STATUS METHOD ===");
                syncDeliveryStatusWhenCancelled(maDatHang, datHang.getLyDoHuy());
                
                // THÊM: Cập nhật trạng thái thanh toán khi hủy
                updatePaymentStatusToCancelled(maDatHang);
            }
            
            // Xử lý cập nhật thanh toán nếu cần
            if (!"Hoàn thành".equals(oldStatus) && "Hoàn thành".equals(newStatus)) {
                System.out.println("Triggering payment update for completed order");
                updatePaymentStatusToCompleted(maDatHang);
            }
            
            System.out.println("=== UPDATE ORDER STATUS METHOD COMPLETED ===");
            return savedDatHang;
        } catch (Exception e) {
            System.err.println("Error in updateOrderStatus: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi cập nhật trạng thái đơn hàng: " + e.getMessage(), e);
        }
    }
    
    /**
     * Cập nhật trạng thái tất cả thanh toán của đơn hàng sang "cancelled"
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void updatePaymentStatusToCancelled(Integer maDatHang) {
        try {
            System.out.println("Updating payment status to CANCELLED for order: " + maDatHang);
            List<ThanhToan> thanhToans = thanhToanService.findByDatHang(maDatHang);
            System.out.println("Found " + thanhToans.size() + " payment records");
            
            for (ThanhToan thanhToan : thanhToans) {
                thanhToan.setTrangThai("cancelled");
                thanhToanService.save(thanhToan);
            }
            System.out.println("✓ Đã cập nhật trạng thái thanh toán sang 'cancelled' cho đơn hàng: " + maDatHang);
        } catch (Exception e) {
            System.err.println("✗ Lỗi khi cập nhật trạng thái thanh toán sang cancelled: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Cập nhật trạng thái tất cả thanh toán của đơn hàng sang "completed"
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void updatePaymentStatusToCompleted(Integer maDatHang) {
        try {
            System.out.println("Updating payment status for order: " + maDatHang);
            List<ThanhToan> thanhToans = thanhToanService.findByDatHang(maDatHang);
            System.out.println("Found " + thanhToans.size() + " payment records");
            
            for (ThanhToan thanhToan : thanhToans) {
                thanhToan.setTrangThai("Đã thanh toán");
                thanhToanService.save(thanhToan);
            }
            System.out.println("✓ Đã cập nhật trạng thái thanh toán sang 'Đã thanh toán' cho đơn hàng: " + maDatHang);
        } catch (Exception e) {
            System.err.println("✗ Lỗi khi cập nhật trạng thái thanh toán: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<DatHang> findRecentOrders(int limit) {
        return datHangRepository.findTop5ByOrderByNgayDatDesc();
    }

    @Override
    public long countAllOrders() {
        return datHangRepository.count();
    }

    @Override
    public long countByCuaHangAndTrangThai(CuaHang cuaHang, String trangThai) {
        return datHangRepository.countByCuaHangAndTrangThai(cuaHang, trangThai);
    }

    @Override
    public List<DatHang> findRecentOrdersByCuaHangAndTrangThai(CuaHang cuaHang, String trangThai, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "ngayDat"));
        return datHangRepository.findByCuaHangAndTrangThai(cuaHang, trangThai, pageable);
    }

    @Override
    @Transactional
    public DatHang datHang(NguoiDung nguoiDung, DatHangRequest datHangRequest) {
        // Lấy giỏ hàng
        GioHang gioHang = gioHangService.findByNguoiDung(nguoiDung);
        if (gioHang == null) {
            throw new RuntimeException("Giỏ hàng trống");
        }

        // Lấy danh sách mặt hàng
        List<MatHang> matHangs = matHangService.findByGioHang(gioHang);
        if (matHangs.isEmpty()) {
            throw new RuntimeException("Giỏ hàng trống");
        }

        // Tạo đơn hàng mới với thông tin mới
        DatHang datHang = DatHang.builder()
                .nguoiDung(nguoiDung)
                .ngayDat(LocalDate.now())
                .trangThai("Chờ xác nhận")
                .tongTien(BigDecimal.ZERO)
                .diaChiGiaoHang(datHangRequest.getDiaChiGiaoHang())
                .soDienThoaiGiaoHang(datHangRequest.getSoDienThoai())
                .ghiChu(datHangRequest.getGhiChu())
                .build();
        
        // Lưu thông tin khuyến mãi đã chọn (nếu có)
        if (datHangRequest.getSelectedPromotions() != null && !datHangRequest.getSelectedPromotions().isEmpty()) {
            // Lưu khuyến mãi đầu tiên (có thể mở rộng để lưu nhiều khuyến mãi)
            Integer firstPromoId = datHangRequest.getSelectedPromotions().values().iterator().next();
            if (firstPromoId != null) {
                KhuyenMai khuyenMai = khuyenMaiService.findByMaKhuyenMai(firstPromoId);
                datHang.setKhuyenMai(khuyenMai);
            }
        }

        // Lưu đơn hàng để có mã
        datHang = datHangRepository.save(datHang);

        BigDecimal tongTien = BigDecimal.ZERO;
        Map<CuaHang, BigDecimal> discountByStore = new HashMap<>();

        // Tính toán giảm giá theo cửa hàng
        if (datHangRequest.getSelectedPromotions() != null) {
            for (Map.Entry<Integer, Integer> entry : datHangRequest.getSelectedPromotions().entrySet()) {
                Integer maCuaHang = entry.getKey();
                Integer maKhuyenMai = entry.getValue();
                
                if (maKhuyenMai != null) {
                    KhuyenMai khuyenMai = khuyenMaiService.findByMaKhuyenMai(maKhuyenMai);
                    if (khuyenMai != null && khuyenMai.isActive()) {
                        // Tính tổng tiền của cửa hàng này
                        BigDecimal storeTotal = matHangs.stream()
                                .filter(mh -> mh.getSanPham().getCuaHang().getMaCuaHang().equals(maCuaHang))
                                .map(mh -> mh.getSanPham().getGiaBan().multiply(BigDecimal.valueOf(mh.getSoLuongDat())))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        
                        // Tính số tiền giảm giá
                        BigDecimal discountAmount = storeTotal.multiply(khuyenMai.getDiscount())
                                .divide(BigDecimal.valueOf(100));
                        
                        discountByStore.put(khuyenMai.getCuaHang(), discountAmount);
                    }
                }
            }
        }

        // Tạo chi tiết đơn hàng và cập nhật tồn kho
        for (MatHang matHang : matHangs) {
            SanPham sanPham = matHang.getSanPham();
            
            // Kiểm tra tồn kho
            if (sanPham.getSoLuongConLai() < matHang.getSoLuongDat()) {
                throw new RuntimeException("Sản phẩm " + sanPham.getTenSanPham() + " không đủ số lượng tồn kho");
            }

            // Tính thành tiền
            BigDecimal thanhTien = sanPham.getGiaBan().multiply(BigDecimal.valueOf(matHang.getSoLuongDat()));
            
            // Áp dụng giảm giá nếu có
            CuaHang cuaHang = sanPham.getCuaHang();
            if (discountByStore.containsKey(cuaHang)) {
                // Tính phần giảm giá cho sản phẩm này (tỷ lệ theo giá trị)
                BigDecimal storeTotalForDiscount = matHangs.stream()
                        .filter(mh -> mh.getSanPham().getCuaHang().equals(cuaHang))
                        .map(mh -> mh.getSanPham().getGiaBan().multiply(BigDecimal.valueOf(mh.getSoLuongDat())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                if (storeTotalForDiscount.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal productRatio = thanhTien.divide(storeTotalForDiscount, 4, BigDecimal.ROUND_HALF_UP);
                    BigDecimal productDiscount = discountByStore.get(cuaHang).multiply(productRatio);
                    thanhTien = thanhTien.subtract(productDiscount);
                }
            }

            // Tạo chi tiết đơn hàng
            DatHangChiTiet chiTiet = DatHangChiTiet.builder()
                    .datHang(datHang)
                    .sanPham(sanPham)
                    .soLuong(matHang.getSoLuongDat())
                    .giaBan(sanPham.getGiaBan())
                    .thanhTien(thanhTien)
                    .build();

            datHangChiTietRepository.save(chiTiet);

            // Cập nhật tồn kho
            sanPham.setSoLuongConLai(sanPham.getSoLuongConLai() - matHang.getSoLuongDat());
            sanPhamService.save(sanPham);

            tongTien = tongTien.add(thanhTien);
        }
 
        // Cập nhật tổng tiền đơn hàng
        datHang.setTongTien(tongTien);
        return datHangRepository.save(datHang);
    }
    
    @Override
    public List<DatHang> findByNguoiDung(Integer maNguoiDung) {
        return datHangRepository.findByNguoiDung_MaNguoiDung(maNguoiDung);
    }
    
    @Override
    public Page<DatHang> findByCuaHangAndFilters(
            CuaHang cuaHang, 
            String keyword, 
            LocalDate startDate, 
            LocalDate endDate,
            String trangThai,
            Pageable pageable) {
        
        return datHangRepository.findByCuaHangAndFilters(
            cuaHang, keyword, startDate, endDate, trangThai, pageable);
    }
    
    @Override
    public List<DatHang> findByCuaHang(CuaHang cuaHang) {
        return datHangRepository.findByCuaHang(cuaHang);
    }
    
    @Override
    public void deleteDatHang(Integer maDatHang) {
        DatHang datHang = datHangRepository.findById(maDatHang)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));
        datHangRepository.delete(datHang);
    }
    
    public Page<DatHangChiTiet> findCompletedOrderDetailsByCuaHang(
            CuaHang cuaHang, String keyword, LocalDate startDate, 
            LocalDate endDate, String productName, Pageable pageable) {
        return datHangChiTietRepository.findCompletedOrderDetailsByCuaHang(
            cuaHang, keyword, startDate, endDate, productName, "Hủy", pageable);
    }

    @Override
    public Double getTotalRevenueByCuaHangAndDateRange(
            CuaHang cuaHang, 
            String trangThai, 
            LocalDate startDate, 
            LocalDate endDate) {
        
        return datHangChiTietRepository.getTotalRevenueByCuaHangAndDateRange(
            cuaHang, trangThai, startDate, endDate);
    }

    @Override
    public Long getTotalProductsSoldByCuaHang(
            CuaHang cuaHang, 
            String trangThai, 
            LocalDate startDate, 
            LocalDate endDate) {
        
        return datHangChiTietRepository.getTotalProductsSoldByCuaHang(
            cuaHang, trangThai, startDate, endDate);
    }
    
    @Override
    public Map<LocalDate, DailyRevenueStats> getDailyRevenueStatsByDateRange(
            CuaHang cuaHang, LocalDate startDate, LocalDate endDate) {
        
        List<Object[]> results = datHangChiTietRepository.getDailyRevenueStatsByCuaHangAndDateRange(
            cuaHang, startDate, endDate);
        
        Map<LocalDate, DailyRevenueStats> statsMap = new HashMap<>();
        
        for (Object[] result : results) {
            LocalDate date = (LocalDate) result[0];
            Double revenue = ((Number) result[1]).doubleValue();
            Long orderCount = ((Number) result[2]).longValue();
            Long productCount = ((Number) result[3]).longValue();
            
            statsMap.put(date, new DailyRevenueStats(revenue, orderCount, productCount));
        }
        
        // Đảm bảo tất cả các ngày trong khoảng đều có dữ liệu
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            statsMap.putIfAbsent(current, new DailyRevenueStats(0.0, 0L, 0L));
            current = current.plusDays(1);
        }
        
        return statsMap;
    }
    
    @Override
    public List<DatHang> findByCuaHangAndTrangThai(CuaHang cuaHang, String trangThai, Pageable pageable) {
        return datHangRepository.findByCuaHangAndTrangThai(cuaHang, trangThai, pageable);
    }
    
    @Override
    public Page<DatHang> findAllOrdersWithFilters(
        String keyword, 
        LocalDate startDate, 
        LocalDate endDate,
        String trangThai,
        Integer maCuaHang,
        Pageable pageable) {
        
        return datHangRepository.findAllOrdersWithFilters(
            keyword, startDate, endDate, trangThai, maCuaHang, pageable);
    }
    
    @Override
    public Page<DatHangChiTiet> findCompletedOrderDetails(
            Integer maCuaHang, 
            String keyword, 
            LocalDate startDate, 
            LocalDate endDate,
            String productName,
            Pageable pageable) {
        
        if (maCuaHang != null) {
            CuaHang cuaHang = cuaHangService.findByMaCuaHang(maCuaHang);
            return datHangChiTietRepository.findCompletedOrderDetailsByCuaHang(
                cuaHang, keyword, startDate, endDate, productName, "Hủy", pageable);
        } else {
            // Lấy tất cả đơn hàng hoàn thành của tất cả cửa hàng
            return datHangChiTietRepository.findCompletedOrderDetails(
                keyword, startDate, endDate, productName, "Hủy", pageable);
        }
    }
    
    @Override
    public Page<DatHang> findByNguoiDungWithPagination(Integer maNguoiDung, Pageable pageable) {
        return datHangRepository.findByNguoiDung_MaNguoiDung(maNguoiDung, pageable);
    }
    
    @Override
    public List<DatHang> findByNguoiDung(NguoiDung nguoiDung) {
        return datHangRepository.findByNguoiDungOrderByNgayDatDesc(nguoiDung);
    }
    
    @Override
    public Page<DatHang> findByNguoiDung(NguoiDung nguoiDung, Pageable pageable) {
        return datHangRepository.findByNguoiDung(nguoiDung, pageable);
    }
}