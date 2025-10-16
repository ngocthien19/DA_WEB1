// Global variables for deletion and status change
let currentProductId = null;
let currentProductName = null;
let currentProductStatus = null;

// Global variables for selection
let selectedProducts = new Set();

// Function to show toast notification
function showToast(type, title, message) {
    const toastContainer = document.getElementById('toastContainer');
    const toastId = 'toast-' + Date.now();
    
    const toastHTML = `
        <div id="${toastId}" class="toast toast-${type}">
            <div class="toast-icon">
                <i class="fas ${type === 'success' ? 'fa-check-circle' : 'fa-exclamation-circle'}"></i>
            </div>
            <div class="toast-content">
                <div class="toast-title">${title}</div>
                <div class="toast-message">${message}</div>
            </div>
            <button class="toast-close" onclick="closeToast('${toastId}')">
                <i class="fas fa-times"></i>
            </button>
        </div>
    `;
    
    toastContainer.insertAdjacentHTML('beforeend', toastHTML);
    
    const toast = document.getElementById(toastId);
    setTimeout(() => {
        toast.classList.add('show');
    }, 100);
    
    // Auto close after 5 seconds
    setTimeout(() => {
        closeToast(toastId);
    }, 5000);
}

// Function to close toast
function closeToast(toastId) {
    const toast = document.getElementById(toastId);
    if (toast) {
        toast.classList.remove('show');
        toast.classList.add('hide');
        setTimeout(() => {
            toast.remove();
        }, 300);
    }
}

// Function to show delete confirmation modal
function showDeleteModal(button) {
    currentProductId = button.getAttribute('data-product-id');
    currentProductName = button.getAttribute('data-product-name');
    
    document.getElementById('productNameToDelete').textContent = currentProductName;
    
    const deleteModal = new bootstrap.Modal(document.getElementById('deleteModal'));
    deleteModal.show();
}

// Function to show status change modal
function showStatusModal(productId, productName, currentStatus) {
    currentProductId = productId;
    currentProductName = productName;
    currentProductStatus = currentStatus;
    
    document.getElementById('productNameForStatus').textContent = productName;
    
    // Pre-select current status
    if (currentStatus) {
        document.getElementById('statusActive').checked = true;
    } else {
        document.getElementById('statusInactive').checked = true;
    }
    
    const statusModal = new bootstrap.Modal(document.getElementById('statusModal'));
    statusModal.show();
}

// Function to delete product
function deleteProduct() {
    if (!currentProductId) return;
    
    const deleteModal = bootstrap.Modal.getInstance(document.getElementById('deleteModal'));
    deleteModal.hide();
    
    const confirmBtn = document.getElementById('confirmDeleteBtn');
    const originalText = confirmBtn.innerHTML;
    confirmBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i>Đang xóa...';
    confirmBtn.disabled = true;
    
    fetch(`/admin/products/delete/${currentProductId}`, {
        method: 'DELETE',
        headers: {
            'Content-Type': 'application/json',
            'X-Requested-With': 'XMLHttpRequest'
        }
    })
    .then(response => {
        if (!response.ok) {
            return response.json().then(error => {
                throw error;
            });
        }
        return response.json();
    })
    .then(data => {
        confirmBtn.innerHTML = originalText;
        confirmBtn.disabled = false;
        
        if (data.success) {
            showToast('success', 'Thành công', data.message);
            setTimeout(() => {
                window.location.reload();
            }, 1000);
        } else {
            showToast('error', 'Lỗi', data.message);
        }
    })
    .catch(error => {
        confirmBtn.innerHTML = originalText;
        confirmBtn.disabled = false;
        
        console.error('Error:', error);
        showToast('error', 'Lỗi', error.message || 'Đã có lỗi xảy ra khi xóa sản phẩm!');
    })
    .finally(() => {
        currentProductId = null;
        currentProductName = null;
    });
}

