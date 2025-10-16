package vn.iotstar.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.entity.GioHang;
import vn.iotstar.entity.MatHang;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.entity.SanPham;
import vn.iotstar.model.ApiResponse;
import vn.iotstar.repository.SanPhamRepository;
import vn.iotstar.service.GioHangService;
import vn.iotstar.service.MatHangService;
import vn.iotstar.service.NguoiDungService;
import vn.iotstar.service.UserDetailsImpl;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List; 
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartRestController {

    @Autowired
    private GioHangService gioHangService;

    @Autowired
    private MatHangService matHangService;

    @Autowired
    private NguoiDungService nguoiDungService;

    @Autowired
    private SanPhamRepository sanPhamRepository;
    
    // ========================= HÀM HỖ TRỢ TÍNH TOÁN =========================

    /**
     * Tính tổng số lượng đặt (total quantity)
     */
    private int calculateTotalQuantity(GioHang gioHang) {
        if (gioHang == null) return 0;
        List<MatHang> matHangs = matHangService.findByGioHang(gioHang);
        return matHangs.stream()
                .mapToInt(MatHang::getSoLuongDat)
                .sum();
    }
    
    /**
     * Tính tổng tiền (total price)
     */
    private BigDecimal calculateTotalPrice(GioHang gioHang) {
        if (gioHang == null) return BigDecimal.ZERO;
        List<MatHang> matHangs = matHangService.findByGioHang(gioHang);
        return matHangs.stream()
                .map(mh -> mh.getSanPham().getGiaBan().multiply(BigDecimal.valueOf(mh.getSoLuongDat())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    // ========================= ENDPOINT CẬP NHẬT =========================

    /**
     * Thêm sản phẩm vào giỏ hàng
     */
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addToCart(
            @RequestBody Map<String, Integer> request) {
        try {
            // Kiểm tra đăng nhập
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication.getPrincipal().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng"));
            }

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            String email = userDetails.getUsername();

         // Lấy thông tin từ request
            Integer maSanPham = (Integer) request.get("maSanPham"); 
            Integer soLuong = request.get("soLuong") != null ? 
                Integer.parseInt(request.get("soLuong").toString()) : 1;

            // Validate
            if (maSanPham == null || soLuong <= 0) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Thông tin sản phẩm không hợp lệ"));
            }

            // Tìm người dùng
            NguoiDung nguoiDung = nguoiDungService.getUserByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            // Tìm sản phẩm
            SanPham sanPham = sanPhamRepository.findById(maSanPham)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

            // Kiểm tra trạng thái sản phẩm
            if (!sanPham.getTrangThai()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Sản phẩm hiện không còn bán"));
            }

            // Lấy hoặc tạo giỏ hàng
            GioHang gioHang = gioHangService.getOrCreateGioHang(nguoiDung);
            
            // Lấy mặt hàng hiện tại (nếu có) để kiểm tra tổng số lượng
            MatHang matHangHienTai = matHangService.findByGioHangAndSanPham(gioHang, sanPham);
            int soLuongDaDat = matHangHienTai != null ? matHangHienTai.getSoLuongDat() : 0;
            int soLuongMoi = soLuongDaDat + soLuong;

            // Kiểm tra số lượng tồn kho TỔNG
            if (sanPham.getSoLuongConLai() < soLuongMoi) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Sản phẩm không đủ số lượng tồn kho"));
            }
            
            // Thêm hoặc cập nhật mặt hàng
            matHangService.addOrUpdateMatHang(gioHang, sanPham, soLuong);

            // Tính tổng số lượng (total quantity)
            int totalQuantity = calculateTotalQuantity(gioHang);

            Map<String, Object> data = new HashMap<>();
            data.put("totalQuantity", totalQuantity); // Đã đổi tên

            ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .message("Đã thêm sản phẩm vào giỏ hàng")
                    .data(data)
                    .build();
            return ResponseEntity.ok(response);	

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Có lỗi xảy ra: " + e.getMessage()));
        }
    }

    /**
     * Cập nhật số lượng mặt hàng trong giỏ
     */
    @PostMapping("/update")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateCart(
            @RequestBody Map<String, Integer> request) {
        try {
            // Kiểm tra đăng nhập
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication.getPrincipal().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Vui lòng đăng nhập"));
            }

            Integer maMatHang = request.get("maMatHang");
            Integer soLuong = request.get("soLuong");

            // Validate
            if (maMatHang == null || soLuong == null || soLuong <= 0) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Thông tin không hợp lệ"));
            }

            // Tìm mặt hàng
            MatHang matHang = matHangService.findById(maMatHang);
            if (matHang == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Không tìm thấy mặt hàng"));
            }

            // Kiểm tra số lượng tồn kho
            if (matHang.getSanPham().getSoLuongConLai() < soLuong) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Sản phẩm không đủ số lượng"));
            }

            // Cập nhật số lượng
            matHang.setSoLuongDat(soLuong);
            matHangService.save(matHang);

            // Tính toán lại
            GioHang gioHang = matHang.getGioHang();
            BigDecimal thanhTien = matHang.getSanPham().getGiaBan()
                    .multiply(BigDecimal.valueOf(soLuong));
            
            BigDecimal tongTien = calculateTotalPrice(gioHang);
            int totalQuantity = calculateTotalQuantity(gioHang); // Tính tổng số lượng

            Map<String, Object> data = new HashMap<>();
            data.put("thanhTien", thanhTien.doubleValue());
            data.put("tongTien", tongTien.doubleValue());
            data.put("totalQuantity", totalQuantity); // THÊM tổng số lượng

            ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .message("Đã cập nhật giỏ hàng")
                    .data(data)
                    .build();
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Có lỗi xảy ra: " + e.getMessage()));
        }
    }

    /**
     * Xóa sản phẩm khỏi giỏ hàng
     */
    @PostMapping("/remove")
    public ResponseEntity<ApiResponse<Map<String, Object>>> removeFromCart(
            @RequestBody Map<String, Integer> request) {
        try {
            // Kiểm tra đăng nhập
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication.getPrincipal().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Vui lòng đăng nhập"));
            }

            Integer maMatHang = request.get("maMatHang");

            // Validate
            if (maMatHang == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Thông tin không hợp lệ"));
            }

            // Tìm mặt hàng
            MatHang matHang = matHangService.findById(maMatHang);
            if (matHang == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Không tìm thấy mặt hàng"));
            }

            GioHang gioHang = matHang.getGioHang();

            // Xóa mặt hàng
            matHangService.deleteById(maMatHang);

            // Tính tổng số lượng và tổng tiền còn lại
            int totalQuantity = calculateTotalQuantity(gioHang);
            BigDecimal tongTien = calculateTotalPrice(gioHang);

            Map<String, Object> data = new HashMap<>();
            data.put("totalQuantity", totalQuantity); // Đã đổi tên
            data.put("tongTien", tongTien.doubleValue());

            ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .message("Đã xóa sản phẩm khỏi giỏ hàng")
                    .data(data)
                    .build();
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Có lỗi xảy ra: " + e.getMessage()));
        }
    }

    /**
     * Lấy tổng số lượng sản phẩm trong giỏ hàng (total quantity)
     */
    @GetMapping("/quantity")
    public ResponseEntity<ApiResponse<Integer>> getCartQuantity() {
        try {
            // Kiểm tra đăng nhập
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication.getPrincipal().equals("anonymousUser")) {
                return ResponseEntity.ok(ApiResponse.success(0));
            }

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            String email = userDetails.getUsername();

            // Tìm người dùng
            NguoiDung nguoiDung = nguoiDungService.getUserByEmail(email).orElse(null);
            if (nguoiDung == null) {
            	return ResponseEntity.ok(ApiResponse.success(0));
            }

            // Lấy giỏ hàng
            GioHang gioHang = gioHangService.findByNguoiDung(nguoiDung);
            if (gioHang == null) {
                return ResponseEntity.ok(ApiResponse.success(0));
            }

            // Tính tổng số lượng mặt hàng
            int totalQuantity = calculateTotalQuantity(gioHang);

            return ResponseEntity.ok(ApiResponse.success(totalQuantity));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(ApiResponse.success(0));
        }
    }

    /**
     * Xóa toàn bộ giỏ hàng
     */
    @PostMapping("/clear")
    public ResponseEntity<ApiResponse<String>> clearCart() {
        try {
            // Kiểm tra đăng nhập
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication.getPrincipal().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Vui lòng đăng nhập"));
            }

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            String email = userDetails.getUsername();

            // Tìm người dùng
            NguoiDung nguoiDung = nguoiDungService.getUserByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            // Lấy giỏ hàng
            GioHang gioHang = gioHangService.findByNguoiDung(nguoiDung);
            if (gioHang != null) {
                matHangService.clearCart(gioHang);
            }

            ApiResponse<String> response = ApiResponse.<String>builder()
                    .success(true)
                    .message("Đã xóa toàn bộ giỏ hàng")
                    .build();
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Có lỗi xảy ra: " + e.getMessage()));
        }
    }
}