// ============= ADMIN STORES MANAGEMENT JS =============

// Global variables
let currentStoreId = null;
let currentStoreName = null;
let selectedStores = new Set(); // Sử dụng Set để lưu trữ các ID cửa hàng được chọn

// ============= TOAST NOTIFICATIONS (Giữ nguyên) =============

// Function to show toast notification
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


// ============= SELECTION MANAGEMENT (Copy từ users.js, đổi tên class) =============

function toggleSelectAll(checkbox) {
    const storeCheckboxes = document.querySelectorAll('.store-checkbox');
    storeCheckboxes.forEach(cb => {
        cb.checked = checkbox.checked;
        if (checkbox.checked) {
            selectedStores.add(cb.value);
        } else {
            selectedStores.delete(cb.value);
        }
    });
    updateSelectionControls();
}

function updateSelectionControls() {
    const selectedCheckboxes = document.querySelectorAll('.store-checkbox:checked');
    selectedStores = new Set(Array.from(selectedCheckboxes).map(cb => cb.value));
    
    const selectionControls = document.getElementById('selectionControls');
    const selectedCount = document.getElementById('selectedCount');
    
    if (selectedStores.size > 0) {
        if (selectionControls) selectionControls.style.display = 'flex';
        if (selectedCount) selectedCount.textContent = selectedStores.size;
    } else {
        if (selectionControls) selectionControls.style.display = 'none';
    }
    
    // Update select all checkbox state
    const totalCheckboxes = document.querySelectorAll('.store-checkbox').length;
    const selectAllCheckbox = document.getElementById('selectAllCheckbox');
    if (selectAllCheckbox && totalCheckboxes > 0) {
        selectAllCheckbox.checked = selectedStores.size === totalCheckboxes;
        selectAllCheckbox.indeterminate = selectedStores.size > 0 && selectedStores.size < totalCheckboxes;
    }
    
    // Thêm class 'selected' cho hàng được chọn
    document.querySelectorAll('.stores-table tbody tr').forEach(row => {
        const checkbox = row.querySelector('.store-checkbox');
        if (checkbox && checkbox.checked) {
            row.classList.add('selected');
        } else {
            row.classList.remove('selected');
        }
    });
}

function clearSelection() {
    const checkboxes = document.querySelectorAll('.store-checkbox:checked');
    checkboxes.forEach(cb => cb.checked = false);
    selectedStores.clear();
    updateSelectionControls();
}


// ============= DELETE MODAL FUNCTIONS (Chỉnh sửa để dùng ID cửa hàng) =============

function showDeleteModal(button) {
    currentStoreId = button.getAttribute('data-store-id');
    currentStoreName = button.getAttribute('data-store-name');
    
    document.getElementById('storeNameToDelete').textContent = currentStoreName;
    
    const confirmBtn = document.getElementById('confirmDeleteBtn');
    if (confirmBtn) {
        confirmBtn.onclick = function() {
            deleteStore(currentStoreId);
        };
    }
    
    const deleteModal = new bootstrap.Modal(document.getElementById('deleteModal'));
    deleteModal.show();
}

// Function to handle store deletion (đơn lẻ)
function deleteStore(storeId) {
    if (!storeId) return;
    
    // Close modal
    const deleteModal = bootstrap.Modal.getInstance(document.getElementById('deleteModal'));
    deleteModal.hide();
    
    // Show loading state
    const confirmBtn = document.getElementById('confirmDeleteBtn');
    const originalText = confirmBtn.innerHTML;
    confirmBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i>Đang xóa...';
    confirmBtn.disabled = true;
    
    fetch(`/admin/stores/delete/${storeId}`, {
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
        // Reset button state
        confirmBtn.innerHTML = originalText;
        confirmBtn.disabled = false;
        
        if (data.success) {
            showToast('success', 'Thành công', data.message);
            // Reload page after 1 second
            setTimeout(() => {
                window.location.reload();
            }, 1000);
        } else {
            showToast('error', 'Lỗi', data.message);
        }
    })
    .catch(error => {
        // Reset button state
        confirmBtn.innerHTML = originalText;
        confirmBtn.disabled = false;
        
        console.error('Error:', error);
        showToast('error', 'Lỗi', error.message || 'Đã có lỗi xảy ra khi xóa cửa hàng!');
    })
    .finally(() => {
        // Reset current store data
        currentStoreId = null;
        currentStoreName = null;
    });
}

