package vn.iotstar.model;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThongKeModel {
    private Integer tongSoDonHang;
    private Integer donHangMoi;
    private Integer donHangDaGiao;
    private BigDecimal doanhThuThang;
    private BigDecimal doanhThuNam;
    private Integer tongSoSanPham;
    private Integer tongSoKhachHang;
    private Integer sanPhamSapHet;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class ThongKeCuaHangModel {
    private Integer maCuaHang;
    private String tenCuaHang;
    private Integer tongSoDonHang;
    private BigDecimal tongDoanhThu;
    private Integer tongSoSanPham;
    private Integer soLuongBanRa;
}