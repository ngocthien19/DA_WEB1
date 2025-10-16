// ============= VENDOR PRODUCTS MANAGEMENT JS =============

// Global variables
let currentProductId = null;
let currentProductName = null;
let currentProductStatus = null;
let selectedProducts = new Set();

// ============= SELECTION MANAGEMENT =============

function toggleSelectAll(checkbox) {
    const productCheckboxes = document.querySelectorAll('.product-checkbox');
    productCheckboxes.forEach(cb => {
        cb.checked = checkbox.checked;
        if (checkbox.checked) {
            selectedProducts.add(cb.value);
        } else {
            selectedProducts.delete(cb.value);
        }
    });
    updateSelectionControls();
}

function updateSelectionControls() {
    const selectedCheckboxes = document.querySelectorAll('.product-checkbox:checked');
    selectedProducts = new Set(Array.from(selectedCheckboxes).map(cb => cb.value));
    
    const selectionControls = document.getElementById('selectionControls');
    const selectedCount = document.getElementById('selectedCount');
    const bulkDeleteBtn = document.querySelector('.btn-bulk-delete');
    
    if (selectedProducts.size > 0) {
        if (selectionControls) selectionControls.style.display = 'flex';
        if (selectedCount) selectedCount.textContent = selectedProducts.size;
        if (bulkDeleteBtn) bulkDeleteBtn.style.display = 'inline-block';
    } else {
        if (selectionControls) selectionControls.style.display = 'none';
        if (bulkDeleteBtn) bulkDeleteBtn.style.display = 'none';
    }
    
    // Update select all checkbox state
    const totalCheckboxes = document.querySelectorAll('.product-checkbox').length;
    const selectAllCheckbox = document.getElementById('selectAllCheckbox');
    if (selectAllCheckbox && totalCheckboxes > 0) {
        selectAllCheckbox.checked = selectedProducts.size === totalCheckboxes;
        selectAllCheckbox.indeterminate = selectedProducts.size > 0 && selectedProducts.size < totalCheckboxes;
    }
}

function clearSelection() {
    const checkboxes = document.querySelectorAll('.product-checkbox:checked');
    checkboxes.forEach(cb => cb.checked = false);
    selectedProducts.clear();
    updateSelectionControls();
}

// ============= DELETE MODAL FUNCTIONS =============

function showDeleteModal(button) {
    currentProductId = button.getAttribute('data-product-id');
    currentProductName = button.getAttribute('data-product-name');
    
    const productNameElement = document.getElementById('productNameToDelete');
    if (productNameElement) {
        productNameElement.textContent = currentProductName;
    }
    
    const confirmBtn = document.getElementById('confirmDeleteBtn');
    if (confirmBtn) {
        confirmBtn.onclick = function() {
            deleteProduct(currentProductId);
        };
    }
    
    const deleteModal = new bootstrap.Modal(document.getElementById('deleteModal'));
    deleteModal.show();
}

function showBulkDeleteModal() {
    if (selectedProducts.size === 0) {
        showToast('error', 'Lỗi', 'Vui lòng chọn ít nhất 1 sản phẩm để xóa');
        return;
    }
    
    const bulkDeleteCount = document.getElementById('bulkDeleteCount');
    const bulkDeleteCountBtn = document.getElementById('bulkDeleteCountBtn');
    if (bulkDeleteCount) bulkDeleteCount.textContent = selectedProducts.size;
    if (bulkDeleteCountBtn) bulkDeleteCountBtn.textContent = selectedProducts.size;
    
    // Display preview of products to be deleted
    const previewContainer = document.getElementById('bulkDeletePreview');
    if (previewContainer) {
        previewContainer.innerHTML = '';
        
        selectedProducts.forEach(productId => {
            const productRow = document.querySelector(`.product-checkbox[value="${productId}"]`);
            if (productRow) {
                const row = productRow.closest('tr');
                const productNameElement = row.querySelector('.product-name');
                const productName = productNameElement ? productNameElement.textContent : 'Unknown';
                
                const div = document.createElement('div');
                div.className = 'preview-item';
                div.innerHTML = `<i class="fas fa-box me-2"></i>${productName}`;
                previewContainer.appendChild(div);
            }
        });
    }
    
    const confirmBtn = document.getElementById('confirmBulkDeleteBtn');
    if (confirmBtn) {
        confirmBtn.onclick = bulkDeleteProducts;
    }
    
    const bulkDeleteModal = new bootstrap.Modal(document.getElementById('bulkDeleteModal'));
    bulkDeleteModal.show();
}

