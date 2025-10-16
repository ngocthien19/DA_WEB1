package vn.iotstar.controller.admin;

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
import vn.iotstar.service.NguoiDungService;
import vn.iotstar.service.ThanhToanService;
import vn.iotstar.service.VanChuyenService;
import vn.iotstar.service.impl.UserDetailsServiceImpl;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Controller
@RequestMapping("/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDatHangController {

    @Autowired
    private DatHangService datHangService;
    
    @Autowired
    private CuaHangService cuaHangService;
    
    @Autowired
    private NguoiDungService nguoiDungService;
    
    @Autowired
    private ThanhToanService thanhToanService;
    
    @Autowired
    private VanChuyenService vanChuyenService;
    
    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @GetMapping
    public String listDatHang(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) Integer maCuaHang,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            Authentication authentication,
            Model model) {
        
        // Lấy thông tin user hiện tại
        NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
        model.addAttribute("user", currentUser);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "maDatHang"));
        
        // Xử lý trạng thái rỗng thành null
        if (trangThai != null && trangThai.isEmpty()) {
            trangThai = null;
        }
        
        // SỬ DỤNG TÊN MỚI
        Page<DatHang> datHangPage = datHangService.findAllOrdersWithFilters(
            keyword, startDate, endDate, trangThai, maCuaHang, pageable);
        
        // Lấy danh sách cửa hàng cho filter
        List<CuaHang> cuaHangs = cuaHangService.findActiveStores();
        
     // Tìm tên cửa hàng được chọn (nếu có)
        String selectedStoreName = "Unknown";
        if (maCuaHang != null) {
            selectedStoreName = cuaHangs.stream()
                    .filter(store -> store.getMaCuaHang().equals(maCuaHang))
                    .findFirst()
                    .map(CuaHang::getTenCuaHang)
                    .orElse("Unknown (ID: " + maCuaHang + ")");
        }
        
        // Thống kê đơn hàng tổng quan
        Map<String, Long> orderStats = new HashMap<>();
        orderStats.put("total", datHangService.countAllOrders());
        orderStats.put("pending", countOrdersByStatus("Chờ xác nhận"));
        orderStats.put("confirmed", countOrdersByStatus("Đã xác nhận"));
        orderStats.put("shipping", countOrdersByStatus("Đang giao"));
        orderStats.put("completed", countOrdersByStatus("Hoàn thành"));
        orderStats.put("cancelled", countOrdersByStatus("Hủy"));
         
        model.addAttribute("datHangPage", datHangPage);
        model.addAttribute("cuaHangs", cuaHangs);
        model.addAttribute("keyword", keyword);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("trangThai", trangThai);
        model.addAttribute("maCuaHang", maCuaHang);
        model.addAttribute("selectedStoreName", selectedStoreName);
        model.addAttribute("currentPage", page);
        model.addAttribute("orderStats", orderStats);
        
        return "admin/orders/orders";
    }

    @GetMapping("/view/{id}")
    public String viewDatHang(@PathVariable Integer id, Authentication authentication, Model model) {
        // Lấy thông tin user hiện tại
        NguoiDung currentUser = userDetailsService.getCurrentUser(authentication);
        model.addAttribute("user", currentUser);
        
        DatHang datHang = datHangService.findByMaDatHang(id);
        if (datHang == null) {
            return "redirect:/admin/orders";
        }
        
        model.addAttribute("datHang", datHang);
        return "admin/orders/order-view";
    }
    
    private NguoiDung getCurrentUser(Authentication authentication) {
        return userDetailsService.getCurrentUser(authentication);
    }

    @PostMapping("/update-status/{id}")
    @ResponseBody
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Integer id,
            @RequestParam String newStatus,
            @RequestParam(required = false) String lyDoHuy) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            DatHang datHang = datHangService.findByMaDatHang(id);
            if (datHang == null) {
                response.put("success", false);
                response.put("message", "Đơn hàng không tồn tại");
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
            DatHang updatedDatHang = datHangService.save(datHang);
            
            // XỬ LÝ KHI HỦY ĐƠN HÀNG: Cập nhật thanh toán và vận chuyển
            if ("Hủy".equals(newStatus) && !"Hủy".equals(oldStatus)) {
                try {
                    // 1. Cập nhật trạng thái thanh toán sang "cancelled"
                    thanhToanService.updateTrangThaiByDatHang(id, "cancelled");
                    
                    // 2. Đồng bộ hóa trạng thái vận chuyển
                    datHangService.syncDeliveryStatusWhenCancelled(id, datHang.getLyDoHuy());
                    
                    response.put("paymentUpdated", true);
                    response.put("deliverySynced", true);
                    response.put("paymentMessage", "Trạng thái thanh toán đã được tự động cập nhật sang 'cancelled'");
                    response.put("deliveryMessage", "Trạng thái vận chuyển đã được tự động đồng bộ sang 'Hủy'");
                    
                } catch (Exception e) {
                    response.put("paymentUpdated", false);
                    response.put("deliverySynced", false);
                    response.put("paymentMessage", "Cảnh báo: Không thể cập nhật trạng thái thanh toán: " + e.getMessage());
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
    public ResponseEntity<?> deleteDatHang(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            DatHang datHang = datHangService.findByMaDatHang(id);
            if (datHang == null) {
                response.put("success", false);
                response.put("message", "Đơn hàng không tồn tại");
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
            @RequestParam(required = false) String lyDoHuy) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (ids == null || ids.isEmpty()) {
                response.put("success", false);
                response.put("message", "Vui lòng chọn ít nhất một đơn hàng để cập nhật trạng thái");
                return ResponseEntity.badRequest().body(response);
            }
            
            int successCount = 0;
            int errorCount = 0;
            int paymentUpdatedCount = 0;
            int deliverySyncedCount = 0;
            List<String> errorMessages = new ArrayList<>();
            
            for (Integer id : ids) {
                try {
                    DatHang datHang = datHangService.findByMaDatHang(id);
                    if (datHang != null) {
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
                        
                        // Lưu lý do hủy nếu có
                        if ("Hủy".equals(newStatus) && lyDoHuy != null && !lyDoHuy.trim().isEmpty()) {
                            datHang.setLyDoHuy(lyDoHuy.trim());
                        } else if (!"Hủy".equals(newStatus)) {
                            // Xóa lý do hủy nếu chuyển sang trạng thái khác
                            datHang.setLyDoHuy(null);
                        }
                        
                        datHang.setTrangThai(newStatus);
                        datHangService.save(datHang);
                        
                        // XỬ LÝ KHI HỦY ĐƠN HÀNG: Cập nhật thanh toán và vận chuyển
                        if ("Hủy".equals(newStatus) && !"Hủy".equals(oldStatus)) {
                            try {
                                // 1. Cập nhật trạng thái thanh toán sang "cancelled"
                                thanhToanService.updateTrangThaiByDatHang(id, "cancelled");
                                paymentUpdatedCount++;
                                
                                // 2. Đồng bộ hóa trạng thái vận chuyển
                                datHangService.syncDeliveryStatusWhenCancelled(id, datHang.getLyDoHuy());
                                deliverySyncedCount++;
                                
                            } catch (Exception e) {
                                errorMessages.add("Cảnh báo: Không thể cập nhật thanh toán/vận chuyển cho đơn hàng ID " + id);
                            }
                        }
                        
                        // Tự động cập nhật trạng thái thanh toán nếu chuyển sang "Hoàn thành"
                        if ("Hoàn thành".equals(newStatus) && !"Hoàn thành".equals(oldStatus)) {
                            try {
                                thanhToanService.updateTrangThaiByDatHang(id, "completed");
                                paymentUpdatedCount++;
                            } catch (Exception e) {
                                errorMessages.add("Cảnh báo: Không thể cập nhật thanh toán cho đơn hàng ID " + id);
                            }
                        }
                        
                        successCount++;
                    } else {
                        errorCount++;
                        errorMessages.add("Đơn hàng ID " + id + " không tồn tại");
                    }
                } catch (Exception e) {
                    errorCount++;
                    errorMessages.add("Lỗi khi cập nhật đơn hàng ID " + id + ": " + e.getMessage());
                }
            }
            
            response.put("success", true);
            response.put("message", "Đã cập nhật trạng thái cho " + successCount + " đơn hàng thành công");
            if (paymentUpdatedCount > 0) {
                response.put("paymentMessage", "Đã tự động cập nhật trạng thái thanh toán cho " + paymentUpdatedCount + " đơn hàng");
            }
            if (deliverySyncedCount > 0) {
                response.put("deliveryMessage", "Đã tự động đồng bộ trạng thái vận chuyển cho " + deliverySyncedCount + " đơn hàng");
            }
            response.put("successCount", successCount);
            response.put("errorCount", errorCount);
            response.put("paymentUpdatedCount", paymentUpdatedCount);
            response.put("deliverySyncedCount", deliverySyncedCount);
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
    public ResponseEntity<?> bulkDelete(@RequestParam("ids") List<Integer> ids) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
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
                    if (datHang != null) {
                        datHangService.deleteDatHang(id);
                        successCount++;
                    } else {
                        errorCount++;
                        errorMessages.add("Đơn hàng ID " + id + " không tồn tại");
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

    /**
     * Đếm số lượng đơn hàng theo trạng thái
     */
    private long countOrdersByStatus(String trangThai) {
        // Cần thêm phương thức này trong DatHangService
        // Tạm thời sử dụng cách lấy tất cả rồi filter
        List<DatHang> allOrders = datHangService.findAll();
        return allOrders.stream()
                .filter(order -> trangThai.equals(order.getTrangThai()))
                .count();
    }
}