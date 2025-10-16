package vn.iotstar.model;

import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KhuyenMaiImportResult {
    private boolean success;
    private String message;
    private List<String> errors;
    private List<KhuyenMaiModel> importedKhuyenMai;
    private int totalRecords;
    private int successCount;
    private int errorCount;

    public void addError(String error) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(error);
    }

    public void addImportedKhuyenMai(KhuyenMaiModel khuyenMai) {
        if (importedKhuyenMai == null) {
            importedKhuyenMai = new ArrayList<>();
        }
        importedKhuyenMai.add(khuyenMai);
    }
}