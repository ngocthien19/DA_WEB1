// ============= VENDOR PROMOTIONS MANAGEMENT JS =============

// Global variables
let currentPromotionId = null;
let currentPromotionCode = null;
let currentPromotionStatus = null;
let selectedPromotions = new Set();

// ============= REAL-TIME STATUS UPDATE =============

function initializeRealTimeStatus() {
    console.log('Initializing real-time status system...');
    
    // Cập nhật ngay khi load trang
    updateRealTimeStatus();
    
    // Cập nhật mỗi phút để đảm bảo trạng thái luôn chính xác
    setInterval(updateRealTimeStatus, 60000);
    
    console.log('Real-time status system initialized');
}

function updateRealTimeStatus() {
    const promotionRows = document.querySelectorAll('.vendor-promotions-table tbody tr:not(.empty-state)');
    const now = new Date();
    
    let updatedCount = 0;
    
    promotionRows.forEach(row => {
        const startDateCell = row.querySelector('td:nth-child(5) span[data-date]');
        const endDateCell = row.querySelector('td:nth-child(6) span[data-date]');
        const statusToggle = row.querySelector('.status-toggle');
        const statusBadge = row.querySelector('.status-badge');
        
        if (startDateCell && endDateCell && statusToggle && statusBadge) {
            const startDateText = startDateCell.getAttribute('data-date');
            const endDateText = endDateCell.getAttribute('data-date');
            const isActive = statusToggle.getAttribute('data-current-status') === 'true';
            
            if (!startDateText || !endDateText) {
                console.warn('Missing date data attributes');
                return;
            }
            
            // Parse dates từ định dạng yyyy-MM-dd và đặt thời gian để so sánh chính xác
            const startDate = new Date(startDateText + 'T00:00:00');
            const endDate = new Date(endDateText + 'T23:59:59');
            
            // Lấy trạng thái hiện tại từ badge để so sánh
            const currentStatusText = statusBadge.querySelector('span')?.textContent;
            
            // Xác định trạng thái mới dựa trên thời gian thực
            let newStatus = '';
            let newStatusText = '';
            let newIcon = '';
            
            if (!isActive) {
                newStatus = 'status-inactive';
                newStatusText = 'Ngừng kích hoạt';
                newIcon = 'fa-times-circle';
            } else if (now < startDate) {
                newStatus = 'status-upcoming';
                newStatusText = 'Sắp diễn ra';
                newIcon = 'fa-clock';
            } else if (now > endDate) {
                newStatus = 'status-expired';
                newStatusText = 'Hết hạn';
                newIcon = 'fa-times-circle';
            } else {
                newStatus = 'status-active';
                newStatusText = 'Đang kích hoạt';
                newIcon = 'fa-check-circle';
            }
            
            // Chỉ cập nhật nếu trạng thái thay đổi
            if (currentStatusText !== newStatusText) {
                // Update badge appearance
                statusBadge.className = 'status-badge ' + newStatus;
                statusBadge.innerHTML = `<i class="fas ${newIcon}"></i><span>${newStatusText}</span>`;
                
                // Thêm tooltip với thông tin chi tiết
                let tooltipText = '';
                if (!isActive) {
                    tooltipText = 'Khuyến mãi đã bị ngừng kích hoạt thủ công';
                } else if (now < startDate) {
                    const daysLeft = Math.ceil((startDate - now) / (1000 * 60 * 60 * 24));
                    tooltipText = `Sẽ bắt đầu sau ${daysLeft} ngày`;
                } else if (now > endDate) {
                    const daysAgo = Math.floor((now - endDate) / (1000 * 60 * 60 * 24));
                    tooltipText = `Đã hết hạn ${daysAgo} ngày trước`;
                } else {
                    const daysLeft = Math.ceil((endDate - now) / (1000 * 60 * 60 * 24));
                    tooltipText = `Còn ${daysLeft} ngày sử dụng`;
                }
                statusBadge.title = tooltipText;
                
                updatedCount++;
                
                // Log để debug
                const promotionCode = row.querySelector('.promotion-code')?.textContent || 'Unknown';
                console.log(`Real-time update: ${promotionCode} changed from "${currentStatusText}" to "${newStatusText}"`);
            }
        }
    });
    
    if (updatedCount > 0) {
        console.log(`Real-time status update completed: ${updatedCount} promotions updated`);
    }
}

// ============= SELECTION MANAGEMENT =============

function toggleSelectAll(checkbox) {
    const promotionCheckboxes = document.querySelectorAll('.promotion-checkbox');
    promotionCheckboxes.forEach(cb => {
        cb.checked = checkbox.checked;
        if (checkbox.checked) {
            selectedPromotions.add(cb.value);
        } else {
            selectedPromotions.delete(cb.value);
        }
    });
    updateSelectionControls();
}

