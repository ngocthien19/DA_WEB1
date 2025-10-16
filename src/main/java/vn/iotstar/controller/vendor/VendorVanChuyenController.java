package vn.iotstar.controller.vendor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.entity.CuaHang;
import vn.iotstar.entity.DatHang;
import vn.iotstar.entity.VanChuyen;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.service.CuaHangService;
import vn.iotstar.service.DatHangService;
import vn.iotstar.service.NguoiDungService;
import vn.iotstar.service.VanChuyenService;
import vn.iotstar.service.impl.UserDetailsServiceImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/vendor/delivery")
@PreAuthorize("hasRole('VENDOR')")
public class VendorVanChuyenController {

    @Autowired
    private VanChuyenService vanChuyenService;

    @Autowired
    private DatHangService datHangService;

    @Autowired
    private CuaHangService cuaHangService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired // THÊM AUTOWIRED NÀY
    private NguoiDungService nguoiDungService;

    // Helper methods - giống với VendorDatHangController
    private CuaHang getCurrentStore(Authentication authentication) {
        NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
        if (currentUser == null) return null;
        
        List<CuaHang> stores = cuaHangService.findByNguoiDung(currentUser);
        return stores.isEmpty() ? null : stores.get(0);
    }

    private NguoiDung getCurrentUser(Authentication authentication) {
        return userDetailsService.getCurrentUser(authentication);
    }

    // Kiểm tra đơn hàng có thuộc về cửa hàng - giống với VendorDatHangController
    private boolean isOrderBelongsToStore(DatHang datHang, CuaHang cuaHang) {
        return datHang.getDatHangChiTiets().stream()
                .anyMatch(chiTiet -> chiTiet.getSanPham().getCuaHang().getMaCuaHang().equals(cuaHang.getMaCuaHang()));
    }

    // ================== QUẢN LÝ VẬN CHUYỂN ==================

