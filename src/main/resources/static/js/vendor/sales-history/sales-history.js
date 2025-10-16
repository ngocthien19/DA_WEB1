// ============= SALES HISTORY MANAGEMENT JS (FIXED FOR PRODUCT COUNT IN TOAST) =============

document.addEventListener('DOMContentLoaded', function() {
    console.log('Sales History page loaded (Fixed)');
    
    initializeSalesHistory();
});

function initializeSalesHistory() {
    initializeDateValidation();
    initializeFilters();
    initializeSelectionManagement();
    initializeExportHandlers();
    initializeKeyboardShortcuts();
}

// ============= DATE VALIDATION (Không đổi) =============

function initializeDateValidation() {
    const startDateInput = document.getElementById('startDateFilter');
    const endDateInput = document.getElementById('endDateFilter');
    
    if (startDateInput && endDateInput) {
        startDateInput.addEventListener('change', function() {
            if (this.value && endDateInput.value && this.value > endDateInput.value) {
                endDateInput.value = this.value;
            }
        });
        
        endDateInput.addEventListener('change', function() {
            if (this.value && startDateInput.value && this.value < startDateInput.value) {
                startDateInput.value = this.value;
            }
        });
    }
}

// ============= FILTER FUNCTIONS (Không đổi) =============

function initializeFilters() {
    // Apply date filter when button is clicked
    const applyDateFilterBtn = document.getElementById('applyDateFilterBtn');
    if (applyDateFilterBtn) {
        applyDateFilterBtn.addEventListener('click', function(e) {
            e.preventDefault();
            document.getElementById('dateFilterForm').submit();
        });
    }
}

function clearFilter(filterType) {
    const urlParams = new URLSearchParams(window.location.search);
    
    switch (filterType) {
        case 'keyword':
            urlParams.delete('keyword');
            break;
        case 'product':
            urlParams.delete('productName');
            break;
        case 'date':
            urlParams.delete('startDate');
            urlParams.delete('endDate');
            break;
        case 'all':
            urlParams.delete('keyword');
            urlParams.delete('productName');
            urlParams.delete('startDate');
            urlParams.delete('endDate');
            urlParams.set('page', '0');
            break;
    }
    
    window.location.href = `${window.location.pathname}?${urlParams.toString()}`;
}

function clearDateFilter() {
    const urlParams = new URLSearchParams(window.location.search);
    urlParams.delete('startDate');
    urlParams.delete('endDate');
    window.location.href = `${window.location.pathname}?${urlParams.toString()}`;
}

// ============= SELECTION MANAGEMENT (Không đổi) =============

let selectedOrders = new Set();

function initializeSelectionManagement() {
    // Lắng nghe sự kiện click cho các checkbox hàng
    document.querySelectorAll('.order-checkbox').forEach(checkbox => {
        checkbox.removeEventListener('change', handleOrderSelection); 
        checkbox.addEventListener('change', function() {
            handleOrderSelection(this);
        });
    });
    
    // Lắng nghe sự kiện click cho checkbox "Chọn tất cả"
    const selectAllCheckbox = document.getElementById('selectAllCheckbox');
    if (selectAllCheckbox) {
        selectAllCheckbox.removeEventListener('change', toggleSelectAll); 
        selectAllCheckbox.addEventListener('change', function() {
            toggleSelectAll(this);
        });
    }
    
    // Khởi tạo trạng thái đã chọn (nếu có)
    updateSelectionControls();
}

function handleOrderSelection(checkbox) {
    const orderId = checkbox.value;
    const row = checkbox.closest('tr');
    
    if (checkbox.checked) {
        selectedOrders.add(orderId); 
        row.classList.add('table-row-selected');
    } else {
        selectedOrders.delete(orderId);
        row.classList.remove('table-row-selected');
    }
    
    updateSelectionControls();
    updateSelectAllCheckbox();
}

function toggleSelectAll(checkbox) {
    const isChecked = checkbox.checked;
    const orderCheckboxes = document.querySelectorAll('.order-checkbox');
    
    selectedOrders.clear(); 

    orderCheckboxes.forEach(cb => {
        cb.checked = isChecked;
        const row = cb.closest('tr');
        if (isChecked) {
            selectedOrders.add(cb.value);
            row.classList.add('table-row-selected');
        } else {
            row.classList.remove('table-row-selected');
        }
    });
    
    updateSelectionControls();
    updateSelectAllCheckbox();
}

function updateSelectAllCheckbox() {
    const selectAllCheckbox = document.getElementById('selectAllCheckbox');
    if (!selectAllCheckbox) return;
    
    const totalCheckboxes = document.querySelectorAll('.order-checkbox').length;
    const checkedCount = document.querySelectorAll('.order-checkbox:checked').length; 
    
    selectAllCheckbox.checked = checkedCount > 0 && checkedCount === totalCheckboxes;
    selectAllCheckbox.indeterminate = checkedCount > 0 && checkedCount < totalCheckboxes;
}

