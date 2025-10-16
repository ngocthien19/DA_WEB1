package vn.iotstar.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorProfileDTO {
    private Integer maNguoiDung;
    private String tenNguoiDung;
    private String email;
    private String sdt;
    private String diaChi;
    private String hinhAnh;
    private String trangThai;
    private UserProfileDTO.VaiTroDTO vaiTro;
    private StoreInfoDTO storeInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreInfoDTO {
        private Integer maCuaHang;
        private String tenCuaHang;
        private String moTa;
        private String diaChi;
        private String soDienThoai;
        private String email;
        private String hinhAnh;
        private Integer namThanhLap;
        private Double danhGiaTrungBinh;
        private Integer soLuongDanhGia;
        private Boolean trangThai;
        private Date ngayTao;
    }
}