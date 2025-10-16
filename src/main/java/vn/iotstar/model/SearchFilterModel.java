package vn.iotstar.model;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchFilterModel {
    private String keyword;
    private Integer maCuaHang;
    private Integer maDanhMuc;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String loaiSanPham;
    private Boolean trangThai;
    private String sortBy;
    private String sortDirection;
    private Integer page;
    private Integer size;
}