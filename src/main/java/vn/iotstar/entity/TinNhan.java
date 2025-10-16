package vn.iotstar.entity;

import lombok.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.Date;

@Entity
@Table(name = "TinNhan")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TinNhan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaTinNhan")
    private Integer maTinNhan;
    
    @NotNull(message = "Người gửi không được để trống")
    @ManyToOne
    @JoinColumn(name = "MaNguoiGui", nullable = false)
    private NguoiDung nguoiGui;
    
    @NotNull(message = "Người nhận không được để trống")
    @ManyToOne
    @JoinColumn(name = "MaNguoiNhan", nullable = false)
    private NguoiDung nguoiNhan;
    
    @NotNull(message = "Cửa hàng không được để trống")
    @ManyToOne
    @JoinColumn(name = "MaCuaHang", nullable = false)
    private CuaHang cuaHang;
    
    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    @Column(name = "NoiDung", columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private String noiDung;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "ThoiGian")
    @Builder.Default
    private Date thoiGian = new Date();
    
    @Column(name = "DaDoc")
    @Builder.Default
    private Boolean daDoc = false;
    
    @Column(name = "FileUrl", columnDefinition = "NVARCHAR(500)")
    private String fileUrl;
}
