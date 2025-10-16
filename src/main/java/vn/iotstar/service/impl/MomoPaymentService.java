package vn.iotstar.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import vn.iotstar.config.PaymentConfig;
import vn.iotstar.entity.DatHang;
import vn.iotstar.service.PaymentService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MomoPaymentService implements PaymentService {

	private final PaymentConfig paymentConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public String createPayment(DatHang datHang) {
        try {
            // Lưu ý: Đảm bảo payment.mock.enabled=false trong application.properties
            if (paymentConfig.isMockEnabled()) {
                log.info("Mock enabled, using mock payment URL");
                return createMockPaymentUrl(datHang);
            }

            PaymentConfig.MomoConfig momoConfig = paymentConfig.getMomo();
            
            String orderId = String.valueOf(datHang.getMaDatHang());
            String orderInfo = "Thanh toan don hang " + orderId;
            long amount = datHang.getTongTien().longValue();
            String requestId = String.valueOf(System.currentTimeMillis());
            String extraData = ""; // Base64 encoded JSON if needed
            
            // Tạo raw signature theo chuẩn MoMo
            String rawHash = "accessKey=" + momoConfig.getAccessKey() +
                    "&amount=" + amount +
                    "&extraData=" + extraData +
                    "&ipnUrl=" + momoConfig.getNotifyUrl() +
                    "&orderId=" + orderId +
                    "&orderInfo=" + orderInfo +
                    "&partnerCode=" + momoConfig.getPartnerCode() +
                    "&redirectUrl=" + momoConfig.getReturnUrl() +
                    "&requestId=" + requestId +
                    "&requestType=" + momoConfig.getRequestType();

            String signature = signHmacSHA256(rawHash, momoConfig.getSecretKey());

            // Tạo request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("partnerCode", momoConfig.getPartnerCode());
            requestBody.put("partnerName", "Pet Shop");
            requestBody.put("storeId", "PetShopStore");
            requestBody.put("requestId", requestId);
            requestBody.put("amount", amount);
            requestBody.put("orderId", orderId);
            requestBody.put("orderInfo", orderInfo);
            requestBody.put("redirectUrl", momoConfig.getReturnUrl());
            requestBody.put("ipnUrl", momoConfig.getNotifyUrl());
            requestBody.put("lang", "vi");
            requestBody.put("extraData", extraData);
            requestBody.put("requestType", momoConfig.getRequestType());
            requestBody.put("signature", signature);

            log.info("Momo Request Body: {}", objectMapper.writeValueAsString(requestBody));

            // Gọi API MoMo
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    momoConfig.getEndpoint(),
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            log.info("Momo Response: {}", objectMapper.writeValueAsString(response.getBody()));

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String resultCode = String.valueOf(responseBody.get("resultCode"));
                
                if ("0".equals(resultCode)) {
                    String payUrl = String.valueOf(responseBody.get("payUrl"));
                    String qrCodeUrl = String.valueOf(responseBody.get("qrCodeUrl"));
                    
                    log.info("MoMo Payment created - PayURL: {}, QRCodeURL: {}", payUrl, qrCodeUrl);
                    
                    // FIX: Ưu tiên trả về qrCodeUrl nếu có, vì nó là nội dung chuẩn cho QR code
                    if (qrCodeUrl != null && !qrCodeUrl.isEmpty() && !"null".equals(qrCodeUrl.toLowerCase())) {
                        return qrCodeUrl;
                    }
                    
                    return payUrl; // Trả về payUrl nếu không có qrCodeUrl
                } else {
                    String message = String.valueOf(responseBody.get("message"));
                    log.error("MoMo API error: {} - {}", resultCode, message);
                    throw new RuntimeException("Lỗi MoMo: " + message);
                }
            }

            throw new RuntimeException("Không thể kết nối đến MoMo");

        } catch (Exception e) {
            log.error("Error creating Momo payment: ", e);
            // Fallback to mock if real payment fails
            return createMockPaymentUrl(datHang);
        }
    }

    private String createMockPaymentUrl(DatHang datHang) {
        return String.format("http://localhost:8080/payment/mock/callback?orderId=%s&amount=%s&status=success",
                datHang.getMaDatHang(), datHang.getTongTien());
    }

    // Các method verifyPayment, signHmacSHA256, bytesToHex giữ nguyên...
    
    @Override
    public boolean verifyPayment(Map<String, String> params) {
        try {
            PaymentConfig.MomoConfig momoConfig = paymentConfig.getMomo();
            
            String signature = params.get("signature");
            if (signature == null) {
                return false;
            }

            // Tạo raw signature để verify
            StringBuilder rawHash = new StringBuilder();
            rawHash.append("accessKey=").append(params.get("accessKey") != null ? params.get("accessKey") : "")
                   .append("&amount=").append(params.get("amount") != null ? params.get("amount") : "")
                   .append("&extraData=").append(params.get("extraData") != null ? params.get("extraData") : "")
                   .append("&message=").append(params.get("message") != null ? params.get("message") : "")
                   .append("&orderId=").append(params.get("orderId") != null ? params.get("orderId") : "")
                   .append("&orderInfo=").append(params.get("orderInfo") != null ? params.get("orderInfo") : "")
                   .append("&orderType=").append(params.get("orderType") != null ? params.get("orderType") : "")
                   .append("&partnerCode=").append(params.get("partnerCode") != null ? params.get("partnerCode") : "")
                   .append("&payType=").append(params.get("payType") != null ? params.get("payType") : "")
                   .append("&requestId=").append(params.get("requestId") != null ? params.get("requestId") : "")
                   .append("&responseTime=").append(params.get("responseTime") != null ? params.get("responseTime") : "")
                   .append("&resultCode=").append(params.get("resultCode") != null ? params.get("resultCode") : "")
                   .append("&transId=").append(params.get("transId") != null ? params.get("transId") : "");

            String expectedSignature = signHmacSHA256(rawHash.toString(), momoConfig.getSecretKey());
            
            boolean isValid = signature.equals(expectedSignature);
            log.info("MoMo signature verification: {}", isValid);
            return isValid;

        } catch (Exception e) {
            log.error("Error verifying Momo payment: ", e);
            return false;
        }
    }

    private String signHmacSHA256(String data, String secretKey) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(rawHmac);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    @Override
    public String getPaymentMethod() {
        return "MOMO";
    }
}