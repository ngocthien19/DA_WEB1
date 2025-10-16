package vn.iotstar.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.iotstar.config.PaymentConfig;
import vn.iotstar.entity.DatHang;
import vn.iotstar.service.PaymentService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VNPayPaymentService implements PaymentService {

	private final PaymentConfig paymentConfig;

    @Override
    public String createPayment(DatHang datHang) {
        try {
            if (paymentConfig.isMockEnabled()) {
                log.info("Mock enabled, using mock payment URL");
                return createMockPaymentUrl(datHang);
            }

            PaymentConfig.VnpayConfig vnpayConfig = paymentConfig.getVnpay();
            
            String vnp_Version = vnpayConfig.getVersion();
            String vnp_Command = vnpayConfig.getCommand();
            String vnp_TmnCode = vnpayConfig.getTmnCode();
            
            long amount = datHang.getTongTien().longValue() * 100; // VNPay tính theo đơn vị VND x 100
            String vnp_Amount = String.valueOf(amount);
            
            String vnp_TxnRef = String.valueOf(datHang.getMaDatHang());
            String vnp_OrderInfo = "Thanh toan don hang " + vnp_TxnRef;
            String vnp_OrderType = "billpayment";
            
            String vnp_Locale = "vn";
            String vnp_IpAddr = "127.0.0.1"; // Trong thực tế nên lấy IP thực
            
            String vnp_ReturnUrl = vnpayConfig.getReturnUrl();
            
            Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String vnp_CreateDate = formatter.format(cld.getTime());
            
            cld.add(Calendar.MINUTE, 15);
            String vnp_ExpireDate = formatter.format(cld.getTime());

            Map<String, String> vnp_Params = new TreeMap<>();
            vnp_Params.put("vnp_Version", vnp_Version);
            vnp_Params.put("vnp_Command", vnp_Command);
            vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
            vnp_Params.put("vnp_Amount", vnp_Amount);
            vnp_Params.put("vnp_CurrCode", vnpayConfig.getCurrency());
            vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
            vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
            vnp_Params.put("vnp_OrderType", vnp_OrderType);
            vnp_Params.put("vnp_Locale", vnp_Locale);
            vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
            vnp_Params.put("vnp_IpAddr", vnp_IpAddr);
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
            vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

            // Tạo URL thanh toán
            StringBuilder queryUrl = new StringBuilder();
            StringBuilder hashData = new StringBuilder();
            
            for (Map.Entry<String, String> entry : vnp_Params.entrySet()) {
                hashData.append(entry.getKey());
                hashData.append('=');
                hashData.append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII.toString()));
                queryUrl.append(URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII.toString()));
                queryUrl.append('=');
                queryUrl.append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII.toString()));
                queryUrl.append('&');
            }

            // Xóa ký tự & cuối cùng
            queryUrl.deleteCharAt(queryUrl.length() - 1);

            String vnp_SecureHash = hmacSHA512(vnpayConfig.getHashSecret(), hashData.toString());
            queryUrl.append("&vnp_SecureHash=").append(vnp_SecureHash);

            String paymentUrl = vnpayConfig.getEndpoint() + "?" + queryUrl;
            
            log.info("VNPay Payment URL created for order: {}", vnp_TxnRef);
            return paymentUrl;

        } catch (Exception e) {
            log.error("Error creating VNPay payment: ", e);
            // Fallback to mock
            return createMockPaymentUrl(datHang);
        }
    }

    private String createMockPaymentUrl(DatHang datHang) {
        return String.format("http://localhost:8080/payment/mock/callback?orderId=%s&amount=%s&status=success",
                datHang.getMaDatHang(), datHang.getTongTien());
    }

    @Override
    public boolean verifyPayment(Map<String, String> params) {
        try {
            PaymentConfig.VnpayConfig vnpayConfig = paymentConfig.getVnpay();
            
            String vnp_SecureHash = params.get("vnp_SecureHash");
            params.remove("vnp_SecureHash");

            // Sắp xếp tham số theo thứ tự alphabet
            Map<String, String> sortedParams = new TreeMap<>(params);
            
            StringBuilder hashData = new StringBuilder();
            for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
                if (entry.getValue() != null && entry.getValue().length() > 0) {
                    hashData.append(entry.getKey());
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII.toString()));
                    hashData.append('&');
                }
            }
            
            // Xóa ký tự & cuối cùng
            if (hashData.length() > 0) {
                hashData.deleteCharAt(hashData.length() - 1);
            }

            String expectedHash = hmacSHA512(vnpayConfig.getHashSecret(), hashData.toString());
            
            return vnp_SecureHash != null && vnp_SecureHash.equals(expectedHash);

        } catch (Exception e) {
            log.error("Error verifying VNPay payment: ", e);
            return false;
        }
    }

    private String hmacSHA512(String key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
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
        return "VNPAY";
    }
}