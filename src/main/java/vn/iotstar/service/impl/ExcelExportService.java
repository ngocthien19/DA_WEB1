package vn.iotstar.service.impl;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.entity.KhuyenMai;
import vn.iotstar.entity.CuaHang;
import vn.iotstar.entity.DatHang;
import vn.iotstar.entity.DatHangChiTiet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class ExcelExportService {

    // ============= CUSTOMER EXPORT =============
    public byte[] exportCustomersToExcel(List<NguoiDung> customers) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Khách hàng");

            // Tạo header style
            CellStyle headerStyle = createHeaderStyle(workbook);
            
            // Tạo data style
            CellStyle dataStyle = createDataStyle(workbook);

            // Tạo header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Tên khách hàng", "Email", "Số điện thoại", "Địa chỉ", "Trạng thái"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Thêm data với xử lý lỗi an toàn
            int rowNum = 1;
            for (NguoiDung customer : customers) {
                Row row = sheet.createRow(rowNum++);

                // Cột ID - Xử lý an toàn
                createSafeCell(row, 0, customer.getMaNguoiDung(), dataStyle);
                
                // Cột Tên khách hàng
                createSafeCell(row, 1, customer.getTenNguoiDung(), dataStyle);
                
                // Cột Email
                createSafeCell(row, 2, customer.getEmail(), dataStyle);
                
                // Cột Số điện thoại
                createSafeCell(row, 3, customer.getSdt(), dataStyle);
                
                // Cột Địa chỉ
                createSafeCell(row, 4, customer.getDiaChi(), dataStyle);
                
                // Cột Trạng thái - Xử lý đặc biệt
                createSafeCell(row, 5, customer.getTrangThai(), dataStyle);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    // ============= PROMOTION EXPORT =============

    /**
     * Export danh sách khuyến mãi cho vendor
     */
    public byte[] exportPromotionsToExcel(List<KhuyenMai> promotions, CuaHang currentStore) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Khuyến mãi - " + currentStore.getTenCuaHang());

            // Tạo styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle percentageStyle = createPercentageStyle(workbook);
            CellStyle statusStyle = createStatusStyle(workbook);

            // Tạo header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "Mã KM", "Mã giảm giá", "Discount (%)", "Ngày bắt đầu", 
                "Ngày kết thúc", "Số lượng", "Đã sử dụng", "Còn lại", 
                "Trạng thái", "Tình trạng hiện tại"
            };
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Thêm dữ liệu khuyến mãi
            int rowNum = 1;
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            
            for (KhuyenMai promotion : promotions) {
                Row row = sheet.createRow(rowNum++);

                // Mã khuyến mãi
                createSafeCell(row, 0, promotion.getMaKhuyenMai(), dataStyle);
                
                // Mã giảm giá
                createSafeCell(row, 1, promotion.getMaGiamGia(), dataStyle);
                
                // Discount (%)
                Cell discountCell = row.createCell(2);
                if (promotion.getDiscount() != null) {
                    discountCell.setCellValue(promotion.getDiscount().doubleValue());
                } else {
                    discountCell.setCellValue(0.0);
                }
                discountCell.setCellStyle(percentageStyle);
                
                // Ngày bắt đầu
                Cell startDateCell = row.createCell(3);
                if (promotion.getNgayBatDau() != null) {
                    startDateCell.setCellValue(promotion.getNgayBatDau().format(dateFormatter));
                } else {
                    startDateCell.setCellValue("N/A");
                }
                startDateCell.setCellStyle(dateStyle);
                
                // Ngày kết thúc
                Cell endDateCell = row.createCell(4);
                if (promotion.getNgayKetThuc() != null) {
                    endDateCell.setCellValue(promotion.getNgayKetThuc().format(dateFormatter));
                } else {
                    endDateCell.setCellValue("N/A");
                }
                endDateCell.setCellStyle(dateStyle);
                
                // Số lượng
                createSafeCell(row, 5, promotion.getSoLuongMaGiamGia(), dataStyle);
                
                // Đã sử dụng
                createSafeCell(row, 6, promotion.getSoLuongDaSuDung(), dataStyle);
                
                // Còn lại
                int remaining = promotion.getSoLuongMaGiamGia() - promotion.getSoLuongDaSuDung();
                createSafeCell(row, 7, remaining, dataStyle);
                
                // Trạng thái kích hoạt
                String statusText = Boolean.TRUE.equals(promotion.getTrangThai()) ? "Kích hoạt" : "Ngừng kích hoạt";
                Cell statusCell = row.createCell(8);
                statusCell.setCellValue(statusText);
                statusCell.setCellStyle(statusStyle);
                
                // Tình trạng hiện tại
                String currentStatus = getCurrentStatusText(promotion);
                createSafeCell(row, 9, currentStatus, dataStyle);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Thêm summary row
            addPromotionSummary(sheet, rowNum, promotions, dataStyle, headerStyle);

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Export chi tiết khuyến mãi (single promotion)
     */
    public byte[] exportPromotionDetailToExcel(KhuyenMai promotion) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Chi tiết khuyến mãi");

            // Tạo styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle labelStyle = createLabelStyle(workbook);
            CellStyle valueStyle = createDataStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);

            // Tiêu đề
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("CHI TIẾT KHUYẾN MÃI");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 3));

            // Thông tin cửa hàng
            Row storeRow = sheet.createRow(1);
            createSafeCell(storeRow, 0, "Cửa hàng:", labelStyle);
            createSafeCell(storeRow, 1, promotion.getCuaHang().getTenCuaHang(), valueStyle);
            createSafeCell(storeRow, 2, "Mã cửa hàng:", labelStyle);
            createSafeCell(storeRow, 3, promotion.getCuaHang().getMaCuaHang(), valueStyle);

            // Thông tin khuyến mãi
            int rowNum = 3;
            String[][] promotionData = {
                {"Mã khuyến mãi", promotion.getMaKhuyenMai().toString()},
                {"Mã giảm giá", promotion.getMaGiamGia()},
                {"Discount", promotion.getDiscount() + "%"},
                {"Ngày bắt đầu", promotion.getNgayBatDau().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))},
                {"Ngày kết thúc", promotion.getNgayKetThuc().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))},
                {"Số lượng mã", promotion.getSoLuongMaGiamGia().toString()},
                {"Đã sử dụng", promotion.getSoLuongDaSuDung().toString()},
                {"Còn lại", String.valueOf(promotion.getSoLuongMaGiamGia() - promotion.getSoLuongDaSuDung())},
                {"Trạng thái", Boolean.TRUE.equals(promotion.getTrangThai()) ? "Kích hoạt" : "Ngừng kích hoạt"},
                {"Tình trạng hiện tại", getCurrentStatusText(promotion)}
            };

            for (String[] data : promotionData) {
                Row row = sheet.createRow(rowNum++);
                createSafeCell(row, 0, data[0], labelStyle);
                createSafeCell(row, 1, data[1], valueStyle);
            }

            // Auto-size columns
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    // ============= HELPER METHODS =============

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setDataFormat(workbook.createDataFormat().getFormat("dd/mm/yyyy"));
        return style;
    }

    private CellStyle createPercentageStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));
        return style;
    }

    private CellStyle createStatusStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createLabelStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private String getCurrentStatusText(KhuyenMai promotion) {
        LocalDate now = LocalDate.now();
        if (!Boolean.TRUE.equals(promotion.getTrangThai())) {
            return "Ngừng kích hoạt";
        }
        if (now.isBefore(promotion.getNgayBatDau())) {
            return "Sắp diễn ra";
        }
        if (now.isAfter(promotion.getNgayKetThuc())) {
            return "Đã hết hạn";
        }
        return "Đang kích hoạt";
    }

    private void addPromotionSummary(Sheet sheet, int startRow, List<KhuyenMai> promotions, CellStyle dataStyle, CellStyle headerStyle) {
        int rowNum = startRow + 1;
        
        // Summary header
        Row summaryHeaderRow = sheet.createRow(rowNum++);
        Cell summaryHeaderCell = summaryHeaderRow.createCell(0);
        summaryHeaderCell.setCellValue("TỔNG HỢP KHUYẾN MÃI");
        summaryHeaderCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum-1, rowNum-1, 0, 4));

        // Calculate totals
        int totalPromotions = promotions.size();
        int activeCount = 0;
        int upcomingCount = 0;
        int expiredCount = 0;
        int inactiveCount = 0;
        int totalQuantity = 0;
        int totalUsed = 0;

        for (KhuyenMai promotion : promotions) {
            String status = getCurrentStatusText(promotion);
            switch (status) {
                case "Đang kích hoạt": activeCount++; break;
                case "Sắp diễn ra": upcomingCount++; break;
                case "Đã hết hạn": expiredCount++; break;
                case "Ngừng kích hoạt": inactiveCount++; break;
            }
            totalQuantity += promotion.getSoLuongMaGiamGia();
            totalUsed += promotion.getSoLuongDaSuDung();
        }

        // Summary data
        String[][] summaryData = {
            {"Tổng số khuyến mãi", String.valueOf(totalPromotions)},
            {"Đang kích hoạt", String.valueOf(activeCount)},
            {"Sắp diễn ra", String.valueOf(upcomingCount)},
            {"Đã hết hạn", String.valueOf(expiredCount)},
            {"Ngừng kích hoạt", String.valueOf(inactiveCount)},
            {"Tổng số lượng mã", String.valueOf(totalQuantity)},
            {"Tổng đã sử dụng", String.valueOf(totalUsed)},
            {"Tổng còn lại", String.valueOf(totalQuantity - totalUsed)}
        };

        for (String[] data : summaryData) {
            Row row = sheet.createRow(rowNum++);
            createSafeCell(row, 0, data[0], headerStyle);
            createSafeCell(row, 1, data[1], dataStyle);
        }
    }
    
    public byte[] exportAdminSalesHistoryToExcel(
            List<DatHangChiTiet> salesHistory, 
            CuaHang selectedStore,
            Map<String, Object> salesStats,
            boolean isSingleStore) {
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Lịch sử Bán hàng");
            
            // Tạo tiêu đề
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("LỊCH SỬ BÁN HÀNG - QUẢN TRỊ HỆ THỐNG");
            
            // Thông tin cửa hàng hoặc hệ thống
            Row storeRow = sheet.createRow(1);
            storeRow.createCell(0).setCellValue(
                isSingleStore ? "Cửa hàng: " + selectedStore.getTenCuaHang() : "Toàn hệ thống");
            
            // Thống kê
            Row statsRow1 = sheet.createRow(2);
            statsRow1.createCell(0).setCellValue("Tổng doanh thu: " + salesStats.get("totalRevenue"));
            statsRow1.createCell(1).setCellValue("Tổng đơn hàng: " + salesStats.get("totalCompletedOrders"));
            
            Row statsRow2 = sheet.createRow(3);
            statsRow2.createCell(0).setCellValue("Tổng sản phẩm: " + salesStats.get("totalProductsSold"));
            statsRow2.createCell(1).setCellValue("Doanh thu trung bình: " + salesStats.get("averageOrderValue"));
            
            if (!isSingleStore) {
                statsRow2.createCell(2).setCellValue("Số cửa hàng: " + salesStats.get("storeCount"));
            }
            
            // Tiêu đề bảng - THÊM CỘT "TRẠNG THÁI ĐH"
            Row headerRow = sheet.createRow(5);
            String[] headers = isSingleStore ? 
                new String[]{"Mã ĐH", "Khách hàng", "Sản phẩm", "Số lượng", "Đơn giá", "Thành tiền", "Ngày thanh toán", "PTTT", "Trạng thái TT", "Người giao", "Trạng thái ĐH"} :
                new String[]{"Mã ĐH", "Cửa hàng", "Khách hàng", "Sản phẩm", "Số lượng", "Đơn giá", "Thành tiền", "Ngày thanh toán", "PTTT", "Trạng thái TT", "Người giao", "Trạng thái ĐH"};
            
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            
            // Dữ liệu
            int rowNum = 6;
            for (DatHangChiTiet detail : salesHistory) {
                Row row = sheet.createRow(rowNum++);
                int cellNum = 0;
                
                row.createCell(cellNum++).setCellValue(detail.getDatHang().getMaDatHang());
                
                if (!isSingleStore) {
                    row.createCell(cellNum++).setCellValue(detail.getSanPham().getCuaHang().getTenCuaHang());
                }
                
                row.createCell(cellNum++).setCellValue(
                    detail.getDatHang().getNguoiDung().getTenNguoiDung() != null ? 
                    detail.getDatHang().getNguoiDung().getTenNguoiDung() : 
                    detail.getDatHang().getNguoiDung().getEmail());
                
                row.createCell(cellNum++).setCellValue(detail.getSanPham().getTenSanPham());
                row.createCell(cellNum++).setCellValue(detail.getSoLuong());
                row.createCell(cellNum++).setCellValue(detail.getGiaBan().doubleValue());
                row.createCell(cellNum++).setCellValue(detail.getThanhTien().doubleValue());
                
                String paymentDate = "Chưa thanh toán";
                if (detail.getDatHang().getThanhToans() != null && !detail.getDatHang().getThanhToans().isEmpty() && 
                    detail.getDatHang().getThanhToans().get(0).getNgayThanhToan() != null) {
                    paymentDate = detail.getDatHang().getThanhToans().get(0).getNgayThanhToan().toString();
                }
                row.createCell(cellNum++).setCellValue(paymentDate);
                
                String paymentMethod = "N/A";
                if (detail.getDatHang().getThanhToans() != null && !detail.getDatHang().getThanhToans().isEmpty()) {
                    paymentMethod = detail.getDatHang().getThanhToans().get(0).getPhuongThuc();
                }
                row.createCell(cellNum++).setCellValue(paymentMethod);
                
                String paymentStatus = "Chưa thanh toán";
                if (detail.getDatHang().getThanhToans() != null && !detail.getDatHang().getThanhToans().isEmpty()) {
                    paymentStatus = detail.getDatHang().getThanhToans().get(0).getTrangThai();
                }
                row.createCell(cellNum++).setCellValue(paymentStatus);
                
                String shipper = "N/A";
                if (detail.getDatHang().getVanChuyens() != null && !detail.getDatHang().getVanChuyens().isEmpty()) {
                    shipper = detail.getDatHang().getVanChuyens().get(0).getNguoiDung().getTenNguoiDung();
                }
                row.createCell(cellNum++).setCellValue(shipper);
                
                // THÊM CỘT TRẠNG THÁI ĐƠN HÀNG
                String orderStatus = detail.getDatHang().getTrangThai();
                row.createCell(cellNum).setCellValue(orderStatus);
            }
            
            // Tự động điều chỉnh độ rộng cột
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo file Excel: " + e.getMessage(), e);
        }
    }

    /**
     * Export lịch sử bán hàng cho vendor
     */
 // Trong phương thức exportSalesHistoryToExcel (cho vendor)
    public byte[] exportSalesHistoryToExcel(List<DatHangChiTiet> salesHistory, 
                                            CuaHang currentStore, 
                                            Map<String, Object> salesStats) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Lịch sử bán hàng");

            // Tạo styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);

            int rowNum = 0;
            
            // Tiêu đề
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("LỊCH SỬ BÁN HÀNG & DOANH THU");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 10)); // Tăng số cột merged
            
            // Thông tin cửa hàng
            Row storeRow = sheet.createRow(rowNum++);
            Cell storeCell = storeRow.createCell(0);
            storeCell.setCellValue("Cửa hàng: " + currentStore.getTenCuaHang());
            storeCell.setCellStyle(dataStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, 10)); // Tăng số cột merged
            
            // Ngày xuất
            Row dateRow = sheet.createRow(rowNum++);
            Cell dateCell = dateRow.createCell(0);
            // SỬA: Dùng LocalDateTime thay vì LocalDate
            dateCell.setCellValue("Ngày xuất: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            dateCell.setCellStyle(dataStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(2, 2, 0, 10));
            
            rowNum++; // Dòng trống

            // Tạo header row - THÊM CỘT "TRẠNG THÁI TT" và "TRẠNG THÁI ĐH"
            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {
                "Mã ĐH", "Khách hàng", "Email", "Sản phẩm", "Số lượng", 
                "Đơn giá", "Thành tiền", "Ngày thanh toán", "Phương thức TT", 
                "Trạng thái TT", "Người giao hàng", "Trạng thái ĐH" // THÊM 2 CỘT MỚI
            };
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Thêm dữ liệu
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            
            for (DatHangChiTiet detail : salesHistory) {
                Row row = sheet.createRow(rowNum++);
                DatHang order = detail.getDatHang();

                // Mã đơn hàng
                createSafeCell(row, 0, order.getMaDatHang(), dataStyle);
                
                // Khách hàng
                String customerName = order.getNguoiDung().getTenNguoiDung() != null ? 
                    order.getNguoiDung().getTenNguoiDung() : order.getNguoiDung().getEmail();
                createSafeCell(row, 1, customerName, dataStyle);
                
                // Email
                createSafeCell(row, 2, order.getNguoiDung().getEmail(), dataStyle);
                
                // Sản phẩm
                createSafeCell(row, 3, detail.getSanPham().getTenSanPham(), dataStyle);
                
                // Số lượng
                createSafeCell(row, 4, detail.getSoLuong(), dataStyle);
                
                // Đơn giá
                Cell priceCell = row.createCell(5);
                priceCell.setCellValue(detail.getGiaBan().doubleValue());
                priceCell.setCellStyle(currencyStyle);
                
                // Thành tiền
                Cell totalCell = row.createCell(6);
                totalCell.setCellValue(detail.getThanhTien().doubleValue());
                totalCell.setCellStyle(currencyStyle);
                
                // Ngày thanh toán
                Cell paymentDateCell = row.createCell(7);
                String paymentDate = "Chưa thanh toán";
                if (order.getThanhToans() != null && !order.getThanhToans().isEmpty()) {
                    var payment = order.getThanhToans().get(0);
                    if (payment.getNgayThanhToan() != null) {
                        paymentDate = new java.text.SimpleDateFormat("dd/MM/yyyy")
                            .format(payment.getNgayThanhToan());
                    }
                }
                paymentDateCell.setCellValue(paymentDate);
                paymentDateCell.setCellStyle(dateStyle);
                
                // Phương thức thanh toán
                String paymentMethod = "N/A";
                if (order.getThanhToans() != null && !order.getThanhToans().isEmpty()) {
                    paymentMethod = order.getThanhToans().get(0).getPhuongThuc();
                }
                createSafeCell(row, 8, paymentMethod, dataStyle);
                
                // TRẠNG THÁI THANH TOÁN - CỘT MỚI
                String paymentStatus = "Chưa thanh toán";
                if (order.getThanhToans() != null && !order.getThanhToans().isEmpty()) {
                    paymentStatus = order.getThanhToans().get(0).getTrangThai();
                    // Chuyển đổi sang tiếng Việt thân thiện
                    paymentStatus = getPaymentStatusText(paymentStatus);
                }
                createSafeCell(row, 9, paymentStatus, dataStyle);
                
                // Người giao hàng
                String shipperName = "N/A";
                if (order.getVanChuyens() != null && !order.getVanChuyens().isEmpty()) {
                    var shipping = order.getVanChuyens().get(0);
                    if (shipping.getNguoiDung() != null && shipping.getNguoiDung().getTenNguoiDung() != null) {
                        shipperName = shipping.getNguoiDung().getTenNguoiDung();
                    }
                }
                createSafeCell(row, 10, shipperName, dataStyle);
                
                // TRẠNG THÁI ĐƠN HÀNG - CỘT MỚI
                String orderStatus = order.getTrangThai();
                createSafeCell(row, 11, orderStatus, dataStyle);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Thêm thống kê
            addSalesStatsSummary(sheet, rowNum, salesStats, headerStyle, currencyStyle);

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    // Thêm phương thức chuyển đổi trạng thái thanh toán
    private String getPaymentStatusText(String status) {
        if (status == null) return "Không xác định";
        
        switch (status.toLowerCase()) {
            case "completed":
            case "hoàn thành":
            case "đã thanh toán":
                return "Hoàn thành";
            case "pending":
            case "đang xử lý":
                return "Đang xử lý";
            case "failed":
            case "thất bại":
            case "cancelled":
                return "Thất bại";
            case "refunded":
                return "Đã hoàn tiền";
            default:
                return status;
        }
    }
    
 // ============= STORES EXPORT =============
    public byte[] exportStoresToExcel(List<CuaHang> stores) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Cửa hàng");

            // Tạo styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle ratingStyle = createRatingStyle(workbook);

            // Tạo header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "ID", "Tên cửa hàng", "Chủ cửa hàng (Email)", "Địa chỉ", 
                "Năm TL", "Ngày tạo", "Đánh giá TB", "Số lượng ĐG", "Trạng thái"
            };
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Thêm data với xử lý lỗi an toàn
            int rowNum = 1;
            java.text.SimpleDateFormat dateFormatter = new java.text.SimpleDateFormat("dd/MM/yyyy");
            
            for (CuaHang store : stores) {
                Row row = sheet.createRow(rowNum++);

                // Cột ID
                createSafeCell(row, 0, store.getMaCuaHang(), dataStyle);
                
                // Cột Tên cửa hàng
                createSafeCell(row, 1, store.getTenCuaHang(), dataStyle);
                
                // Cột Chủ cửa hàng (Email)
                String ownerEmail = store.getNguoiDung() != null ? store.getNguoiDung().getEmail() : "N/A";
                createSafeCell(row, 2, ownerEmail, dataStyle);
                
                // Cột Địa chỉ
                createSafeCell(row, 3, store.getDiaChi(), dataStyle);
                
                // Cột Năm thành lập
                createSafeCell(row, 4, store.getNamThanhLap(), dataStyle);
                
                // Cột Ngày tạo
                Cell dateCell = row.createCell(5);
                if (store.getNgayTao() != null) {
                    dateCell.setCellValue(dateFormatter.format(store.getNgayTao()));
                } else {
                    dateCell.setCellValue("N/A");
                }
                dateCell.setCellStyle(dateStyle);
                
                // Cột Đánh giá TB
                Cell ratingCell = row.createCell(6);
                if (store.getDanhGiaTrungBinh() != null) {
                    ratingCell.setCellValue(store.getDanhGiaTrungBinh());
                } else {
                    ratingCell.setCellValue(0.0);
                }
                ratingCell.setCellStyle(ratingStyle);
                
                // Cột Số lượng ĐG
                createSafeCell(row, 7, store.getSoLuongDanhGia(), dataStyle);
                
                // Cột Trạng thái - SỬA LẠI PHẦN NÀY
                Cell statusCell = row.createCell(8);
                if (store.getTrangThai() != null) {
                    statusCell.setCellValue(store.getTrangThai() ? "Hoạt động" : "Đã khóa");
                } else {
                    statusCell.setCellValue("N/A");
                }
                statusCell.setCellStyle(dataStyle);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
    
    private CellStyle createRatingStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        // Format hiển thị số với 1 chữ số thập phân
        style.setDataFormat(workbook.createDataFormat().getFormat("0.0")); 
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private void addSalesStatsSummary(Sheet sheet, int startRow, Map<String, Object> salesStats, 
                                      CellStyle headerStyle, CellStyle currencyStyle) {
        int rowNum = startRow + 2; // Dòng trống
        
        // Summary header
        Row summaryHeaderRow = sheet.createRow(rowNum++);
        Cell summaryHeaderCell = summaryHeaderRow.createCell(0);
        summaryHeaderCell.setCellValue("TỔNG HỢP DOANH THU");
        summaryHeaderCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum-1, rowNum-1, 0, 3));

        // Tổng doanh thu
        Row revenueRow = sheet.createRow(rowNum++);
        Cell revenueLabel = revenueRow.createCell(0);
        revenueLabel.setCellValue("Tổng doanh thu:");
        revenueLabel.setCellStyle(headerStyle);
        
        Cell revenueValue = revenueRow.createCell(1);
        revenueValue.setCellValue((Double) salesStats.get("totalRevenue"));
        revenueValue.setCellStyle(currencyStyle);
        
        // Tổng đơn hàng
        Row ordersRow = sheet.createRow(rowNum++);
        Cell ordersLabel = ordersRow.createCell(0);
        ordersLabel.setCellValue("Tổng đơn hàng hoàn thành:");
        ordersLabel.setCellStyle(headerStyle);
        
        Cell ordersValue = ordersRow.createCell(1);
        ordersValue.setCellValue((Long) salesStats.get("totalCompletedOrders"));
        ordersValue.setCellStyle(headerStyle);
        
        // Tổng sản phẩm đã bán
        Row productsRow = sheet.createRow(rowNum++);
        Cell productsLabel = productsRow.createCell(0);
        productsLabel.setCellValue("Tổng sản phẩm đã bán:");
        productsLabel.setCellStyle(headerStyle);
        
        Cell productsValue = productsRow.createCell(1);
        productsValue.setCellValue((Long) salesStats.get("totalProductsSold"));
        productsValue.setCellStyle(headerStyle);
        
        // Giá trị đơn trung bình
        Row avgRow = sheet.createRow(rowNum++);
        Cell avgLabel = avgRow.createCell(0);
        avgLabel.setCellValue("Giá trị đơn trung bình:");
        avgLabel.setCellStyle(headerStyle);
        
        Cell avgValue = avgRow.createCell(1);
        avgValue.setCellValue((Double) salesStats.get("averageOrderValue"));
        avgValue.setCellStyle(currencyStyle);
    }
    
    private CellStyle createCurrencyStyle(Workbook workbook) {
	    CellStyle style = workbook.createCellStyle();
	    org.apache.poi.ss.usermodel.DataFormat format = workbook.createDataFormat();
	    style.setDataFormat(format.getFormat("#,##0.00"));
	    
	    // Tùy chọn: thêm border và alignment
	    style.setBorderTop(BorderStyle.THIN);
	    style.setBorderBottom(BorderStyle.THIN);
	    style.setBorderLeft(BorderStyle.THIN);
	    style.setBorderRight(BorderStyle.THIN);
	    style.setAlignment(HorizontalAlignment.RIGHT);
	    
	    return style;
	}

    /**
     * Phương thức hỗ trợ tạo cell an toàn với nhiều kiểu dữ liệu
     */
    private void createSafeCell(Row row, int column, Object value, CellStyle style) {
        Cell cell = row.createCell(column);
        
        try {
            if (value == null) {
                cell.setCellValue("N/A");
            } else if (value instanceof Integer) {
                cell.setCellValue((Integer) value);
            } else if (value instanceof Long) {
                cell.setCellValue((Long) value);
            } else if (value instanceof Double) {
                cell.setCellValue((Double) value);
            } else if (value instanceof Boolean) {
                cell.setCellValue((Boolean) value ? "Có" : "Không");
            } else if (value instanceof String) {
                // Xử lý đặc biệt cho trường hợp String có thể chuyển thành số
                String stringValue = (String) value;
                if (stringValue.matches("\\d+")) { // Nếu là chuỗi số
                    try {
                        cell.setCellValue(Long.parseLong(stringValue));
                    } catch (NumberFormatException e) {
                        cell.setCellValue(stringValue);
                    }
                } else {
                    cell.setCellValue(stringValue);
                }
            } else {
                // Mặc định sử dụng toString()
                cell.setCellValue(value.toString());
            }
        } catch (Exception e) {
            // Fallback an toàn
            cell.setCellValue(value != null ? value.toString() : "N/A");
        }
        
        if (style != null) {
            cell.setCellStyle(style);
        }
    }
}