// ============= BULK STATUS FUNCTIONS =============

function showBulkStatusModal(newStatus) {
    if (selectedProducts.size === 0) {
        showToast('error', 'Lỗi', 'Vui lòng chọn ít nhất 1 sản phẩm để thay đổi trạng thái');
        return;
    }
    
    const statusModal = new bootstrap.Modal(document.getElementById('bulkStatusModal'));
    const statusTitle = document.getElementById('bulkStatusTitle');
    const statusMessage = document.getElementById('bulkStatusMessage');
    const confirmBtn = document.getElementById('confirmBulkStatusBtn');
    
    if (newStatus) {
        statusTitle.textContent = 'Kích hoạt hàng loạt';
        statusMessage.innerHTML = `Bạn có chắc chắn muốn kích hoạt <strong>${selectedProducts.size}</strong> sản phẩm đã chọn?`;
        confirmBtn.innerHTML = '<i class="fas fa-play-circle me-1"></i>Kích hoạt';
        confirmBtn.className = 'btn btn-success';
    } else {
        statusTitle.textContent = 'Ngừng bán hàng loạt';
        statusMessage.innerHTML = `Bạn có chắc chắn muốn ngừng bán <strong>${selectedProducts.size}</strong> sản phẩm đã chọn?`;
        confirmBtn.innerHTML = '<i class="fas fa-pause-circle me-1"></i>Ngừng bán';
        confirmBtn.className = 'btn btn-warning';
    }
    
    // Hiển thị preview sản phẩm
    const previewContainer = document.getElementById('bulkStatusPreview');
    if (previewContainer) {
        previewContainer.innerHTML = '';
        
        selectedProducts.forEach(productId => {
            const productRow = document.querySelector(`.product-checkbox[value="${productId}"]`);
            if (productRow) {
                const row = productRow.closest('tr');
                const productNameElement = row.querySelector('.product-name');
                const productName = productNameElement ? productNameElement.textContent : 'Unknown';
                
                const div = document.createElement('div');
                div.className = 'preview-item';
                div.innerHTML = `<i class="fas fa-box me-2"></i>${productName}`;
                previewContainer.appendChild(div);
            }
        });
    }
    
    confirmBtn.onclick = function() {
        updateBulkStatus(newStatus);
    };
    
    statusModal.show();
}

function updateBulkStatus(newStatus) {
    const bulkStatusModal = bootstrap.Modal.getInstance(document.getElementById('bulkStatusModal'));
    if (bulkStatusModal) {
        bulkStatusModal.hide();
    }
    
    const productIds = Array.from(selectedProducts);
    
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';
    
    const headers = {
        'Content-Type': 'application/json'
    };
    
    if (csrfToken) {
        headers[csrfHeader] = csrfToken;
    }
    
    fetch('/vendor/products/bulk-change-status', {
        method: 'POST',
        headers: headers,
        body: JSON.stringify({ 
            productIds: productIds,
            status: newStatus
        })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showToast('success', 'Thành công', data.message);
            
            if (data.hasErrors && data.errors && data.errors.length > 0) {
                data.errors.forEach(error => {
                    showToast('error', 'Lỗi từng phần', error);
                });
            }
            
            setTimeout(() => {
                location.reload();
            }, 1000);
        } else {
            showToast('error', 'Lỗi', data.message || 'Có lỗi xảy ra khi thay đổi trạng thái');
            
            if (data.errors && data.errors.length > 0) {
                data.errors.forEach(error => {
                    console.error(error);
                });
            }
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('error', 'Lỗi', 'Có lỗi xảy ra khi thay đổi trạng thái hàng loạt');
    });
}

// ============= STATUS MODAL FUNCTIONS =============

