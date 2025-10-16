package vn.iotstar.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpResponse {
    private String email;
    private String message;
    private LocalDateTime expiryTime;
    private Integer remainingAttempts;
}