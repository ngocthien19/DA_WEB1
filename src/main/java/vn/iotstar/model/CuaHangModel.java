package vn.iotstar.model;

import lombok.*;
import jakarta.validation.constraints.*;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuaHangModel {
    private Integer maCuaHang;
    
    @NotNull(message = "Chủ cửa hàng không được để trống")
    private Integer maNguoiDung;
    
    @NotBlank(message = "Tên cửa hàng không được để trống")
    @Size(min = 2, max = 100, message = "Tên cửa hàng phải từ 2 đến 100 ký tự")
    private String tenCuaHang;
    
    @Size(max = 2000, message = "Mô tả không được quá 2000 ký tự")
    private String moTa;
    
    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 255, message = "Địa chỉ không được quá 255 ký tự")
    private String diaChi;
    
    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại phải bắt đầu bằng 0 và có 10 chữ số")
    private String soDienThoai;
    
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;
    
    private Integer namThanhLap;
    
    @NotNull(message = "Trạng thái không được để trống")
    private Boolean trangThai;
    
    private String hinhAnh;
    private String lyDoKhoa;
    
    private Date ngayTao;
    private Double danhGiaTrungBinh;
    private Integer soLuongDanhGia;
    
    // For response
    private String tenNguoiDung;
    private String emailNguoiDung;
    private String sdtNguoiDung;
    private Integer soLuongSanPham;
    private Integer soLuongKhuyenMai;
}