// Function to show bulk delete confirmation modal
function showBulkDeleteModal() {
    if (selectedStores.size === 0) {
        showToast('error', 'Lỗi', 'Vui lòng chọn ít nhất 1 cửa hàng để xóa');
        return;
    }
    
    const bulkDeleteCount = document.getElementById('bulkDeleteCount');
    const bulkDeleteCountBtn = document.getElementById('bulkDeleteCountBtn');
    if (bulkDeleteCount) bulkDeleteCount.textContent = selectedStores.size;
    if (bulkDeleteCountBtn) bulkDeleteCountBtn.textContent = selectedStores.size;
    
    // Display preview of stores to be deleted
    const previewContainer = document.getElementById('bulkDeletePreview');
    if (previewContainer) {
        previewContainer.innerHTML = '';
        
        selectedStores.forEach(storeId => {
            const storeRow = document.querySelector(`.store-checkbox[value="${storeId}"]`);
            if (storeRow) {
                const row = storeRow.closest('tr');
                const storeNameElement = row.querySelector('td:nth-child(4)'); // Tên cửa hàng ở cột 4
                const storeName = storeNameElement ? storeNameElement.textContent : 'Unknown';
                
                const div = document.createElement('div');
                div.className = 'bulk-preview-item';
                div.innerHTML = `<i class="fas fa-store me-2"></i>${storeName}`;
                previewContainer.appendChild(div);
            }
        });
    }
    
    const confirmBtn = document.getElementById('confirmBulkDeleteBtn');
    if (confirmBtn) {
        confirmBtn.onclick = bulkDeleteStores;
    }
    
    const bulkDeleteModal = new bootstrap.Modal(document.getElementById('bulkDeleteModal'));
    bulkDeleteModal.show();
}

// Function to handle bulk store deletion
function bulkDeleteStores() {
    // Close modal first
    const bulkDeleteModal = bootstrap.Modal.getInstance(document.getElementById('bulkDeleteModal'));
    if (bulkDeleteModal) {
        bulkDeleteModal.hide();
    }
    
    const storeIds = Array.from(selectedStores);
    
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';
    
    // Create form data
    const formData = new FormData();
    formData.append('ids', storeIds.join(','));
    
    const headers = {};
    if (csrfToken) {
        headers[csrfHeader] = csrfToken;
    }
    
    fetch('/admin/stores/bulk-delete', {
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
                    showToast('error', 'Lỗi xóa', error); // Dùng error toast cho lỗi
                });
            }
            
            setTimeout(() => {
                location.reload();
            }, 1000);
        } else {
            showToast('error', 'Lỗi', data.message || 'Có lỗi xảy ra khi xóa cửa hàng hàng loạt');
            
            if (data.errors && data.errors.length > 0) {
                data.errors.forEach(error => {
                    console.error(error);
                });
            }
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('error', 'Lỗi', 'Có lỗi xảy ra khi xóa nhiều cửa hàng');
    });
}

// ============= STATUS MODAL FUNCTIONS (Chỉnh sửa để dùng ID cửa hàng) =============

function showStatusModal(button) {
    currentStoreId = button.getAttribute('data-store-id');
    currentStoreName = button.getAttribute('data-store-name');
    const currentStoreStatus = button.getAttribute('data-current-status') === 'true'; // status là boolean
    
    const storeNameElement = document.getElementById('storeNameForStatus');
    if (storeNameElement) {
        storeNameElement.textContent = currentStoreName;
    }
    
    // Set radio button based on current status (true=Hoạt động, false=Đã khóa)
    const statusActive = document.getElementById('statusActive');
    const statusInactive = document.getElementById('statusInactive');
    
    if (currentStoreStatus) {
        if (statusActive) statusActive.checked = true;
    } else {
        if (statusInactive) statusInactive.checked = true;
    }
    
    const confirmBtn = document.getElementById('confirmStatusBtn');
    if (confirmBtn) {
        confirmBtn.onclick = updateStoreStatus;
    }
    
    const statusModal = new bootstrap.Modal(document.getElementById('statusModal'));
    statusModal.show();
}