// Hàm này đếm TỔNG SẢN PHẨM đã chọn (dùng cho cả thanh control và toast)
function calculateTotalSelectedProducts() {
    const checkedCheckboxes = document.querySelectorAll('.order-checkbox:checked');
    let totalProducts = 0;
    
    checkedCheckboxes.forEach(checkbox => {
        // Đảm bảo thuộc tính data-quantity đã được thêm vào HTML
        const quantity = parseInt(checkbox.getAttribute('data-quantity') || '0');
        totalProducts += quantity;
    });
    return totalProducts;
}

function updateSelectionControls() {
    const selectionControls = document.getElementById('selectionControls');
    const selectedCountElement = document.getElementById('selectedCount');
    
    if (!selectionControls || !selectedCountElement) return;
    
    const totalProducts = calculateTotalSelectedProducts();
    
    selectedCountElement.textContent = totalProducts;
    
    if (totalProducts > 0) {
        selectionControls.style.display = 'flex';
        selectionControls.classList.add('slide-down');
    } else {
        selectionControls.style.display = 'none';
        selectionControls.classList.remove('slide-down');
    }
}

function clearSelection() {
    selectedOrders.clear();
    
    const orderCheckboxes = document.querySelectorAll('.order-checkbox');
    orderCheckboxes.forEach(cb => {
        cb.checked = false;
        cb.closest('tr').classList.remove('table-row-selected');
    });
    
    const selectAllCheckbox = document.getElementById('selectAllCheckbox');
    if (selectAllCheckbox) {
        selectAllCheckbox.checked = false;
        selectAllCheckbox.indeterminate = false;
    }
    
    updateSelectionControls();
}

// ============= EXPORT FUNCTIONS (FIXED TOAST MESSAGE) =============

function initializeExportHandlers() {
    // Export selected orders - SỬ DỤNG ID
    const exportSelectedBtn = document.getElementById('exportSelectedBtn');
    if (exportSelectedBtn) {
        exportSelectedBtn.removeEventListener('click', exportSelectedToExcel); // Đảm bảo không trùng
        exportSelectedBtn.addEventListener('click', exportSelectedToExcel);
    }
    
    // Export total orders
    const exportTotalBtn = document.querySelector('a[href*="/vendor/sales-history/export"]');
    if (exportTotalBtn) {
        exportTotalBtn.removeEventListener('click', handleTotalExport); // Đảm bảo không trùng
        exportTotalBtn.addEventListener('click', handleTotalExport);
    }
}

// Hàm mới: xử lý xuất tổng
async function handleTotalExport(e) {
    e.preventDefault();
    
    try {
        showLoading(true);
        showToast('Đang xuất dữ liệu tổng...', 'info');
        
        const response = await fetch(this.href);
        
        if (response.ok) {
            const blob = await response.blob();
            const filename = `LichSuBanHang_Tong_${getCurrentDateString()}.xlsx`;
            downloadExcelFile(blob, filename);
            showToast('Xuất Excel tổng thành công!', 'success');
        } else {
            throw new Error('Export failed');
        }
    } catch (error) {
        console.error('Export error:', error);
        showToast('Có lỗi xảy ra khi xuất file tổng', 'error');
    } finally {
        showLoading(false);
    }
}

async function exportSelectedToExcel() {
    // Vẫn cần đếm số lượng đơn hàng duy nhất để kiểm tra và gửi lên server
    const distinctOrdersCount = new Set(Array.from(document.querySelectorAll('.order-checkbox:checked')).map(cb => cb.value)).size;
    
    if (distinctOrdersCount === 0) {
        showToast('Vui lòng chọn ít nhất một đơn hàng để xuất', 'warning');
        return;
    }
    
    // Lấy TỔNG SỐ SẢN PHẨM đã chọn
    const totalSelectedProducts = calculateTotalSelectedProducts();

    try {
        showLoading(true);
        
        // Xóa toast cũ trước khi hiển thị toast mới
        const existingToasts = document.querySelectorAll('.toast');
        existingToasts.forEach(toast => {
            toast.classList.add('hide');
            setTimeout(() => toast.remove(), 300);
        });
        
        const distinctOrderIds = Array.from(new Set(Array.from(document.querySelectorAll('.order-checkbox:checked')).map(cb => cb.value))).join(',');
        
        const formData = new URLSearchParams();
        formData.append('orderIds', distinctOrderIds);

        const response = await fetch('/vendor/sales-history/export-selected', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: formData.toString()
        });

        if (response.ok) {
            const blob = await response.blob();
            downloadExcelFile(blob, `LichSuBanHang_DaChon_${getCurrentDateString()}.xlsx`);
            
            // FIXED: Thay đổi nội dung thông báo
            showToast(`Đã xuất thành công ${totalSelectedProducts} sản phẩm`, 'success');
            
            clearSelection();
        } else {
            const errorText = await response.text();
            throw new Error('Export failed: ' + errorText);
        }
    } catch (error) {
        console.error('Export error:', error);
        showToast('Có lỗi xảy ra khi xuất file. Vui lòng thử lại.', 'error');
    } finally {
        showLoading(false);
    }
}

