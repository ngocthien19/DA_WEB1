package vn.iotstar.model;

import lombok.*;
import jakarta.validation.constraints.*;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DanhGiaModel {
    private Integer maDanhGia;
    
    @NotNull(message = "Sản phẩm không được để trống")
    private Integer maSanPham;
    
    @NotNull(message = "Người dùng không được để trống")
    private Integer maNguoiDung;
    
    @NotNull(message = "Số sao không được để trống")
    @Min(value = 1, message = "Số sao phải từ 1 đến 5")
    @Max(value = 5, message = "Số sao phải từ 1 đến 5")
    private Integer soSao;
    
    @NotBlank(message = "Bình luận không được để trống")
    @Size(min = 10, max = 1000, message = "Bình luận phải từ 10 đến 1000 ký tự")
    private String binhLuan;
    
    private Date ngayDanhGia;
    
    private String anhVideo;
    
    // For response
    private String tenSanPham;
    private String tenNguoiDung;
    private String hinhAnhSanPham;
}