package vn.iotstar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.entity.OtpToken;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Integer> {
    
    Optional<OtpToken> findByEmailAndIsUsedFalse(String email);
    
    Optional<OtpToken> findByEmailAndOtpCodeAndIsUsedFalse(String email, String otpCode);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM OtpToken o WHERE o.expiryTime < :now OR o.isUsed = true")
    void deleteExpiredAndUsedTokens(LocalDateTime now);
    
    @Modifying
    @Transactional
    void deleteByEmail(String email);
    
    // Thêm methods cho chức năng quên mật khẩu
    Optional<OtpToken> findByEmailAndOtpTypeAndIsUsedFalse(String email, String otpType);
    
    @Modifying
    @Transactional
    void deleteByEmailAndOtpType(String email, String otpType);
}