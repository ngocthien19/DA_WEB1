package vn.iotstar.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExcelImportResult {
    private boolean success;
    private String message;
    private int totalRecords;
    private int successCount;
    private int errorCount;
    private List<String> errors;
    private List<CuaHangModel> importedStores;
    private List<SanPhamModel> importedProducts;
    
    public void addImportedStore(CuaHangModel store) {
        if (this.importedStores == null) {
            this.importedStores = new ArrayList<>();
        }
        this.importedStores.add(store);
    }

    public void addError(String error) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(error);
    }

    public void addImportedProduct(SanPhamModel product) {
        if (importedProducts == null) {
            importedProducts = new ArrayList<>();
        }
        importedProducts.add(product);
    }
}