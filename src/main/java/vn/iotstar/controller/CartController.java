package vn.iotstar.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.iotstar.entity.GioHang;
import vn.iotstar.entity.MatHang;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.service.DanhMucService;
import vn.iotstar.service.GioHangService;
import vn.iotstar.service.MatHangService;
import vn.iotstar.service.NguoiDungService;
import vn.iotstar.service.UserDetailsImpl;
import vn.iotstar.entity.DanhMuc;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private GioHangService gioHangService;

    @Autowired
    private MatHangService matHangService;

    @Autowired
    private NguoiDungService nguoiDungService;
    
    @Autowired
    private DanhMucService danhMucService;

    @GetMapping
    public String viewCart(Model model) {
        try {
            // Lấy thông tin người dùng đang đăng nhập
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication.getPrincipal().equals("anonymousUser")) {
                // Chưa đăng nhập, chuyển đến trang đăng nhập
                return "redirect:/login";
            }

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            String email = userDetails.getUsername();

            // Tìm người dùng
            NguoiDung nguoiDung = nguoiDungService.getUserByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            // Lấy hoặc tạo giỏ hàng
            GioHang gioHang = gioHangService.getOrCreateGioHang(nguoiDung);

            // Lấy danh sách mặt hàng trong giỏ
            List<MatHang> matHangs = matHangService.findByGioHang(gioHang);
            
            // Xử lý hình ảnh sản phẩm trong giỏ hàng
            matHangs.forEach(matHang -> {
                if (matHang.getSanPham() != null && matHang.getSanPham().getHinhAnh() != null) {
                    String hinhAnh = matHang.getSanPham().getHinhAnh();
                    if (!hinhAnh.startsWith("/") && !hinhAnh.startsWith("http")) {
                        matHang.getSanPham().setHinhAnh("/" + hinhAnh); 
                    }
                }
            });

            // Tính tổng tiền (sử dụng BigDecimal)
            BigDecimal tongTien = matHangs.stream()
                    .map(mh -> mh.getSanPham().getGiaBan().multiply(BigDecimal.valueOf(mh.getSoLuongDat())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // TÍNH TỔNG SỐ LƯỢNG SẢN PHẨM ĐẶT (total quantity)
            int tongSoLuongSanPham = matHangs.stream()
                    .mapToInt(MatHang::getSoLuongDat)
                    .sum();

            // Thêm danh mục vào model (cho header)
            List<DanhMuc> danhMucs = danhMucService.findAllActiveCategories();
            model.addAttribute("danhMucs", danhMucs);

            // Thêm vào model
            model.addAttribute("matHangs", matHangs);
            model.addAttribute("tongTien", tongTien.doubleValue());
            model.addAttribute("soLuongMatHang", tongSoLuongSanPham);
            model.addAttribute("gioHang", gioHang);

            return "web/cart";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Có lỗi xảy ra khi tải giỏ hàng");
            model.addAttribute("soLuongMatHang", 0);
            
            // Thêm danh mục vào model ngay cả khi có lỗi
            List<DanhMuc> danhMucs = danhMucService.findAllActiveCategories();
            model.addAttribute("danhMucs", danhMucs);
            
            return "web/cart";
        }
    }
}