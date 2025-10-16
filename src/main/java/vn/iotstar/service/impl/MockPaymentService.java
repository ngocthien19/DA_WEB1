package vn.iotstar.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.iotstar.entity.DatHang;
import vn.iotstar.service.PaymentService;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class MockPaymentService implements PaymentService {

    @Override
    public String createPayment(DatHang datHang) {
        // Tạo URL thanh toán mock
        String mockPaymentUrl = String.format(
            "http://localhost:8080/payment/mock/callback?orderId=%s&amount=%s&status=success",
            datHang.getMaDatHang(),
            datHang.getTongTien()
        );
        
        log.info("Mock payment created for order: {}", datHang.getMaDatHang());
        return mockPaymentUrl;
    }

    @Override
    public boolean verifyPayment(Map<String, String> params) {
        // Luôn trả về true cho mock
        return true;
    }

    @Override
    public String getPaymentMethod() {
        return "MOCK";
    }
}