package vn.iotstar.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    
    @Column(name = "email", nullable = false, 
            columnDefinition = "VARCHAR(100)")
    private String email;
    
    @Column(name = "otp_code", nullable = false, 
            columnDefinition = "VARCHAR(10)")
    private String otpCode;
    
    @Column(name = "expiry_time", nullable = false)
    private LocalDateTime expiryTime;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;
    
    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;
    
    @Column(name = "TenNguoiDung", nullable = false, 
            columnDefinition = "NVARCHAR(100)")
    private String tenNguoiDung;
    
    @Column(name = "mat_khau", nullable = true, 
            columnDefinition = "NVARCHAR(255)")
    private String matKhau;
    
    @Column(name = "sdt", nullable = true, columnDefinition = "VARCHAR(20)")
    private String sdt;
    
    @Column(name = "DiaChi", nullable = true, columnDefinition = "NVARCHAR(255)")
    private String diaChi;
    
    @Column(name = "otp_type", columnDefinition = "VARCHAR(20)")
    @Builder.Default
    private String otpType = "REGISTER"; // REGISTER hoặc FORGOT_PASSWORD
     
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryTime);
    }
    
    public boolean canAttempt() {
        return attemptCount < 5;
    }
    
    public void incrementAttempt() {
        this.attemptCount++;
    }
}