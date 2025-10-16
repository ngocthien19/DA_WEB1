package vn.iotstar.service;

import vn.iotstar.entity.DatHang;

import java.util.Map;

public interface PaymentService {
    
    /**
     * Tạo URL thanh toán
     */
    String createPayment(DatHang datHang);
    
    /**
     * Xác minh kết quả thanh toán
     */
    boolean verifyPayment(Map<String, String> params);
    
    /**
     * Lấy phương thức thanh toán
     */
    String getPaymentMethod();
}