package vn.iotstar.controller.vendor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.entity.CuaHang;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.entity.VaiTro;
import vn.iotstar.model.CuaHangModel;
import vn.iotstar.model.NguoiDungModel;
import vn.iotstar.service.CuaHangService;
import vn.iotstar.service.NguoiDungService;
import vn.iotstar.service.VaiTroService;

import java.util.Optional;

@Controller
public class VendorStoreController {
    
    @Autowired
    private CuaHangService cuaHangService;
    
    @Autowired
    private NguoiDungService nguoiDungService;
    
    @Autowired
    private VaiTroService vaiTroService;
    
    @GetMapping("/register-store")
    public String registerStorePage(Model model, RedirectAttributes redirectAttributes) {
        try {
            System.out.println("=== ACCESSING REGISTER STORE PAGE ===");
            
            // Lấy thông tin người dùng hiện tại
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = authentication.getName();
            System.out.println("Current user: " + currentUsername);
            
            // Kiểm tra xem người dùng đã có cửa hàng chưa
            Optional<NguoiDung> currentUser = nguoiDungService.getUserByEmail(currentUsername);
            if (currentUser.isPresent()) {
                System.out.println("User found: " + currentUser.get().getTenNguoiDung());
                
                // Kiểm tra nếu người dùng đã là VENDOR
                if ("VENDOR".equals(currentUser.get().getVaiTro().getMaVaiTro())) {
                    System.out.println("User is already VENDOR. Redirecting to Vendor Dashboard.");
                    // Chuyển hướng thẳng đến Vendor Dashboard
                    return "redirect:/vendor/dashboard"; 
                }
                
                model.addAttribute("currentUser", currentUser.get());
            } else {
                System.out.println("User not found with email: " + currentUsername);
            }
            
            // QUAN TRỌNG: Thêm đối tượng cuaHangModel vào model
            if (!model.containsAttribute("cuaHangModel")) {
                model.addAttribute("cuaHangModel", new CuaHangModel());
                System.out.println("Added new CuaHangModel to model");
            }
            
            System.out.println("=== RETURNING REGISTER STORE PAGE ===");
            return "vendor/register-store";
            
        } catch (Exception e) {
            System.err.println("Error in registerStorePage: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống: " + e.getMessage());
            return "redirect:/";
        }
    }
    
    @PostMapping("/register-store")
    public String registerStore(@ModelAttribute CuaHangModel cuaHangModel, 
                               Model model,
                               RedirectAttributes redirectAttributes) {
        try {
            System.out.println("=== PROCESSING STORE REGISTRATION ===");
            
            // Lấy thông tin người dùng hiện tại
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = authentication.getName();
            
            Optional<NguoiDung> currentUserOpt = nguoiDungService.getUserByEmail(currentUsername);
            if (!currentUserOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Người dùng không tồn tại");
                return "redirect:/register-store";
            }
            
            NguoiDung currentUser = currentUserOpt.get();
            
            // 1. Đổi role người dùng thành VENDOR
            Optional<VaiTro> vendorRole = vaiTroService.getVaiTroById("VENDOR");
            if (!vendorRole.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Vai trò VENDOR không tồn tại trong hệ thống");
                return "redirect:/register-store";
            }
            
            // Cập nhật role
            currentUser.setVaiTro(vendorRole.get());
            nguoiDungService.updateUser(currentUser.getMaNguoiDung(), 
                convertToNguoiDungModel(currentUser));
            
            // 2. Tạo cửa hàng mới
            cuaHangModel.setMaNguoiDung(currentUser.getMaNguoiDung());
            cuaHangModel.setTrangThai(true); // Cửa hàng hoạt động
            
            CuaHang cuaHang = cuaHangService.createStore(cuaHangModel);
            
            System.out.println("Store created successfully: " + cuaHang.getTenCuaHang());
            
            // *** THAY ĐỔI Ở ĐÂY: Chuyển hướng đến Vendor Dashboard và thêm thông báo Toast ***
            String successMessage = "Đăng ký cửa hàng thành công! Bây giờ bạn có thể bán hàng.";
            redirectAttributes.addFlashAttribute("toastMessage", successMessage);

            // Chuyển hướng đến Vendor Dashboard
            return "redirect:/vendor/dashboard"; 

        } catch (Exception e) {
            System.err.println("Error in registerStore: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Lỗi khi đăng ký cửa hàng: " + e.getMessage());
            redirectAttributes.addFlashAttribute("cuaHangModel", cuaHangModel);
            return "redirect:/register-store";
        }
    }
    
    private NguoiDungModel convertToNguoiDungModel(NguoiDung nguoiDung) {
        return NguoiDungModel.builder()
                .maNguoiDung(nguoiDung.getMaNguoiDung())
                .tenNguoiDung(nguoiDung.getTenNguoiDung())
                .email(nguoiDung.getEmail())
                .sdt(nguoiDung.getSdt())
                .diaChi(nguoiDung.getDiaChi())
                .hinhAnh(nguoiDung.getHinhAnh())
                .maVaiTro(nguoiDung.getVaiTro().getMaVaiTro())
                .trangThai(nguoiDung.getTrangThai())
                .build();
    }
}