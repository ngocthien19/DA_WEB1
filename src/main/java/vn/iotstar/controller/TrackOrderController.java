package vn.iotstar.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import vn.iotstar.entity.DanhMuc;
import vn.iotstar.entity.DatHang;
import vn.iotstar.entity.DatHangChiTiet;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.service.DanhMucService;
import vn.iotstar.service.DatHangService;
import vn.iotstar.service.NguoiDungService;
import vn.iotstar.service.ThanhToanService;
import vn.iotstar.service.UserDetailsImpl;

@Controller
public class TrackOrderController {
	
	@Autowired
    private NguoiDungService nguoiDungService;
	
	@Autowired
    private DatHangService datHangService;
	
	@Autowired
    private DanhMucService danhMucService;
    
    @Autowired
    private ThanhToanService thanhToanService;
	
	@GetMapping("/track-order")
    public String trackOrder(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) { 
        
        try {
            // Kiểm tra đăng nhập
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication.getPrincipal().equals("anonymousUser")) {
                return "redirect:/login";
            }

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            String email = userDetails.getUsername();

            // Tìm người dùng
            NguoiDung nguoiDung = nguoiDungService.getUserByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            // Phân trang
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "maDatHang")); 
            
            // Lấy danh sách đơn hàng với phân trang
            Page<DatHang> ordersPage = datHangService.findByNguoiDung(nguoiDung, pageable);
            
            // Xử lý hình ảnh sản phẩm trong đơn hàng
            if (ordersPage != null && !ordersPage.getContent().isEmpty()) {
                for (DatHang order : ordersPage.getContent()) {
                    if (order.getDatHangChiTiets() != null) {
                        for (DatHangChiTiet detail : order.getDatHangChiTiets()) {
                            if (detail.getSanPham() != null && detail.getSanPham().getHinhAnh() != null) {
                                String hinhAnh = detail.getSanPham().getHinhAnh();
                                if (!hinhAnh.startsWith("/") && !hinhAnh.startsWith("http")) {
                                    detail.getSanPham().setHinhAnh("/" + hinhAnh);
                                }
                            }
                        }
                    }
                }
            }

            // Thêm danh mục vào model (cho header)
            List<DanhMuc> danhMucs = danhMucService.findAllActiveCategories();
            model.addAttribute("danhMucs", danhMucs);
            
            // ⭐ QUAN TRỌNG: Thêm người dùng vào model
            model.addAttribute("nguoiDung", nguoiDung);
            
            model.addAttribute("ordersPage", ordersPage);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", ordersPage.getTotalPages());

            return "web/track-order";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Có lỗi xảy ra khi tải trang theo dõi đơn hàng: " + e.getMessage());
            
            // Thêm danh mục vào model ngay cả khi có lỗi
            List<DanhMuc> danhMucs = danhMucService.findAllActiveCategories();
            model.addAttribute("danhMucs", danhMucs);
            
            return "web/track-order";
        }
    }
	
	// THÊM endpoint mới với URL /api/orders/{orderId}/cancel
	@PostMapping("/api/orders/{orderId}/cancels")
	public ResponseEntity<?> cancelOrderAPI(
	        @PathVariable Integer orderId,
	        @RequestBody Map<String, String> requestBody) {
	    
	    try {
	        String reason = requestBody.get("reason");
	        
	        System.out.println("🟡 Starting cancel order process for: " + orderId);
	        System.out.println("🟡 Reason: " + reason);
	        
	        // Lấy đơn hàng
	        DatHang datHang = datHangService.findByMaDatHang(orderId);
	        if (datHang == null) {
	            System.out.println("❌ Order not found: " + orderId);
	            return ResponseEntity.badRequest().body(Map.of(
	                "success", false,
	                "message", "Đơn hàng không tồn tại"
	            ));
	        }

	        // Kiểm tra trạng thái đơn hàng
	        if (!"Chờ xác nhận".equals(datHang.getTrangThai())) {
	            System.out.println("❌ Invalid order status: " + datHang.getTrangThai());
	            return ResponseEntity.badRequest().body(Map.of(
	                "success", false,
	                "message", "Chỉ có thể hủy đơn hàng đang chờ xác nhận"
	            ));
	        }

	        // Cập nhật trạng thái đơn hàng và lý do hủy
	        datHang.setTrangThai("Hủy");
	        datHang.setLyDoHuy(reason);
	        
	        System.out.println("🟡 Updating order status to 'Hủy'");
	        
	        // Lưu đơn hàng (sẽ kích hoạt syncDeliveryStatusWhenCancelled)
	        datHangService.save(datHang);
	        
	        System.out.println("🟡 Order saved, now updating payment status");
	        
	        // Cập nhật trạng thái thanh toán thành "cancelled" (theo database)
	        thanhToanService.updateTrangThaiByDatHang(orderId, "cancelled");

	        System.out.println("✅ Order cancellation completed successfully");
	        
	        return ResponseEntity.ok(Map.of(
	            "success", true,
	            "message", "Đã hủy đơn hàng thành công"
	        ));

	    } catch (Exception e) {
	        System.err.println("❌ Error cancelling order: " + e.getMessage());
	        e.printStackTrace();
	        return ResponseEntity.badRequest().body(Map.of(
	            "success", false,
	            "message", "Có lỗi xảy ra: " + e.getMessage()
	        ));
	    }
	}

	// GIỮ LẠI endpoint cũ để tương thích
	@PostMapping("/{orderId}/cancel")
	public ResponseEntity<?> cancelOrder(
	        @PathVariable Integer orderId,
	        @RequestBody Map<String, String> requestBody) {
	    return cancelOrderAPI(orderId, requestBody);
	}
	
	@GetMapping("/track-order/all")
	@ResponseBody
	@CrossOrigin(origins = "*") // THÊM DÒNG NÀY
	public List<DatHang> getAllUserOrders() {
	    try {
	        System.out.println("=== DEBUG: GET /track-order/all called ===");
	        
	        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
	        if (authentication == null || !authentication.isAuthenticated() || 
	            authentication.getPrincipal().equals("anonymousUser")) {
	            System.out.println("❌ User not authenticated");
	            return new ArrayList<>();
	        }

	        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
	        String email = userDetails.getUsername();
	        System.out.println("✅ User email: " + email);

	        NguoiDung nguoiDung = nguoiDungService.getUserByEmail(email)
	                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

	        // Lấy TẤT CẢ đơn hàng của người dùng (không phân trang)
	        List<DatHang> allOrders = datHangService.findByNguoiDung(nguoiDung);
	        
	        System.out.println("✅ Found " + allOrders.size() + " orders for user");
	        
	        // Xử lý hình ảnh sản phẩm
	        for (DatHang order : allOrders) {
	            if (order.getDatHangChiTiets() != null) {
	                for (DatHangChiTiet detail : order.getDatHangChiTiets()) {
	                    if (detail.getSanPham() != null && detail.getSanPham().getHinhAnh() != null) {
	                        String hinhAnh = detail.getSanPham().getHinhAnh();
	                        if (!hinhAnh.startsWith("/") && !hinhAnh.startsWith("http")) {
	                            detail.getSanPham().setHinhAnh("/" + hinhAnh);
	                        }
	                    }
	                }
	            }
	        }
	        
	        return allOrders;

	    } catch (Exception e) {
	        System.err.println("❌ Error in /track-order/all: " + e.getMessage());
	        e.printStackTrace();
	        return new ArrayList<>();
	    }
	}
}