package vn.iotstar.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.entity.*;
import vn.iotstar.repository.*;
import vn.iotstar.service.DatHangService;
import vn.iotstar.service.ThanhToanService;
import vn.iotstar.service.VanChuyenService;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class VanChuyenServiceImpl implements VanChuyenService {
    
    @Autowired
    private VanChuyenRepository vanChuyenRepository;
    
    @Autowired
    private NguoiDungRepository nguoiDungRepository;
    
    @Autowired
    private DatHangRepository datHangRepository;
    
    @Autowired
    private DatHangService datHangService;
    
    @Autowired
    private ThanhToanService thanhToanService;
    
    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private KhuyenMaiRepository khuyenMaiRepository;
    
    // ================== SHIPPER MANAGEMENT ==================
    
    @Override
    public List<NguoiDung> getAllActiveShippers() {
        List<NguoiDung> shippers = nguoiDungRepository.findByVaiTro_MaVaiTroAndTrangThai("SHIPPER", "Hoạt động");
        
        System.out.println("DEBUG: Số lượng shipper hoạt động: " + shippers.size());
        
        for (NguoiDung shipper : shippers) {
            System.out.println("  - Shipper: " + shipper.getTenNguoiDung() + 
                             " - Email: " + shipper.getEmail() +
                             " - SDT: " + shipper.getSdt() +
                             " - Vai trò: " + (shipper.getVaiTro() != null ? 
                                 shipper.getVaiTro().getTenVaiTro() + " (Mã: " + shipper.getVaiTro().getMaVaiTro() + ")" : "null") +
                             " - Trạng thái: " + shipper.getTrangThai());
        }
        
        return shippers;
    }
    
    // ================== ORDER ASSIGNMENT ==================
    
    @Override
    public Page<DatHang> getUnassignedConfirmedOrdersByCuaHang(CuaHang cuaHang, String keyword, Pageable pageable) {
        return vanChuyenRepository.findUnassignedConfirmedOrdersByCuaHang(cuaHang, keyword, "Đã xác nhận", pageable);
    }
    
    @Override
    public Page<VanChuyen> getAssignedOrdersByCuaHang(CuaHang cuaHang, String keyword, Pageable pageable) {
        System.out.println("========== DEBUG: GET ASSIGNED ORDERS ==========");
        System.out.println("Cửa hàng ID: " + cuaHang.getMaCuaHang());
        Page<VanChuyen> result = vanChuyenRepository.findAssignedOrdersByCuaHang(cuaHang, keyword, pageable);
        System.out.println("Số lượng đơn hàng ĐÃ GÁN SHIPPER (chưa hoàn thành/hủy): " + result.getTotalElements());
        
        result.getContent().forEach(delivery -> {
            System.out.println("  - Đơn #" + delivery.getDatHang().getMaDatHang() + 
                             " - Trạng thái VC: " + delivery.getTrangThai() +
                             " - Trạng thái ĐH: " + delivery.getDatHang().getTrangThai());
        });
        System.out.println("===============================================");
        return result;
    }
    
    @Override
    public long countUnassignedConfirmedOrdersByCuaHang(CuaHang cuaHang) {
        long count = vanChuyenRepository.countUnassignedConfirmedOrdersByCuaHang(cuaHang, "Đã xác nhận");
        System.out.println("DEBUG: Count unassigned orders (chưa hủy/hoàn thành): " + count);
        return count;
    }
    
    @Override
    public long countAssignedOrdersByCuaHang(CuaHang cuaHang) {
        long count = vanChuyenRepository.countAssignedOrdersByCuaHang(cuaHang);
        System.out.println("DEBUG: Count assigned orders (chưa hoàn thành/hủy): " + count);
        return count;
    }
    
    @Override
    public VanChuyen assignShipperToOrder(Integer orderId, Integer shipperId) {
        System.out.println("========== DEBUG: ASSIGN SHIPPER ==========");
        System.out.println("Order ID: " + orderId);
        System.out.println("Shipper ID: " + shipperId);
        
        Optional<DatHang> orderOpt = datHangRepository.findById(orderId);
        Optional<NguoiDung> shipperOpt = nguoiDungRepository.findById(shipperId);
        
        if (orderOpt.isPresent() && shipperOpt.isPresent()) {
            DatHang order = orderOpt.get();
            NguoiDung shipper = shipperOpt.get();
            
            System.out.println("Order tìm thấy - Trạng thái hiện tại: " + order.getTrangThai());
            System.out.println("Shipper tìm thấy: " + shipper.getTenNguoiDung());
            
            if (!"SHIPPER".equals(shipper.getVaiTro().getMaVaiTro())) {
                throw new RuntimeException("Người dùng không phải là shipper");
            }
            
            if (isOrderAssigned(orderId)) {
                throw new RuntimeException("Đơn hàng đã được gán shipper");
            }
            
            System.out.println("Giữ nguyên trạng thái đơn hàng: " + order.getTrangThai());
            
            VanChuyen vanChuyen = VanChuyen.builder()
                    .datHang(order)
                    .nguoiDung(shipper)
                    .trangThai("Đã bàn giao")
                    .build();
            
            VanChuyen saved = vanChuyenRepository.save(vanChuyen);
            System.out.println("Đã tạo VanChuyen ID: " + saved.getMaVanChuyen() + " với trạng thái: Đã bàn giao");
            System.out.println("==========================================");
            
            return saved;
        }
        throw new RuntimeException("Đơn hàng hoặc shipper không tồn tại");
    }
    
    @Override
    public boolean isOrderAssigned(Integer orderId) {
        return vanChuyenRepository.findByOrderId(orderId).isPresent();
    }
    
    @Override
    public VanChuyen getDeliveryByOrderId(Integer orderId) {
        return vanChuyenRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng chưa được gán shipper"));
    }
    
    @Override
    public void unassignShipper(Integer deliveryId) {
        Optional<VanChuyen> deliveryOpt = vanChuyenRepository.findById(deliveryId);
        if (deliveryOpt.isPresent()) {
            VanChuyen delivery = deliveryOpt.get();
            DatHang order = delivery.getDatHang();
            
            order.setTrangThai("Đã xác nhận");
            datHangRepository.save(order);
            
            vanChuyenRepository.delete(delivery);
        } else {
            throw new RuntimeException("Bản ghi vận chuyển không tồn tại");
        }
    }
    
    // ================== DELIVERY MANAGEMENT ==================
    
    @Override
    public VanChuyen getDeliveryById(Integer deliveryId) {
        return vanChuyenRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Bản ghi vận chuyển không tồn tại"));
    }
    
    @Override
    public Page<VanChuyen> getDeliveriesByShipperAndStatus(Integer shipperId, String status, Pageable pageable) {
        return vanChuyenRepository.findByShipperIdAndStatus(shipperId, status, pageable);
    }
    
    @Override
    public long countDeliveriesByShipperAndStatus(Integer shipperId, String status) {
        return vanChuyenRepository.countByShipperIdAndStatus(shipperId, status);
    }
    
    @Override
    public List<VanChuyen> getDeliveriesByShipperId(Integer shipperId) {
        return vanChuyenRepository.findByShipperId(shipperId);
    }
    
    // ================== DELIVERY STATUS UPDATES ==================
    
    @Override
    @Transactional
    public VanChuyen updateDeliveryStatus(Integer deliveryId, String newStatus, String lyDoHuy) {
        VanChuyen vanChuyen = vanChuyenRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn vận chuyển"));
        
        System.out.println("=== DEBUG: UPDATE DELIVERY STATUS ===");
        System.out.println("Delivery ID: " + deliveryId);
        System.out.println("New Status: " + newStatus);
        System.out.println("Ly do huy: " + lyDoHuy);
        System.out.println("Current VC Status: " + vanChuyen.getTrangThai());
        System.out.println("Current Order Status: " + vanChuyen.getDatHang().getTrangThai());
        
        vanChuyen.setTrangThai(newStatus);
        
        DatHang datHang = vanChuyen.getDatHang();
        
        if ("Hủy".equals(newStatus)) {
            datHang.setTrangThai("Hủy");
            datHang.setLyDoHuy(lyDoHuy);
            System.out.println("Đã cập nhật đơn hàng thành Hủy: " + datHang.getMaDatHang());
            
            try {
                updatePaymentStatusToCancelled(datHang.getMaDatHang());
            } catch (Exception e) {
                System.err.println("Error updating payment status to cancelled: " + e.getMessage());
            }
            
        } else if ("Hoàn thành".equals(newStatus)) {
            datHang.setTrangThai("Hoàn thành");
            System.out.println("Đã cập nhật đơn hàng thành Hoàn thành: " + datHang.getMaDatHang());
            
            try {
                updatePaymentStatusToCompleted(datHang.getMaDatHang());
                updateProductQuantitiesAndPromotions(datHang.getMaDatHang());
            } catch (Exception e) {
                System.err.println("Error in side effects: " + e.getMessage());
            }
        } else if ("Đang giao".equals(newStatus)) {
            datHang.setTrangThai("Đang giao");
            System.out.println("Đã cập nhật đơn hàng thành Đang giao: " + datHang.getMaDatHang());
        }
        
        datHangRepository.save(datHang);
        VanChuyen saved = vanChuyenRepository.save(vanChuyen);
        
        System.out.println("After update - VC Status: " + saved.getTrangThai());
        System.out.println("After update - Order Status: " + saved.getDatHang().getTrangThai());
        System.out.println("=== END DEBUG ===");
        
        return saved;
    }
    
    @Override
    @Transactional
    public VanChuyen processDeliveryCancellation(Integer deliveryId, String lyDoHuy, String action) {
        VanChuyen vanChuyen = vanChuyenRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn vận chuyển"));
        
        System.out.println("=== DEBUG: PROCESS DELIVERY CANCELLATION ===");
        System.out.println("Delivery ID: " + deliveryId);
        System.out.println("Action: " + action);
        System.out.println("Lý do: " + lyDoHuy);
        System.out.println("Current VC Status: " + vanChuyen.getTrangThai());
        System.out.println("Current Order Status: " + vanChuyen.getDatHang().getTrangThai());
        
        DatHang datHang = vanChuyen.getDatHang();
        
        // Lưu lý do hủy vào VanChuyen trong cả hai trường hợp
        vanChuyen.setLyDoHuy(lyDoHuy);
        
        if ("RETURN_TO_VENDOR".equals(action)) {
            // Lý do thuộc về shipper - Chuyển về trạng thái "Đã xác nhận"
            vanChuyen.setTrangThai("Hủy nhận");
            datHang.setTrangThai("Đã xác nhận");
            System.out.println("🔄 Chuyển đơn hàng về trạng thái 'Đã xác nhận'");
            
        } else if ("COMPLETE_CANCELLATION".equals(action)) {
            // Lý do thuộc về hệ thống - Hủy hoàn toàn
            vanChuyen.setTrangThai("Hủy");
            datHang.setTrangThai("Hủy");
            datHang.setLyDoHuy(lyDoHuy); // Lưu lý do vào đơn hàng nếu cần
            
            try {
                updatePaymentStatusToCancelled(datHang.getMaDatHang());
            } catch (Exception e) {
                System.err.println("Error updating payment status to cancelled: " + e.getMessage());
            }
            
            System.out.println("❌ Hủy đơn hàng hoàn toàn");
        }
        
        // Lưu các thay đổi
        datHangRepository.save(datHang);
        VanChuyen saved = vanChuyenRepository.save(vanChuyen);
        
        System.out.println("After update - VC Status: " + saved.getTrangThai());
        System.out.println("After update - Order Status: " + saved.getDatHang().getTrangThai());
        System.out.println("=== END DEBUG ===");
        
        return saved;
    }
    
    // ================== PRIVATE HELPER METHODS ==================
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void updatePaymentStatusToCompleted(Integer maDatHang) {
        try {
            System.out.println("=== UPDATING PAYMENT STATUS ===");
            System.out.println("Order ID: " + maDatHang);
            
            List<ThanhToan> thanhToans = thanhToanService.findByDatHang(maDatHang);
            System.out.println("Found " + thanhToans.size() + " payment records");
            
            for (ThanhToan thanhToan : thanhToans) {
                System.out.println("Payment ID: " + thanhToan.getMaThanhToan() + 
                                 ", Current Status: " + thanhToan.getTrangThai());
                thanhToan.setTrangThai("completed");
                ThanhToan saved = thanhToanService.save(thanhToan);
                System.out.println("Updated Payment ID: " + saved.getMaThanhToan() + 
                                 ", New Status: " + saved.getTrangThai());
            }
            
            System.out.println("=== PAYMENT STATUS UPDATE COMPLETE ===");
        } catch (Exception e) {
            System.err.println("Lỗi khi cập nhật trạng thái thanh toán: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void updatePaymentStatusToCancelled(Integer maDatHang) {
        try {
            System.out.println("=== UPDATING PAYMENT STATUS TO CANCELLED ===");
            System.out.println("Order ID: " + maDatHang);
            
            List<ThanhToan> thanhToans = thanhToanService.findByDatHang(maDatHang);
            System.out.println("Found " + thanhToans.size() + " payment records");
            
            for (ThanhToan thanhToan : thanhToans) {
                System.out.println("Payment ID: " + thanhToan.getMaThanhToan() + 
                                 ", Current Status: " + thanhToan.getTrangThai());
                thanhToan.setTrangThai("cancelled");
                ThanhToan saved = thanhToanService.save(thanhToan);
                System.out.println("Updated Payment ID: " + saved.getMaThanhToan() + 
                                 ", New Status: " + saved.getTrangThai());
            }
            
            System.out.println("=== PAYMENT STATUS UPDATE TO CANCELLED COMPLETE ===");
        } catch (Exception e) {
            System.err.println("Lỗi khi cập nhật trạng thái thanh toán sang cancelled: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void updateProductQuantitiesAndPromotions(Integer maDatHang) {
        try {
            System.out.println("=== UPDATING PRODUCT QUANTITIES AND PROMOTIONS ===");
            System.out.println("Order ID: " + maDatHang);
            
            Optional<DatHang> orderOpt = datHangRepository.findById(maDatHang);
            if (!orderOpt.isPresent()) {
                System.out.println("Order not found: " + maDatHang);
                return;
            }
            
            DatHang order = orderOpt.get();
            List<DatHangChiTiet> orderDetails = order.getDatHangChiTiets();
            
            System.out.println("Found " + orderDetails.size() + " order details");
            
            for (DatHangChiTiet detail : orderDetails) {
                SanPham product = detail.getSanPham();
                Integer quantity = detail.getSoLuong();
                
                System.out.println("Updating product: " + product.getTenSanPham() + 
                                 ", Quantity: " + quantity + 
                                 ", Current Stock: " + product.getSoLuongConLai() +
                                 ", Current Sold: " + product.getSoLuongDaBan());
                
                int newStock = product.getSoLuongConLai() - quantity;
                if (newStock < 0) {
                    newStock = 0;
                }
                product.setSoLuongConLai(newStock);
                
                int newSold = product.getSoLuongDaBan() + quantity;
                product.setSoLuongDaBan(newSold);
                
                sanPhamRepository.save(product);
                
                System.out.println("Updated product: " + product.getTenSanPham() + 
                                 ", New Stock: " + product.getSoLuongConLai() +
                                 ", New Sold: " + product.getSoLuongDaBan());
            }
            
            KhuyenMai promotion = order.getKhuyenMai();
            if (promotion != null) {
                System.out.println("Updating promotion: " + promotion.getMaGiamGia() + 
                                 ", Current Quantity: " + promotion.getSoLuongMaGiamGia() +
                                 ", Current Used: " + promotion.getSoLuongDaSuDung());
                
                int newQuantity = promotion.getSoLuongMaGiamGia() - 1;
                if (newQuantity < 0) {
                    newQuantity = 0;
                }
                promotion.setSoLuongMaGiamGia(newQuantity);
                
                int newUsed = promotion.getSoLuongDaSuDung() + 1;
                promotion.setSoLuongDaSuDung(newUsed);
                
                khuyenMaiRepository.save(promotion);
                
                System.out.println("Updated promotion: " + promotion.getMaGiamGia() + 
                                 ", New Quantity: " + promotion.getSoLuongMaGiamGia() +
                                 ", New Used: " + promotion.getSoLuongDaSuDung());
            } else {
                System.out.println("No promotion found for this order");
            }
            
            System.out.println("=== PRODUCT QUANTITIES AND PROMOTIONS UPDATE COMPLETE ===");
            
        } catch (Exception e) {
            System.err.println("Lỗi khi cập nhật số lượng sản phẩm và khuyến mãi: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi cập nhật số lượng sản phẩm và khuyến mãi: " + e.getMessage());
        }
    }
}