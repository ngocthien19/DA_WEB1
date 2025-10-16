package vn.iotstar.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.iotstar.entity.DatHang;
import vn.iotstar.entity.ThanhToan;
import vn.iotstar.service.DatHangService;
import vn.iotstar.service.ThanhToanService;

import java.util.Date;

@Controller
@RequestMapping("/payment/mock")
@RequiredArgsConstructor
public class MockPaymentController {

    private final DatHangService datHangService;
    private final ThanhToanService thanhToanService;

    @GetMapping("/callback")
    public String mockCallback(@RequestParam Integer orderId,
                             @RequestParam String status,
                             @RequestParam(required = false) String method,
                             Model model) {
        try {
            DatHang datHang = datHangService.findByMaDatHang(orderId);
            if (datHang == null) {
                throw new RuntimeException("Không tìm thấy đơn hàng");
            }

            // Cập nhật trạng thái thanh toán
            ThanhToan thanhToan = thanhToanService.findByDatHang(orderId)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thanh toán"));

            if ("success".equals(status)) {
                // Thanh toán thành công
                thanhToan.setTrangThai("Completed");
                thanhToanService.save(thanhToan);

                // Cập nhật trạng thái đơn hàng
                datHang.setTrangThai("Đã thanh toán");
                datHangService.save(datHang);

                model.addAttribute("datHang", datHang);
                model.addAttribute("success", "Thanh toán thành công! (Mock)");
                model.addAttribute("paymentMethod", method != null ? method : "MOCK");

                return "web/order-success";

            } else {
                // Thanh toán thất bại
                thanhToan.setTrangThai("Failed");
                thanhToanService.save(thanhToan);

                model.addAttribute("datHang", datHang);
                model.addAttribute("error", "Thanh toán thất bại! (Mock)");

                return "web/payment-error";
            }

        } catch (Exception e) {
            model.addAttribute("error", "Lỗi mock payment: " + e.getMessage());
            return "web/payment-error";
        }
    }
}