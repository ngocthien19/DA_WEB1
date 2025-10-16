package vn.iotstar.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "payment")
public class PaymentConfig {
    
    private boolean mockEnabled;
    private MomoConfig momo;
    private VnpayConfig vnpay;
    
    @Data
    public static class MomoConfig {
        private String partnerCode;
        private String accessKey;
        private String secretKey;
        private String endpoint;
        private String returnUrl;
        private String notifyUrl;
        private String requestType;
    }
    
    @Data
    public static class VnpayConfig {
        private String tmnCode;
        private String hashSecret;
        private String endpoint;
        private String returnUrl;
        private String version;
        private String command;
        private String currency;
    }
}