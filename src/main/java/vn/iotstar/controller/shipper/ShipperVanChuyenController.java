package vn.iotstar.controller.shipper;

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
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.entity.VanChuyen;
import vn.iotstar.service.ThanhToanService;
import vn.iotstar.service.VanChuyenService;
import vn.iotstar.service.impl.UserDetailsServiceImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/shipper")
@PreAuthorize("hasRole('SHIPPER')")
public class ShipperVanChuyenController {

    @Autowired
    private VanChuyenService vanChuyenService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;
    
    @Autowired
    private ThanhToanService thanhToanService;

    private NguoiDung getCurrentShipper(Authentication authentication) {
        return userDetailsService.getCurrentUser(authentication);
    }

    @GetMapping("/dashboard")
    public String shipperDashboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "pending") String tab,
            Authentication authentication,
            Model model) {
        
        NguoiDung shipper = getCurrentShipper(authentication);
        
        if (shipper == null) {
            return "redirect:/login";
        }

        model.addAttribute("shipper", shipper);

        // THÊM: Kiểm tra trạng thái khóa của shipper
        boolean isLocked = "Khóa".equals(shipper.getTrangThai()) || 
                          "INACTIVE".equals(shipper.getTrangThai()) ||
                          "BANNED".equals(shipper.getTrangThai());
        model.addAttribute("isLocked", isLocked);

        // THÊM: Nếu bị khóa, chỉ cho phép xem tab completed và cancelled
        if (isLocked && !"completed".equals(tab) && !"cancelled".equals(tab)) {
            tab = "completed"; // Mặc định chuyển về tab completed
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "datHang.ngayDat"));
        
        // Lấy danh sách đơn hàng theo shipper
        Page<VanChuyen> deliveriesPage;
        
        if ("completed".equals(tab)) {
            // Đơn hàng đã hoàn thành
            deliveriesPage = vanChuyenService.getDeliveriesByShipperAndStatus(shipper.getMaNguoiDung(), "Hoàn thành", pageable);
        } else if ("cancelled".equals(tab)) {
            // Đơn hàng đã hủy
            deliveriesPage = vanChuyenService.getDeliveriesByShipperAndStatus(shipper.getMaNguoiDung(), "Hủy", pageable);
        } else if ("active".equals(tab)) {
            // Đơn hàng đang hoạt động (Đang giao)
            deliveriesPage = vanChuyenService.getDeliveriesByShipperAndStatus(shipper.getMaNguoiDung(), "Đang giao", pageable);
        } else {
            // Mặc định: Đơn hàng chờ xác nhận (Đã bàn giao)
            deliveriesPage = vanChuyenService.getDeliveriesByShipperAndStatus(shipper.getMaNguoiDung(), "Đã bàn giao", pageable);
        }

        // Thống kê
        long pendingCount = vanChuyenService.countDeliveriesByShipperAndStatus(shipper.getMaNguoiDung(), "Đã bàn giao");
        long activeCount = vanChuyenService.countDeliveriesByShipperAndStatus(shipper.getMaNguoiDung(), "Đang giao");
        long completedCount = vanChuyenService.countDeliveriesByShipperAndStatus(shipper.getMaNguoiDung(), "Hoàn thành");
        long cancelledCount = vanChuyenService.countDeliveriesByShipperAndStatus(shipper.getMaNguoiDung(), "Hủy");

        model.addAttribute("deliveriesPage", deliveriesPage);
        model.addAttribute("deliveries", deliveriesPage.getContent());
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentTab", tab);
        model.addAttribute("currentPage", page);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("cancelledCount", cancelledCount);
        
        return "shipper/dashboard";
    }
    
 // ================== KIỂM TRA TRẠNG THÁI TÀI KHOẢN SHIPPER ==================
    @GetMapping("/check-account-status")
    @ResponseBody
    public ResponseEntity<?> checkAccountStatus(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            NguoiDung shipper = getCurrentShipper(authentication);
            
            if (shipper == null) {
                response.put("success", false);
                response.put("message", "Không tìm thấy thông tin shipper");
                return ResponseEntity.badRequest().body(response);
            }
            
            boolean isLocked = "Khóa".equals(shipper.getTrangThai());
            
            response.put("success", true);
            response.put("isLocked", isLocked);
            response.put("lyDoKhoa", shipper.getLyDoKhoa());
            response.put("tenNguoiDung", shipper.getTenNguoiDung());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ================== CHI TIẾT ĐƠN HÀNG ================== 
    @GetMapping("/order/{orderId}")
    public String orderDetail(@PathVariable Integer orderId, Authentication authentication, Model model) {
        NguoiDung shipper = getCurrentShipper(authentication);
        
        try {
            VanChuyen delivery = vanChuyenService.getDeliveryByOrderId(orderId);
            
            // Kiểm tra đơn hàng có thuộc về shipper này không
            if (!delivery.getNguoiDung().getMaNguoiDung().equals(shipper.getMaNguoiDung())) {
                model.addAttribute("error", "Bạn không có quyền truy cập đơn hàng này");
                return "shipper/error";
            }
            
            model.addAttribute("delivery", delivery);
            model.addAttribute("order", delivery.getDatHang());
            model.addAttribute("shipper", shipper);
            
            return "shipper/order-view";
            
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "shipper/error";
        }
    }

    // ================== CẬP NHẬT TRẠNG THÁI GIAO HÀNG ==================
    @PostMapping("/update-delivery-status")
    @ResponseBody
    public ResponseEntity<?> updateDeliveryStatus(
            @RequestParam Integer deliveryId,
            @RequestParam String newStatus,
            @RequestParam(required = false) String lyDoHuy, // THÊM THAM SỐ LÝ DO HỦY
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            NguoiDung shipper = getCurrentShipper(authentication);
            
            // Kiểm tra delivery có thuộc về shipper này không
            VanChuyen delivery = vanChuyenService.getDeliveryById(deliveryId);
            if (!delivery.getNguoiDung().getMaNguoiDung().equals(shipper.getMaNguoiDung())) {
                response.put("success", false);
                response.put("message", "Bạn không có quyền cập nhật đơn hàng này");
                return ResponseEntity.badRequest().body(response);
            }
            
            // KIỂM TRA LÝ DO HỦY NẾU TRẠNG THÁI LÀ "Hủy"
            if ("Hủy".equals(newStatus)) {
                if (lyDoHuy == null || lyDoHuy.trim().isEmpty()) {
                    response.put("success", false);
                    response.put("message", "Vui lòng nhập lý do hủy đơn hàng");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            // Cập nhật trạng thái
            VanChuyen updatedDelivery = vanChuyenService.updateDeliveryStatus(deliveryId, newStatus, lyDoHuy);
            
            response.put("success", true);
            response.put("message", "Cập nhật trạng thái thành công!");
            response.put("newStatus", updatedDelivery.getTrangThai());
            response.put("orderStatus", updatedDelivery.getDatHang().getTrangThai());
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ================== XÁC NHẬN NHẬN ĐƠN ==================
    @PostMapping("/confirm-delivery")
    @ResponseBody
    public ResponseEntity<?> confirmDelivery(
            @RequestParam Integer deliveryId,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            NguoiDung shipper = getCurrentShipper(authentication);
            
            // Kiểm tra delivery có thuộc về shipper này không
            VanChuyen delivery = vanChuyenService.getDeliveryById(deliveryId);
            if (!delivery.getNguoiDung().getMaNguoiDung().equals(shipper.getMaNguoiDung())) {
                response.put("success", false);
                response.put("message", "Bạn không có quyền xác nhận đơn hàng này");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Chỉ cho phép xác nhận đơn ở trạng thái "Đã bàn giao"
            if (!"Đã bàn giao".equals(delivery.getTrangThai())) {
                response.put("success", false);
                response.put("message", "Chỉ có thể xác nhận đơn hàng ở trạng thái 'Đã bàn giao'");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Xác nhận nhận đơn - chuyển trạng thái thành "Đang giao"
            VanChuyen updatedDelivery = vanChuyenService.updateDeliveryStatus(deliveryId, "Đang giao", null);
            
            response.put("success", true);
            response.put("message", "Xác nhận nhận đơn thành công! Đơn hàng đang được giao.");
            response.put("newStatus", updatedDelivery.getTrangThai());
            response.put("orderStatus", updatedDelivery.getDatHang().getTrangThai());
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ================== XÁC NHẬN TẤT CẢ ĐƠN HÀNG ==================
    @PostMapping("/confirm-all-deliveries")
    @ResponseBody
    public ResponseEntity<?> confirmAllDeliveries(
            @RequestParam String deliveryIds,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            NguoiDung shipper = getCurrentShipper(authentication);
            
            if (deliveryIds == null || deliveryIds.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Không có đơn hàng nào để xác nhận");
                return ResponseEntity.badRequest().body(response);
            }
            
            String[] ids = deliveryIds.split(",");
            int successCount = 0;
            List<String> errors = new ArrayList<>();
            
            for (String idStr : ids) {
                try {
                    Integer deliveryId = Integer.parseInt(idStr.trim());
                    VanChuyen delivery = vanChuyenService.getDeliveryById(deliveryId);
                    
                    // Kiểm tra quyền và trạng thái
                    if (delivery.getNguoiDung().getMaNguoiDung().equals(shipper.getMaNguoiDung()) 
                        && "Đã bàn giao".equals(delivery.getTrangThai())) {
                        vanChuyenService.updateDeliveryStatus(deliveryId, "Đang giao", null);
                        successCount++;
                    }
                } catch (Exception e) {
                    errors.add("Đơn hàng " + idStr + ": " + e.getMessage());
                }
            }
            
            response.put("success", true);
            response.put("message", "Đã xác nhận " + successCount + " đơn hàng thành công!");
            if (!errors.isEmpty()) {
                response.put("errors", errors);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ================== HỦY NHẬN ĐƠN ==================
    @PostMapping("/cancel-delivery")
    @ResponseBody
    public ResponseEntity<?> cancelDelivery(
            @RequestParam Integer deliveryId,
            @RequestParam String lyDoHuy,
            @RequestParam String reasonCategory,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            NguoiDung shipper = getCurrentShipper(authentication);
            
            // Kiểm tra delivery có thuộc về shipper này không
            VanChuyen delivery = vanChuyenService.getDeliveryById(deliveryId);
            if (!delivery.getNguoiDung().getMaNguoiDung().equals(shipper.getMaNguoiDung())) {
                response.put("success", false);
                response.put("message", "Bạn không có quyền hủy đơn hàng này");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Xác định hành động dựa trên phân loại lý do
            String action;
            if ("shipper".equals(reasonCategory)) {
                // Lý do thuộc về shipper - Chuyển về trạng thái cũ
                action = "RETURN_TO_VENDOR";
            } else {
                // Lý do thuộc về hệ thống - Hủy thật sự
                action = "COMPLETE_CANCELLATION";
            }
            
            // Gọi service xử lý
            VanChuyen updatedDelivery = vanChuyenService.processDeliveryCancellation(
                deliveryId, lyDoHuy, action);
            
            // Thông báo phù hợp
            String message;
            if ("RETURN_TO_VENDOR".equals(action)) {
                message = "Hủy nhận đơn thành công! Đơn hàng đã được chuyển về cửa hàng để chọn shipper khác.";
            } else {
                message = "Hủy đơn hàng thành công! Đơn hàng đã bị hủy hoàn toàn.";
            }
            
            response.put("success", true);
            response.put("message", message);
            response.put("newStatus", updatedDelivery.getTrangThai());
            response.put("orderStatus", updatedDelivery.getDatHang().getTrangThai());
            response.put("lyDoHuy", updatedDelivery.getLyDoHuy()); // Trả về lý do để hiển thị
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ================== HOÀN THÀNH ĐƠN HÀNG ==================
    @PostMapping("/complete-delivery")
    @ResponseBody
    public ResponseEntity<?> completeDelivery(
            @RequestParam Integer deliveryId,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            NguoiDung shipper = getCurrentShipper(authentication);
            
            // Kiểm tra delivery có thuộc về shipper này không
            VanChuyen delivery = vanChuyenService.getDeliveryById(deliveryId);
            if (!delivery.getNguoiDung().getMaNguoiDung().equals(shipper.getMaNguoiDung())) {
                response.put("success", false);
                response.put("message", "Bạn không có quyền hoàn thành đơn hàng này");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Hoàn thành đơn hàng
            VanChuyen updatedDelivery = vanChuyenService.updateDeliveryStatus(deliveryId, "Hoàn thành", null);
            
            response.put("success", true);
            response.put("message", "Xác nhận giao hàng thành công! Đơn hàng đã hoàn thành.");
            response.put("newStatus", updatedDelivery.getTrangThai());
            response.put("orderStatus", updatedDelivery.getDatHang().getTrangThai());
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ================== API CHO SHIPPER HỦY ĐƠN HÀNG VỚI LÝ DO ==================
    @PostMapping("/cancel-delivery-with-reason")
    @ResponseBody
    public ResponseEntity<?> cancelDeliveryWithReason(
            @RequestParam Integer deliveryId,
            @RequestParam String lyDoHuy,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            NguoiDung shipper = getCurrentShipper(authentication);
            
            // Kiểm tra delivery có thuộc về shipper này không
            VanChuyen delivery = vanChuyenService.getDeliveryById(deliveryId);
            if (!delivery.getNguoiDung().getMaNguoiDung().equals(shipper.getMaNguoiDung())) {
                response.put("success", false);
                response.put("message", "Bạn không có quyền hủy đơn hàng này");
                return ResponseEntity.badRequest().body(response);
            }
            
            // KIỂM TRA LÝ DO HỦY
            if (lyDoHuy == null || lyDoHuy.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Vui lòng nhập lý do hủy đơn hàng");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Hủy đơn hàng với lý do
            VanChuyen updatedDelivery = vanChuyenService.updateDeliveryStatus(deliveryId, "Hủy", lyDoHuy);
            
            response.put("success", true);
            response.put("message", "Hủy đơn hàng thành công!");
            response.put("newStatus", updatedDelivery.getTrangThai());
            response.put("orderStatus", updatedDelivery.getDatHang().getTrangThai());
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}