function showStatusModal(productId, productName, currentStatus) {
    currentProductId = productId;
    currentProductName = productName;
    currentProductStatus = currentStatus;
    
    const productNameElement = document.getElementById('productNameForStatus');
    if (productNameElement) {
        productNameElement.textContent = productName;
    }
    
    // Set radio button based on current status
    const statusActive = document.getElementById('statusActive');
    const statusInactive = document.getElementById('statusInactive');
    
    if (currentStatus) {
        if (statusActive) statusActive.checked = true;
    } else {
        if (statusInactive) statusInactive.checked = true;
    }
    
    const confirmBtn = document.getElementById('confirmStatusBtn');
    if (confirmBtn) {
        confirmBtn.onclick = updateProductStatus;
    }
    
    const statusModal = new bootstrap.Modal(document.getElementById('statusModal'));
    statusModal.show();
}

// ============= API CALLS =============

function deleteProduct(productId) {
    // Close modal first
    const deleteModal = bootstrap.Modal.getInstance(document.getElementById('deleteModal'));
    if (deleteModal) {
        deleteModal.hide();
    }
    
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';
    
    const headers = {
        'Content-Type': 'application/json'
    };
    
    if (csrfToken) {
        headers[csrfHeader] = csrfToken;
    }
    
    fetch(`/vendor/products/delete/${productId}`, {
        method: 'DELETE',
        headers: headers
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showToast('success', 'Thành công', data.message || 'Xóa sản phẩm thành công');
            setTimeout(() => {
                location.reload();
            }, 1000);
        } else {
            showToast('error', 'Lỗi', data.message || 'Có lỗi xảy ra khi xóa sản phẩm');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('error', 'Lỗi', 'Có lỗi xảy ra khi xóa sản phẩm');
    });
}

function bulkDeleteProducts() {
    // Close modal first
    const bulkDeleteModal = bootstrap.Modal.getInstance(document.getElementById('bulkDeleteModal'));
    if (bulkDeleteModal) {
        bulkDeleteModal.hide();
    }
    
    const productIds = Array.from(selectedProducts);
    
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';
    
    const headers = {
        'Content-Type': 'application/json'
    };
    
    if (csrfToken) {
        headers[csrfHeader] = csrfToken;
    }
    
    fetch('/vendor/products/bulk-delete', {
        method: 'DELETE',
        headers: headers,
        body: JSON.stringify({ productIds: productIds })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showToast('success', 'Thành công', data.message);
            
            if (data.hasErrors && data.errors && data.errors.length > 0) {
                console.warn('Some products could not be deleted:', data.errors);
            }
            
            setTimeout(() => {
                location.reload();
            }, 1000);
        } else {
            showToast('error', 'Lỗi', data.message || 'Có lỗi xảy ra khi xóa sản phẩm');
            
            if (data.errors && data.errors.length > 0) {
                data.errors.forEach(error => {
                    console.error(error);
                });
            }
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('error', 'Lỗi', 'Có lỗi xảy ra khi xóa nhiều sản phẩm');
    });
}

function updateProductStatus() {
    const statusRadio = document.querySelector('input[name="statusRadio"]:checked');
    if (!statusRadio) {
        showToast('error', 'Lỗi', 'Vui lòng chọn trạng thái');
        return;
    }
    
    const newStatus = statusRadio.value === 'true';
    
    // Close modal first
    const statusModal = bootstrap.Modal.getInstance(document.getElementById('statusModal'));
    if (statusModal) {
        statusModal.hide();
    }
    
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';
    
    const headers = {
        'Content-Type': 'application/x-www-form-urlencoded'
    };
    
    if (csrfToken) {
        headers[csrfHeader] = csrfToken;
    }
    
    fetch(`/vendor/products/change-status/${currentProductId}?status=${newStatus}`, {
        method: 'POST',
        headers: headers
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showToast('success', 'Thành công', data.message || 'Thay đổi trạng thái thành công');
            setTimeout(() => {
                location.reload();
            }, 1000);
        } else {
            showToast('error', 'Lỗi', data.message || 'Có lỗi xảy ra khi cập nhật trạng thái');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('error', 'Lỗi', 'Có lỗi xảy ra khi cập nhật trạng thái');
    });
}