// Thêm hàm xử lý lý do khóa
function handleLyDoKhoaPresetChange() {
    const presetSelect = document.getElementById('lyDoKhoaPreset');
    const lyDoInput = document.getElementById('lyDoKhoaInput');
    
    if (presetSelect.value === 'other') {
        lyDoInput.value = '';
        lyDoInput.disabled = false;
        lyDoInput.focus();
    } else if (presetSelect.value) {
        lyDoInput.value = presetSelect.value;
        lyDoInput.disabled = true;
    } else {
        lyDoInput.value = '';
        lyDoInput.disabled = false;
    }
}

// Cập nhật hàm showStatusModal
function showStatusModal(button) {
    currentStoreId = button.getAttribute('data-store-id');
    currentStoreName = button.getAttribute('data-store-name');
    const currentStoreStatus = button.getAttribute('data-current-status') === 'true';
    
    document.getElementById('storeNameForStatus').textContent = currentStoreName;
    
    // Reset lý do khóa section
    const lyDoKhoaSection = document.getElementById('lyDoKhoaSection');
    const lyDoKhoaPreset = document.getElementById('lyDoKhoaPreset');
    const lyDoKhoaInput = document.getElementById('lyDoKhoaInput');
    
    lyDoKhoaPreset.value = '';
    lyDoKhoaInput.value = '';
    lyDoKhoaInput.disabled = false;
    lyDoKhoaSection.style.display = 'none';
    
    // Set radio button
    const statusActive = document.getElementById('statusActive');
    const statusInactive = document.getElementById('statusInactive');
    
    if (currentStoreStatus) {
        statusActive.checked = true;
    } else {
        statusInactive.checked = true;
    }
    
    // Thêm event listener cho radio buttons
    document.querySelectorAll('input[name="statusRadio"]').forEach(radio => {
        radio.addEventListener('change', function() {
            if (this.value === 'false') {
                lyDoKhoaSection.style.display = 'block';
            } else {
                lyDoKhoaSection.style.display = 'none';
            }
        });
    });
    
    const confirmBtn = document.getElementById('confirmStatusBtn');
    if (confirmBtn) {
        confirmBtn.onclick = updateStoreStatus;
    }
    
    const statusModal = new bootstrap.Modal(document.getElementById('statusModal'));
    statusModal.show();
}

