// ============= ADMIN SALES HISTORY MANAGEMENT JS =============

document.addEventListener('DOMContentLoaded', function() {
    console.log('Admin Sales History page loaded');
    
    initializeAdminSalesHistory();
});

function initializeAdminSalesHistory() {
    initializeDateValidation();
    initializeFilters();
    initializeSelectionManagement();
    initializeExportHandlers();
    initializeKeyboardShortcuts();
}

// ============= DATE VALIDATION =============

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

// ============= FILTER FUNCTIONS =============

function initializeFilters() {
    // Apply filter when button is clicked
    const applyFilterBtn = document.getElementById('applyFilterBtn');
    if (applyFilterBtn) {
        applyFilterBtn.addEventListener('click', function(e) {
            e.preventDefault();
            document.getElementById('filterForm').submit();
        });
    }
    
    // Auto-submit when store is selected
    const storeSelect = document.getElementById('storeSelect');
    if (storeSelect) {
        storeSelect.addEventListener('change', function() {
            // Thêm delay nhỏ để đảm bảo giá trị đã được chọn
            setTimeout(() => {
                document.getElementById('filterForm').submit();
            }, 100);
        });
    }
}

function clearAllFilters() {
    const urlParams = new URLSearchParams(window.location.search);
    urlParams.delete('keyword');
    urlParams.delete('productName');
    urlParams.delete('maCuaHang');
    urlParams.delete('startDate');
    urlParams.delete('endDate');
    urlParams.set('page', '0');
    window.location.href = `${window.location.pathname}?${urlParams.toString()}`;
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
        case 'store':
            urlParams.delete('maCuaHang');
            break;
        case 'date':
            urlParams.delete('startDate');
            urlParams.delete('endDate');
            break;
        case 'all':
            urlParams.delete('keyword');
            urlParams.delete('productName');
            urlParams.delete('maCuaHang');
            urlParams.delete('startDate');
            urlParams.delete('endDate');
            urlParams.set('page', '0');
            break;
    }
    
    // Reset về page 0 khi xóa filter
    if (filterType !== 'all') {
        urlParams.set('page', '0');
    }
    
    window.location.href = `${window.location.pathname}?${urlParams.toString()}`;
}

function clearDateFilter() {
    const urlParams = new URLSearchParams(window.location.search);
    urlParams.delete('startDate');
    urlParams.delete('endDate');
    window.location.href = `${window.location.pathname}?${urlParams.toString()}`;
}

// ============= SELECTION MANAGEMENT =============

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

function calculateTotalSelectedProducts() {
    const checkedCheckboxes = document.querySelectorAll('.order-checkbox:checked');
    let totalProducts = 0;
    
    checkedCheckboxes.forEach(checkbox => {
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

// ============= EXPORT FUNCTIONS =============

function initializeExportHandlers() {
    // Export selected orders
    const exportSelectedBtn = document.getElementById('exportSelectedBtn');
    if (exportSelectedBtn) {
        exportSelectedBtn.removeEventListener('click', exportSelectedToExcel);
        exportSelectedBtn.addEventListener('click', exportSelectedToExcel);
    }
    
    // Export total - THÊM PHẦN NÀY
    const exportTotalBtn = document.getElementById('exportTotalBtn');
    if (exportTotalBtn) {
        exportTotalBtn.removeEventListener('click', exportTotalToExcel);
        exportTotalBtn.addEventListener('click', exportTotalToExcel);
    }
}

// THÊM HÀM EXPORT TỔNG
async function exportTotalToExcel() {
    try {
        showLoading(true);
        
        // Lấy các tham số filter hiện tại
        const urlParams = new URLSearchParams(window.location.search);
        const keyword = urlParams.get('keyword') || '';
        const productName = urlParams.get('productName') || '';
        const maCuaHang = urlParams.get('maCuaHang') || '';
        const startDate = urlParams.get('startDate') || '';
        const endDate = urlParams.get('endDate') || '';
        
        // Xây dựng URL export
        let exportUrl = '/admin/sales-history/export?';
        const params = [];
        
        if (keyword) params.push(`keyword=${encodeURIComponent(keyword)}`);
        if (productName) params.push(`productName=${encodeURIComponent(productName)}`);
        if (maCuaHang) params.push(`maCuaHang=${maCuaHang}`);
        if (startDate) params.push(`startDate=${startDate}`);
        if (endDate) params.push(`endDate=${endDate}`);
        
        exportUrl += params.join('&');
        
        const response = await fetch(exportUrl);
        
        if (response.ok) {
            const blob = await response.blob();
            
            // Lấy tên file từ header response hoặc tạo mặc định
            const contentDisposition = response.headers.get('content-disposition');
            let filename = `LichSuBanHang_Admin_${getCurrentDateString()}.xlsx`;
            
            if (contentDisposition) {
                const filenameMatch = contentDisposition.match(/filename="(.+)"/);
                if (filenameMatch) {
                    filename = filenameMatch[1];
                }
            }
            
            downloadExcelFile(blob, filename);
            
            // Hiển thị toast thông báo
            showToast('Đã xuất file Excel tổng thành công', 'success');
            
        } else {
            const errorText = await response.text();
            throw new Error('Export failed: ' + errorText);
        }
    } catch (error) {
        console.error('Export total error:', error);
        showToast('Có lỗi xảy ra khi xuất file Excel tổng. Vui lòng thử lại.', 'error');
    } finally {
        showLoading(false);
    }
}

async function exportSelectedToExcel() {
    const distinctOrdersCount = new Set(Array.from(document.querySelectorAll('.order-checkbox:checked')).map(cb => cb.value)).size;
    
    if (distinctOrdersCount === 0) {
        showToast('Vui lòng chọn ít nhất một đơn hàng để xuất', 'warning');
        return;
    }
    
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

        const response = await fetch('/admin/sales-history/export-selected', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: formData.toString()
        });

        if (response.ok) {
            const blob = await response.blob();
            downloadExcelFile(blob, `LichSuBanHang_DaChon_Admin_${getCurrentDateString()}.xlsx`);
            
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
    // Tạo URL object từ blob
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.style.display = 'none';
    
    // Thêm vào DOM và click
    document.body.appendChild(a);
    a.click();
    
    // Dọn dẹp
    setTimeout(() => {
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
    }, 100);
}

function getCurrentDateString() {
    const now = new Date();
    const day = String(now.getDate()).padStart(2, '0');
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const year = now.getFullYear();
    return `${day}-${month}-${year}`;
}

// ============= UTILITY FUNCTIONS =============

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

// ============= KEYBOARD SHORTCUTS =============

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

// ============= GLOBAL EXPORTS =============

window.clearFilter = clearFilter;
window.clearDateFilter = clearDateFilter;
window.toggleSelectAll = toggleSelectAll;
window.clearSelection = clearSelection;
window.clearAllFilters = clearAllFilters;
window.updateSelectionControls = updateSelectionControls; 
window.exportSelectedToExcel = exportSelectedToExcel;