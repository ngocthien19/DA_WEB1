package vn.iotstar.service.impl;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.iotstar.entity.CuaHang;
import vn.iotstar.entity.DanhMuc;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.model.CuaHangModel;
import vn.iotstar.model.ExcelImportResult;
import vn.iotstar.model.KhuyenMaiImportResult;
import vn.iotstar.model.KhuyenMaiModel;
import vn.iotstar.model.SanPhamModel;
import vn.iotstar.service.CuaHangService;
import vn.iotstar.service.DanhMucService;
import vn.iotstar.service.KhuyenMaiService;
import vn.iotstar.service.NguoiDungService;
import vn.iotstar.service.SanPhamService;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ExcelImportService {

    @Autowired
    private CuaHangService cuaHangService;

    @Autowired
    private DanhMucService danhMucService;

    @Autowired
    private SanPhamService sanPhamService;

    @Autowired
    private KhuyenMaiService khuyenMaiService;

    @Autowired
    private NguoiDungService nguoiDungService;

    // ============= METHODS FOR KHUYENMAI IMPORT =============

    /**
     * Import khuyến mãi từ single Excel file cho vendor
     */
    public KhuyenMaiImportResult importKhuyenMaiFromExcelForVendor(MultipartFile file, CuaHang currentStore) {
        KhuyenMaiImportResult result = KhuyenMaiImportResult.builder()
                .success(false)
                .errors(new ArrayList<>())
                .importedKhuyenMai(new ArrayList<>())
                .build();

        try {
            // Validate file
            if (file.isEmpty()) {
                result.addError("File Excel không được để trống");
                return result;
            }

            if (!file.getOriginalFilename().endsWith(".xlsx")) {
                result.addError("Chỉ hỗ trợ file Excel định dạng .xlsx");
                return result;
            }

            InputStream inputStream = file.getInputStream();
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0); // Lấy sheet đầu tiên

            // Validate header cho vendor
            Row headerRow = sheet.getRow(0);
            if (!validateHeaderForKhuyenMaiVendor(headerRow)) {
                result.addError("Định dạng file Excel không đúng. Vui lòng tải template mẫu cho vendor.");
                workbook.close();
                return result;
            }

            // Process data rows
            int totalRecords = 0;
            int successCount = 0;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                totalRecords++;
                try {
                    KhuyenMaiModel khuyenMaiModel = parseRowToKhuyenMaiModelForVendor(row, i + 1, currentStore);
                    if (khuyenMaiModel != null) {
                        // Validate required fields
                        if (validateKhuyenMaiModel(khuyenMaiModel)) {
                            result.addImportedKhuyenMai(khuyenMaiModel);
                            successCount++;
                        }
                    }
                } catch (Exception e) {
                    result.addError("Dòng " + (i + 1) + ": " + e.getMessage());
                }
            }

            workbook.close();

            result.setTotalRecords(totalRecords);
            result.setSuccessCount(successCount);
            result.setErrorCount(totalRecords - successCount);
            result.setSuccess(true);
            result.setMessage(String.format("Import thành công %d/%d khuyến mãi", successCount, totalRecords));

        } catch (IOException e) {
            result.addError("Lỗi đọc file: " + e.getMessage());
        } catch (Exception e) {
            result.addError("Lỗi xử lý file: " + e.getMessage());
        }

        return result;
    }

    /**
     * Import khuyến mãi từ multiple Excel files cho vendor
     */
    public KhuyenMaiImportResult importKhuyenMaiFromMultipleExcelForVendor(List<MultipartFile> excelFiles, CuaHang currentStore) {
        KhuyenMaiImportResult finalResult = KhuyenMaiImportResult.builder()
                .success(false)
                .errors(new ArrayList<>())
                .importedKhuyenMai(new ArrayList<>())
                .totalRecords(0)
                .successCount(0)
                .errorCount(0)
                .build();

        try {
            // Validate files
            if (excelFiles == null || excelFiles.isEmpty()) {
                finalResult.addError("Không có file nào được chọn");
                return finalResult;
            }

            // Validate số lượng file
            if (excelFiles.size() > 5) {
                finalResult.addError("Chỉ được phép import tối đa 5 file");
                return finalResult;
            }

            int totalFiles = excelFiles.size();
            int successFiles = 0;
            int failedFiles = 0;

            // Process each file
            for (int i = 0; i < excelFiles.size(); i++) {
                MultipartFile file = excelFiles.get(i);
                String fileName = file.getOriginalFilename();

                try {
                    KhuyenMaiImportResult fileResult = importKhuyenMaiFromExcelForVendor(file, currentStore);
                    
                    if (fileResult.isSuccess()) {
                        successFiles++;
                        // Merge results
                        finalResult.setTotalRecords(finalResult.getTotalRecords() + fileResult.getTotalRecords());
                        finalResult.setSuccessCount(finalResult.getSuccessCount() + fileResult.getSuccessCount());
                        finalResult.setErrorCount(finalResult.getErrorCount() + fileResult.getErrorCount());
                        
                        if (fileResult.getImportedKhuyenMai() != null) {
                            finalResult.getImportedKhuyenMai().addAll(fileResult.getImportedKhuyenMai());
                        }
                        
                        // Add success message for this file
                        finalResult.addError("✓ File " + fileName + ": " + fileResult.getMessage());
                    } else {
                        failedFiles++;
                        // Add errors from this file
                        if (fileResult.getErrors() != null) {
                            for (String error : fileResult.getErrors()) {
                                finalResult.addError("✗ File " + fileName + ": " + error);
                            }
                        }
                    }
                    
                } catch (Exception e) {
                    failedFiles++;
                    finalResult.addError("✗ File " + fileName + ": Lỗi xử lý - " + e.getMessage());
                }
            }

            // Set final result
            if (successFiles > 0) {
                finalResult.setSuccess(true);
                finalResult.setMessage(String.format(
                    "Import hoàn tất. Thành công: %d/%d file. Tổng khuyến mãi: %d thành công, %d lỗi",
                    successFiles, totalFiles, finalResult.getSuccessCount(), finalResult.getErrorCount()
                ));
            } else {
                finalResult.setSuccess(false);
                finalResult.setMessage("Import thất bại tất cả " + totalFiles + " file");
            }

        } catch (Exception e) {
            finalResult.addError("Lỗi hệ thống: " + e.getMessage());
        }

        return finalResult;
    }

    /**
     * Import khuyến mãi từ multiple Excel files và lưu vào database cho vendor
     * VỚI XỬ LÝ BỎ QUA MÃ GIẢM GIÁ TRÙNG
     */
    public KhuyenMaiImportResult importAndSaveKhuyenMaiFromMultipleExcelForVendor(List<MultipartFile> excelFiles, CuaHang currentStore) {
        KhuyenMaiImportResult importResult = importKhuyenMaiFromMultipleExcelForVendor(excelFiles, currentStore);
        
        if (importResult.isSuccess() && importResult.getImportedKhuyenMai() != null) {
            // Save valid khuyến mãi to database với xử lý mã trùng
            int savedCount = 0;
            int skippedCount = 0;
            List<String> saveErrors = new ArrayList<>();
            List<String> skipMessages = new ArrayList<>();
            
            for (KhuyenMaiModel khuyenMaiModel : importResult.getImportedKhuyenMai()) {
                try {
                    // ✅ KIỂM TRA MÃ GIẢM GIÁ TRƯỚC KHI LƯU - BỎ QUA NẾU TRÙNG
                    if (khuyenMaiService.isMaGiamGiaExists(currentStore, khuyenMaiModel.getMaGiamGia())) {
                        skippedCount++;
                        skipMessages.add("Mã giảm giá '" + khuyenMaiModel.getMaGiamGia() + "' đã tồn tại - Đã bỏ qua");
                        continue; // Bỏ qua mã trùng và tiếp tục với mã khác
                    }
                    
                    khuyenMaiService.createKhuyenMai(khuyenMaiModel);
                    savedCount++;
                    
                } catch (Exception e) {
                    saveErrors.add("Lỗi lưu khuyến mãi '" + khuyenMaiModel.getMaGiamGia() + "': " + e.getMessage());
                }
            }
            
            // Update result với save information
            importResult.setSuccessCount(savedCount);
            importResult.setErrorCount(importResult.getTotalRecords() - savedCount - skippedCount);
            
            // Add save errors và skip messages to result
            if (!saveErrors.isEmpty()) {
                importResult.getErrors().addAll(saveErrors);
            }
            if (!skipMessages.isEmpty()) {
                importResult.getErrors().addAll(skipMessages);
            }
            
            // Update message với thông tin đầy đủ
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append(importResult.getMessage());
            if (savedCount > 0) {
                messageBuilder.append(" - Đã lưu ").append(savedCount).append(" khuyến mãi vào database");
            }
            if (skippedCount > 0) {
                messageBuilder.append(" - Đã bỏ qua ").append(skippedCount).append(" khuyến mãi (mã trùng)");
            }
            importResult.setMessage(messageBuilder.toString());
            
            // Đánh dấu thành công nếu có ít nhất một bản ghi được lưu
            importResult.setSuccess(savedCount > 0);
        }
        
        return importResult;
    }

    /**
     * Tạo template Excel mẫu cho vendor khuyến mãi
     */
    public byte[] generateKhuyenMaiTemplateForVendor(CuaHang currentStore) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Template Khuyến mãi - Vendor");
            
            // Tạo style cho header
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            // Tạo header row cho vendor khuyến mãi
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "Mã giảm giá", "Discount (%)", "Ngày bắt đầu (yyyy-MM-dd)", 
                "Ngày kết thúc (yyyy-MM-dd)", "Số lượng", "Trạng thái"
            };
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Tạo dòng mẫu
            Row sampleRow = sheet.createRow(1);
            sampleRow.createCell(0).setCellValue("SUMMER2024");
            sampleRow.createCell(1).setCellValue(15.5);
            
            // Định dạng ngày theo LocalDate (yyyy-MM-dd)
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy-MM-dd"));
            
            Cell startDateCell = sampleRow.createCell(2);
            startDateCell.setCellValue(LocalDate.now().toString()); // Định dạng yyyy-MM-dd
            startDateCell.setCellStyle(dateStyle);
            
            Cell endDateCell = sampleRow.createCell(3);
            endDateCell.setCellValue(LocalDate.now().plusDays(7).toString()); // +7 days
            endDateCell.setCellStyle(dateStyle);
            
            sampleRow.createCell(4).setCellValue(100);
            sampleRow.createCell(5).setCellValue("true");
            
            // Thêm thông tin cửa hàng
            Row infoRow1 = sheet.createRow(3);
            infoRow1.createCell(0).setCellValue("Lưu ý:");
            Row infoRow2 = sheet.createRow(4);
            infoRow2.createCell(0).setCellValue("Cửa hàng: " + currentStore.getTenCuaHang() + " (Mã: " + currentStore.getMaCuaHang() + ")");
            Row infoRow3 = sheet.createRow(5);
            infoRow3.createCell(0).setCellValue("Tất cả khuyến mãi sẽ được thêm vào cửa hàng của bạn");
            Row infoRow4 = sheet.createRow(6);
            infoRow4.createCell(0).setCellValue("Định dạng ngày: yyyy-MM-dd (ví dụ: 2024-12-25)");
            Row infoRow5 = sheet.createRow(7);
            infoRow5.createCell(0).setCellValue("Discount: từ 0.0 đến 100.0 (%)");
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Convert to byte array
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    // ============= PRIVATE HELPER METHODS FOR KHUYENMAI =============

    private boolean validateHeaderForKhuyenMaiVendor(Row headerRow) {
        if (headerRow == null) return false;

        String[] expectedHeaders = {
            "Mã giảm giá", "Discount (%)", "Ngày bắt đầu (yyyy-MM-dd)", 
            "Ngày kết thúc (yyyy-MM-dd)", "Số lượng", "Trạng thái"
        };

        for (int i = 0; i < expectedHeaders.length; i++) {
            Cell cell = headerRow.getCell(i);
            if (cell == null || !expectedHeaders[i].equals(getCellValueAsString(cell))) {
                return false;
            }
        }
        return true;
    }

    private KhuyenMaiModel parseRowToKhuyenMaiModelForVendor(Row row, int rowNumber, CuaHang currentStore) {
        try {
            KhuyenMaiModel.KhuyenMaiModelBuilder builder = KhuyenMaiModel.builder();

            // Mã giảm giá (required)
            String maGiamGia = getCellValueAsString(row.getCell(0));
            if (maGiamGia == null || maGiamGia.trim().isEmpty()) {
                throw new RuntimeException("Mã giảm giá không được để trống");
            }
            if (maGiamGia.length() < 3 || maGiamGia.length() > 50) {
                throw new RuntimeException("Mã giảm giá phải từ 3 đến 50 ký tự");
            }
            builder.maGiamGia(maGiamGia.trim());

            // Mã cửa hàng (tự động lấy từ currentStore)
            builder.maCuaHang(currentStore.getMaCuaHang());

            // Discount (required)
            BigDecimal discount = getCellValueAsBigDecimal(row.getCell(1));
            if (discount == null) {
                throw new RuntimeException("Discount không được để trống");
            }
            if (discount.compareTo(BigDecimal.ZERO) < 0 || discount.compareTo(new BigDecimal("100")) > 0) {
                throw new RuntimeException("Discount phải từ 0.0 đến 100.0");
            }
            builder.discount(discount);

            // Ngày bắt đầu (required) - Sử dụng LocalDate
            LocalDate ngayBatDau = getCellValueAsLocalDate(row.getCell(2));
            if (ngayBatDau == null) {
                throw new RuntimeException("Ngày bắt đầu không được để trống");
            }
            // Kiểm tra ngày bắt đầu phải là hiện tại hoặc tương lai
            LocalDate today = LocalDate.now();
            if (ngayBatDau.isBefore(today)) {
                throw new RuntimeException("Ngày bắt đầu phải là hiện tại hoặc tương lai");
            }
            builder.ngayBatDau(ngayBatDau);

            // Ngày kết thúc (required) - Sử dụng LocalDate
            LocalDate ngayKetThuc = getCellValueAsLocalDate(row.getCell(3));
            if (ngayKetThuc == null) {
                throw new RuntimeException("Ngày kết thúc không được để trống");
            }
            // Kiểm tra ngày kết thúc phải sau ngày bắt đầu
            if (ngayKetThuc.isBefore(ngayBatDau) || ngayKetThuc.isEqual(ngayBatDau)) {
                throw new RuntimeException("Ngày kết thúc phải sau ngày bắt đầu");
            }
            builder.ngayKetThuc(ngayKetThuc);

            // Số lượng (required)
            Integer soLuong = getCellValueAsInteger(row.getCell(4));
            if (soLuong == null || soLuong < 0) {
                throw new RuntimeException("Số lượng không được âm");
            }
            builder.soLuongMaGiamGia(soLuong);

            // Trạng thái (optional, default true)
            Boolean trangThai = getCellValueAsBoolean(row.getCell(5));
            builder.trangThai(trangThai != null ? trangThai : true);

            // Số lượng đã sử dụng mặc định là 0
            builder.soLuongDaSuDung(0);

            return builder.build();

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private boolean validateKhuyenMaiModel(KhuyenMaiModel model) {
        return model.getMaGiamGia() != null && !model.getMaGiamGia().trim().isEmpty()
                && model.getMaCuaHang() != null
                && model.getDiscount() != null 
                && model.getDiscount().compareTo(BigDecimal.ZERO) >= 0 
                && model.getDiscount().compareTo(new BigDecimal("100")) <= 0
                && model.getNgayBatDau() != null
                && model.getNgayKetThuc() != null
                && model.getSoLuongMaGiamGia() != null && model.getSoLuongMaGiamGia() >= 0;
    }

    // Helper method để lấy giá trị cell dạng LocalDate
    private LocalDate getCellValueAsLocalDate(Cell cell) {
        if (cell == null) return null;
        
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        Date date = cell.getDateCellValue();
                        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    } else {
                        // Nếu là số, chuyển đổi từ số serial Excel date
                        Date date = DateUtil.getJavaDate(cell.getNumericCellValue());
                        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    }
                case STRING:
                    String dateString = cell.getStringCellValue().trim();
                    if (dateString.isEmpty()) return null;
                    
                    // Parse các định dạng ngày thông dụng
                    DateTimeFormatter[] formatters = {
                        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                        DateTimeFormatter.ofPattern("MM/dd/yyyy")
                    };
                    
                    for (DateTimeFormatter formatter : formatters) {
                        try {
                            return LocalDate.parse(dateString, formatter);
                        } catch (DateTimeParseException e) {
                            // Continue to next format
                        }
                    }
                    throw new RuntimeException("Định dạng ngày không hợp lệ: " + dateString + ". Định dạng hỗ trợ: yyyy-MM-dd, dd/MM/yyyy, dd-MM-yyyy");
                default:
                    return null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Không thể đọc giá trị ngày: " + e.getMessage());
        }
    }
    
    /**
     * Import products từ single Excel file
     */
    public ExcelImportResult importProductsFromExcel(MultipartFile file) {
        ExcelImportResult result = ExcelImportResult.builder()
                .success(false)
                .errors(new ArrayList<>())
                .importedProducts(new ArrayList<>())
                .build();

        try {
            // Validate file
            if (file.isEmpty()) {
                result.addError("File Excel không được để trống");
                return result;
            }

            if (!file.getOriginalFilename().endsWith(".xlsx")) {
                result.addError("Chỉ hỗ trợ file Excel định dạng .xlsx");
                return result;
            }

            InputStream inputStream = file.getInputStream();
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0); // Lấy sheet đầu tiên

            // Validate header
            Row headerRow = sheet.getRow(0);
            if (!validateHeader(headerRow)) {
                result.addError("Định dạng file Excel không đúng. Vui lòng tải template mẫu.");
                workbook.close();
                return result;
            }

            // Process data rows
            int totalRecords = 0;
            int successCount = 0;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                totalRecords++;
                try {
                    SanPhamModel productModel = parseRowToProductModel(row, i + 1);
                    if (productModel != null) {
                        // Validate required fields
                        if (validateProductModel(productModel)) {
                            result.addImportedProduct(productModel);
                            successCount++;
                        }
                    }
                } catch (Exception e) {
                    result.addError("Dòng " + (i + 1) + ": " + e.getMessage());
                }
            }

            workbook.close();

            result.setTotalRecords(totalRecords);
            result.setSuccessCount(successCount);
            result.setErrorCount(totalRecords - successCount);
            result.setSuccess(true);
            result.setMessage(String.format("Import thành công %d/%d sản phẩm", successCount, totalRecords));

        } catch (IOException e) {
            result.addError("Lỗi đọc file: " + e.getMessage());
        } catch (Exception e) {
            result.addError("Lỗi xử lý file: " + e.getMessage());
        }

        return result;
    }

    /**
     * Import products từ multiple Excel files
     */
    public ExcelImportResult importProductsFromMultipleExcel(List<MultipartFile> excelFiles) {
        ExcelImportResult finalResult = ExcelImportResult.builder()
                .success(false)
                .errors(new ArrayList<>())
                .importedProducts(new ArrayList<>())
                .totalRecords(0)
                .successCount(0)
                .errorCount(0)
                .build();

        try {
            // Validate files
            if (excelFiles == null || excelFiles.isEmpty()) {
                finalResult.addError("Không có file nào được chọn");
                return finalResult;
            }

            // Validate số lượng file
            if (excelFiles.size() > 5) {
                finalResult.addError("Chỉ được phép import tối đa 5 file");
                return finalResult;
            }

            int totalFiles = excelFiles.size();
            int successFiles = 0;
            int failedFiles = 0;

            // Process each file
            for (int i = 0; i < excelFiles.size(); i++) {
                MultipartFile file = excelFiles.get(i);
                String fileName = file.getOriginalFilename();

                try {
                    ExcelImportResult fileResult = importProductsFromExcel(file);
                    
                    if (fileResult.isSuccess()) {
                        successFiles++;
                        // Merge results
                        finalResult.setTotalRecords(finalResult.getTotalRecords() + fileResult.getTotalRecords());
                        finalResult.setSuccessCount(finalResult.getSuccessCount() + fileResult.getSuccessCount());
                        finalResult.setErrorCount(finalResult.getErrorCount() + fileResult.getErrorCount());
                        
                        if (fileResult.getImportedProducts() != null) {
                            finalResult.getImportedProducts().addAll(fileResult.getImportedProducts());
                        }
                        
                        // Add success message for this file
                        finalResult.addError("✓ File " + fileName + ": " + fileResult.getMessage());
                    } else {
                        failedFiles++;
                        // Add errors from this file
                        if (fileResult.getErrors() != null) {
                            for (String error : fileResult.getErrors()) {
                                finalResult.addError("✗ File " + fileName + ": " + error);
                            }
                        }
                    }
                    
                } catch (Exception e) {
                    failedFiles++;
                    finalResult.addError("✗ File " + fileName + ": Lỗi xử lý - " + e.getMessage());
                }
            }

            // Set final result
            if (successFiles > 0) {
                finalResult.setSuccess(true);
                finalResult.setMessage(String.format(
                    "Import hoàn tất. Thành công: %d/%d file. Tổng sản phẩm: %d thành công, %d lỗi",
                    successFiles, totalFiles, finalResult.getSuccessCount(), finalResult.getErrorCount()
                ));
            } else {
                finalResult.setSuccess(false);
                finalResult.setMessage("Import thất bại tất cả " + totalFiles + " file");
            }

        } catch (Exception e) {
            finalResult.addError("Lỗi hệ thống: " + e.getMessage());
        }

        return finalResult;
    }

    /**
     * Import products từ multiple Excel files và lưu vào database
     */
    public ExcelImportResult importAndSaveProductsFromMultipleExcel(List<MultipartFile> excelFiles) {
        ExcelImportResult importResult = importProductsFromMultipleExcel(excelFiles);
        
        if (importResult.isSuccess() && importResult.getImportedProducts() != null) {
            // Save valid products to database
            int savedCount = 0;
            List<String> saveErrors = new ArrayList<>();
            
            for (SanPhamModel productModel : importResult.getImportedProducts()) {
                try {
                    sanPhamService.createProduct(productModel);
                    savedCount++;
                } catch (Exception e) {
                    saveErrors.add("Lỗi lưu sản phẩm '" + productModel.getTenSanPham() + "': " + e.getMessage());
                }
            }
            
            // Update result with save information
            importResult.setSuccessCount(savedCount);
            importResult.setErrorCount(importResult.getTotalRecords() - savedCount);
            
            // Add save errors to result
            if (!saveErrors.isEmpty()) {
                importResult.getErrors().addAll(saveErrors);
            }
            
            // Update message
            if (savedCount > 0) {
                importResult.setMessage(importResult.getMessage() + " - Đã lưu " + savedCount + " sản phẩm vào database");
            }
        }
        
        return importResult;
    }

    private boolean validateHeader(Row headerRow) {
        if (headerRow == null) return false;

        String[] expectedHeaders = {
            "Tên sản phẩm", "Mã cửa hàng", "Mã danh mục", "Mô tả", 
            "Giá bán", "Số lượng", "Loại sản phẩm", "Trạng thái"
        };

        for (int i = 0; i < expectedHeaders.length; i++) {
            Cell cell = headerRow.getCell(i);
            if (cell == null || !expectedHeaders[i].equals(getCellValueAsString(cell))) {
                return false;
            }
        }
        return true;
    }

    private SanPhamModel parseRowToProductModel(Row row, int rowNumber) {
        try {
            SanPhamModel.SanPhamModelBuilder builder = SanPhamModel.builder();

            // Tên sản phẩm (required)
            String tenSanPham = getCellValueAsString(row.getCell(0));
            if (tenSanPham == null || tenSanPham.trim().isEmpty()) {
                throw new RuntimeException("Tên sản phẩm không được để trống");
            }
            builder.tenSanPham(tenSanPham.trim());

            // Mã cửa hàng (required)
            Integer maCuaHang = getCellValueAsInteger(row.getCell(1));
            if (maCuaHang == null) {
                throw new RuntimeException("Mã cửa hàng không được để trống");
            }
            CuaHang cuaHang = cuaHangService.findByMaCuaHang(maCuaHang);
            if (cuaHang == null) {
                throw new RuntimeException("Cửa hàng với mã " + maCuaHang + " không tồn tại");
            }
            builder.maCuaHang(maCuaHang);

            // Mã danh mục (required)
            Integer maDanhMuc = getCellValueAsInteger(row.getCell(2));
            if (maDanhMuc == null) {
                throw new RuntimeException("Mã danh mục không được để trống");
            }
            DanhMuc danhMuc = danhMucService.findByMaDanhMuc(maDanhMuc);
            if (danhMuc == null) {
                throw new RuntimeException("Danh mục với mã " + maDanhMuc + " không tồn tại");
            }
            builder.maDanhMuc(maDanhMuc);

            // Mô tả (optional)
            String moTa = getCellValueAsString(row.getCell(3));
            builder.moTaSanPham(moTa != null ? moTa.trim() : null);

            // Giá bán (required)
            BigDecimal giaBan = getCellValueAsBigDecimal(row.getCell(4));
            if (giaBan == null || giaBan.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Giá bán phải lớn hơn 0");
            }
            builder.giaBan(giaBan);

            // Số lượng (required)
            Integer soLuong = getCellValueAsInteger(row.getCell(5));
            if (soLuong == null || soLuong < 0) {
                throw new RuntimeException("Số lượng không được âm");
            }
            builder.soLuongConLai(soLuong);
            builder.soLuongDaBan(0);

            // Loại sản phẩm (required)
            String loaiSanPham = getCellValueAsString(row.getCell(6));
            if (loaiSanPham == null || loaiSanPham.trim().isEmpty()) {
                throw new RuntimeException("Loại sản phẩm không được để trống");
            }
            builder.loaiSanPham(loaiSanPham.trim());

            // Trạng thái (optional, default true)
            Boolean trangThai = getCellValueAsBoolean(row.getCell(7));
            builder.trangThai(trangThai != null ? trangThai : true);

            // Default values
            builder.luotThich(BigDecimal.ZERO);
            builder.ngayNhap(new Date());

            return builder.build();

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private boolean validateProductModel(SanPhamModel model) {
        return model.getTenSanPham() != null && !model.getTenSanPham().trim().isEmpty()
                && model.getMaCuaHang() != null
                && model.getMaDanhMuc() != null
                && model.getGiaBan() != null && model.getGiaBan().compareTo(BigDecimal.ZERO) > 0
                && model.getSoLuongConLai() != null && model.getSoLuongConLai() >= 0
                && model.getLoaiSanPham() != null && !model.getLoaiSanPham().trim().isEmpty();
    }

 // Helper methods for cell value extraction - IMPROVED VERSION
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        
        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue().trim();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getDateCellValue().toString();
                    } else {
                        // For integer values, remove decimal part if it's .0
                        double numValue = cell.getNumericCellValue();
                        if (numValue == Math.floor(numValue)) {
                            return String.valueOf((long) numValue);
                        } else {
                            return String.valueOf(numValue);
                        }
                    }
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    try {
                        return cell.getStringCellValue().trim();
                    } catch (Exception e) {
                        try {
                            return String.valueOf(cell.getNumericCellValue());
                        } catch (Exception e2) {
                            return cell.getCellFormula();
                        }
                    }
                case BLANK:
                    return "";
                default:
                    return "";
            }
        } catch (Exception e) {
            System.err.println("Error getting cell value: " + e.getMessage());
            return "";
        }
    }

    private Integer getCellValueAsInteger(Cell cell) {
        if (cell == null) return null;
        
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return (int) cell.getNumericCellValue();
                case STRING:
                    String value = cell.getStringCellValue().trim();
                    return value.isEmpty() ? null : Integer.parseInt(value);
                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null) return null;
        
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return BigDecimal.valueOf(cell.getNumericCellValue());
                case STRING:
                    String value = cell.getStringCellValue().trim();
                    return value.isEmpty() ? null : new BigDecimal(value);
                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean getCellValueAsBoolean(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case STRING:
                String value = cell.getStringCellValue().trim().toLowerCase();
                return value.equals("true") || value.equals("1") || value.equals("có") || value.equals("yes");
            case NUMERIC:
                return cell.getNumericCellValue() == 1;
            default:
                return null;
        }
    }

    /**
     * Tạo template Excel mẫu
     */
    public byte[] generateExcelTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Template Sản phẩm");
            
            // Tạo style cho header
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            // Tạo header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "Tên sản phẩm", "Mã cửa hàng", "Mã danh mục", "Mô tả", 
                "Giá bán", "Số lượng", "Loại sản phẩm", "Trạng thái"
            };
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Tạo dòng mẫu
            Row sampleRow = sheet.createRow(1);
            sampleRow.createCell(0).setCellValue("Thức ăn cho chó con");
            sampleRow.createCell(1).setCellValue(1); // Mã cửa hàng
            sampleRow.createCell(2).setCellValue(1); // Mã danh mục
            sampleRow.createCell(3).setCellValue("Thức ăn dinh dưỡng cho chó con từ 2-12 tháng tuổi");
            sampleRow.createCell(4).setCellValue(150000);
            sampleRow.createCell(5).setCellValue(50);
            sampleRow.createCell(6).setCellValue("Thức ăn");
            sampleRow.createCell(7).setCellValue("true");
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Convert to byte array
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
    
 // ============= METHODS FOR VENDOR =============

    /**
     * Import products từ single Excel file cho vendor (không cần mã cửa hàng)
     */
    public ExcelImportResult importProductsFromExcelForVendor(MultipartFile file, CuaHang currentStore) {
        ExcelImportResult result = ExcelImportResult.builder()
                .success(false)
                .errors(new ArrayList<>())
                .importedProducts(new ArrayList<>())
                .build();

        try {
            // Validate file
            if (file.isEmpty()) {
                result.addError("File Excel không được để trống");
                return result;
            }

            if (!file.getOriginalFilename().endsWith(".xlsx")) {
                result.addError("Chỉ hỗ trợ file Excel định dạng .xlsx");
                return result;
            }

            InputStream inputStream = file.getInputStream();
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0); // Lấy sheet đầu tiên

            // Validate header cho vendor
            Row headerRow = sheet.getRow(0);
            if (!validateHeaderForVendor(headerRow)) {
                result.addError("Định dạng file Excel không đúng. Vui lòng tải template mẫu cho vendor.");
                workbook.close();
                return result;
            }

            // Process data rows
            int totalRecords = 0;
            int successCount = 0;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                totalRecords++;
                try {
                    SanPhamModel productModel = parseRowToProductModelForVendor(row, i + 1, currentStore);
                    if (productModel != null) {
                        // Validate required fields
                        if (validateProductModel(productModel)) {
                            result.addImportedProduct(productModel);
                            successCount++;
                        }
                    }
                } catch (Exception e) {
                    result.addError("Dòng " + (i + 1) + ": " + e.getMessage());
                }
            }

            workbook.close();

            result.setTotalRecords(totalRecords);
            result.setSuccessCount(successCount);
            result.setErrorCount(totalRecords - successCount);
            result.setSuccess(true);
            result.setMessage(String.format("Import thành công %d/%d sản phẩm", successCount, totalRecords));

        } catch (IOException e) {
            result.addError("Lỗi đọc file: " + e.getMessage());
        } catch (Exception e) {
            result.addError("Lỗi xử lý file: " + e.getMessage());
        }

        return result;
    }

    /**
     * Import products từ multiple Excel files cho vendor
     */
    public ExcelImportResult importProductsFromMultipleExcelForVendor(List<MultipartFile> excelFiles, CuaHang currentStore) {
        ExcelImportResult finalResult = ExcelImportResult.builder()
                .success(false)
                .errors(new ArrayList<>())
                .importedProducts(new ArrayList<>())
                .totalRecords(0)
                .successCount(0)
                .errorCount(0)
                .build();

        try {
            // Validate files
            if (excelFiles == null || excelFiles.isEmpty()) {
                finalResult.addError("Không có file nào được chọn");
                return finalResult;
            }

            // Validate số lượng file
            if (excelFiles.size() > 5) {
                finalResult.addError("Chỉ được phép import tối đa 5 file");
                return finalResult;
            }

            int totalFiles = excelFiles.size();
            int successFiles = 0;
            int failedFiles = 0;

            // Process each file
            for (int i = 0; i < excelFiles.size(); i++) {
                MultipartFile file = excelFiles.get(i);
                String fileName = file.getOriginalFilename();

                try {
                    ExcelImportResult fileResult = importProductsFromExcelForVendor(file, currentStore);
                    
                    if (fileResult.isSuccess()) {
                        successFiles++;
                        // Merge results
                        finalResult.setTotalRecords(finalResult.getTotalRecords() + fileResult.getTotalRecords());
                        finalResult.setSuccessCount(finalResult.getSuccessCount() + fileResult.getSuccessCount());
                        finalResult.setErrorCount(finalResult.getErrorCount() + fileResult.getErrorCount());
                        
                        if (fileResult.getImportedProducts() != null) {
                            finalResult.getImportedProducts().addAll(fileResult.getImportedProducts());
                        }
                        
                        // Add success message for this file
                        finalResult.addError("✓ File " + fileName + ": " + fileResult.getMessage());
                    } else {
                        failedFiles++;
                        // Add errors from this file
                        if (fileResult.getErrors() != null) {
                            for (String error : fileResult.getErrors()) {
                                finalResult.addError("✗ File " + fileName + ": " + error);
                            }
                        }
                    }
                    
                } catch (Exception e) {
                    failedFiles++;
                    finalResult.addError("✗ File " + fileName + ": Lỗi xử lý - " + e.getMessage());
                }
            }

            // Set final result
            if (successFiles > 0) {
                finalResult.setSuccess(true);
                finalResult.setMessage(String.format(
                    "Import hoàn tất. Thành công: %d/%d file. Tổng sản phẩm: %d thành công, %d lỗi",
                    successFiles, totalFiles, finalResult.getSuccessCount(), finalResult.getErrorCount()
                ));
            } else {
                finalResult.setSuccess(false);
                finalResult.setMessage("Import thất bại tất cả " + totalFiles + " file");
            }

        } catch (Exception e) {
            finalResult.addError("Lỗi hệ thống: " + e.getMessage());
        }

        return finalResult;
    }

    /**
     * Import products từ multiple Excel files và lưu vào database cho vendor
     */
    public ExcelImportResult importAndSaveProductsFromMultipleExcelForVendor(List<MultipartFile> excelFiles, CuaHang currentStore) {
        ExcelImportResult importResult = importProductsFromMultipleExcelForVendor(excelFiles, currentStore);
        
        if (importResult.isSuccess() && importResult.getImportedProducts() != null) {
            // Save valid products to database
            int savedCount = 0;
            List<String> saveErrors = new ArrayList<>();
            
            for (SanPhamModel productModel : importResult.getImportedProducts()) {
                try {
                    sanPhamService.createProduct(productModel);
                    savedCount++;
                } catch (Exception e) {
                    saveErrors.add("Lỗi lưu sản phẩm '" + productModel.getTenSanPham() + "': " + e.getMessage());
                }
            }
            
            // Update result with save information
            importResult.setSuccessCount(savedCount);
            importResult.setErrorCount(importResult.getTotalRecords() - savedCount);
            
            // Add save errors to result
            if (!saveErrors.isEmpty()) {
                importResult.getErrors().addAll(saveErrors);
            }
            
            // Update message
            if (savedCount > 0) {
                importResult.setMessage(importResult.getMessage() + " - Đã lưu " + savedCount + " sản phẩm vào database");
            }
        }
        
        return importResult;
    }

    /**
     * Tạo template Excel mẫu cho vendor (không có cột mã cửa hàng)
     */
    public byte[] generateExcelTemplateForVendor(CuaHang currentStore) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Template Sản phẩm - Vendor");
            
            // Tạo style cho header
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            // Tạo header row cho vendor (không có mã cửa hàng)
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "Tên sản phẩm", "Mã danh mục", "Mô tả", 
                "Giá bán", "Số lượng", "Loại sản phẩm", "Trạng thái"
            };
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Tạo dòng mẫu với thông tin cửa hàng hiện tại
            Row sampleRow = sheet.createRow(1);
            sampleRow.createCell(0).setCellValue("Thức ăn cho chó con");
            sampleRow.createCell(1).setCellValue(1); // Mã danh mục
            sampleRow.createCell(2).setCellValue("Thức ăn dinh dưỡng cho chó con từ 2-12 tháng tuổi");
            sampleRow.createCell(3).setCellValue(150000);
            sampleRow.createCell(4).setCellValue(50);
            sampleRow.createCell(5).setCellValue("Thức ăn");
            sampleRow.createCell(6).setCellValue("true");
            
            // Thêm thông tin cửa hàng
            Row infoRow1 = sheet.createRow(3);
            infoRow1.createCell(0).setCellValue("Lưu ý:");
            Row infoRow2 = sheet.createRow(4);
            infoRow2.createCell(0).setCellValue("Cửa hàng: " + currentStore.getTenCuaHang() + " (Mã: " + currentStore.getMaCuaHang() + ")");
            Row infoRow3 = sheet.createRow(5);
            infoRow3.createCell(0).setCellValue("Tất cả sản phẩm sẽ được thêm vào cửa hàng của bạn");
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Convert to byte array
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    // ============= PRIVATE HELPER METHODS FOR VENDOR =============

    private boolean validateHeaderForVendor(Row headerRow) {
        if (headerRow == null) return false;

        // Header cho vendor không có "Mã cửa hàng"
        String[] expectedHeaders = {
            "Tên sản phẩm", "Mã danh mục", "Mô tả", 
            "Giá bán", "Số lượng", "Loại sản phẩm", "Trạng thái"
        };

        for (int i = 0; i < expectedHeaders.length; i++) {
            Cell cell = headerRow.getCell(i);
            if (cell == null || !expectedHeaders[i].equals(getCellValueAsString(cell))) {
                return false;
            }
        }
        return true;
    }

    private SanPhamModel parseRowToProductModelForVendor(Row row, int rowNumber, CuaHang currentStore) {
        try {
            SanPhamModel.SanPhamModelBuilder builder = SanPhamModel.builder();

            // Tên sản phẩm (required)
            String tenSanPham = getCellValueAsString(row.getCell(0));
            if (tenSanPham == null || tenSanPham.trim().isEmpty()) {
                throw new RuntimeException("Tên sản phẩm không được để trống");
            }
            builder.tenSanPham(tenSanPham.trim());

            // Mã danh mục (required) - cell index 1 (không có mã cửa hàng)
            Integer maDanhMuc = getCellValueAsInteger(row.getCell(1));
            if (maDanhMuc == null) {
                throw new RuntimeException("Mã danh mục không được để trống");
            }
            DanhMuc danhMuc = danhMucService.findByMaDanhMuc(maDanhMuc);
            if (danhMuc == null) {
                throw new RuntimeException("Danh mục với mã " + maDanhMuc + " không tồn tại");
            }
            builder.maDanhMuc(maDanhMuc);

            // Mã cửa hàng (tự động lấy từ currentStore)
            builder.maCuaHang(currentStore.getMaCuaHang());

            // Mô tả (optional) - cell index 2
            String moTa = getCellValueAsString(row.getCell(2));
            builder.moTaSanPham(moTa != null ? moTa.trim() : null);

            // Giá bán (required) - cell index 3
            BigDecimal giaBan = getCellValueAsBigDecimal(row.getCell(3));
            if (giaBan == null || giaBan.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Giá bán phải lớn hơn 0");
            }
            builder.giaBan(giaBan);

            // Số lượng (required) - cell index 4
            Integer soLuong = getCellValueAsInteger(row.getCell(4));
            if (soLuong == null || soLuong < 0) {
                throw new RuntimeException("Số lượng không được âm");
            }
            builder.soLuongConLai(soLuong);
            builder.soLuongDaBan(0);

            // Loại sản phẩm (required) - cell index 5
            String loaiSanPham = getCellValueAsString(row.getCell(5));
            if (loaiSanPham == null || loaiSanPham.trim().isEmpty()) {
                throw new RuntimeException("Loại sản phẩm không được để trống");
            }
            builder.loaiSanPham(loaiSanPham.trim());

            // Trạng thái (optional, default true) - cell index 6
            Boolean trangThai = getCellValueAsBoolean(row.getCell(6));
            builder.trangThai(trangThai != null ? trangThai : true);

            // Default values
            builder.luotThich(BigDecimal.ZERO);
            builder.ngayNhap(new Date());

            return builder.build();

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
    
    /**
     * Import cửa hàng từ multiple Excel files và lưu vào database
     */
    public ExcelImportResult importAndSaveStoresFromMultipleExcel(List<MultipartFile> excelFiles) {
        ExcelImportResult importResult = importStoresFromMultipleExcel(excelFiles);
        
        if (importResult.isSuccess() && importResult.getImportedStores() != null) {
            // Save valid stores to database
            int savedCount = 0;
            List<String> saveErrors = new ArrayList<>();
            
            for (CuaHangModel storeModel : importResult.getImportedStores()) {
                try {
                    cuaHangService.createStore(storeModel);
                    savedCount++;
                } catch (Exception e) {
                    saveErrors.add("Lỗi lưu cửa hàng '" + storeModel.getTenCuaHang() + "': " + e.getMessage());
                }
            }
            
            // Update result with save information
            importResult.setSuccessCount(savedCount);
            importResult.setErrorCount(importResult.getTotalRecords() - savedCount);
            
            // Add save errors to result
            if (!saveErrors.isEmpty()) {
                importResult.getErrors().addAll(saveErrors);
            }
            
            // Update message
            if (savedCount > 0) {
                importResult.setMessage(importResult.getMessage() + " - Đã lưu " + savedCount + " cửa hàng vào database");
            }
            
            // Đánh dấu thành công nếu có ít nhất một bản ghi được lưu
            importResult.setSuccess(savedCount > 0);
        }
        
        return importResult;
    }

    /**
     * Import cửa hàng từ single Excel file
     */
    public ExcelImportResult importStoresFromExcel(MultipartFile file) {
        ExcelImportResult result = ExcelImportResult.builder()
                .success(false)
                .errors(new ArrayList<>())
                .importedStores(new ArrayList<>())
                .build();

        try {
            System.out.println("=== IMPORT STORES FROM EXCEL START ===");
            System.out.println("File name: " + file.getOriginalFilename());
            System.out.println("File size: " + file.getSize());
            
            // Validate file
            if (file.isEmpty()) {
                result.addError("File Excel không được để trống");
                return result;
            }

            if (!file.getOriginalFilename().endsWith(".xlsx")) {
                result.addError("Chỉ hỗ trợ file Excel định dạng .xlsx");
                return result;
            }

            InputStream inputStream = file.getInputStream();
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0); // Lấy sheet đầu tiên

            System.out.println("Sheet name: " + sheet.getSheetName());
            System.out.println("Number of rows: " + (sheet.getLastRowNum() + 1));

            // Validate header
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                result.addError("File Excel không có dữ liệu");
                workbook.close();
                return result;
            }

            System.out.println("Header validation...");
            if (!validateStoresHeader(headerRow)) {
                result.addError("Định dạng file Excel không đúng. Vui lòng tải template mẫu.");
                workbook.close();
                return result;
            }
            System.out.println("Header validation passed");

            // Process data rows
            int totalRecords = 0;
            int successCount = 0;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    System.out.println("Row " + i + " is null, skipping");
                    continue;
                }

                // Check if row is empty
                boolean isEmptyRow = true;
                for (int cellNum = 0; cellNum < headerRow.getLastCellNum(); cellNum++) {
                    Cell cell = row.getCell(cellNum);
                    if (cell != null && getCellValueAsString(cell) != null && 
                        !getCellValueAsString(cell).trim().isEmpty()) {
                        isEmptyRow = false;
                        break;
                    }
                }
                
                if (isEmptyRow) {
                    System.out.println("Row " + i + " is empty, skipping");
                    continue;
                }

                totalRecords++;
                try {
                    System.out.println("Processing row " + (i + 1));
                    CuaHangModel storeModel = parseRowToStoreModel(row, i + 1);
                    if (storeModel != null) {
                        // Validate required fields
                        if (validateStoreModel(storeModel)) {
                            result.addImportedStore(storeModel);
                            successCount++;
                            System.out.println("Row " + (i + 1) + " - SUCCESS: " + storeModel.getTenCuaHang());
                        } else {
                            result.addError("Dòng " + (i + 1) + ": Dữ liệu không hợp lệ");
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Row " + (i + 1) + " - ERROR: " + e.getMessage());
                    result.addError("Dòng " + (i + 1) + ": " + e.getMessage());
                }
            }

            workbook.close();

            result.setTotalRecords(totalRecords);
            result.setSuccessCount(successCount);
            result.setErrorCount(totalRecords - successCount);
            result.setSuccess(successCount > 0);
            result.setMessage(String.format("Import thành công %d/%d cửa hàng", successCount, totalRecords));

            System.out.println("=== IMPORT STORES FROM EXCEL END ===");
            System.out.println("Total: " + totalRecords + ", Success: " + successCount + ", Errors: " + result.getErrorCount());

        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
            result.addError("Lỗi đọc file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Exception: " + e.getMessage());
            result.addError("Lỗi xử lý file: " + e.getMessage());
        }

        return result;
    }

    /**
     * Validate header cho file Excel cửa hàng - FIXED VERSION
     */
    private boolean validateStoresHeader(Row headerRow) {
        if (headerRow == null) return false;

        String[] expectedHeaders = {
            "Tên cửa hàng", "Mã người dùng", "Mô tả", "Địa chỉ", 
            "Số điện thoại", "Email", "Năm thành lập", "Trạng thái"
        }; 

        System.out.println("Expected headers: " + String.join(", ", expectedHeaders));
        
        for (int i = 0; i < expectedHeaders.length; i++) {
            Cell cell = headerRow.getCell(i);
            String cellValue = getCellValueAsString(cell);
            System.out.println("Header cell " + i + ": '" + cellValue + "'");
            
            if (cell == null || !expectedHeaders[i].equals(cellValue)) {
                System.err.println("Header mismatch at position " + i + ": expected '" + expectedHeaders[i] + "', got '" + cellValue + "'");
                return false;
            }
        }
        System.out.println("All headers matched successfully");
        return true;
    }

    private CuaHangModel parseRowToStoreModel(Row row, int rowNumber) {
        try {
            System.out.println("Parsing row " + rowNumber);
            CuaHangModel.CuaHangModelBuilder builder = CuaHangModel.builder();

            // Tên cửa hàng (required) - Cell 0
            String tenCuaHang = getCellValueAsString(row.getCell(0));
            System.out.println("TenCuaHang: '" + tenCuaHang + "'");
            if (tenCuaHang == null || tenCuaHang.trim().isEmpty()) {
                throw new RuntimeException("Tên cửa hàng không được để trống");
            }
            if (tenCuaHang.length() < 2 || tenCuaHang.length() > 100) {
                throw new RuntimeException("Tên cửa hàng phải từ 2 đến 100 ký tự");
            }
            builder.tenCuaHang(tenCuaHang.trim());

            // Mã người dùng (required) - Cell 1
            Integer maNguoiDung = getCellValueAsInteger(row.getCell(1));
            System.out.println("MaNguoiDung: " + maNguoiDung);
            if (maNguoiDung == null) {
                throw new RuntimeException("Mã người dùng không được để trống");
            }
            
            // DEBUG: Kiểm tra xem có tìm thấy người dùng không
            System.out.println("Searching for user with ID: " + maNguoiDung);
            NguoiDung nguoiDung = nguoiDungService.findByMaNguoiDung(maNguoiDung);
            
            if (nguoiDung == null) {
                throw new RuntimeException("Người dùng với mã " + maNguoiDung + " không tồn tại");
            }
            
            // DEBUG: Kiểm tra thông tin người dùng
            System.out.println("Found user: " + nguoiDung.getTenNguoiDung());
            System.out.println("User role: " + (nguoiDung.getVaiTro() != null ? nguoiDung.getVaiTro().getMaVaiTro() : "NULL"));
            
            // SỬA LỖI: Kiểm tra vai trò đúng cách
            if (nguoiDung.getVaiTro() == null) {
                throw new RuntimeException("Người dùng " + nguoiDung.getTenNguoiDung() + " không có vai trò");
            }
            
            String userRole = nguoiDung.getVaiTro().getMaVaiTro();
            System.out.println("User role code: " + userRole);
            
            if (!"VENDOR".equalsIgnoreCase(userRole)) {
                throw new RuntimeException("Người dùng với mã " + maNguoiDung + " không có vai trò VENDOR. Vai trò hiện tại: " + userRole);
            }
            
            // Kiểm tra người dùng đã có cửa hàng chưa
            List<CuaHang> existingStores = cuaHangService.findByNguoiDung(nguoiDung);
            System.out.println("Existing stores count: " + (existingStores != null ? existingStores.size() : 0));
            
            if (existingStores != null && !existingStores.isEmpty()) {
                throw new RuntimeException("Người dùng " + nguoiDung.getTenNguoiDung() + " đã có cửa hàng. Mỗi người dùng chỉ được sở hữu 1 cửa hàng.");
            }
            
            builder.maNguoiDung(maNguoiDung);

            // Mô tả (optional) - Cell 2
            String moTa = getCellValueAsString(row.getCell(2));
            System.out.println("MoTa: '" + moTa + "'");
            builder.moTa(moTa != null ? moTa.trim() : null);

            // Địa chỉ (required) - Cell 3
            String diaChi = getCellValueAsString(row.getCell(3));
            System.out.println("DiaChi: '" + diaChi + "'");
            if (diaChi == null || diaChi.trim().isEmpty()) {
                throw new RuntimeException("Địa chỉ không được để trống");
            }
            if (diaChi.length() > 255) {
                throw new RuntimeException("Địa chỉ không được quá 255 ký tự");
            }
            builder.diaChi(diaChi.trim());

            // Số điện thoại (required) - Cell 4
            String soDienThoai = getCellValueAsString(row.getCell(4));
            System.out.println("SoDienThoai: '" + soDienThoai + "'");
            if (soDienThoai == null || soDienThoai.trim().isEmpty()) {
                throw new RuntimeException("Số điện thoại không được để trống");
            }
            // Validate số điện thoại Việt Nam - FIXED REGEX
            String phoneRegex = "^(84|0[3|5|7|8|9])[0-9]{8,9}$";
            String cleanedPhone = soDienThoai.trim().replaceAll("\\s+", "");
            if (!cleanedPhone.matches(phoneRegex)) {
                throw new RuntimeException("Số điện thoại không hợp lệ. Định dạng: 84xxxxxxxxx hoặc 0xxxxxxxxx (10-11 số)");
            }
            builder.soDienThoai(cleanedPhone);

            // Email (required) - Cell 5
            String email = getCellValueAsString(row.getCell(5));
            System.out.println("Email: '" + email + "'");
            if (email == null || email.trim().isEmpty()) {
                throw new RuntimeException("Email không được để trống");
            }
            // Validate email format - FIXED REGEX
            String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
            if (!email.matches(emailRegex)) {
                throw new RuntimeException("Email không hợp lệ");
            }
            builder.email(email.trim());

            // Năm thành lập (optional) - Cell 6
            Integer namThanhLap = getCellValueAsInteger(row.getCell(6));
            System.out.println("NamThanhLap: " + namThanhLap);
            if (namThanhLap != null) {
                int currentYear = java.time.Year.now().getValue();
                if (namThanhLap < 1900 || namThanhLap > currentYear + 1) {
                    throw new RuntimeException("Năm thành lập phải từ 1900 đến " + (currentYear + 1));
                }
            }
            builder.namThanhLap(namThanhLap);

            // Trạng thái (optional, default true) - Cell 7
            Boolean trangThai = getCellValueAsBoolean(row.getCell(7));
            System.out.println("TrangThai: " + trangThai);
            builder.trangThai(trangThai != null ? trangThai : true);

            // Default values
            builder.danhGiaTrungBinh(0.0);
            builder.soLuongDanhGia(0);
            builder.ngayTao(new Date());

            CuaHangModel model = builder.build();
            System.out.println("Successfully parsed store: " + model.getTenCuaHang());
            return model;

        } catch (Exception e) {
            System.err.println("Error parsing row " + rowNumber + ": " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Validate CuaHangModel
     */
    private boolean validateStoreModel(CuaHangModel model) {
        return model.getTenCuaHang() != null && !model.getTenCuaHang().trim().isEmpty()
                && model.getMaNguoiDung() != null
                && model.getDiaChi() != null && !model.getDiaChi().trim().isEmpty()
                && model.getSoDienThoai() != null && !model.getSoDienThoai().trim().isEmpty()
                && model.getEmail() != null && !model.getEmail().trim().isEmpty();
    }

    /**
     * Lưu cửa hàng đã import vào database
     */
    public ExcelImportResult saveImportedStores(ExcelImportResult importResult) {
        if (importResult.isSuccess() && importResult.getImportedStores() != null) {
            // Save valid stores to database
            int savedCount = 0;
            List<String> saveErrors = new ArrayList<>();
            
            for (CuaHangModel storeModel : importResult.getImportedStores()) {
                try {
                    cuaHangService.createStore(storeModel);
                    savedCount++;
                } catch (Exception e) {
                    saveErrors.add("Lỗi lưu cửa hàng '" + storeModel.getTenCuaHang() + "': " + e.getMessage());
                }
            }
            
            // Update result with save information
            importResult.setSuccessCount(savedCount);
            importResult.setErrorCount(importResult.getTotalRecords() - savedCount);
            
            // Add save errors to result
            if (!saveErrors.isEmpty()) {
                importResult.getErrors().addAll(saveErrors);
            }
            
            // Update message
            if (savedCount > 0) {
                importResult.setMessage(importResult.getMessage() + " - Đã lưu " + savedCount + " cửa hàng vào database");
            }
            
            // Đánh dấu thành công nếu có ít nhất một bản ghi được lưu
            importResult.setSuccess(savedCount > 0);
        }
        
        return importResult;
    }

    /**
     * Import cửa hàng từ multiple Excel files (chỉ import, không lưu)
     */
    public ExcelImportResult importStoresFromMultipleExcel(List<MultipartFile> excelFiles) {
        ExcelImportResult finalResult = ExcelImportResult.builder()
                .success(false)
                .errors(new ArrayList<>())
                .importedStores(new ArrayList<>())
                .totalRecords(0)
                .successCount(0)
                .errorCount(0)
                .build();

        try {
            // Validate files
            if (excelFiles == null || excelFiles.isEmpty()) {
                finalResult.addError("Không có file nào được chọn");
                return finalResult;
            }

            // Validate số lượng file
            if (excelFiles.size() > 5) {
                finalResult.addError("Chỉ được phép import tối đa 5 file");
                return finalResult;
            }

            int totalFiles = excelFiles.size();
            int successFiles = 0;
            int failedFiles = 0;

            // Process each file
            for (int i = 0; i < excelFiles.size(); i++) {
                MultipartFile file = excelFiles.get(i);
                String fileName = file.getOriginalFilename();

                try {
                    ExcelImportResult fileResult = importStoresFromExcel(file);
                    
                    if (fileResult.isSuccess()) {
                        successFiles++;
                        // Merge results
                        finalResult.setTotalRecords(finalResult.getTotalRecords() + fileResult.getTotalRecords());
                        finalResult.setSuccessCount(finalResult.getSuccessCount() + fileResult.getSuccessCount());
                        finalResult.setErrorCount(finalResult.getErrorCount() + fileResult.getErrorCount());
                        
                        if (fileResult.getImportedStores() != null) {
                            finalResult.getImportedStores().addAll(fileResult.getImportedStores());
                        }
                        
                        // Add success message for this file
                        finalResult.addError("✓ File " + fileName + ": " + fileResult.getMessage());
                    } else {
                        failedFiles++;
                        // Add errors from this file
                        if (fileResult.getErrors() != null) {
                            for (String error : fileResult.getErrors()) {
                                finalResult.addError("✗ File " + fileName + ": " + error);
                            }
                        }
                    }
                    
                } catch (Exception e) {
                    failedFiles++;
                    finalResult.addError("✗ File " + fileName + ": Lỗi xử lý - " + e.getMessage());
                }
            }

            // Set final result
            if (successFiles > 0) {
                finalResult.setSuccess(true);
                finalResult.setMessage(String.format(
                    "Import hoàn tất. Thành công: %d/%d file. Tổng cửa hàng: %d thành công, %d lỗi",
                    successFiles, totalFiles, finalResult.getSuccessCount(), finalResult.getErrorCount()
                ));
            } else {
                finalResult.setSuccess(false);
                finalResult.setMessage("Import thất bại tất cả " + totalFiles + " file");
            }

        } catch (Exception e) {
            finalResult.addError("Lỗi hệ thống: " + e.getMessage());
        }

        return finalResult;
    } 	
    
    /**
     * Tạo template Excel mẫu cho cửa hàng
     */
    public byte[] generateStoresTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Template Cửa hàng");
            
            // Tạo style cho header
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            // Tạo header row cho cửa hàng
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "Tên cửa hàng", "Mã người dùng", "Mô tả", "Địa chỉ", 
                "Số điện thoại", "Email", "Năm thành lập", "Trạng thái"
            };
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Tạo dòng mẫu
            Row sampleRow = sheet.createRow(1);
            sampleRow.createCell(0).setCellValue("Cửa hàng Pet mẫu");
            sampleRow.createCell(1).setCellValue(1); // Mã người dùng (VENDOR)
            sampleRow.createCell(2).setCellValue("Cửa hàng chuyên cung cấp thức ăn và phụ kiện cho thú cưng");
            sampleRow.createCell(3).setCellValue("123 Đường ABC, Quận 1, TP.HCM");
            sampleRow.createCell(4).setCellValue("0909123456");
            sampleRow.createCell(5).setCellValue("store@example.com");
            sampleRow.createCell(6).setCellValue(2020);
            sampleRow.createCell(7).setCellValue("true");
            
            // Thêm thông tin hướng dẫn
            Row infoRow1 = sheet.createRow(3);
            infoRow1.createCell(0).setCellValue("Lưu ý:");
            Row infoRow2 = sheet.createRow(4);
            infoRow2.createCell(0).setCellValue("- Mã người dùng phải tồn tại trong hệ thống và có vai trò VENDOR");
            Row infoRow3 = sheet.createRow(5);
            infoRow3.createCell(0).setCellValue("- Mỗi người dùng chỉ được sở hữu 1 cửa hàng");
            Row infoRow4 = sheet.createRow(6);
            infoRow4.createCell(0).setCellValue("- Trạng thái: true (Hoạt động) hoặc false (Đã khóa)");
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Convert to byte array
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}