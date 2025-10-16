package vn.iotstar.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.entity.*;
import vn.iotstar.model.DatHangRequest;
import vn.iotstar.service.*;
import vn.iotstar.service.UserDetailsImpl;
import vn.iotstar.service.impl.QRCodeService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private GioHangService gioHangService;

    @Autowired
    private MatHangService matHangService;

    @Autowired
    private NguoiDungService nguoiDungService;

    @Autowired
    private KhuyenMaiService khuyenMaiService;

    @Autowired
    private DatHangService datHangService;

    @Autowired
    private DanhMucService danhMucService;

    @Autowired
    private ThanhToanService thanhToanService;
    
    @Autowired
    private QRCodeService qrCodeService;
    
    @GetMapping("/qr-payment")
    public String showQRPayment(@RequestParam Integer orderId, 
                              @RequestParam String method,
                              Model model) {
        try {
            DatHang datHang = datHangService.findByMaDatHang(orderId);
            if (datHang == null) {
                throw new RuntimeException("Đơn hàng không tồn tại");
            }

            String amount = datHang.getTongTien().toString();
            String description = "Thanh toan don hang " + orderId;
            
            String qrCodeBase64 = qrCodeService.generatePaymentQRCode(
                String.format("%s://payment?amount=%s&orderId=%s&description=%s", 
                    method.toLowerCase(), amount, orderId, description)
            );

            model.addAttribute("qrCode", qrCodeBase64);
            model.addAttribute("amount", datHang.getTongTien());
            model.addAttribute("description", "Thanh toán đơn hàng #" + orderId);
            model.addAttribute("method", method);
            model.addAttribute("title", "Quét mã để thanh toán bằng " + method);
            
            return "web/qr-payment";
            
        } catch (Exception e) {
            model.addAttribute("error", "Không thể tạo mã QR: " + e.getMessage());
            return "web/error";
        }
    }

    @GetMapping
    public String viewOrderPage(Model model) {
        try {
            // Kiểm tra đăng nhập
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication.getPrincipal().equals("anonymousUser")) {
                return "redirect:/login";
            }

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            String email = userDetails.getUsername();

            // Tìm người dùng
            NguoiDung nguoiDung = nguoiDungService.getUserByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            // Lấy giỏ hàng
            GioHang gioHang = gioHangService.findByNguoiDung(nguoiDung);
            if (gioHang == null) {
                return "redirect:/cart";
            }

            // Lấy danh sách mặt hàng
            List<MatHang> matHangs = matHangService.findByGioHang(gioHang);
            if (matHangs.isEmpty()) {
                return "redirect:/cart";
            }

            // Xử lý hình ảnh sản phẩm
            matHangs.forEach(matHang -> {
                if (matHang.getSanPham() != null && matHang.getSanPham().getHinhAnh() != null) {
                    String hinhAnh = matHang.getSanPham().getHinhAnh();
                    if (!hinhAnh.startsWith("/") && !hinhAnh.startsWith("http")) {
                        matHang.getSanPham().setHinhAnh("/" + hinhAnh);
                    }
                }
            });

            // Nhóm sản phẩm theo cửa hàng
            Map<CuaHang, List<MatHang>> productsByStore = matHangs.stream()
                    .collect(Collectors.groupingBy(mh -> mh.getSanPham().getCuaHang()));
            
            // Lấy khuyến mãi đang hoạt động cho từng cửa hàng
            Map<Integer, List<KhuyenMai>> promotionsByStore = new HashMap<>();
            // Tính tổng tiền cho từng cửa hàng
            Map<CuaHang, BigDecimal> storeTotals = new HashMap<>();
            
            for (CuaHang cuaHang : productsByStore.keySet()) {
                List<KhuyenMai> activePromotions = getValidPromotions(cuaHang);
                promotionsByStore.put(cuaHang.getMaCuaHang(), activePromotions);
                
                // Tính tổng tiền cho cửa hàng
                BigDecimal storeTotal = calculateStoreTotal(productsByStore.get(cuaHang));
                storeTotals.put(cuaHang, storeTotal);
            }

            // Tính tổng tiền
            BigDecimal tongTien = matHangs.stream()
                    .map(mh -> mh.getSanPham().getGiaBan().multiply(BigDecimal.valueOf(mh.getSoLuongDat())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Thêm danh mục vào model (cho header)
            List<DanhMuc> danhMucs = danhMucService.findAllActiveCategories();
            model.addAttribute("danhMucs", danhMucs);

            // Tạo DatHangRequest với thông tin mặc định
            DatHangRequest datHangRequest = new DatHangRequest();
            
            // Lấy địa chỉ mặc định từ thông tin người dùng
            String defaultDiaChi = getDefaultAddress(nguoiDung);
            datHangRequest.setDiaChiGiaoHang(defaultDiaChi);
            
            // Lấy số điện thoại mặc định từ thông tin người dùng
            String defaultSoDienThoai = getDefaultPhone(nguoiDung);
            datHangRequest.setSoDienThoai(defaultSoDienThoai);

            // Thêm vào model
            model.addAttribute("productsByStore", productsByStore);
            model.addAttribute("promotionsByStore", promotionsByStore);
            model.addAttribute("storeTotals", storeTotals);
            model.addAttribute("tongTien", tongTien);
            model.addAttribute("gioHang", gioHang);
            model.addAttribute("nguoiDung", nguoiDung);
            model.addAttribute("datHangRequest", datHangRequest);

            return "web/order";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Có lỗi xảy ra khi tải trang đặt hàng");
            
            // Thêm danh mục vào model ngay cả khi có lỗi
            List<DanhMuc> danhMucs = danhMucService.findAllActiveCategories();
            model.addAttribute("danhMucs", danhMucs);
            
            return "web/order";
        }
    }

    @PostMapping("/place")
    public String placeOrder(@ModelAttribute DatHangRequest datHangRequest, Model model) {
        try {
            // Kiểm tra đăng nhập
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication.getPrincipal().equals("anonymousUser")) {
                return "redirect:/login";
            }
            
         // Debug log để kiểm tra dữ liệu
            System.out.println("Địa chỉ giao hàng: " + datHangRequest.getDiaChiGiaoHang());
            System.out.println("Số điện thoại: " + datHangRequest.getSoDienThoai());
            System.out.println("Ghi chú: " + datHangRequest.getGhiChu());
            System.out.println("Khuyến mãi đã chọn: " + datHangRequest.getSelectedPromotions());

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            String email = userDetails.getUsername();

            // Tìm người dùng
            NguoiDung nguoiDung = nguoiDungService.getUserByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            // Kiểm tra phương thức thanh toán
            if (datHangRequest.getPhuongThucThanhToan() == null || datHangRequest.getPhuongThucThanhToan().isEmpty()) {
                throw new RuntimeException("Vui lòng chọn phương thức thanh toán");
            }

         // Xử lý đặt hàng
            DatHang datHang = datHangService.datHang(nguoiDung, datHangRequest);

            // Xử lý thanh toán
            if ("COD".equals(datHangRequest.getPhuongThucThanhToan())) {
                // COD: Tạo thanh toán với trạng thái "Pending"
                ThanhToan thanhToan = ThanhToan.builder()
                        .datHang(datHang)
                        .phuongThuc("COD")
                        .soTienThanhToan(datHang.getTongTien())
                        .ngayThanhToan(new Date())
                        .trangThai("Pending")
                        .build();
                thanhToanService.save(thanhToan);
                
                // Xóa giỏ hàng
                GioHang gioHang = gioHangService.findByNguoiDung(nguoiDung);
                if (gioHang != null) {
                    matHangService.clearCart(gioHang);
                }

                model.addAttribute("datHang", datHang);
                model.addAttribute("success", "Đặt hàng thành công! Vui lòng chuẩn bị tiền khi nhận hàng.");

                return "web/order-success";

            } else if ("MOMO".equals(datHangRequest.getPhuongThucThanhToan()) || 
                      "VNPAY".equals(datHangRequest.getPhuongThucThanhToan())) {
                // Sử dụng Mock Service cho thanh toán online
                return "redirect:/payment/process?orderId=" + datHang.getMaDatHang() + 
                       "&method=" + datHangRequest.getPhuongThucThanhToan();
            } else {
                throw new RuntimeException("Phương thức thanh toán không hợp lệ");
            }

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Có lỗi xảy ra khi đặt hàng: " + e.getMessage());
            
            // Thêm danh mục vào model
            List<DanhMuc> danhMucs = danhMucService.findAllActiveCategories();
            model.addAttribute("danhMucs", danhMucs);
            
            return "web/order";
        }
    }
    
    
    /**
     * Lấy địa chỉ mặc định từ thông tin người dùng
     */
    private String getDefaultAddress(NguoiDung nguoiDung) {
        // Ưu tiên theo thứ tự:
        // 1. Địa chỉ trong thông tin người dùng (nếu có)
        // 2. Địa chỉ từ các đơn hàng trước (nếu có)
        // 3. Địa chỉ mặc định
        
        if (nguoiDung.getDiaChi() != null && !nguoiDung.getDiaChi().trim().isEmpty()) {
            return nguoiDung.getDiaChi();
        }
        
        // Nếu không có địa chỉ trong thông tin người dùng, tìm từ đơn hàng trước
        List<DatHang> previousOrders = datHangService.findByNguoiDung(nguoiDung.getMaNguoiDung());
        if (previousOrders != null && !previousOrders.isEmpty()) {
            // Lấy địa chỉ từ đơn hàng gần nhất
            DatHang lastOrder = previousOrders.get(0);
            // Giả sử DatHang có trường diaChiGiaoHang
            // Nếu không có, bạn có thể thêm trường này vào entity DatHang
            // return lastOrder.getDiaChiGiaoHang();
        }
        
        // Trả về địa chỉ mặc định nếu không tìm thấy
        return "Chưa cập nhật địa chỉ";
    }

    /**
     * Lấy số điện thoại mặc định từ thông tin người dùng
     */
    private String getDefaultPhone(NguoiDung nguoiDung) {
        // Ưu tiên theo thứ tự:
        // 1. Số điện thoại trong thông tin người dùng (nếu có)
        // 2. Số điện thoại từ các đơn hàng trước (nếu có)
        // 3. Số điện thoại mặc định
        
        if (nguoiDung.getSdt() != null && !nguoiDung.getSdt().trim().isEmpty()) {
            return nguoiDung.getSdt();
        }
        
        // Nếu không có số điện thoại trong thông tin người dùng, tìm từ đơn hàng trước
        List<DatHang> previousOrders = datHangService.findByNguoiDung(nguoiDung.getMaNguoiDung());
        if (previousOrders != null && !previousOrders.isEmpty()) {
            // Lấy số điện thoại từ đơn hàng gần nhất
            DatHang lastOrder = previousOrders.get(0);
            // Giả sử DatHang có trường soDienThoai
            // return lastOrder.getSoDienThoai();
        }
        
        // Trả về số điện thoại mặc định nếu không tìm thấy
        return "Chưa cập nhật số điện thoại";
    }
    
    /**
     * Lọc khuyến mãi hợp lệ
     */
    private List<KhuyenMai> getValidPromotions(CuaHang cuaHang) {
        List<KhuyenMai> allPromotions = khuyenMaiService.findByCuaHang(cuaHang);
        LocalDate currentDate = LocalDate.now();
        
        return allPromotions.stream()
                .filter(promo -> promo.getTrangThai() != null && promo.getTrangThai()) // Đang kích hoạt
                .filter(promo -> promo.getNgayBatDau() != null && !promo.getNgayBatDau().isAfter(currentDate)) // Đã bắt đầu
                .filter(promo -> promo.getNgayKetThuc() != null && !promo.getNgayKetThuc().isBefore(currentDate)) // Chưa kết thúc
                .filter(promo -> promo.getSoLuongMaGiamGia() == null || 
                               promo.getSoLuongDaSuDung() == null || 
                               promo.getSoLuongDaSuDung() < promo.getSoLuongMaGiamGia()) // Còn số lượng
                .collect(Collectors.toList());
    }
    
    /**
     * Tính tổng tiền cho từng cửa hàng
     */
    private BigDecimal calculateStoreTotal(List<MatHang> matHangs) {
        return matHangs.stream()
                .filter(mh -> mh.getSanPham() != null)
                .map(mh -> mh.getSanPham().getGiaBan().multiply(BigDecimal.valueOf(mh.getSoLuongDat())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Tính tổng tiền cho từng cửa hàng (phương thức thay thế nếu cần)
     */
    private BigDecimal calculateStoreTotalAlternative(List<MatHang> matHangs) {
        BigDecimal total = BigDecimal.ZERO;
        for (MatHang matHang : matHangs) {
            if (matHang.getSanPham() != null) {
                BigDecimal itemTotal = matHang.getSanPham().getGiaBan()
                        .multiply(BigDecimal.valueOf(matHang.getSoLuongDat()));
                total = total.add(itemTotal);
            }
        }
        return total;
    }
    
    /**
     * Lấy tổng tiền dạng số nguyên (để sử dụng trong JavaScript)
     */
    private long getTotalAsLong(BigDecimal total) {
        return total != null ? total.longValue() : 0L;
    }
    
    /**
     * Lấy thông tin khuyến mãi cho template
     */
    private Map<String, Object> getPromotionInfo(KhuyenMai promotion) {
        Map<String, Object> info = new HashMap<>();
        info.put("maKhuyenMai", promotion.getMaKhuyenMai());
        info.put("maGiamGia", promotion.getMaGiamGia());
        info.put("discount", promotion.getDiscount());
        info.put("ngayBatDau", promotion.getNgayBatDau());
        info.put("ngayKetThuc", promotion.getNgayKetThuc());
        return info;
    }
    
    /**
     * Kiểm tra xem khuyến mãi có thể áp dụng không
     */
    private boolean isPromotionApplicable(KhuyenMai promotion, BigDecimal storeTotal) {
        if (promotion == null || storeTotal == null) {
            return false;
        }
        
        LocalDate today = LocalDate.now();
        
        // Kiểm tra trạng thái
        if (promotion.getTrangThai() == null || !promotion.getTrangThai()) {
            return false;
        }
        
        // Kiểm tra thời gian
        if (promotion.getNgayBatDau() == null || promotion.getNgayKetThuc() == null) {
            return false;
        }
        
        if (today.isBefore(promotion.getNgayBatDau()) || today.isAfter(promotion.getNgayKetThuc())) {
            return false;
        }
        
        // Kiểm tra số lượng
        if (promotion.getSoLuongMaGiamGia() != null && promotion.getSoLuongDaSuDung() != null) {
            if (promotion.getSoLuongDaSuDung() >= promotion.getSoLuongMaGiamGia()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Lấy danh sách khuyến mãi có thể áp dụng cho cửa hàng
     */
    private List<KhuyenMai> getApplicablePromotions(CuaHang cuaHang, BigDecimal storeTotal) {
        List<KhuyenMai> validPromotions = getValidPromotions(cuaHang);
        return validPromotions.stream()
                .filter(promo -> isPromotionApplicable(promo, storeTotal))
                .collect(Collectors.toList());
    }
    
    /**
     * Tạo thông báo lỗi chi tiết
     */
    private String createErrorMessage(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return "Đã xảy ra lỗi không xác định";
        }
        
        // Xử lý các loại lỗi phổ biến
        if (message.contains("Giỏ hàng trống")) {
            return "Giỏ hàng của bạn đang trống";
        } else if (message.contains("Không tìm thấy người dùng")) {
            return "Vui lòng đăng nhập để đặt hàng";
        } else if (message.contains("không đủ số lượng tồn kho")) {
            return message; // Giữ nguyên thông báo lỗi về số lượng
        } else {
            return "Có lỗi xảy ra: " + message;
        }
    }
    
    /**
     * Validate dữ liệu đơn hàng
     */
    private boolean validateOrderData(List<MatHang> matHangs) {
        if (matHangs == null || matHangs.isEmpty()) {
            return false;
        }
        
        for (MatHang matHang : matHangs) {
            if (matHang.getSanPham() == null) {
                return false;
            }
            
            if (matHang.getSoLuongDat() == null || matHang.getSoLuongDat() <= 0) {
                return false;
            }
            
            if (matHang.getSanPham().getGiaBan() == null || matHang.getSanPham().getGiaBan().compareTo(BigDecimal.ZERO) < 0) {
                return false;
            }
            
            // Kiểm tra tồn kho
            if (matHang.getSanPham().getSoLuongConLai() < matHang.getSoLuongDat()) {
                return false;
            }
        }
        
        return true;
    }
}