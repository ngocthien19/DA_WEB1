package vn.iotstar.controller;

import lombok.RequiredArgsConstructor;
import vn.iotstar.service.impl.QRCodeService;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/qrcode")
@RequiredArgsConstructor
public class QRCodeController {

    private final QRCodeService qrCodeService;

    /**
     * API tạo QR code từ text
     */
    @GetMapping("/generate")
    public ResponseEntity<Map<String, String>> generateQRCode(
            @RequestParam String text,
            @RequestParam(defaultValue = "300") int width,
            @RequestParam(defaultValue = "300") int height) {
        
        try {
            String qrCodeBase64 = qrCodeService.generateQRCodeBase64(text, width, height);
            
            Map<String, String> response = new HashMap<>();
            response.put("qrCode", qrCodeBase64);
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to generate QR code");
            response.put("status", "error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Trang hiển thị QR code cho thanh toán
     */
    @GetMapping("/payment")
    public String showPaymentQRCode(
            @RequestParam String amount,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String method,
            Model model) {
        
        try {
            String qrText = "";
            String qrTitle = "";

            if ("MOMO".equals(method)) {
                // Tạo URL thanh toán MoMo
                qrText = String.format("momo://payment?amount=%s&description=%s", 
                        amount, description != null ? description : "Thanh%20toan%20don%20hang");
                qrTitle = "Quét mã để thanh toán bằng MoMo";
            } else if ("VNPAY".equals(method)) {
                // Tạo URL thanh toán VNPay
                qrText = String.format("vnpay://transfer?amount=%s&content=%s", 
                        amount, description != null ? description : "Thanh%20toan%20don%20hang");
                qrTitle = "Quét mã để thanh toán bằng VNPay";
            } else {
                // QR code thông thường với thông tin thanh toán
                qrText = String.format("PAYMENT:%s:%s", amount, 
                        description != null ? description : "Payment");
                qrTitle = "Mã QR thanh toán";
            }

            String qrCodeBase64 = qrCodeService.generateQRCodeBase64(qrText, 300, 300);
            
            model.addAttribute("qrCode", qrCodeBase64);
            model.addAttribute("amount", amount);
            model.addAttribute("description", description);
            model.addAttribute("method", method);
            model.addAttribute("title", qrTitle);
            
            return "web/qr-payment";
        } catch (Exception e) {
            model.addAttribute("error", "Không thể tạo mã QR: " + e.getMessage());
            return "web/error";
        }
    }

    /**
     * API trả về hình ảnh QR code trực tiếp
     */
    @GetMapping(value = "/image", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public byte[] getQRCodeImage(
            @RequestParam String text,
            @RequestParam(defaultValue = "300") int width,
            @RequestParam(defaultValue = "300") int height) {
        
        try {
            String qrCodeBase64 = qrCodeService.generateQRCodeBase64(text, width, height);
            return java.util.Base64.getDecoder().decode(qrCodeBase64);
        } catch (Exception e) {
            return new byte[0];
        }
    }
}