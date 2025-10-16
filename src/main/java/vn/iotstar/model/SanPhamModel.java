package vn.iotstar.model;

import lombok.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SanPhamModel {
    private Integer maSanPham;
    
    @NotNull(message = "Cửa hàng không được để trống")
    private Integer maCuaHang;
    
    @NotNull(message = "Danh mục không được để trống")
    private Integer maDanhMuc;
    
    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(min = 2, max = 255, message = "Tên sản phẩm phải từ 2 đến 255 ký tự")
    private String tenSanPham;
    
    @Size(max = 2000, message = "Mô tả sản phẩm không được quá 2000 ký tự")
    private String moTaSanPham;
    
    @NotNull(message = "Lượt thích không được để trống")
    @DecimalMin(value = "0", message = "Lượt thích không được âm")
    private BigDecimal luotThich;
    
    @NotNull(message = "Giá bán không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá bán phải lớn hơn 0")
    private BigDecimal giaBan;
    
    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 0, message = "Số lượng không được âm")
    private Integer soLuongConLai;
    
    @NotNull(message = "Số lượng đã bán không được để trống")
    @Min(value = 0, message = "Số lượng đã bán không được âm")
    private Integer soLuongDaBan;
    
    private Date ngayNhap;
    
    private String hinhAnh;
    
    @NotBlank(message = "Loại sản phẩm không được để trống")
    private String loaiSanPham;
    
    private Boolean trangThai;
    
    // For response
    private String tenDanhMuc;
    private String tenCuaHang;
    
    // Thêm trường mới cho danh sách loại sản phẩm có sẵn
    private List<String> availableLoaiSanPham;
}