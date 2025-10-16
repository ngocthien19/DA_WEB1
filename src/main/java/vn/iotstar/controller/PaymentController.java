package vn.iotstar.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.iotstar.entity.DatHang;
import vn.iotstar.entity.ThanhToan;
import vn.iotstar.service.DatHangService;
import vn.iotstar.service.PaymentService;
import vn.iotstar.service.ThanhToanService;
import vn.iotstar.service.factory.PaymentServiceFactory;
import vn.iotstar.service.impl.QRCodeService;

import java.util.Date;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final DatHangService datHangService;
    private final ThanhToanService thanhToanService;
    private final PaymentServiceFactory paymentServiceFactory;
    private final QRCodeService qrCodeService;

    @GetMapping("/process")
    public String processPayment(@RequestParam Integer orderId, 
                               @RequestParam String method, 
                               Model model) {
        try {
            DatHang datHang = datHangService.findByMaDatHang(orderId);
            if (datHang == null) {
                throw new RuntimeException("Đơn hàng không tồn tại");
            }

            // Tạo thanh toán với trạng thái "Processing"
            ThanhToan thanhToan = ThanhToan.builder()
                    .datHang(datHang)
                    .phuongThuc(method)
                    .soTienThanhToan(datHang.getTongTien())
                    .ngayThanhToan(new Date())
                    .trangThai("Processing")
                    .build();
            thanhToanService.save(thanhToan);

            // Sử dụng PaymentService thực
            PaymentService paymentService = paymentServiceFactory.getPaymentService(method);
            String paymentUrl = paymentService.createPayment(datHang);

            // Tạo QR code từ URL thanh toán thực
            String qrCodeBase64 = null;
            if ("MOMO".equals(method)) {
                qrCodeBase64 = qrCodeService.generateMomoPaymentQRCode(datHang, paymentUrl);
            } else if ("VNPAY".equals(method)) {
                qrCodeBase64 = qrCodeService.generateVNPayPaymentQRCode(datHang, paymentUrl);
            }

            // Fallback nếu không tạo được QR code
            if (qrCodeBase64 == null) {
                qrCodeBase64 = generateFallbackQRCode(datHang, method);
            }

            model.addAttribute("paymentUrl", paymentUrl);
            model.addAttribute("datHang", datHang);
            model.addAttribute("phuongThuc", method);
            model.addAttribute("amount", datHang.getTongTien());
            model.addAttribute("description", "Thanh toán đơn hàng #" + orderId);
            model.addAttribute("qrCode", qrCodeBase64);
            
            return "web/payment-process";

        } catch (Exception e) {
            log.error("Error processing payment: ", e);
            model.addAttribute("error", "Lỗi khi xử lý thanh toán: " + e.getMessage());
            return "web/payment-error";
        }
    }

    // Thêm method fallback
    private String generateFallbackQRCode(DatHang datHang, String method) {
        try {
            String simpleText = String.format("Payment Info - Order: %s - Amount: %s - Method: %s",
                    datHang.getMaDatHang(), 
                    datHang.getTongTien(), 
                    method);
            return qrCodeService.generateQRCodeBase64(simpleText, 300, 300);
        } catch (Exception e) {
            log.error("Fallback QR code generation also failed: ", e);
            return null;
        }
    }

    @GetMapping("/qr-payment")
    public String showQRPayment(@RequestParam Integer orderId, 
                              @RequestParam String method,
                              Model model) {
        try {
            DatHang datHang = datHangService.findByMaDatHang(orderId);
            if (datHang == null) {
                throw new RuntimeException("Đơn hàng không tồn tại");
            }

            // Sử dụng PaymentService để lấy URL thanh toán thực
            PaymentService paymentService = paymentServiceFactory.getPaymentService(method);
            String paymentUrl = paymentService.createPayment(datHang);

            String qrCodeBase64 = null;
            if ("MOMO".equals(method)) {
                qrCodeBase64 = qrCodeService.generateMomoPaymentQRCode(datHang, paymentUrl);
            } else if ("VNPAY".equals(method)) {
                qrCodeBase64 = qrCodeService.generateVNPayPaymentQRCode(datHang, paymentUrl);
            } else {
                throw new RuntimeException("Phương thức thanh toán không hỗ trợ QR code");
            }

            if (qrCodeBase64 == null) {
                throw new RuntimeException("Không thể tạo mã QR");
            }

            model.addAttribute("qrCode", qrCodeBase64);
            model.addAttribute("amount", datHang.getTongTien());
            model.addAttribute("description", "Thanh toán đơn hàng #" + orderId);
            model.addAttribute("method", method);
            model.addAttribute("title", "Quét mã để thanh toán bằng " + method);
            model.addAttribute("orderId", orderId);
            model.addAttribute("paymentUrl", paymentUrl);
            
            return "web/qr-payment";
            
        } catch (Exception e) {
            model.addAttribute("error", "Không thể tạo mã QR: " + e.getMessage());
            return "web/error";
        }
    }

    // Callback từ MoMo
    @GetMapping("/momo/callback")
    public String momoCallback(@RequestParam Map<String, String> params, Model model) {
        try {
            log.info("MoMo callback received: {}", params);
            
            PaymentService momoService = paymentServiceFactory.getPaymentService("MOMO");
            
            if (momoService.verifyPayment(params)) {
                String resultCode = params.get("resultCode");
                String orderId = params.get("orderId");
                
                log.info("MoMo callback - Order: {}, Result: {}", orderId, resultCode);
                
                if ("0".equals(resultCode)) {
                    // Thanh toán thành công
                    return handlePaymentSuccess(Integer.parseInt(orderId), "MOMO", model);
                } else {
                    // Thanh toán thất bại
                    return handlePaymentFailure(Integer.parseInt(orderId), "MOMO", 
                            params.get("message"), model);
                }
            } else {
                log.error("MoMo signature verification failed");
                throw new RuntimeException("Chữ ký không hợp lệ");
            }
            
        } catch (Exception e) {
            log.error("Error processing MoMo callback: ", e);
            model.addAttribute("error", "Lỗi xác thực thanh toán MoMo: " + e.getMessage());
            return "web/payment-error";
        }
    }

    // IPN URL cho MoMo (cho server-to-server)
    @PostMapping("/momo/ipn")
    public ResponseEntity<String> momoIPN(@RequestBody Map<String, String> params) {
        try {
            log.info("MoMo IPN received: {}", params);
            
            PaymentService momoService = paymentServiceFactory.getPaymentService("MOMO");
            
            if (momoService.verifyPayment(params)) {
                String resultCode = params.get("resultCode");
                String orderId = params.get("orderId");
                
                log.info("MoMo IPN - Order: {}, Result: {}", orderId, resultCode);
                
                if ("0".equals(resultCode)) {
                    // Cập nhật trạng thái thanh toán
                    handlePaymentSuccess(Integer.parseInt(orderId), "MOMO");
                }
                
                return ResponseEntity.ok("OK");
            } else {
                log.error("MoMo IPN signature verification failed");
                return ResponseEntity.badRequest().body("Invalid signature");
            }
            
        } catch (Exception e) {
            log.error("Error processing MoMo IPN: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
        }
    }

    // Callback từ VNPay
    @GetMapping("/vnpay/callback")
    public String vnpayCallback(@RequestParam Map<String, String> params, Model model) {
        try {
            log.info("VNPay callback received: {}", params);
            
            PaymentService vnpayService = paymentServiceFactory.getPaymentService("VNPAY");
            
            if (vnpayService.verifyPayment(params)) {
                String responseCode = params.get("vnp_ResponseCode");
                String orderId = params.get("vnp_TxnRef");
                
                log.info("VNPay callback - Order: {}, Response: {}", orderId, responseCode);
                
                if ("00".equals(responseCode)) {
                    // Thanh toán thành công
                    return handlePaymentSuccess(Integer.parseInt(orderId), "VNPAY", model);
                } else {
                    // Thanh toán thất bại
                    return handlePaymentFailure(Integer.parseInt(orderId), "VNPAY", 
                            params.get("vnp_ResponseCode"), model);
                }
            } else {
                log.error("VNPay signature verification failed");
                throw new RuntimeException("Chữ ký không hợp lệ");
            }
            
        } catch (Exception e) {
            log.error("Error processing VNPay callback: ", e);
            model.addAttribute("error", "Lỗi xác thực thanh toán VNPay: " + e.getMessage());
            return "web/payment-error";
        }
    }

    private String handlePaymentSuccess(Integer orderId, String method, Model model) {
        try {
            DatHang datHang = datHangService.findByMaDatHang(orderId);
            if (datHang == null) {
                throw new RuntimeException("Không tìm thấy đơn hàng: " + orderId);
            }
            
            // Cập nhật trạng thái thanh toán thành "Completed"
            ThanhToan thanhToan = thanhToanService.findByDatHang(orderId)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thanh toán cho đơn hàng: " + orderId));
            
            thanhToan.setTrangThai("Completed");
            thanhToanService.save(thanhToan);

            // Cập nhật trạng thái đơn hàng
            datHang.setTrangThai("Đã thanh toán");
            datHangService.save(datHang);

            // Xóa giỏ hàng sau khi thanh toán thành công
            // ... code xóa giỏ hàng ...

            model.addAttribute("datHang", datHang);
            model.addAttribute("success", "Thanh toán thành công!");
            model.addAttribute("paymentMethod", method);

            log.info("Payment successful for order: {}, method: {}", orderId, method);
            return "web/order-success";

        } catch (Exception e) {
            log.error("Error confirming payment success: ", e);
            model.addAttribute("error", "Lỗi khi xác nhận thanh toán: " + e.getMessage());
            return "web/payment-error";
        }
    }

    // Overload method for IPN (không có Model)
    private void handlePaymentSuccess(Integer orderId, String method) {
        try {
            DatHang datHang = datHangService.findByMaDatHang(orderId);
            if (datHang == null) {
                log.error("Order not found for IPN: {}", orderId);
                return;
            }
            
            // Cập nhật trạng thái thanh toán thành "Completed"
            ThanhToan thanhToan = thanhToanService.findByDatHang(orderId)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thanh toán"));
            
            thanhToan.setTrangThai("Completed");
            thanhToanService.save(thanhToan);

            // Cập nhật trạng thái đơn hàng
            datHang.setTrangThai("Đã thanh toán");
            datHangService.save(datHang);

            log.info("IPN - Payment successful for order: {}, method: {}", orderId, method);
            
        } catch (Exception e) {
            log.error("Error processing IPN payment success: ", e);
        }
    }

    private String handlePaymentFailure(Integer orderId, String method, String errorMessage, Model model) {
        try {
            DatHang datHang = datHangService.findByMaDatHang(orderId);
            if (datHang == null) {
                throw new RuntimeException("Không tìm thấy đơn hàng: " + orderId);
            }
            
            // Cập nhật trạng thái thanh toán thành "Failed"
            ThanhToan thanhToan = thanhToanService.findByDatHang(orderId)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thanh toán"));
            
            thanhToan.setTrangThai("Failed");
            thanhToanService.save(thanhToan);

            model.addAttribute("datHang", datHang);
            model.addAttribute("error", "Thanh toán thất bại: " + errorMessage);

            log.info("Payment failed for order: {}, method: {}, reason: {}", orderId, method, errorMessage);
            return "web/payment-error";

        } catch (Exception e) {
            log.error("Error processing payment failure: ", e);
            model.addAttribute("error", "Lỗi khi xử lý thanh toán thất bại: " + e.getMessage());
            return "web/payment-error";
        }
    }

    @GetMapping("/success")
    public String paymentSuccess(@RequestParam Integer orderId, Model model) {
        try {
            DatHang datHang = datHangService.findByMaDatHang(orderId);
            if (datHang == null) {
                throw new RuntimeException("Không tìm thấy đơn hàng: " + orderId);
            }
            
            // Cập nhật trạng thái thanh toán thành "Completed"
            ThanhToan thanhToan = thanhToanService.findByDatHang(orderId)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thanh toán"));
            
            thanhToan.setTrangThai("Completed");
            thanhToanService.save(thanhToan);

            // Xóa giỏ hàng
            // ... code xóa giỏ hàng ...

            model.addAttribute("datHang", datHang);
            model.addAttribute("success", "Thanh toán thành công!");

            return "web/order-success";

        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi xác nhận thanh toán: " + e.getMessage());
            return "web/payment-error";
        }
    }

    @GetMapping("/cancel")
    public String paymentCancel(@RequestParam Integer orderId, Model model) {
        try {
            DatHang datHang = datHangService.findByMaDatHang(orderId);
            if (datHang == null) {
                throw new RuntimeException("Không tìm thấy đơn hàng: " + orderId);
            }
            
            // Cập nhật trạng thái thanh toán thành "Cancelled"
            ThanhToan thanhToan = thanhToanService.findByDatHang(orderId)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thanh toán"));
            
            thanhToan.setTrangThai("Cancelled");
            thanhToanService.save(thanhToan);

            model.addAttribute("datHang", datHang);
            model.addAttribute("error", "Thanh toán đã bị hủy");

            return "web/payment-error";

        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi hủy thanh toán: " + e.getMessage());
            return "web/payment-error";
        }
    }

    // Endpoint để test payment flow
    @GetMapping("/test")
    public String testPayment(@RequestParam(defaultValue = "MOMO") String method, Model model) {
        try {
            // Tạo đơn hàng test
            // ... code tạo đơn hàng test ...
            
            return "redirect:/payment/process?orderId=1&method=" + method;
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi test: " + e.getMessage());
            return "web/error";
        }
    }
}