package vn.iotstar.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import vn.iotstar.entity.*;
import vn.iotstar.repository.*;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private CuaHangRepository cuaHangRepository;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private DatHangRepository datHangRepository;

    @Autowired
    private DanhMucRepository danhMucRepository;

    /**
     * Hiển thị trang Dashboard chính
     */
    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        try {
            // Sử dụng SecurityContext để lấy thông tin authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication instanceof AnonymousAuthenticationToken) {
                return "redirect:/login?error=unauthorized";
            }
            
            // Lấy thông tin user từ principal
            String email = authentication.getName();
            NguoiDung user = nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Kiểm tra role ADMIN
            if (!"ADMIN".equals(user.getVaiTro().getMaVaiTro())) {
                return "redirect:/login?error=access_denied";
            }

            // Lấy dữ liệu thống kê từ database
            long totalUsers = nguoiDungRepository.count();
            long totalStores = cuaHangRepository.count();
            long totalProducts = sanPhamRepository.count();
            long totalOrders = datHangRepository.count();
            
            // Lấy danh sách mới nhất
            List<NguoiDung> recentUsers = nguoiDungRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "maNguoiDung"))).getContent();
            List<CuaHang> recentStores = cuaHangRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "ngayTao"))).getContent();
            List<SanPham> recentProducts = sanPhamRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "ngayNhap"))).getContent();
            List<DatHang> recentOrders = datHangRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "ngayDat"))).getContent();

            // Thêm dữ liệu vào model
            model.addAttribute("user", user);
            model.addAttribute("totalUsers", totalUsers);
            model.addAttribute("totalStores", totalStores);
            model.addAttribute("totalProducts", totalProducts);
            model.addAttribute("totalOrders", totalOrders);
            model.addAttribute("recentUsers", recentUsers);
            model.addAttribute("recentStores", recentStores);
            model.addAttribute("recentProducts", recentProducts);
            model.addAttribute("recentOrders", recentOrders);
            
            return "admin/dashboard";
            
        } catch (Exception e) {
            System.err.println("Error loading admin dashboard: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/login?error=system_error";
        }
    }

    /**
     * Redirect đến trang quản lý người dùng
     * NOTE: Trang quản lý người dùng thực tế được xử lý bởi AdminUserController
     */
    @GetMapping("/users-redirect")
    public String redirectToUserManagement() {
        return "redirect:/admin/users";
    }

    /**
     * Redirect đến trang quản lý cửa hàng
     * NOTE: Trang quản lý cửa hàng thực tế được xử lý bởi AdminStoreController
     */
    @GetMapping("/stores-redirect")
    public String redirectToStoreManagement() {
        return "redirect:/admin/stores";
    }

    /**
     * Redirect đến trang quản lý sản phẩm
     * NOTE: Trang quản lý sản phẩm thực tế được xử lý bởi AdminSanPhamController tại /admin/products
     */
    @GetMapping("/products-redirect")
    public String redirectToProductManagement() {
        return "redirect:/admin/products";
    }

    /**
     * Redirect đến trang quản lý danh mục
     * NOTE: Trang quản lý danh mục thực tế được xử lý bởi AdminCategoryController (nếu có)
     */
    @GetMapping("/categories-redirect")
    public String redirectToCategoryManagement() {
        return "redirect:/admin/categories";
    }

    /**
     * Redirect đến trang quản lý đơn hàng
     * NOTE: Trang quản lý đơn hàng thực tế được xử lý bởi AdminOrderController (nếu có)
     */
    @GetMapping("/orders-redirect")
    public String redirectToOrderManagement() {
        return "redirect:/admin/orders";
    }
}