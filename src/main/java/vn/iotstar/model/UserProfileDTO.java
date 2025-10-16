package vn.iotstar.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDTO {
    private Integer maNguoiDung;
    private String tenNguoiDung;
    private String email;
    private String sdt;
    private String diaChi;
    private String hinhAnh;
    private String trangThai;
    private VaiTroDTO vaiTro;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VaiTroDTO {
        private String maVaiTro;
        private String tenVaiTro;
    }
}