function updateSelectionControls() {
    const selectedCheckboxes = document.querySelectorAll('.promotion-checkbox:checked');
    selectedPromotions = new Set(Array.from(selectedCheckboxes).map(cb => cb.value));
    
    const selectionControls = document.getElementById('selectionControls');
    const selectedCount = document.getElementById('selectedCount');
    const bulkDeleteBtn = document.querySelector('.btn-bulk-delete');
    const bulkStatusBtn = document.querySelector('.btn-bulk-status');
    
    if (selectedPromotions.size > 0) {
        if (selectionControls) selectionControls.style.display = 'flex';
        if (selectedCount) selectedCount.textContent = selectedPromotions.size;
        if (bulkDeleteBtn) bulkDeleteBtn.style.display = 'inline-block';
        if (bulkStatusBtn) bulkStatusBtn.style.display = 'inline-block';
    } else {
        if (selectionControls) selectionControls.style.display = 'none';
        if (bulkDeleteBtn) bulkDeleteBtn.style.display = 'none';
        if (bulkStatusBtn) bulkStatusBtn.style.display = 'none';
    }
    
    // Update select all checkbox state
    const totalCheckboxes = document.querySelectorAll('.promotion-checkbox').length;
    const selectAllCheckbox = document.getElementById('selectAllCheckbox');
    if (selectAllCheckbox && totalCheckboxes > 0) {
        selectAllCheckbox.checked = selectedPromotions.size === totalCheckboxes;
        selectAllCheckbox.indeterminate = selectedPromotions.size > 0 && selectedPromotions.size < totalCheckboxes;
    }
}

function clearSelection() {
    const checkboxes = document.querySelectorAll('.promotion-checkbox:checked');
    checkboxes.forEach(cb => cb.checked = false);
    selectedPromotions.clear();
    updateSelectionControls();
}

// ============= DELETE MODAL FUNCTIONS =============

function showDeleteModal(button) {
    currentPromotionId = button.getAttribute('data-promotion-id');
    currentPromotionCode = button.getAttribute('data-promotion-code');
    
    const promotionCodeElement = document.getElementById('promotionCodeToDelete');
    if (promotionCodeElement) {
        promotionCodeElement.textContent = currentPromotionCode;
    }
    
    const confirmBtn = document.getElementById('confirmDeleteBtn');
    if (confirmBtn) {
        confirmBtn.onclick = function() {
            deletePromotion(currentPromotionId);
        };
    }
    
    const deleteModal = new bootstrap.Modal(document.getElementById('deleteModal'));
    deleteModal.show();
}

function showBulkDeleteModal() {
    if (selectedPromotions.size === 0) {
        showToast('error', 'Lỗi', 'Vui lòng chọn ít nhất 1 khuyến mãi để xóa');
        return;
    }
    
    const bulkDeleteCount = document.getElementById('bulkDeleteCount');
    const bulkDeleteCountBtn = document.getElementById('bulkDeleteCountBtn');
    if (bulkDeleteCount) bulkDeleteCount.textContent = selectedPromotions.size;
    if (bulkDeleteCountBtn) bulkDeleteCountBtn.textContent = selectedPromotions.size;
    
    // Display preview of promotions to be deleted
    const previewContainer = document.getElementById('bulkDeletePreview');
    if (previewContainer) {
        previewContainer.innerHTML = '';
        
        selectedPromotions.forEach(promotionId => {
            const promotionRow = document.querySelector(`.promotion-checkbox[value="${promotionId}"]`);
            if (promotionRow) {
                const row = promotionRow.closest('tr');
                const promotionCodeElement = row.querySelector('.promotion-code');
                const promotionCode = promotionCodeElement ? promotionCodeElement.textContent : 'Unknown';
                
                const div = document.createElement('div');
                div.className = 'preview-item';
                div.innerHTML = `<i class="fas fa-tags me-2"></i>${promotionCode}`;
                previewContainer.appendChild(div);
            }
        });
    }
    
    const confirmBtn = document.getElementById('confirmBulkDeleteBtn');
    if (confirmBtn) {
        confirmBtn.onclick = bulkDeletePromotions;
    }
    
    const bulkDeleteModal = new bootstrap.Modal(document.getElementById('bulkDeleteModal'));
    bulkDeleteModal.show();
}

// ============= STATUS MODAL FUNCTIONS =============

