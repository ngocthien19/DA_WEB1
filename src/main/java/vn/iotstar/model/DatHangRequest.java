package vn.iotstar.model;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class DatHangRequest {
    private Map<Integer, Integer> selectedPromotions; // Map<MaCuaHang, MaKhuyenMai>
    private String ghiChu;
    private String diaChiGiaoHang;
    private String soDienThoai;
    private String phuongThucThanhToan;
}