// Function to initialize status buttons
function initStatusButtons() {
    const statusToggles = document.querySelectorAll('.status-toggle');
    
    statusToggles.forEach(toggle => {
        toggle.addEventListener('click', function(e) {
            e.stopPropagation();
            
            // Get product info
            const productId = this.getAttribute('data-product-id');
            const currentStatus = this.getAttribute('data-current-status') === 'true';
            
            // Find product name from table row
            const row = this.closest('tr');
            const productName = row.querySelector('.product-name').textContent;
            
            // Show status modal
            showStatusModal(productId, productName, currentStatus);
        });
    });
}

// Function to update product status
function updateProductStatus() {
    if (!currentProductId) return;
    
    const newStatus = document.getElementById('statusActive').checked;
    
    // If status hasn't changed, just close modal
    if (newStatus === currentProductStatus) {
        const statusModal = bootstrap.Modal.getInstance(document.getElementById('statusModal'));
        statusModal.hide();
        return;
    }
    
    const statusModal = bootstrap.Modal.getInstance(document.getElementById('statusModal'));
    statusModal.hide();
    
    const confirmBtn = document.getElementById('confirmStatusBtn');
    const originalText = confirmBtn.innerHTML;
    confirmBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i>Đang cập nhật...';
    confirmBtn.disabled = true;
    
    fetch(`/admin/products/change-status/${currentProductId}?status=${newStatus}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-Requested-With': 'XMLHttpRequest'
        }
    })
    .then(response => {
        if (!response.ok) {
            return response.json().then(error => {
                throw error;
            });
        }
        return response.json();
    })
    .then(data => {
        confirmBtn.innerHTML = originalText;
        confirmBtn.disabled = false;
        
        if (data.success) {
            showToast('success', 'Thành công', data.message);
            setTimeout(() => {
                window.location.reload();
            }, 1000);
        } else {
            showToast('error', 'Lỗi', data.message);
            // KHÔNG reload trang khi có lỗi
        }
    })
    .catch(error => {
        confirmBtn.innerHTML = originalText;
        confirmBtn.disabled = false;
        
        console.error('Error:', error);
        // Hiển thị thông báo lỗi cụ thể từ server
        const errorMessage = error.message || 'Đã có lỗi xảy ra khi thay đổi trạng thái!';
        showToast('error', 'Lỗi', errorMessage);
    })
    .finally(() => {
        currentProductId = null;
        currentProductName = null;
        currentProductStatus = null;
    });
}

// Function to handle sorting - FIXED VERSION
function sortTable(sortBy) {
    const urlParams = new URLSearchParams(window.location.search);
    const currentSort = urlParams.get('sortBy');
    const currentDir = urlParams.get('sortDir') || 'desc';
    const keyword = urlParams.get('keyword');
    const loaiSanPhamFilter = urlParams.get('loaiSanPhamFilter');
    
    // Determine new sort direction
    let newDir = 'desc';
    if (currentSort === sortBy) {
        newDir = currentDir === 'desc' ? 'asc' : 'desc';
    }
    
    // Build URL with all parameters - FIXED: chỉ gửi sortBy và sortDir riêng biệt
    let url = '/admin/products?';
    url += `sortBy=${sortBy}`;
    url += `&sortDir=${newDir}`;
    url += `&page=0`; // Reset về trang đầu khi sort
    
    if (keyword) {
        url += `&keyword=${encodeURIComponent(keyword)}`;
    }
    
    if (loaiSanPhamFilter) {
        url += `&loaiSanPhamFilter=${encodeURIComponent(loaiSanPhamFilter)}`;
    }
    
    window.location.href = url;
}

// Update sort icons - FIXED VERSION
function updateSortIcons() {
    const urlParams = new URLSearchParams(window.location.search);
    const currentSort = urlParams.get('sortBy');
    const currentDir = urlParams.get('sortDir') || 'desc';
    
    // Reset all sort icons
    const sortIcons = document.querySelectorAll('.sort-icon');
    sortIcons.forEach(icon => {
        icon.classList.remove('active', 'fa-sort-up', 'fa-sort-down');
        icon.classList.add('fa-sort');
    });
    
    // Set active sort icon
    if (currentSort) {
        const sortHeader = document.querySelector(`th[data-sort="${currentSort}"]`);
        if (sortHeader) {
            const icon = sortHeader.querySelector('.sort-icon');
            if (icon) {
                icon.classList.add('active');
                icon.classList.remove('fa-sort');
                if (currentDir === 'asc') {
                    icon.classList.add('fa-sort-up');
                } else {
                    icon.classList.add('fa-sort-down');
                }
            }
        }
    }
}

// Function to format currency
function formatCurrency(amount) {
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(amount);
}

// Function to show bulk delete modal
function showBulkDeleteModal() {
    if (selectedProducts.size === 0) {
        showToast('warning', 'Cảnh báo', 'Vui lòng chọn ít nhất một sản phẩm để xóa!');
        return;
    }

    // Update modal content
    document.getElementById('bulkDeleteCount').textContent = selectedProducts.size;
    document.getElementById('bulkDeleteCountBtn').textContent = selectedProducts.size;
    
    // Show selected products preview
    const previewContainer = document.getElementById('bulkDeletePreview');
    if (previewContainer) {
        previewContainer.innerHTML = '';
        
        const selectedRows = document.querySelectorAll('tbody tr.selected');
        selectedRows.forEach((row, index) => {
            if (index < 10) { // Hiển thị tối đa 10 sản phẩm
                const productName = row.querySelector('.product-name').textContent;
                const productId = row.querySelector('.product-checkbox').value;
                const productItem = document.createElement('div');
                productItem.className = 'd-flex justify-content-between align-items-center py-1';
                productItem.innerHTML = `
                    <span>${productName}</span>
                    <small class="text-muted">ID: ${productId}</small>
                `;
                previewContainer.appendChild(productItem);
            }
        });
        
        if (selectedProducts.size > 10) {
            const moreItem = document.createElement('div');
            moreItem.className = 'text-center text-muted py-1';
            moreItem.textContent = `... và ${selectedProducts.size - 10} sản phẩm khác`;
            previewContainer.appendChild(moreItem);
        }
    }

    const bulkDeleteModal = new bootstrap.Modal(document.getElementById('bulkDeleteModal'));
    bulkDeleteModal.show();
}

// ============= SELECTION FUNCTIONS =============

// Function to toggle select all
function toggleSelectAll(checkbox) {
    const productCheckboxes = document.querySelectorAll('.product-checkbox');
    const isChecked = checkbox.checked;
    
    selectedProducts.clear();
    
    productCheckboxes.forEach(cb => {
        cb.checked = isChecked;
        if (isChecked) {
            selectedProducts.add(parseInt(cb.value));
        }
    });
    
    updateSelectionControls();
    updateRowSelection();
}

function updateSelectionControls() {
    const productCheckboxes = document.querySelectorAll('.product-checkbox');
    const selectAllCheckbox = document.getElementById('selectAllCheckbox');
    const selectionControls = document.getElementById('selectionControls');
    const selectedCount = document.getElementById('selectedCount');
    const bulkDeleteButton = document.getElementById('bulkDeleteButton');
    const bulkStatusButton = document.getElementById('bulkStatusButton');
    
    // Update selected products set
    selectedProducts.clear();
    productCheckboxes.forEach(cb => {
        if (cb.checked) {
            selectedProducts.add(parseInt(cb.value));
        }
    });
    
    const checkedCount = selectedProducts.size;
    
    // Update UI based on selection
    if (checkedCount > 0) {
        // SHOW controls
        selectionControls.style.cssText = 'display: flex !important; align-items: center; gap: 10px; padding: 10px; background-color: #e9ecef; border-radius: 5px; margin-bottom: 15px;';
        selectedCount.textContent = checkedCount;
        bulkDeleteButton.style.cssText = 'display: inline-block !important;';
        bulkStatusButton.style.cssText = 'display: inline-block !important;';
    } else {
        // HIDE controls
        selectionControls.style.cssText = 'display: none !important;';
        bulkDeleteButton.style.cssText = 'display: none !important;';
        bulkStatusButton.style.cssText = 'display: none !important;';
    }
    
    // Update select all checkbox
    if (checkedCount === 0) {
        selectAllCheckbox.checked = false;
        selectAllCheckbox.indeterminate = false;
    } else if (checkedCount === productCheckboxes.length) {
        selectAllCheckbox.checked = true;
        selectAllCheckbox.indeterminate = false;
    } else {
        selectAllCheckbox.checked = false;
        selectAllCheckbox.indeterminate = true;
    }
    
    updateRowSelection();
}


// Function to show bulk status modal
function showBulkStatusModal() {
    if (selectedProducts.size === 0) {
        showToast('warning', 'Cảnh báo', 'Vui lòng chọn ít nhất một sản phẩm để thay đổi trạng thái!');
        return;
    }

    // Update modal content
    document.getElementById('bulkStatusCount').textContent = selectedProducts.size;
    document.getElementById('bulkStatusCountBtn').textContent = selectedProducts.size;
    
    // Show selected products preview
    const previewContainer = document.getElementById('bulkStatusPreview');
    if (previewContainer) {
        previewContainer.innerHTML = '';
        
        const selectedRows = document.querySelectorAll('tbody tr.selected');
        selectedRows.forEach((row, index) => {
            if (index < 10) { // Hiển thị tối đa 10 sản phẩm
                const productName = row.querySelector('.product-name').textContent;
                const productId = row.querySelector('.product-checkbox').value;
                const productItem = document.createElement('div');
                productItem.className = 'd-flex justify-content-between align-items-center py-1';
                productItem.innerHTML = `
                    <span>${productName}</span>
                    <small class="text-muted">ID: ${productId}</small>
                `;
                previewContainer.appendChild(productItem);
            }
        });
        
        if (selectedProducts.size > 10) {
            const moreItem = document.createElement('div');
            moreItem.className = 'text-center text-muted py-1';
            moreItem.textContent = `... và ${selectedProducts.size - 10} sản phẩm khác`;
            previewContainer.appendChild(moreItem);
        }
    }

    const bulkStatusModal = new bootstrap.Modal(document.getElementById('bulkStatusModal'));
    bulkStatusModal.show();
}

// Function to update multiple product status
function updateMultipleProductStatus() {
    if (selectedProducts.size === 0) {
        showToast('warning', 'Cảnh báo', 'Không có sản phẩm nào được chọn!');
        return;
    }

    const bulkStatusModal = bootstrap.Modal.getInstance(document.getElementById('bulkStatusModal'));
    bulkStatusModal.hide();

    const confirmBtn = document.getElementById('confirmBulkStatusBtn');
    const originalText = confirmBtn.innerHTML;
    confirmBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i>Đang cập nhật...';
    confirmBtn.disabled = true;

    // Get selected status
    const newStatus = document.querySelector('input[name="bulkStatusRadio"]:checked').value === 'true';

    // Convert Set to Array
    const productIds = Array.from(selectedProducts);

    fetch('/admin/products/bulk-change-status', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-Requested-With': 'XMLHttpRequest'
        },
        body: JSON.stringify({ 
            productIds: productIds,
            status: newStatus
        })
    })
    .then(response => {
        if (!response.ok) {
            return response.json().then(error => {
                throw error;
            });
        }
        return response.json();
    })
    .then(data => {
        confirmBtn.innerHTML = originalText;
        confirmBtn.disabled = false;
        
        if (data.success) {
            showToast('success', 'Thành công', data.message);
            // Clear selection and reload
            clearSelection();
            setTimeout(() => {
                window.location.reload();
            }, 1000);
        } else {
            showToast('error', 'Lỗi', data.message);
        }
    })
    .catch(error => {
        confirmBtn.innerHTML = originalText;
        confirmBtn.disabled = false;
        
        console.error('Error:', error);
        showToast('error', 'Lỗi', error.message || 'Đã có lỗi xảy ra khi cập nhật trạng thái!');
    });
}


// Function to delete multiple products
function deleteMultipleProducts() {
    if (selectedProducts.size === 0) {
        showToast('warning', 'Cảnh báo', 'Không có sản phẩm nào được chọn!');
        return;
    }

    const bulkDeleteModal = bootstrap.Modal.getInstance(document.getElementById('bulkDeleteModal'));
    bulkDeleteModal.hide();

    const confirmBtn = document.getElementById('confirmBulkDeleteBtn');
    const originalText = confirmBtn.innerHTML;
    confirmBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i>Đang xóa...';
    confirmBtn.disabled = true;

    // Convert Set to Array
    const productIds = Array.from(selectedProducts);

    fetch('/admin/products/bulk-delete', {
        method: 'DELETE',
        headers: {
            'Content-Type': 'application/json',
            'X-Requested-With': 'XMLHttpRequest'
        },
        body: JSON.stringify({ productIds: productIds })
    })
    .then(response => {
        if (!response.ok) {
            return response.json().then(error => {
                throw error;
            });
        }
        return response.json();
    })
    .then(data => {
        confirmBtn.innerHTML = originalText;
        confirmBtn.disabled = false;
        
        if (data.success) {
            showToast('success', 'Thành công', data.message);
            // Clear selection and reload
            clearSelection();
            setTimeout(() => {
                window.location.reload();
            }, 1000);
        } else {
            showToast('error', 'Lỗi', data.message);
        }
    })
    .catch(error => {
        confirmBtn.innerHTML = originalText;
        confirmBtn.disabled = false;
        
        console.error('Error:', error);
        showToast('error', 'Lỗi', error.message || 'Đã có lỗi xảy ra khi xóa sản phẩm!');
    });
}

// Cập nhật hàm updateSelectionControls để thêm style cho các hàng được chọn
function updateRowSelection() {
    const rows = document.querySelectorAll('tbody tr');
    
    rows.forEach(row => {
        const checkbox = row.querySelector('.product-checkbox');
        if (checkbox && checkbox.checked) {
            row.classList.add('selected');
            row.style.backgroundColor = '#f8f9fa'; // Màu nền khi được chọn
        } else {
            row.classList.remove('selected');
            row.style.backgroundColor = ''; // Xóa màu nền
        }
    });
}

// Function to clear selection
function clearSelection() {
    selectedProducts.clear();
    
    const productCheckboxes = document.querySelectorAll('.product-checkbox');
    productCheckboxes.forEach(cb => {
        cb.checked = false;
    });
    
    const selectAllCheckbox = document.getElementById('selectAllCheckbox');
    selectAllCheckbox.checked = false;
    selectAllCheckbox.indeterminate = false;
    
    updateSelectionControls();
}

// Function to export selected products to Excel - IFRAME VERSION
function exportSelectedToExcel() {
    // Lấy các tham số filter hiện tại
    const urlParams = new URLSearchParams(window.location.search);
    const keyword = urlParams.get('keyword') || '';
    const loaiSanPhamFilter = urlParams.get('loaiSanPhamFilter') || '';
    const sortBy = urlParams.get('sortBy') || 'ngayNhap';
    const sortDir = urlParams.get('sortDir') || 'desc';
    
    // Tạo URL export
    let exportUrl = '/admin/products/export-excel?';
    
    // Thêm các tham số filter
    if (keyword) exportUrl += `keyword=${encodeURIComponent(keyword)}&`;
    if (loaiSanPhamFilter) exportUrl += `loaiSanPhamFilter=${encodeURIComponent(loaiSanPhamFilter)}&`;
    exportUrl += `sortBy=${sortBy}&sortDir=${sortDir}`;
    
    // Thêm selected products nếu có
    if (selectedProducts.size > 0) {
        const selectedIds = Array.from(selectedProducts).join(',');
        exportUrl += `&selectedProducts=${selectedIds}`;
        
        showToast('success', 'Xuất Excel', `Đang xuất ${selectedProducts.size} sản phẩm được chọn...`);
    } else {
        showToast('success', 'Xuất Excel', 'Đang xuất tất cả sản phẩm...');
    }
    
    // Tạo iframe ẩn để tải file
    const iframe = document.createElement('iframe');
    iframe.style.display = 'none';
    iframe.src = exportUrl;
    document.body.appendChild(iframe);
    
    // Tự động xóa iframe sau khi tải
    setTimeout(() => {
        document.body.removeChild(iframe);
    }, 5000);
}

// Replace existing exportToExcel function
function exportToExcel() {
    exportSelectedToExcel();
}

// Event listeners
document.addEventListener('DOMContentLoaded', function() {
    // Delete confirmation
    const confirmDeleteBtn = document.getElementById('confirmDeleteBtn');
    if (confirmDeleteBtn) {
        confirmDeleteBtn.addEventListener('click', deleteProduct);
    }
    
	// Bulk delete confirmation
	    const confirmBulkDeleteBtn = document.getElementById('confirmBulkDeleteBtn');
	    if (confirmBulkDeleteBtn) {
	        confirmBulkDeleteBtn.addEventListener('click', deleteMultipleProducts);
	    }
		
		// Bulk status confirmation
		    const confirmBulkStatusBtn = document.getElementById('confirmBulkStatusBtn');
		    if (confirmBulkStatusBtn) {
		        confirmBulkStatusBtn.addEventListener('click', updateMultipleProductStatus);
		    }
		
    // Status confirmation
    const confirmStatusBtn = document.getElementById('confirmStatusBtn');
    if (confirmStatusBtn) {
        confirmStatusBtn.addEventListener('click', updateProductStatus);
    }
    
    // Reset delete modal on close
    const deleteModal = document.getElementById('deleteModal');
    if (deleteModal) {
        deleteModal.addEventListener('hidden.bs.modal', function() {
            currentProductId = null;
            currentProductName = null;
            
            const confirmBtn = document.getElementById('confirmDeleteBtn');
            if (confirmBtn) {
                confirmBtn.innerHTML = '<i class="fas fa-trash me-1"></i>Xóa';
                confirmBtn.disabled = false;
            }
        });
    }
    
    // Reset status modal on close
    const statusModal = document.getElementById('statusModal');
    if (statusModal) {
        statusModal.addEventListener('hidden.bs.modal', function() {
            currentProductId = null;
            currentProductName = null;
            currentProductStatus = null;
            
            const confirmBtn = document.getElementById('confirmStatusBtn');
            if (confirmBtn) {
                confirmBtn.innerHTML = '<i class="fas fa-save me-1"></i>Lưu thay đổi';
                confirmBtn.disabled = false;
            }
        });
    }
    
    // Initialize status buttons
    initStatusButtons();
    
    // Add click handlers for sortable columns
    const sortableHeaders = document.querySelectorAll('th.sortable');
    sortableHeaders.forEach(header => {
        header.addEventListener('click', function() {
            const sortBy = this.getAttribute('data-sort');
            if (sortBy) {
                sortTable(sortBy);
            }
        });
        
        // Add cursor pointer style
        header.style.cursor = 'pointer';
    });
    
    // Update sort icons
    const urlParams = new URLSearchParams(window.location.search);
    const currentSort = urlParams.get('sortBy');
    const currentDir = urlParams.get('sortDir') || 'desc';
    
    if (currentSort) {
        const sortHeader = document.querySelector(`th[data-sort="${currentSort}"]`);
        if (sortHeader) {
            const icon = sortHeader.querySelector('.sort-icon');
            if (icon) {
                icon.classList.add('active');
                icon.classList.remove('fa-sort');
                icon.classList.add(currentDir === 'asc' ? 'fa-sort-up' : 'fa-sort-down');
            }
        }
    }
    
    // Initialize selection
    updateSelectionControls();
});