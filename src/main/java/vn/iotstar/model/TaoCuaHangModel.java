package vn.iotstar.model;

import lombok.*;
import jakarta.validation.constraints.*;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaoCuaHangModel {
    
    @NotNull(message = "Chủ cửa hàng không được để trống")
    private Integer maNguoiDung;
    
    @NotBlank(message = "Tên cửa hàng không được để trống")
    @Size(min = 2, max = 100, message = "Tên cửa hàng phải từ 2 đến 100 ký tự")
    private String tenCuaHang;
    
    @Size(max = 2000, message = "Mô tả không được quá 2000 ký tự")
    private String moTa;
    
    private MultipartFile hinhAnh;
}