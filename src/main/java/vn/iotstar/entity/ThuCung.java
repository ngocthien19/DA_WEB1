package vn.iotstar.entity;

import lombok.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "ThuCung")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThuCung {
    @Id
    @Column(name = "MaSanPham")
    private Integer maSanPham;
    
    @OneToOne
    @JoinColumn(name = "MaSanPham", referencedColumnName = "MaSanPham")
    @MapsId
    private SanPham sanPham;
    
    @NotBlank(message = "Giới tính không được để trống")
    @Column(name = "GioiTinh", columnDefinition = "NVARCHAR(20)")
    private String gioiTinh;
    
    @NotNull(message = "Độ tuổi không được để trống")
    @Min(value = 1, message = "Độ tuổi phải lớn hơn 0")
    @Column(name = "DoTuoi")
    private Integer doTuoi;
    
    @NotBlank(message = "Tình trạng sức khỏe không được để trống")
    @Column(name = "SucKhoe", columnDefinition = "NVARCHAR(100)")
    private String sucKhoe;
}