function showStatusModal(promotionId, promotionCode, currentStatus) {
    currentPromotionId = promotionId;
    currentPromotionCode = promotionCode;
    currentPromotionStatus = currentStatus;
    
    const promotionCodeElement = document.getElementById('promotionCodeForStatus');
    if (promotionCodeElement) {
        promotionCodeElement.textContent = promotionCode;
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
        confirmBtn.onclick = updatePromotionStatus;
    }
    
    const statusModal = new bootstrap.Modal(document.getElementById('statusModal'));
    statusModal.show();
}

function showBulkStatusModal() {
    if (selectedPromotions.size === 0) {
        showToast('error', 'Lỗi', 'Vui lòng chọn ít nhất 1 khuyến mãi để thay đổi trạng thái');
        return;
    }
    
    const bulkStatusCount = document.getElementById('bulkStatusCount');
    const bulkStatusCountBtn = document.getElementById('bulkStatusCountBtn');
    if (bulkStatusCount) bulkStatusCount.textContent = selectedPromotions.size;
    if (bulkStatusCountBtn) bulkStatusCountBtn.textContent = selectedPromotions.size;
    
    // Display preview of promotions to be updated
    const previewContainer = document.getElementById('bulkStatusPreview');
    if (previewContainer) {
        previewContainer.innerHTML = '';
        
        selectedPromotions.forEach(promotionId => {
            const promotionRow = document.querySelector(`.promotion-checkbox[value="${promotionId}"]`);
            if (promotionRow) {
                const row = promotionRow.closest('tr');
                const promotionCodeElement = row.querySelector('.promotion-code');
                const promotionCode = promotionCodeElement ? promotionCodeElement.textContent : 'Unknown';
                
                const div = document.createElement('div');
                div.className = 'preview-item';
                div.innerHTML = `<i class="fas fa-tags me-2"></i>${promotionCode}`;
                previewContainer.appendChild(div);
            }
        });
    }
    
    const confirmBtn = document.getElementById('confirmBulkStatusBtn');
    if (confirmBtn) {
        confirmBtn.onclick = bulkUpdatePromotionStatus;
    }
    
    const bulkStatusModal = new bootstrap.Modal(document.getElementById('bulkStatusModal'));
    bulkStatusModal.show();
}

// ============= API CALLS =============

function deletePromotion(promotionId) {
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
    
    fetch(`/vendor/promotions/delete/${promotionId}`, {
        method: 'DELETE',
        headers: headers
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showToast('success', 'Thành công', data.message || 'Xóa khuyến mãi thành công');
            setTimeout(() => {
                location.reload();
            }, 1000);
        } else {
            showToast('error', 'Lỗi', data.message || 'Có lỗi xảy ra khi xóa khuyến mãi');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('error', 'Lỗi', 'Có lỗi xảy ra khi xóa khuyến mãi');
    });
}

// ============= BULK DELETE FUNCTION =============

function bulkDeletePromotions() {
    // Close modal first
    const bulkDeleteModal = bootstrap.Modal.getInstance(document.getElementById('bulkDeleteModal'));
    if (bulkDeleteModal) {
        bulkDeleteModal.hide();
    }
    
    const promotionIds = Array.from(selectedPromotions);
    
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';
    
    // Create form data instead of JSON
    const formData = new FormData();
    formData.append('ids', promotionIds.join(','));
    
    const headers = {};
    if (csrfToken) {
        headers[csrfHeader] = csrfToken;
    }
    
    fetch('/vendor/promotions/bulk-delete', {
        method: 'POST',
        headers: headers,
        body: formData
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showToast('success', 'Thành công', data.message);
            
            if (data.errorCount > 0 && data.errors && data.errors.length > 0) {
                data.errors.forEach(error => {
                    showToast('warning', 'Cảnh báo', error);
                });
            }
            
            setTimeout(() => {
                location.reload();
            }, 1000);
        } else {
            showToast('error', 'Lỗi', data.message || 'Có lỗi xảy ra khi xóa khuyến mãi');
            
            if (data.errors && data.errors.length > 0) {
                data.errors.forEach(error => {
                    console.error(error);
                });
            }
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('error', 'Lỗi', 'Có lỗi xảy ra khi xóa nhiều khuyến mãi');
    });
}

