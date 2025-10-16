package vn.iotstar.entity;

import lombok.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "DanhGiaCuaHang")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DanhGiaCuaHang {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaDanhGiaCuaHang")
    private Integer maDanhGiaCuaHang;
    
    @NotNull(message = "Cửa hàng không được để trống")
    @ManyToOne
    @JoinColumn(name = "MaCuaHang", nullable = false)
    private CuaHang cuaHang;
    
    @NotNull(message = "Người dùng không được để trống")
    @ManyToOne
    @JoinColumn(name = "MaNguoiDung", nullable = false)
    @JsonIgnoreProperties({"danhGias", "cuaHangs", "gioHangs", "datHangs", "matKhau"})
    private NguoiDung nguoiDung;
    
    @NotNull(message = "Đơn hàng không được để trống")
    @ManyToOne
    @JoinColumn(name = "MaDatHang", nullable = false)
    private DatHang datHang;
    
    @NotNull(message = "Số sao không được để trống")
    @Min(value = 1, message = "Số sao phải từ 1 đến 5")
    @Max(value = 5, message = "Số sao phải từ 1 đến 5")
    @Column(name = "SoSao")
    private Integer soSao;
    
    @NotBlank(message = "Bình luận không được để trống")
    @Size(min = 10, max = 1000, message = "Bình luận phải từ 10 đến 1000 ký tự")
    @Column(name = "BinhLuan", columnDefinition = "NVARCHAR(MAX)")
    private String binhLuan;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "NgayDanhGia")
    @Builder.Default
    private Date ngayDanhGia = new Date();
}
