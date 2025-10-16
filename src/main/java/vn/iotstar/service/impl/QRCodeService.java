package vn.iotstar.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import vn.iotstar.entity.DatHang;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class QRCodeService {

    /**
     * Tạo QR code từ text và trả về dạng base64
     */
    public String generateQRCodeBase64(String text, int width, int height) {
        try {
            BufferedImage qrImage = generateQRCodeImage(text, width, height);
            return convertToBase64(qrImage);
        } catch (Exception e) {
            log.error("Error generating QR code: ", e);
            return null;
        }
    }

    /**
     * Tạo QR code với logo (dùng cho MoMo, VNPay, etc.)
     */
    public String generateQRCodeWithLogoBase64(String text, int width, int height, BufferedImage logo) {
        try {
            BufferedImage qrImage = generateQRCodeImage(text, width, height);
            BufferedImage combinedImage = addLogoToQRCode(qrImage, logo);
            return convertToBase64(combinedImage);
        } catch (Exception e) {
            log.error("Error generating QR code with logo: ", e);
            return null;
        }
    }

    /**
     * Tạo QR code từ URL thanh toán MoMo thực (OVERLOADED version với paymentUrl)
     * FIX: Ưu tiên link HTTPS từ API MoMo, tránh dùng Deep Link momo:// cho QR
     */
    public String generateMomoPaymentQRCode(DatHang datHang, String paymentUrl) {
        try {
            log.info("Generating MoMo QR from payment URL: {}", paymentUrl);
            
            // Nếu paymentUrl là URL thanh toán thực (HTTPS link) hoặc QR Code URL
            if (paymentUrl != null && (paymentUrl.startsWith("http") || paymentUrl.contains("momo://"))) {
                return generateQRCodeBase64(paymentUrl, 300, 300);
            }
            
            // Fallback: tạo QR với deep link (phương án cuối cùng)
            return generateMomoDeepLinkQRCode(datHang);
            
        } catch (Exception e) {
            log.error("Error generating MoMo payment QR code: ", e);
            return null;
        }
    }

    /**
     * Tạo QR code từ URL thanh toán VNPay thực (OVERLOADED version với paymentUrl)
     */
    public String generateVNPayPaymentQRCode(DatHang datHang, String paymentUrl) {
        try {
            log.info("Generating VNPay QR from payment URL: {}", paymentUrl);
            
            // Nếu paymentUrl là URL thanh toán VNPay, tạo QR từ đó
            if (paymentUrl != null && (paymentUrl.contains("vnpayment.vn") || paymentUrl.contains("vnpay://"))) {
                return generateQRCodeBase64(paymentUrl, 300, 300);
            }
            
            // Fallback: tạo QR với deep link
            return generateVNPayDeepLinkQRCode(datHang);
            
        } catch (Exception e) {
            log.error("Error generating VNPay payment QR code: ", e);
            return null;
        }
    }

    /**
     * Tạo QR code cho thanh toán MoMo với thông tin đầy đủ (ORIGINAL version)
     * LƯU Ý: Deep link này có thể không hoạt động như QR thanh toán chuẩn
     */
    public String generateMomoPaymentQRCode(DatHang datHang) {
        try {
            String orderId = String.valueOf(datHang.getMaDatHang());
            String amount = String.valueOf(datHang.getTongTien().longValue());
            String description = "Thanh toan don hang " + orderId;
            
            // Deep Link MoMo: nên dùng payUrl/qrCodeUrl từ API thay vì tự tạo
            String momoUrl = String.format("momo://payment?partner=petShop&orderId=%s&amount=%s&description=%s",
                    orderId, amount, URLEncoder.encode(description, StandardCharsets.UTF_8.toString()));
            
            log.info("Generating MoMo QR for: {}", momoUrl);
            return generateQRCodeBase64(momoUrl, 300, 300);
        } catch (Exception e) {
            log.error("Error generating MoMo payment QR code: ", e);
            return null;
        }
    }

    /**
     * Tạo QR code cho thanh toán VNPay với thông tin đầy đủ (ORIGINAL version)
     */
    public String generateVNPayPaymentQRCode(DatHang datHang) {
        try {
            String orderId = String.valueOf(datHang.getMaDatHang());
            String amount = String.valueOf(datHang.getTongTien().longValue());
            String description = "Thanh toan don hang " + orderId;
            
            // Sửa URL thành định dạng đúng cho VNPay app
            String vnpayUrl = String.format("vnpay://qr/?i=PetShop&am=%s&add=%s&m=%s",
                    amount, URLEncoder.encode(description, StandardCharsets.UTF_8.toString()), orderId);
            
            log.info("Generating VNPay QR for: {}", vnpayUrl);
            return generateQRCodeBase64(vnpayUrl, 300, 300);
        } catch (Exception e) {
            log.error("Error generating VNPay payment QR code: ", e);
            return null;
        }
    }

    /**
     * Tạo QR code cho đơn hàng
     */
    public String generateOrderQRCode(Integer orderId, String totalAmount, String storeName) {
        try {
            String orderInfo = String.format("ORDER_%s_%s_%s", 
                    orderId, totalAmount, storeName.replace(" ", "_"));
            
            return generateQRCodeBase64(orderInfo, 300, 300);
        } catch (Exception e) {
            log.error("Error generating order QR code: ", e);
            return null;
        }
    }

    /**
     * Tạo QR code từ URL thanh toán
     */
    public String generatePaymentQRCode(String paymentUrl) {
        return generateQRCodeBase64(paymentUrl, 300, 300);
    }

    /**
     * Tạo QR code từ URL thanh toán thực tế
     */
    public String generatePaymentQRCodeFromUrl(String paymentUrl) {
        return generateQRCodeBase64(paymentUrl, 300, 300);
    }

    /**
     * Tạo QR code thông báo thanh toán đơn giản
     */
    public String generatePaymentInfoQRCode(DatHang datHang, String method) {
        try {
            String orderId = String.valueOf(datHang.getMaDatHang());
            String amount = formatCurrency(datHang.getTongTien());
            
            // Tạo text thông báo rõ ràng
            String paymentInfo = String.format(
                "THANH TOÁN %s\n\nMã đơn: %s\nSố tiền: %s\nNội dung: Thanh toán đơn hàng %s\n\nQuét mã này để nhập thủ công trong app %s",
                method, orderId, amount, orderId, method
            );
            
            log.info("Generating payment info QR: {}", paymentInfo);
            return generateQRCodeBase64(paymentInfo, 350, 350); // Kích thước lớn hơn cho dễ đọc
        } catch (Exception e) {
            log.error("Error generating payment info QR: ", e);
            return null;
        }
    }

    // Thêm method để tạo QR code đơn giản hơn cho testing
    public String generateSimplePaymentQRCode(DatHang datHang, String method) {
        try {
            String orderId = String.valueOf(datHang.getMaDatHang());
            String amount = String.valueOf(datHang.getTongTien().longValue());
            
            // Tạo text đơn giản để test
            String qrText = String.format("PAYMENT-%s-%s-%s-%s", 
                    method, orderId, amount, "PetShop");
            
            log.info("Generating simple QR for testing: {}", qrText);
            return generateQRCodeBase64(qrText, 300, 300);
        } catch (Exception e) {
            log.error("Error generating simple payment QR code: ", e);
            return null;
        }
    }

    /**
     * Tạo QR code với deep link MoMo (phương án dự phòng)
     */
    private String generateMomoDeepLinkQRCode(DatHang datHang) {
        try {
            String amount = String.valueOf(datHang.getTongTien().longValue());
            String orderId = String.valueOf(datHang.getMaDatHang());
            String description = URLEncoder.encode("Thanh toán đơn hàng " + orderId, "UTF-8");
            
            // Deep link MoMo (cho phiên bản app cũ)
            String deepLink = String.format("momo://transfer?phone=0900000000&amount=%s&comment=%s", 
                amount, description);
            
            log.info("Using MoMo deep link: {}", deepLink);
            return generateQRCodeBase64(deepLink, 300, 300);
        } catch (Exception e) {
            log.error("Error generating MoMo deep link QR: ", e);
            return null;
        }
    }

    /**
     * Tạo QR code với deep link VNPay (phương án dự phòng)
     */
    private String generateVNPayDeepLinkQRCode(DatHang datHang) {
        try {
            String amount = String.valueOf(datHang.getTongTien().longValue());
            String orderId = String.valueOf(datHang.getMaDatHang());
            String description = URLEncoder.encode("Thanh toán đơn hàng " + orderId, "UTF-8");
            
            // Deep link VNPay
            String deepLink = String.format("vnpay://qr/payment?amount=%s&addInfo=%s", 
                amount, description);
            
            log.info("Using VNPay deep link: {}", deepLink);
            return generateQRCodeBase64(deepLink, 300, 300);
        } catch (Exception e) {
            log.error("Error generating VNPay deep link QR: ", e);
            return null;
        }
    }

    /**
     * Định dạng tiền tệ
     */
    private String formatCurrency(java.math.BigDecimal amount) {
        java.text.DecimalFormat formatter = new java.text.DecimalFormat("###,###");
        return formatter.format(amount) + " VND";
    }

    private BufferedImage generateQRCodeImage(String text, int width, int height) 
            throws WriterException, IOException {
        
        log.info("Generating QR code with text: {}", text);
        log.info("QR code dimensions: {}x{}", width, height);
        
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 1);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);

        BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);
        log.info("QR code generated successfully, image size: {}x{}", image.getWidth(), image.getHeight());
        
        return image;
    }

    private BufferedImage addLogoToQRCode(BufferedImage qrImage, BufferedImage logo) {
        int qrWidth = qrImage.getWidth();
        int qrHeight = qrImage.getHeight();
        
        // Tính toán kích thước logo (20% của QR code)
        int logoWidth = qrWidth / 5;
        int logoHeight = qrHeight / 5;
        
        // Scale logo
        Image scaledLogo = logo.getScaledInstance(logoWidth, logoHeight, Image.SCALE_SMOOTH);
        BufferedImage resizedLogo = new BufferedImage(logoWidth, logoHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedLogo.createGraphics();
        g2d.drawImage(scaledLogo, 0, 0, null);
        g2d.dispose();

        // Vẽ logo lên QR code
        Graphics2D combined = qrImage.createGraphics();
        int x = (qrWidth - logoWidth) / 2;
        int y = (qrHeight - logoHeight) / 2;
        combined.drawImage(resizedLogo, x, y, null);
        combined.dispose();

        return qrImage;
    }

    private String convertToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }
}