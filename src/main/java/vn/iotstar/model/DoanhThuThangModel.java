package vn.iotstar.model;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoanhThuThangModel {
    private Integer thang;
    private Integer nam;
    private BigDecimal doanhThu;
    private Integer soDonHang;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class TopSanPhamModel {
    private Integer maSanPham;
    private String tenSanPham;
    private Integer soLuongBan;
    private BigDecimal doanhThu;
    private String hinhAnh;
}