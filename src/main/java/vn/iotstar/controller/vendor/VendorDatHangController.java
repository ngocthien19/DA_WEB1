package vn.iotstar.controller.vendor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.entity.CuaHang;
import vn.iotstar.entity.DatHang;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.service.CuaHangService;
import vn.iotstar.service.DatHangService;
import vn.iotstar.service.ThanhToanService;
import vn.iotstar.service.impl.UserDetailsServiceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Controller
@RequestMapping("/vendor/orders")
@PreAuthorize("hasRole('VENDOR')")
public class VendorDatHangController {

    @Autowired
    private DatHangService datHangService;
    
    @Autowired
    private CuaHangService cuaHangService;
    
    @Autowired
    private UserDetailsServiceImpl userDetailsService;
    
    @Autowired
    private ThanhToanService thanhToanService;

    private CuaHang getCurrentStore(Authentication authentication) {
        NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
        if (currentUser == null) return null;
        
        List<CuaHang> stores = cuaHangService.findByNguoiDung(currentUser);
        return stores.isEmpty() ? null : stores.get(0);
    }
    
    private NguoiDung getCurrentUser(Authentication authentication) {
        return userDetailsService.getCurrentUser(authentication);
    }

    @GetMapping
    public String listDatHang(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String dateFilter,
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            Authentication authentication,
            Model model) {
        
        NguoiDung currentUser = getCurrentUser(authentication);
        CuaHang cuaHang = getCurrentStore(authentication);
        
        model.addAttribute("user", currentUser);

        if (cuaHang == null) {
            return "redirect:/vendor/dashboard"; 
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "maDatHang"));
        
        // FIX: Xử lý trạng thái rỗng thành null
        if (trangThai != null && trangThai.isEmpty()) {
            trangThai = null;
        }
        
        // Xử lý bộ lọc ngày (chỉ dùng cho dateFilter cũ, nay đã được thay thế bằng startDate/endDate)
        // BỎ QUA dateFilter logic nếu startDate/endDate đã có giá trị
        
        Page<DatHang> datHangPage = datHangService.findByCuaHangAndFilters(
            cuaHang, keyword, startDate, endDate, trangThai, pageable);
        
        // Thống kê đơn hàng
        Map<String, Long> orderStats = new HashMap<>();
        orderStats.put("pending", datHangService.countByCuaHangAndTrangThai(cuaHang, "Chờ xác nhận"));
        orderStats.put("confirmed", datHangService.countByCuaHangAndTrangThai(cuaHang, "Đã xác nhận"));
        orderStats.put("shipping", datHangService.countByCuaHangAndTrangThai(cuaHang, "Đang giao"));
        orderStats.put("completed", datHangService.countByCuaHangAndTrangThai(cuaHang, "Hoàn thành"));
        orderStats.put("cancelled", datHangService.countByCuaHangAndTrangThai(cuaHang, "Hủy"));
        
        model.addAttribute("datHangPage", datHangPage);
        model.addAttribute("cuaHang", cuaHang);
        model.addAttribute("keyword", keyword);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("trangThai", trangThai);
        model.addAttribute("currentPage", page);
        model.addAttribute("orderStats", orderStats);
        
