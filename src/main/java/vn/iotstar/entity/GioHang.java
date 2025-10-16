package vn.iotstar.entity;

import lombok.*;
import java.time.LocalDate;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Entity
@Table(name = "GioHang")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GioHang {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaGioHang")
    private Integer maGioHang;
    
    @NotNull(message = "Người dùng không được để trống")
    @ManyToOne
    @JoinColumn(name = "MaNguoiDung", nullable = false)
    private NguoiDung nguoiDung;
    
    @Column(name = "NgayTao")
    @Builder.Default
    private LocalDate ngayTao = LocalDate.now();
    
    @OneToMany(mappedBy = "gioHang", cascade = CascadeType.ALL)
    private List<MatHang> matHangs;
}