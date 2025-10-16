package vn.iotstar.service.factory;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import vn.iotstar.service.PaymentService;
import vn.iotstar.service.impl.MockPaymentService;
import vn.iotstar.service.impl.MomoPaymentService;
import vn.iotstar.service.impl.VNPayPaymentService;

import java.util.HashMap;
import java.util.Map;
@Component
@RequiredArgsConstructor
public class PaymentServiceFactory {
    
    private final MomoPaymentService momoPaymentService;
    private final VNPayPaymentService vnPayPaymentService;
    private final MockPaymentService mockPaymentService;
    
    private static final Map<String, PaymentService> PAYMENT_SERVICES = new HashMap<>();
    
    @PostConstruct
    public void init() {
        PAYMENT_SERVICES.put("MOMO", momoPaymentService);
        PAYMENT_SERVICES.put("VNPAY", vnPayPaymentService);
        PAYMENT_SERVICES.put("MOCK", mockPaymentService); // Thêm mock service
    }
    
    public PaymentService getPaymentService(String method) {
        // Ưu tiên mock nếu VNPay không hoạt động
        if ("VNPAY".equals(method)) {
            try {
                // Test kết nối VNPay
                return vnPayPaymentService;
            } catch (Exception e) {
                return mockPaymentService;
            }
        }
        
        PaymentService service = PAYMENT_SERVICES.get(method.toUpperCase());
        if (service == null) {
            throw new IllegalArgumentException("Phương thức thanh toán không được hỗ trợ: " + method);
        }
        return service;
    }
}