        return "vendor/orders/orders";
    }

    @GetMapping("/view/{id}")
    public String viewDatHang(@PathVariable Integer id, Authentication authentication, Model model) {
        NguoiDung currentUser = getCurrentUser(authentication);
        CuaHang cuaHang = getCurrentStore(authentication);
        
        model.addAttribute("user", currentUser);

        if (cuaHang == null) {
            return "redirect:/vendor/dashboard";
        }
        
        DatHang datHang = datHangService.findByMaDatHang(id);
        if (datHang == null || !isOrderBelongsToStore(datHang, cuaHang)) {
            return "redirect:/vendor/orders";
        }
        
        model.addAttribute("datHang", datHang);
        model.addAttribute("cuaHang", cuaHang);
        return "vendor/orders/order-view";
    }

    @PostMapping("/update-status/{id}")
    @ResponseBody
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Integer id,
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
            
            DatHang datHang = datHangService.findByMaDatHang(id);
            if (datHang == null || !isOrderBelongsToStore(datHang, cuaHang)) {
                response.put("success", false);
                response.put("message", "Đơn hàng không tồn tại hoặc không thuộc về cửa hàng của bạn");
                return ResponseEntity.badRequest().body(response);
            }
            
            // KIỂM TRA: Nếu đơn hàng đã hoàn thành hoặc đã hủy thì không cho phép cập nhật
            if ("Hoàn thành".equals(datHang.getTrangThai()) || "Hủy".equals(datHang.getTrangThai())) {
                response.put("success", false);
                response.put("message", "Không thể cập nhật trạng thái đơn hàng đã " + datHang.getTrangThai().toLowerCase());
                return ResponseEntity.badRequest().body(response);
            }
            
            // KIỂM TRA: Nếu đơn hàng đang giao, không cho phép chuyển về trạng thái trước đó
            if ("Đang giao".equals(datHang.getTrangThai()) && 
                ("Chờ xác nhận".equals(newStatus) || "Đã xác nhận".equals(newStatus))) {
                response.put("success", false);
                response.put("message", "Không thể chuyển đơn hàng đang giao về trạng thái '" + newStatus + "'");
                return ResponseEntity.badRequest().body(response);
            }
            
            String oldStatus = datHang.getTrangThai();
            
            // Lưu lý do hủy nếu có
            if ("Hủy".equals(newStatus) && lyDoHuy != null && !lyDoHuy.trim().isEmpty()) {
                datHang.setLyDoHuy(lyDoHuy.trim());
            } else if (!"Hủy".equals(newStatus)) {
                // Xóa lý do hủy nếu chuyển sang trạng thái khác
                datHang.setLyDoHuy(null);
            }
            
            datHang.setTrangThai(newStatus);
            DatHang updatedDatHang = datHangService.save(datHang); // Sử dụng save để kích hoạt đồng bộ hóa
            
            // THÊM: Gọi đồng bộ hóa vận chuyển khi hủy đơn hàng
            if ("Hủy".equals(newStatus) && !"Hủy".equals(oldStatus)) {
                try {
                    // Gọi trực tiếp service để đồng bộ hóa vận chuyển
                    datHangService.syncDeliveryStatusWhenCancelled(id, datHang.getLyDoHuy());
                    response.put("deliverySynced", true);
                    response.put("deliveryMessage", "Trạng thái vận chuyển đã được tự động đồng bộ sang 'Hủy'");
                    
                    // THÊM: Cập nhật trạng thái thanh toán khi hủy đơn hàng
                    try {
                        thanhToanService.updateTrangThaiByDatHang(id, "cancelled");
                        response.put("paymentUpdated", true);
                        response.put("paymentMessage", "Trạng thái thanh toán đã được tự động cập nhật sang 'cancelled'");
                    } catch (Exception e) {
                        response.put("paymentUpdated", false);
                        response.put("paymentMessage", "Cảnh báo: Không thể cập nhật trạng thái thanh toán: " + e.getMessage());
                    }
                    
                } catch (Exception e) {
                    response.put("deliverySynced", false);
                    response.put("deliveryMessage", "Cảnh báo: Không thể đồng bộ trạng thái vận chuyển: " + e.getMessage());
                }
            }
            
            // Tự động cập nhật trạng thái thanh toán nếu đơn hàng chuyển sang "Hoàn thành"
            if ("Hoàn thành".equals(newStatus) && !"Hoàn thành".equals(oldStatus)) {
                try {
                    thanhToanService.updateTrangThaiByDatHang(id, "completed");
                    response.put("paymentUpdated", true);
                    response.put("paymentMessage", "Trạng thái thanh toán đã được tự động cập nhật sang 'completed'");
                } catch (Exception e) {
                    response.put("paymentUpdated", false);
                    response.put("paymentMessage", "Cảnh báo: Không thể cập nhật trạng thái thanh toán: " + e.getMessage());
                }
            }
            
            response.put("success", true);
            response.put("message", "Cập nhật trạng thái đơn hàng thành công!");
            response.put("newStatus", newStatus);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteDatHang(@PathVariable Integer id, Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            CuaHang cuaHang = getCurrentStore(authentication);
            if (cuaHang == null) {
                response.put("success", false);
                response.put("message", "Bạn chưa đăng nhập hoặc chưa có cửa hàng.");
                return ResponseEntity.badRequest().body(response);
            }

            DatHang datHang = datHangService.findByMaDatHang(id);
            if (datHang == null || !isOrderBelongsToStore(datHang, cuaHang)) {
                response.put("success", false);
                response.put("message", "Đơn hàng không tồn tại hoặc không thuộc về cửa hàng của bạn");
                return ResponseEntity.badRequest().body(response);
            }
            
            datHangService.deleteDatHang(id);
            
            response.put("success", true);
            response.put("message", "Xóa đơn hàng thành công");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/bulk-update-status")
    @ResponseBody
    public ResponseEntity<?> bulkUpdateStatus(
            @RequestParam("ids") List<Integer> ids,
            @RequestParam String newStatus,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            CuaHang cuaHang = getCurrentStore(authentication);
            if (cuaHang == null) {
                response.put("success", false);
                response.put("message", "Bạn chưa đăng nhập hoặc chưa có cửa hàng.");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (ids == null || ids.isEmpty()) {
                response.put("success", false);
                response.put("message", "Vui lòng chọn ít nhất một đơn hàng để cập nhật trạng thái");
                return ResponseEntity.badRequest().body(response);
            }
            
            int successCount = 0;
            int errorCount = 0;
            int paymentUpdatedCount = 0;
            int paymentCancelledCount = 0; // THÊM: Đếm số thanh toán bị hủy
            List<String> errorMessages = new ArrayList<>();
            
            for (Integer id : ids) {
                try {
                    DatHang datHang = datHangService.findByMaDatHang(id);
                    if (datHang != null && isOrderBelongsToStore(datHang, cuaHang)) {
                        // KIỂM TRA: Nếu đơn hàng đã hoàn thành hoặc đã hủy thì bỏ qua
                        if ("Hoàn thành".equals(datHang.getTrangThai()) || "Hủy".equals(datHang.getTrangThai())) {
                            errorCount++;
                            errorMessages.add("Đơn hàng ID " + id + " đã " + datHang.getTrangThai().toLowerCase() + ", không thể cập nhật trạng thái");
                            continue;
                        }
                        
                        // KIỂM TRA: Nếu đơn hàng đang giao, không cho phép chuyển về trạng thái trước đó
                        if ("Đang giao".equals(datHang.getTrangThai()) && 
                            ("Chờ xác nhận".equals(newStatus) || "Đã xác nhận".equals(newStatus))) {
                            errorCount++;
                            errorMessages.add("Đơn hàng ID " + id + " đang giao, không thể chuyển về trạng thái '" + newStatus + "'");
                            continue;
                        }
                        
                        String oldStatus = datHang.getTrangThai();
                        datHang.setTrangThai(newStatus);
                        datHangService.save(datHang);
                        
                        // Tự động cập nhật trạng thái thanh toán nếu chuyển sang "Hoàn thành"
                        if ("Hoàn thành".equals(newStatus) && !"Hoàn thành".equals(oldStatus)) {
                            try {
                                thanhToanService.updateTrangThaiByDatHang(id, "completed");
                                paymentUpdatedCount++;
                            } catch (Exception e) {
                                errorMessages.add("Cảnh báo: Không thể cập nhật thanh toán cho đơn hàng ID " + id);
                            }
                        }
                        
                        // THÊM: Tự động cập nhật trạng thái thanh toán nếu chuyển sang "Hủy"
                        if ("Hủy".equals(newStatus) && !"Hủy".equals(oldStatus)) {
                            try {
                                thanhToanService.updateTrangThaiByDatHang(id, "cancelled");
                                paymentCancelledCount++;
                            } catch (Exception e) {
                                errorMessages.add("Cảnh báo: Không thể cập nhật thanh toán cho đơn hàng ID " + id);
                            }
                        }
                        
                        if ("Hủy".equals(newStatus) && !"Hủy".equals(oldStatus)) {
                            try {
                                datHangService.syncDeliveryStatusWhenCancelled(id, null); // hoặc lyDoHuy nếu có
                            } catch (Exception e) {
                                errorMessages.add("Cảnh báo: Không thể đồng bộ vận chuyển cho đơn hàng ID " + id);
                            }
                        }
                        
                        successCount++;
                    } else {
                        errorCount++;
                        errorMessages.add("Đơn hàng ID " + id + " không tồn tại hoặc không thuộc về cửa hàng của bạn");
                    }
                } catch (Exception e) {
                    errorCount++;
                    errorMessages.add("Lỗi khi cập nhật đơn hàng ID " + id + ": " + e.getMessage());
                }
            }
            
            response.put("success", true);
            response.put("message", "Đã cập nhật trạng thái cho " + successCount + " đơn hàng thành công");
            if (paymentUpdatedCount > 0) {
                response.put("paymentMessage", "Đã tự động cập nhật trạng thái thanh toán sang 'completed' cho " + paymentUpdatedCount + " đơn hàng");
            }
            if (paymentCancelledCount > 0) {
                response.put("paymentCancelledMessage", "Đã tự động cập nhật trạng thái thanh toán sang 'cancelled' cho " + paymentCancelledCount + " đơn hàng");
            }
            response.put("successCount", successCount);
            response.put("errorCount", errorCount);
            response.put("paymentUpdatedCount", paymentUpdatedCount);
            response.put("paymentCancelledCount", paymentCancelledCount);
            response.put("errors", errorMessages);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }


    @PostMapping("/bulk-delete")
    @ResponseBody
    public ResponseEntity<?> bulkDelete(
            @RequestParam("ids") List<Integer> ids,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            CuaHang cuaHang = getCurrentStore(authentication);
            if (cuaHang == null) {
                response.put("success", false);
                response.put("message", "Bạn chưa đăng nhập hoặc chưa có cửa hàng.");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (ids == null || ids.isEmpty()) {
                response.put("success", false);
                response.put("message", "Vui lòng chọn ít nhất một đơn hàng để xóa");
                return ResponseEntity.badRequest().body(response);
            }
            
            int successCount = 0;
            int errorCount = 0;
            List<String> errorMessages = new ArrayList<>();
            
            for (Integer id : ids) {
                try {
                    DatHang datHang = datHangService.findByMaDatHang(id);
                    if (datHang != null && isOrderBelongsToStore(datHang, cuaHang)) {
                        datHangService.deleteDatHang(id);
                        successCount++;
                    } else {
                        errorCount++;
                        errorMessages.add("Đơn hàng ID " + id + " không tồn tại hoặc không thuộc về cửa hàng của bạn");
                    }
                } catch (Exception e) {
                    errorCount++;
                    errorMessages.add("Lỗi khi xóa đơn hàng ID " + id + ": " + e.getMessage());
                }
            }
            
            response.put("success", true);
            response.put("message", "Đã xóa " + successCount + " đơn hàng thành công");
            response.put("successCount", successCount);
            response.put("errorCount", errorCount);
            response.put("errors", errorMessages);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    private boolean isOrderBelongsToStore(DatHang datHang, CuaHang cuaHang) {
        // Kiểm tra xem đơn hàng có sản phẩm thuộc cửa hàng không
        return datHang.getDatHangChiTiets().stream()
                .anyMatch(chiTiet -> chiTiet.getSanPham().getCuaHang().getMaCuaHang().equals(cuaHang.getMaCuaHang()));
    }
}