package vn.iotstar.model;

import lombok.*;

import java.math.BigDecimal;

import jakarta.validation.constraints.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThuCungModel {
    private Integer maSanPham;
    
    @NotBlank(message = "Giới tính không được để trống")
    private String gioiTinh;
    
    @NotNull(message = "Độ tuổi không được để trống")
    @Min(value = 1, message = "Độ tuổi phải lớn hơn 0")
    private Integer doTuoi;
    
    @NotBlank(message = "Tình trạng sức khỏe không được để trống")
    private String sucKhoe;
    
    // For response
    private String tenSanPham;
    private BigDecimal giaBan;
    private String hinhAnh;
}