function downloadExcelFile(blob, filename) {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.style.display = 'none';
    
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    
    window.URL.revokeObjectURL(url);
}

function getCurrentDateString() {
    const now = new Date();
    const day = String(now.getDate()).padStart(2, '0');
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const year = now.getFullYear();
    return `${day}-${month}-${year}`;
}

// ============= UTILITY FUNCTIONS (ĐÃ SỬA DÙNG TOAST MỚI) =============

function showLoading(show) {
    if (show) {
        document.body.style.cursor = 'wait';
    } else {
        document.body.style.cursor = 'default';
    }
}

function showToast(message, type = 'info') {
    // Xóa toast cũ
    const existingToasts = document.querySelectorAll('.toast');
    existingToasts.forEach(toast => {
        toast.classList.add('hide');
        setTimeout(() => toast.remove(), 300);
    });

    // Tạo toast mới
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    
    const icons = {
        'success': 'fa-check-circle',
        'error': 'fa-exclamation-circle',
        'warning': 'fa-exclamation-triangle',
        'info': 'fa-info-circle'
    };
    
    const titles = {
        'success': 'Thành công',
        'error': 'Lỗi',
        'warning': 'Cảnh báo',
        'info': 'Thông tin'
    };

    toast.innerHTML = `
        <div class="toast-icon">
            <i class="fas ${icons[type]}"></i>
        </div>
        <div class="toast-content">
            <div class="toast-title">${titles[type]}</div>
            <div class="toast-message">${message}</div>
        </div>
        <button class="toast-close">
            <i class="fas fa-times"></i>
        </button>
    `;

    document.body.appendChild(toast);

    // Thêm sự kiện đóng toast
    const closeBtn = toast.querySelector('.toast-close');
    closeBtn.addEventListener('click', () => {
        toast.classList.add('hide');
        setTimeout(() => toast.remove(), 300);
    });

    // Hiển thị toast
    setTimeout(() => {
        toast.classList.add('show');
    }, 10);

    // Tự động đóng sau 5 giây
    setTimeout(() => {
        if (toast.parentNode) {
            toast.classList.add('hide');
            setTimeout(() => {
                if (toast.parentNode) {
                    toast.remove();
                }
            }, 300);
        }
    }, 5000);
}

// ============= KEYBOARD SHORTCUTS (Không đổi) =============

function initializeKeyboardShortcuts() {
    document.addEventListener('keydown', function(e) {
        // Ctrl + E for export selected
        if (e.ctrlKey && e.key === 'e') {
            e.preventDefault();
            exportSelectedToExcel();
        }
        
        // Escape to clear selection
        if (e.key === 'Escape') {
            clearSelection();
        }
        
        // Ctrl + A to select all (only when not in input field)
        if (e.ctrlKey && e.key === 'a' && !isInputField(e.target)) {
            e.preventDefault();
            const selectAllCheckbox = document.getElementById('selectAllCheckbox');
            if (selectAllCheckbox) {
                if (!selectAllCheckbox.checked || selectAllCheckbox.indeterminate) {
                    selectAllCheckbox.checked = true;
                } else {
                    selectAllCheckbox.checked = false;
                }
                toggleSelectAll(selectAllCheckbox);
            }
        }
    });
}

function isInputField(element) {
    const tag = element.tagName;
    return tag === 'INPUT' || tag === 'TEXTAREA' || element.isContentEditable;
}

// ============= BULK ACTIONS (Không đổi) =============

function deleteSelectedOrders() {
    const distinctOrdersCount = new Set(Array.from(document.querySelectorAll('.order-checkbox:checked')).map(cb => cb.value)).size;

    if (distinctOrdersCount === 0) {
        showToast('Vui lòng chọn ít nhất một đơn hàng để xóa', 'warning');
        return;
    }
    
    if (!confirm(`Bạn có chắc muốn xóa ${distinctOrdersCount} đơn hàng đã chọn? Hành động này không thể hoàn tả.`)) {
        return;
    }
    
    console.log('Deleting orders:', Array.from(selectedOrders));
    showToast('Tính năng xóa hàng loạt đang được phát triển', 'info');
}

// ============= GLOBAL EXPORTS (Không đổi) =============

window.clearFilter = clearFilter;
window.clearDateFilter = clearDateFilter;
window.toggleSelectAll = toggleSelectAll;
window.clearSelection = clearSelection;
window.updateSelectionControls = updateSelectionControls; 
window.exportSelectedToExcel = exportSelectedToExcel;
window.deleteSelectedOrders = deleteSelectedOrders;