    @GetMapping
    public String deliveryManagement(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "unassigned") String tab,
            Authentication authentication,
            Model model) {
        
        NguoiDung currentUser = getCurrentUser(authentication);
        CuaHang cuaHang = getCurrentStore(authentication);
        
        model.addAttribute("user", currentUser);

        if (cuaHang == null) {
            return "redirect:/vendor/dashboard"; 
        }

        Pageable pageable;
        
        if ("assigned".equals(tab)) {
            pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "datHang.ngayDat"));
        } else {
            pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "ngayDat"));
        }
        
        List<NguoiDung> shippers = vanChuyenService.getAllActiveShippers();
        
        // Đếm số lượng cho các tab
        long unassignedCount = vanChuyenService.countUnassignedConfirmedOrdersByCuaHang(cuaHang);
        long assignedCount = vanChuyenService.countAssignedOrdersByCuaHang(cuaHang);
        
        if ("assigned".equals(tab)) {
            // Tab 'Đơn đang giao hàng' - đã được gán shipper
            Page<VanChuyen> assignedPage = vanChuyenService.getAssignedOrdersByCuaHang(cuaHang, keyword, pageable);
            model.addAttribute("ordersPage", assignedPage);
            model.addAttribute("assignedOrders", assignedPage.getContent());
        } else { 
            // Tab 'Đơn chờ gán Shipper' - đã xác nhận & chưa gán shipper
            Page<DatHang> unassignedPage = vanChuyenService.getUnassignedConfirmedOrdersByCuaHang(cuaHang, keyword, pageable);
            model.addAttribute("ordersPage", unassignedPage);
            model.addAttribute("unassignedOrders", unassignedPage.getContent());
        }
        
        model.addAttribute("cuaHang", cuaHang);
        model.addAttribute("shippers", shippers);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentTab", tab);
        model.addAttribute("currentPage", page);
        model.addAttribute("unassignedCount", unassignedCount);
        model.addAttribute("assignedCount", assignedCount);
        
        return "vendor/delivery/delivery";
    }

    // ================== GÁN SHIPPER CHO ĐƠN HÀNG ==================

    @PostMapping("/assign-shipper")
    @ResponseBody
    public ResponseEntity<?> assignShipperToOrder(
            @RequestParam Integer orderId,
            @RequestParam Integer shipperId,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            CuaHang cuaHang = getCurrentStore(authentication);
            if (cuaHang == null) {
                response.put("success", false);
                response.put("message", "Bạn chưa đăng nhập hoặc chưa có cửa hàng.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 1. Kiểm tra đơn hàng có tồn tại và thuộc về cửa hàng
            DatHang order = datHangService.findByMaDatHang(orderId);
            if (order == null || !isOrderBelongsToStore(order, cuaHang)) {
                response.put("success", false);
                response.put("message", "Đơn hàng không tồn tại hoặc không thuộc về cửa hàng của bạn");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 2. Kiểm tra trạng thái đơn hàng PHẢI là "Đã xác nhận"
            if (!"Đã xác nhận".equals(order.getTrangThai())) {
                response.put("success", false);
                response.put("message", "Chỉ có thể gán shipper cho đơn hàng ở trạng thái 'Đã xác nhận'. Trạng thái hiện tại: " + order.getTrangThai());
                return ResponseEntity.badRequest().body(response);
            }

            // 3. Kiểm tra đơn hàng chưa được gán shipper
            if (vanChuyenService.isOrderAssigned(orderId)) {
                response.put("success", false);
                response.put("message", "Đơn hàng đã được gán shipper trước đó");
                return ResponseEntity.badRequest().body(response);
            }

            // 4. Gán shipper cho đơn hàng
            VanChuyen vanChuyen = vanChuyenService.assignShipperToOrder(orderId, shipperId);
            
            response.put("success", true);
            response.put("message", "Gán shipper thành công! Đơn hàng đã được bàn giao và chờ shipper xác nhận");
            response.put("orderId", vanChuyen.getDatHang().getMaDatHang());
            response.put("shipperName", vanChuyen.getNguoiDung().getTenNguoiDung());
            response.put("deliveryId", vanChuyen.getMaVanChuyen());
            response.put("newOrderStatus", order.getTrangThai()); // Giữ nguyên trạng thái
            response.put("deliveryStatus", "Đã bàn giao"); // Trạng thái vận chuyển mới
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", "Lỗi nghiệp vụ: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

 // ================== CẬP NHẬT TRẠNG THÁI VẬN CHUYỂN ==================
    @PostMapping("/update-status")
    @ResponseBody
    public ResponseEntity<?> updateDeliveryStatus(
            @RequestParam Integer deliveryId,
            @RequestParam String newStatus,
            @RequestParam(required = false) String lyDoHuy,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            CuaHang cuaHang = getCurrentStore(authentication);
            if (cuaHang == null) {
                response.put("success", false);
                response.put("message", "Bạn chưa đăng nhập hoặc chưa có cửa hàng.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 1. Lấy thông tin vận chuyển bằng deliveryId (MA_VAN_CHUYEN)
            VanChuyen vanChuyen = vanChuyenService.getDeliveryById(deliveryId);
            if (vanChuyen == null || !isOrderBelongsToStore(vanChuyen.getDatHang(), cuaHang)) {
                response.put("success", false);
                response.put("message", "Đơn vận chuyển không tồn tại hoặc không thuộc về cửa hàng của bạn");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 2. Kiểm tra lý do hủy nếu trạng thái là "Hủy"
            if ("Hủy".equals(newStatus)) {
                if (lyDoHuy == null || lyDoHuy.trim().isEmpty()) {
                    response.put("success", false);
                    response.put("message", "Vui lòng nhập lý do hủy đơn hàng");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            // 3. Cập nhật trạng thái vận chuyển và lý do hủy nếu có
            VanChuyen updatedVanChuyen = vanChuyenService.updateDeliveryStatus(deliveryId, newStatus, lyDoHuy);
            
            response.put("success", true);
            response.put("message", "Cập nhật trạng thái giao hàng thành công!");
            response.put("newStatus", updatedVanChuyen.getTrangThai());
            response.put("orderStatus", updatedVanChuyen.getDatHang().getTrangThai());
            response.put("orderId", updatedVanChuyen.getDatHang().getMaDatHang());
            
            // THÊM: Thông báo nếu đơn hàng sẽ biến mất khỏi danh sách
            if ("Hủy".equals(newStatus) || "Hoàn thành".equals(newStatus)) {
                response.put("willDisappear", true);
                response.put("disappearMessage", "Đơn hàng sẽ không hiển thị trong danh sách vận chuyển nữa");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", "Lỗi nghiệp vụ: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ================== HỦY GÁN SHIPPER ==================
    @PostMapping("/unassign-shipper")
    @ResponseBody
    public ResponseEntity<?> unassignShipper(
            @RequestParam Integer deliveryId,  // Đây là MA_VAN_CHUYEN
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            CuaHang cuaHang = getCurrentStore(authentication);
            if (cuaHang == null) {
                response.put("success", false);
                response.put("message", "Bạn chưa đăng nhập hoặc chưa có cửa hàng.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 1. Lấy thông tin vận chuyển bằng deliveryId (MA_VAN_CHUYEN)
            VanChuyen vanChuyen = vanChuyenService.getDeliveryById(deliveryId);
            if (vanChuyen == null || !isOrderBelongsToStore(vanChuyen.getDatHang(), cuaHang)) {
                response.put("success", false);
                response.put("message", "Đơn vận chuyển không tồn tại hoặc không thuộc về cửa hàng của bạn");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 2. Chỉ cho phép hủy gán nếu đơn hàng chưa được giao
            if ("Hoàn thành".equals(vanChuyen.getTrangThai()) || "Hủy".equals(vanChuyen.getTrangThai())) {
                response.put("success", false);
                response.put("message", "Không thể hủy gán shipper cho đơn hàng đã hoàn thành hoặc đã hủy");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 3. Hủy gán shipper
            vanChuyenService.unassignShipper(deliveryId);
            
            response.put("success", true);
            response.put("message", "Hủy gán shipper thành công! Đơn hàng đã được chuyển về trạng thái 'Đã xác nhận'");
            response.put("orderId", vanChuyen.getDatHang().getMaDatHang());
            response.put("newOrderStatus", "Đã xác nhận");
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", "Lỗi nghiệp vụ: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}