// ============= EXPORT FUNCTIONS =============

function exportSelectedToExcel() {
    const productIds = selectedProducts.size > 0 ? Array.from(selectedProducts) : [];
    
    const urlParams = new URLSearchParams(window.location.search);
    const keyword = urlParams.get('keyword') || '';
    const loaiSanPhamFilter = urlParams.get('loaiSanPhamFilter') || '';
    const sortBy = urlParams.get('sortBy') || 'ngayNhap';
    const sortDir = urlParams.get('sortDir') || 'desc';
    
    let url = '/vendor/products/export-excel?';
    const params = [];
    
    if (keyword) params.push(`keyword=${encodeURIComponent(keyword)}`);
    if (loaiSanPhamFilter) params.push(`loaiSanPhamFilter=${encodeURIComponent(loaiSanPhamFilter)}`);
    params.push(`sortBy=${sortBy}`);
    params.push(`sortDir=${sortDir}`);
    
    if (productIds.length > 0) {
        params.push(`selectedProducts=${productIds.join(',')}`);
    }
    
    url += params.join('&');
    
    // Show loading toast
    showToast('info', 'Đang xuất', 'Đang chuẩn bị file Excel...');
    
    window.location.href = url;
}

// ============= TOAST NOTIFICATIONS =============

function showToast(type, title, message) {
    const toastContainer = document.getElementById('toastContainer');
    if (!toastContainer) return;
    
    const toastId = 'toast-' + Date.now();
    
    const iconClass = type === 'success' ? 'fa-check-circle' : 
                     type === 'error' ? 'fa-exclamation-circle' : 
                     'fa-info-circle';
    
    const toastHTML = `
        <div id="${toastId}" class="toast toast-${type}">
            <div class="toast-icon">
                <i class="fas ${iconClass}"></i>
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
    
    setTimeout(() => {
        closeToast(toastId);
    }, 5000);
}

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

// ============= EVENT LISTENERS =============

document.addEventListener('DOMContentLoaded', function() {
    // Initialize selection controls
    updateSelectionControls();
    
    // Add change listeners to all product checkboxes
    const productCheckboxes = document.querySelectorAll('.product-checkbox');
    productCheckboxes.forEach(checkbox => {
        checkbox.addEventListener('change', updateSelectionControls);
    });
    
    // Add click handlers for status toggle buttons
    const statusButtons = document.querySelectorAll('.status-toggle');
    statusButtons.forEach(button => {
        button.addEventListener('click', function() {
            const productId = this.getAttribute('data-product-id');
            const productNameElement = this.closest('tr').querySelector('.product-name');
            const productName = productNameElement ? productNameElement.textContent : 'Unknown';
            const currentStatus = this.getAttribute('data-current-status') === 'true';
            showStatusModal(productId, productName, currentStatus);
        });
    });
    
    // Add click handlers for delete buttons
    const deleteButtons = document.querySelectorAll('.btn-delete');
    deleteButtons.forEach(button => {
        button.addEventListener('click', function() {
            showDeleteModal(this);
        });
    });
    
    // Fix modal backdrop issue - Remove all backdrops when modal is hidden
    const modals = document.querySelectorAll('.modal');
    modals.forEach(modal => {
        modal.addEventListener('hidden.bs.modal', function() {
            // Remove all backdrops
            const backdrops = document.querySelectorAll('.modal-backdrop');
            backdrops.forEach(backdrop => backdrop.remove());
            // Restore body scroll
            document.body.classList.remove('modal-open');
            document.body.style.removeProperty('padding-right');
            document.body.style.removeProperty('overflow');
        });
    });
    
    // Auto-dismiss alerts
    const alerts = document.querySelectorAll('.alert');
    alerts.forEach(function(alert) {
        setTimeout(function() {
            const bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        }, 5000);
    });
});

// ============= UTILITY FUNCTIONS =============

function formatCurrency(amount) {
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(amount);
}

function formatNumber(number) {
    return new Intl.NumberFormat('vi-VN').format(number);
}