// Cập nhật hàm updateStoreStatus
function updateStoreStatus() {
    const statusRadio = document.querySelector('input[name="statusRadio"]:checked');
    if (!statusRadio) {
        showToast('error', 'Lỗi', 'Vui lòng chọn trạng thái');
        return;
    }
    
    const newStatus = statusRadio.value;
    let lyDoKhoa = '';
    
    // Nếu là khóa cửa hàng, lấy lý do
    if (newStatus === 'false') {
        lyDoKhoa = document.getElementById('lyDoKhoaInput').value.trim();
        if (!lyDoKhoa) {
            showToast('error', 'Lỗi', 'Vui lòng nhập lý do khóa cửa hàng');
            return;
        }
    }
    
    // Close modal
    const statusModal = bootstrap.Modal.getInstance(document.getElementById('statusModal'));
    if (statusModal) {
        statusModal.hide();
    }
    
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';
    
    const formData = new FormData();
    formData.append('status', newStatus);
    if (lyDoKhoa) {
        formData.append('lyDoKhoa', lyDoKhoa);
    }
    
    const headers = {};
    if (csrfToken) {
        headers[csrfHeader] = csrfToken;
    }
    
    fetch(`/admin/stores/change-status/${currentStoreId}`, {
        method: 'POST',
        headers: headers,
        body: formData
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

// Thêm vào file stores.js
function initLyDoKhoaCounter() {
    const lyDoInput = document.getElementById('lyDoKhoaInput');
    if (!lyDoInput) return;

    // Tạo counter element
    const counter = document.createElement('div');
    counter.id = 'lyDoKhoaCounter';
    counter.className = 'lyDoKhoaCounter';
    counter.textContent = '0/500';
    lyDoInput.parentNode.appendChild(counter);

    // Event listener để update counter
    lyDoInput.addEventListener('input', function() {
        const length = this.value.length;
        counter.textContent = `${length}/500`;
        
        // Đổi màu khi gần đạt giới hạn
        if (length > 450) {
            counter.className = 'lyDoKhoaCounter warning';
        } else if (length >= 500) {
            counter.className = 'lyDoKhoaCounter danger';
        } else {
            counter.className = 'lyDoKhoaCounter';
        }
    });
}

// ============= BULK STATUS FUNCTIONS =============

function handleBulkLyDoKhoaPresetChange() {
    const presetSelect = document.getElementById('bulkLyDoKhoaPreset');
    const lyDoInput = document.getElementById('bulkLyDoKhoaInput');
    
    if (presetSelect.value === 'other') {
        lyDoInput.value = '';
        lyDoInput.disabled = false;
        lyDoInput.focus();
    } else if (presetSelect.value) {
        lyDoInput.value = presetSelect.value;
        lyDoInput.disabled = true;
    } else {
        lyDoInput.value = '';
        lyDoInput.disabled = false;
    }
    
    // Update counter
    updateBulkLyDoKhoaCounter();
}

function updateBulkLyDoKhoaCounter() {
    const lyDoInput = document.getElementById('bulkLyDoKhoaInput');
    const counter = document.getElementById('bulkLyDoKhoaCounter');
    if (!lyDoInput || !counter) return;
    
    const length = lyDoInput.value.length;
    counter.textContent = `${length}/500`;
    
    if (length > 450) {
        counter.className = 'lyDoKhoaCounter warning';
    } else if (length >= 500) {
        counter.className = 'lyDoKhoaCounter danger';
    } else {
        counter.className = 'lyDoKhoaCounter';
    }
}

function showBulkStatusModal() {
    if (selectedStores.size === 0) {
        showToast('error', 'Lỗi', 'Vui lòng chọn ít nhất 1 cửa hàng để thay đổi trạng thái');
        return;
    }
    
    const bulkStatusCount = document.getElementById('bulkStatusCount');
    const bulkStatusCountBtn = document.getElementById('bulkStatusCountBtn');
    if (bulkStatusCount) bulkStatusCount.textContent = selectedStores.size;
    if (bulkStatusCountBtn) bulkStatusCountBtn.textContent = selectedStores.size;
    
    // Reset form
    const bulkLyDoKhoaSection = document.getElementById('bulkLyDoKhoaSection');
    const bulkLyDoKhoaPreset = document.getElementById('bulkLyDoKhoaPreset');
    const bulkLyDoKhoaInput = document.getElementById('bulkLyDoKhoaInput');
    
    bulkLyDoKhoaPreset.value = '';
    bulkLyDoKhoaInput.value = '';
    bulkLyDoKhoaInput.disabled = false;
    bulkLyDoKhoaSection.style.display = 'none';
    
    // Display preview of stores to be updated
    const previewContainer = document.getElementById('bulkStatusPreview');
    if (previewContainer) {
        previewContainer.innerHTML = '';
        
        selectedStores.forEach(storeId => {
            const storeRow = document.querySelector(`.store-checkbox[value="${storeId}"]`);
            if (storeRow) {
                const row = storeRow.closest('tr');
                const storeNameElement = row.querySelector('td:nth-child(4)');
                const storeStatusElement = row.querySelector('.status-toggle');
                const storeName = storeNameElement ? storeNameElement.textContent : 'Unknown';
                const currentStatus = storeStatusElement ? storeStatusElement.textContent.includes('Hoạt động') : true;
                
                const div = document.createElement('div');
                div.className = 'bulk-preview-item';
                div.innerHTML = `
                    <div class="d-flex justify-content-between align-items-center">
                        <div>
                            <i class="fas fa-store me-2"></i>${storeName}
                        </div>
                        <span class="badge ${currentStatus ? 'bg-success' : 'bg-danger'}">
                            ${currentStatus ? 'Đang hoạt động' : 'Đã khóa'}
                        </span>
                    </div>
                `;
                previewContainer.appendChild(div);
            }
        });
    }
    
    // Thêm event listener cho radio buttons
    document.querySelectorAll('input[name="bulkStatusRadio"]').forEach(radio => {
        radio.addEventListener('change', function() {
            if (this.value === 'false') {
                bulkLyDoKhoaSection.style.display = 'block';
            } else {
                bulkLyDoKhoaSection.style.display = 'none';
            }
        });
    });
    
    // Thêm event listener cho textarea counter
    const bulkLyDoInput = document.getElementById('bulkLyDoKhoaInput');
    if (bulkLyDoInput) {
        bulkLyDoInput.addEventListener('input', updateBulkLyDoKhoaCounter);
    }
    
    const confirmBtn = document.getElementById('confirmBulkStatusBtn');
    if (confirmBtn) {
        confirmBtn.onclick = bulkUpdateStoreStatus;
    }
    
    const bulkStatusModal = new bootstrap.Modal(document.getElementById('bulkStatusModal'));
    bulkStatusModal.show();
}

// Function to handle bulk store status update
function bulkUpdateStoreStatus() {
    const statusRadio = document.querySelector('input[name="bulkStatusRadio"]:checked');
    if (!statusRadio) {
        showToast('error', 'Lỗi', 'Vui lòng chọn trạng thái');
        return;
    }
    
    const newStatus = statusRadio.value;
    let lyDoKhoa = '';
    
    // Nếu là khóa cửa hàng, lấy lý do
    if (newStatus === 'false') {
        lyDoKhoa = document.getElementById('bulkLyDoKhoaInput').value.trim();
        if (!lyDoKhoa) {
            showToast('error', 'Lỗi', 'Vui lòng nhập lý do khóa cửa hàng');
            return;
        }
    }
    
    const storeIds = Array.from(selectedStores);
    
    // Close modal first
    const bulkStatusModal = bootstrap.Modal.getInstance(document.getElementById('bulkStatusModal'));
    if (bulkStatusModal) {
        bulkStatusModal.hide();
    }
    
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';
    
    // Create form data
    const formData = new FormData();
    formData.append('ids', storeIds.join(','));
    formData.append('status', newStatus);
    if (lyDoKhoa) {
        formData.append('lyDoKhoa', lyDoKhoa);
    }
    
    const headers = {};
    if (csrfToken) {
        headers[csrfHeader] = csrfToken;
    }
    
    // Show loading state
    const confirmBtn = document.getElementById('confirmBulkStatusBtn');
    const originalText = confirmBtn.innerHTML;
    confirmBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i>Đang xử lý...';
    confirmBtn.disabled = true;
    
    fetch('/admin/stores/bulk-change-status', {
        method: 'POST',
        headers: headers,
        body: formData
    })
    .then(response => response.json())
    .then(data => {
        // Reset button state
        confirmBtn.innerHTML = originalText;
        confirmBtn.disabled = false;
        
        if (data.success) {
            let successMessage = data.message;
            
            if (data.errorCount > 0 && data.errors && data.errors.length > 0) {
                // Hiển thị lỗi chi tiết
                data.errors.forEach(error => {
                    showToast('error', 'Lỗi cập nhật', error);
                });
                successMessage += ` (${data.errorCount} lỗi)`;
            }
            
            showToast('success', 'Thành công', successMessage);
            
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
        // Reset button state
        confirmBtn.innerHTML = originalText;
        confirmBtn.disabled = false;
        
        console.error('Error:', error);
        showToast('error', 'Lỗi', 'Có lỗi xảy ra khi cập nhật trạng thái hàng loạt');
    });
}

// ============= EXPORT FUNCTIONS (Copy từ users.js, đổi tên biến) =============

function exportSelectedStores() {
    const storeIds = Array.from(selectedStores);
    
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
    
    const form = document.createElement('form');
    form.method = 'POST';
    
    let successMessage = '';
    
    // Nếu không có cửa hàng nào được chọn, export tất cả
    if (storeIds.length === 0) {
        form.action = '/admin/stores/export-all';
        successMessage = 'Đã xuất tất cả cửa hàng thành công. File sẽ tự động tải về.';
    } else {
        form.action = '/admin/stores/export-selected';
        successMessage = `Đã xuất ${storeIds.length} cửa hàng đã chọn thành công. File sẽ tự động tải về.`;
    }
    
    // Add CSRF token
    const csrfInput = document.createElement('input');
    csrfInput.type = 'hidden';
    csrfInput.name = '_csrf';
    csrfInput.value = csrfToken;
    form.appendChild(csrfInput);
    
    // Chỉ thêm store IDs nếu có cửa hàng được chọn
    if (storeIds.length > 0) {
        // Tạo một input duy nhất với danh sách IDs phân cách bằng dấu phẩy
        const idsInput = document.createElement('input');
        idsInput.type = 'hidden';
        idsInput.name = 'storeIds';
        idsInput.value = storeIds.join(',');
        form.appendChild(idsInput);
    }
    
    // Gửi form
    document.body.appendChild(form);
    form.submit();
    document.body.removeChild(form);
    
    // Hiển thị thông báo sau khi submit
    setTimeout(() => {
        showToast('success', 'Thành công', successMessage); 
    }, 1000); 
    
    // Xóa lựa chọn sau khi xuất file
    clearSelection();
}

function showImportExcelModal() {
    window.location.href = '/admin/stores/import';
}
function initBulkLyDoKhoaCounter() {
    const lyDoInput = document.getElementById('bulkLyDoKhoaInput');
    if (!lyDoInput) return;

    // Counter đã được tạo trong HTML, chỉ cần thêm event listener
    lyDoInput.addEventListener('input', updateBulkLyDoKhoaCounter);
}

// ============= EVENT LISTENERS (Cập nhật) =============

document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM loaded - Initializing stores management...');
    
    // Initialize selection controls
    updateSelectionControls();
	
	initLyDoKhoaCounter();
    
    // Add change listeners to all store checkboxes
    const storeCheckboxes = document.querySelectorAll('.store-checkbox');
    storeCheckboxes.forEach(checkbox => {
        checkbox.addEventListener('change', updateSelectionControls);
    });
	
	initBulkLyDoKhoaCounter();
    
    // Reset modal data when hidden for Delete Modal
    const deleteModal = document.getElementById('deleteModal');
    if (deleteModal) {
        deleteModal.addEventListener('hidden.bs.modal', function() {
            currentStoreId = null;
            currentStoreName = null;
            
            // Reset confirm button state
            const confirmBtn = document.getElementById('confirmDeleteBtn');
            if (confirmBtn) {
                confirmBtn.innerHTML = '<i class="fas fa-trash me-1"></i>Xóa';
                confirmBtn.disabled = false;
            }
        });
    }
    
    // Reset modal data when hidden for Status Modal
    const statusModal = document.getElementById('statusModal');
    if (statusModal) {
        statusModal.addEventListener('hidden.bs.modal', function() {
            currentStoreId = null;
            currentStoreName = null;
        });
    }
    
    // Fix modal backdrop issue (Copy từ users.js)
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
    
    // Search form enhancement (chuyển logic từ stores.js cũ sang đây)
    const searchForm = document.querySelector('form[th\\:action="@{/admin/stores}"]');
    if (searchForm) {
        searchForm.addEventListener('submit', function(event) {
            // Kiểm tra keyword nếu cần
            const keywordInput = this.querySelector('input[name="keyword"]');
            const keyword = keywordInput ? keywordInput.value.trim() : '';
            
            if (keyword.length > 0 && keyword.length < 2) {
                event.preventDefault();
                showToast('error', 'Lỗi', 'Vui lòng nhập ít nhất 2 ký tự để tìm kiếm');
                return;
            }
            // Form submit bình thường nếu hợp lệ
        });
    }

    console.log('Stores management system fully initialized');
});