function updatePromotionStatus() {
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
    
    fetch(`/vendor/promotions/change-status/${currentPromotionId}?status=${newStatus}`, {
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

// ============= BULK STATUS UPDATE FUNCTION =============

function bulkUpdatePromotionStatus() {
    const statusRadio = document.querySelector('input[name="bulkStatusRadio"]:checked');
    if (!statusRadio) {
        showToast('error', 'Lỗi', 'Vui lòng chọn trạng thái');
        return;
    }
    
    const newStatus = statusRadio.value === 'true';
    const promotionIds = Array.from(selectedPromotions);
    
    // Close modal first
    const bulkStatusModal = bootstrap.Modal.getInstance(document.getElementById('bulkStatusModal'));
    if (bulkStatusModal) {
        bulkStatusModal.hide();
    }
    
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';
    
    // Create form data instead of JSON
    const formData = new FormData();
    formData.append('ids', promotionIds.join(','));
    formData.append('status', newStatus);
    
    const headers = {};
    if (csrfToken) {
        headers[csrfHeader] = csrfToken;
    }
    
    fetch('/vendor/promotions/bulk-change-status', {
        method: 'POST',
        headers: headers,
        body: formData
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showToast('success', 'Thành công', data.message);
            
            if (data.errorCount > 0 && data.errors && data.errors.length > 0) {
                data.errors.forEach(error => {
                    showToast('warning', 'Cảnh báo', error);
                });
            }
            
            setTimeout(() => {
                location.reload();
            }, 1500);
        } else {
            showToast('error', 'Lỗi', data.message || 'Có lỗi xảy ra khi cập nhật trạng thái');
            
            if (data.errors && data.errors.length > 0) {
                data.errors.forEach(error => {
                    console.error(error);
                });
            }
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('error', 'Lỗi', 'Có lỗi xảy ra khi cập nhật trạng thái hàng loạt');
    });
}

// ============= EXPORT FUNCTIONS =============

function exportSelectedToExcel() {
    const promotionIds = Array.from(selectedPromotions);
    
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
    
    const form = document.createElement('form');
    form.method = 'POST';
    
    // Nếu không có khuyến mãi nào được chọn, export tất cả
    if (promotionIds.length === 0) {
        form.action = '/vendor/promotions/export-all';
        showToast('info', 'Thông báo', 'Đang xuất tất cả khuyến mãi...');
    } else {
        form.action = '/vendor/promotions/export-selected';
        showToast('info', 'Thông báo', `Đang xuất ${promotionIds.length} khuyến mãi được chọn...`);
    }
    
    // Add CSRF token
    const csrfInput = document.createElement('input');
    csrfInput.type = 'hidden';
    csrfInput.name = '_csrf';
    csrfInput.value = csrfToken;
    form.appendChild(csrfInput);
    
    // Chỉ thêm promotion IDs nếu có khuyến mãi được chọn
    if (promotionIds.length > 0) {
        // Tạo một input duy nhất với danh sách IDs phân cách bằng dấu phẩy
        const idsInput = document.createElement('input');
        idsInput.type = 'hidden';
        idsInput.name = 'promotionIds';
        idsInput.value = promotionIds.join(',');
        form.appendChild(idsInput);
    }
    
    document.body.appendChild(form);
    form.submit();
    document.body.removeChild(form);
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
    console.log('DOM loaded - Initializing promotions management...');
    
    // Initialize selection controls
    updateSelectionControls();
    
    // Initialize real-time status system
    initializeRealTimeStatus();
    
    // Add change listeners to all promotion checkboxes
    const promotionCheckboxes = document.querySelectorAll('.promotion-checkbox');
    promotionCheckboxes.forEach(checkbox => {
        checkbox.addEventListener('change', updateSelectionControls);
    });
    
    // Status toggle buttons
    const statusButtons = document.querySelectorAll('.status-toggle');
    console.log('Found status buttons:', statusButtons.length);
    
    statusButtons.forEach(button => {
        button.addEventListener('click', function() {
            const promotionId = this.getAttribute('data-promotion-id');
            const promotionCodeElement = this.closest('tr').querySelector('.promotion-code');
            const promotionCode = promotionCodeElement ? promotionCodeElement.textContent : 'Unknown';
            const currentStatus = this.getAttribute('data-current-status') === 'true';
            
            console.log('Status button clicked - ID:', promotionId, 'Current status:', currentStatus);
            showStatusModal(promotionId, promotionCode, currentStatus);
        });
    });
    
    // Delete buttons
    const deleteButtons = document.querySelectorAll('.btn-delete');
    deleteButtons.forEach(button => {
        button.addEventListener('click', function() {
            showDeleteModal(this);
        });
    });
    
    // Fix modal backdrop issue
    const modals = document.querySelectorAll('.modal');
    modals.forEach(modal => {
        modal.addEventListener('hidden.bs.modal', function() {
            const backdrops = document.querySelectorAll('.modal-backdrop');
            backdrops.forEach(backdrop => backdrop.remove());
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
    
    console.log('Promotions management system fully initialized');
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

function formatDate(dateString) {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('vi-VN').format(date);
}