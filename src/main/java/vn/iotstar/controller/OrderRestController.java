package vn.iotstar.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import vn.iotstar.entity.DatHang;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.model.ApiResponse;
import vn.iotstar.service.DatHangService;
import vn.iotstar.service.NguoiDungService;
import vn.iotstar.service.UserDetailsImpl;

@RestController
@RequestMapping("/api/orders") 
public class OrderRestController {

    @Autowired
    private DatHangService datHangService;
    
    @Autowired
    private NguoiDungService nguoiDungService;

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<String>> cancelOrder(@PathVariable Integer orderId) {
        try {
            // Kiểm tra đăng nhập
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.ok(ApiResponse.<String>builder()
                        .success(false)
                        .message("Vui lòng đăng nhập")
                        .build());
            }

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            String email = userDetails.getUsername();

            NguoiDung nguoiDung = nguoiDungService.getUserByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            // Lấy đơn hàng
            DatHang datHang = datHangService.findByMaDatHang(orderId);
            if (datHang == null) {
                return ResponseEntity.ok(ApiResponse.<String>builder()
                        .success(false)
                        .message("Không tìm thấy đơn hàng")
                        .build());
            }

            // Kiểm tra quyền sở hữu
            if (!datHang.getNguoiDung().getMaNguoiDung().equals(nguoiDung.getMaNguoiDung())) {
                return ResponseEntity.ok(ApiResponse.<String>builder()
                        .success(false)
                        .message("Bạn không có quyền hủy đơn hàng này")
                        .build());
            }

            // Kiểm tra trạng thái (chỉ cho phép hủy khi "Chờ xác nhận")
            if (!"Chờ xác nhận".equals(datHang.getTrangThai())) {
                return ResponseEntity.ok(ApiResponse.<String>builder()
                        .success(false)
                        .message("Chỉ có thể hủy đơn hàng khi đang chờ xác nhận")
                        .build());
            }

            // Cập nhật trạng thái
            datHang.setTrangThai("Đã hủy");
            datHangService.save(datHang);

            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .success(true)
                    .message("Đã hủy đơn hàng thành công")
                    .data("Đơn hàng #" + orderId + " đã được hủy")
                    .build());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .success(false)
                    .message("Có lỗi xảy ra: " + e.getMessage())
                    .build());
        }
    }
}