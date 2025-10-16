// ============= ADMIN USERS MANAGEMENT JS =============

// Global variables
let currentUserId = null;
let currentUserName = null;
let currentUserStatus = null;
let selectedUsers = new Set();

// ============= SELECTION MANAGEMENT =============

function toggleSelectAll(checkbox) {
    const userCheckboxes = document.querySelectorAll('.user-checkbox');
    userCheckboxes.forEach(cb => {
        cb.checked = checkbox.checked;
        if (checkbox.checked) {
            selectedUsers.add(cb.value);
        } else {
            selectedUsers.delete(cb.value);
        }
    });
    updateSelectionControls();
}

function updateSelectionControls() {
    const selectedCheckboxes = document.querySelectorAll('.user-checkbox:checked');
    selectedUsers = new Set(Array.from(selectedCheckboxes).map(cb => cb.value));
    
    const selectionControls = document.getElementById('selectionControls');
    const selectedCount = document.getElementById('selectedCount');
    
    if (selectedUsers.size > 0) {
        if (selectionControls) selectionControls.style.display = 'flex';
        if (selectedCount) selectedCount.textContent = selectedUsers.size;
    } else {
        if (selectionControls) selectionControls.style.display = 'none';
    }
    
    // Update select all checkbox state
    const totalCheckboxes = document.querySelectorAll('.user-checkbox').length;
    const selectAllCheckbox = document.getElementById('selectAllCheckbox');
    if (selectAllCheckbox && totalCheckboxes > 0) {
        selectAllCheckbox.checked = selectedUsers.size === totalCheckboxes;
        selectAllCheckbox.indeterminate = selectedUsers.size > 0 && selectedUsers.size < totalCheckboxes;
    }
}

function clearSelection() {
    const checkboxes = document.querySelectorAll('.user-checkbox:checked');
    checkboxes.forEach(cb => cb.checked = false);
    selectedUsers.clear();
    updateSelectionControls();
}

// Hàm thay đổi trạng thái người dùng
function changeUserStatus(userId, status) {
    let lyDoKhoa = '';
    
    // Nếu là khóa tài khoản, hiển thị prompt nhập lý do
    if (status === 'Khóa') {
        lyDoKhoa = prompt('Vui lòng nhập lý do khóa tài khoản:');
        if (lyDoKhoa === null) return; // User cancelled
        if (lyDoKhoa.trim() === '') {
            showToast('error', 'Lỗi', 'Vui lòng nhập lý do khóa tài khoản!');
            return;
        }
    }
    
    // Gọi API thay đổi trạng thái
    fetch(`/admin/users/change-status/${userId}?status=${status}&lyDoKhoa=${encodeURIComponent(lyDoKhoa)}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').getAttribute('content')
        }
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            // Hiển thị toast thành công
            showToast('success', 'Thành công', data.message);
            
            // Cập nhật giao diện
            updateUserStatusUI(userId, data.trangThai, data.lyDoKhoa);
        } else {
            // Hiển thị toast lỗi
            showToast('error', 'Lỗi', data.message);
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('error', 'Lỗi', 'Có lỗi xảy ra khi thay đổi trạng thái!');
    });
}

// Hàm cập nhật giao diện
function updateUserStatusUI(userId, trangThai, lyDoKhoa) {
    const statusBadge = document.querySelector(`#status-badge-${userId}`);
    const statusButton = document.querySelector(`#status-button-${userId}`);
    const reasonCell = document.querySelector(`#reason-cell-${userId}`);
    
    if (statusBadge) {
        statusBadge.className = `badge ${trangThai === 'Hoạt động' ? 'bg-success' : 'bg-danger'}`;
        statusBadge.textContent = trangThai;
    }
    
    if (statusButton) {
        if (trangThai === 'Hoạt động') {
            statusButton.innerHTML = '<i class="fas fa-lock"></i> Khóa';
            statusButton.className = 'btn btn-warning btn-sm';
            statusButton.onclick = () => changeUserStatus(userId, 'Khóa');
        } else {
            statusButton.innerHTML = '<i class="fas fa-unlock"></i> Mở khóa';
            statusButton.className = 'btn btn-success btn-sm';
            statusButton.onclick = () => changeUserStatus(userId, 'Hoạt động');
        }
    }
    
    if (reasonCell && lyDoKhoa !== undefined) {
        reasonCell.textContent = lyDoKhoa || '';
    }
}

// ============= DELETE MODAL FUNCTIONS =============

function showDeleteModal(button) {
    currentUserId = button.getAttribute('data-user-id');
    currentUserName = button.getAttribute('data-user-name');
    
    const userNameElement = document.getElementById('userNameToDelete');
    if (userNameElement) {
        userNameElement.textContent = currentUserName;
    }
    
    const confirmBtn = document.getElementById('confirmDeleteBtn');
    if (confirmBtn) {
        confirmBtn.onclick = function() {
            deleteUser(currentUserId);
        };
    }
    
    const deleteModal = new bootstrap.Modal(document.getElementById('deleteModal'));
    deleteModal.show();
}

function showBulkDeleteModal() {
    if (selectedUsers.size === 0) {
        showToast('error', 'Lỗi', 'Vui lòng chọn ít nhất 1 người dùng để xóa');
        return;
    }
    
    const bulkDeleteCount = document.getElementById('bulkDeleteCount');
    const bulkDeleteCountBtn = document.getElementById('bulkDeleteCountBtn');
    if (bulkDeleteCount) bulkDeleteCount.textContent = selectedUsers.size;
    if (bulkDeleteCountBtn) bulkDeleteCountBtn.textContent = selectedUsers.size;
    
    // Display preview of users to be deleted
    const previewContainer = document.getElementById('bulkDeletePreview');
    if (previewContainer) {
        previewContainer.innerHTML = '';
        
        selectedUsers.forEach(userId => {
            const userRow = document.querySelector(`.user-checkbox[value="${userId}"]`);
            if (userRow) {
                const row = userRow.closest('tr');
                const userNameElement = row.querySelector('td:nth-child(4)');
                const userName = userNameElement ? userNameElement.textContent : 'Unknown';
                
                const div = document.createElement('div');
                div.className = 'bulk-preview-item';
                div.innerHTML = `<i class="fas fa-user me-2"></i>${userName}`;
                previewContainer.appendChild(div);
            }
        });
    }
    
    const confirmBtn = document.getElementById('confirmBulkDeleteBtn');
    if (confirmBtn) {
        confirmBtn.onclick = bulkDeleteUsers;
    }
    
    const bulkDeleteModal = new bootstrap.Modal(document.getElementById('bulkDeleteModal'));
    bulkDeleteModal.show();
}

// ============= STATUS MODAL FUNCTIONS =============

function showStatusModal(button) {
    currentUserId = button.getAttribute('data-user-id');
    currentUserName = button.getAttribute('data-user-name');
    const currentUserStatus = button.getAttribute('data-current-status');
    
    document.getElementById('userNameForStatus').textContent = currentUserName;
    
    // Reset lý do khóa section
    const lyDoKhoaSection = document.getElementById('lyDoKhoaSection');
    const lyDoKhoaPreset = document.getElementById('lyDoKhoaPreset');
    const lyDoKhoaInput = document.getElementById('lyDoKhoaInput');
    
    lyDoKhoaPreset.value = '';
    lyDoKhoaInput.value = '';
    lyDoKhoaInput.disabled = false;
    lyDoKhoaSection.style.display = 'none';
    
    // Set radio button dựa trên trạng thái hiện tại
    const statusActive = document.getElementById('statusActive');
    const statusInactive = document.getElementById('statusInactive');
    
    if (currentUserStatus === 'Hoạt động') {
        statusActive.checked = true;
    } else {
        statusInactive.checked = true;
    }
    
    // Thêm event listener cho radio buttons
    document.querySelectorAll('input[name="statusRadio"]').forEach(radio => {
        radio.addEventListener('change', function() {
            if (this.value === 'Khóa') {
                lyDoKhoaSection.style.display = 'block';
            } else {
                lyDoKhoaSection.style.display = 'none';
            }
        });
    });
    
    const confirmBtn = document.getElementById('confirmStatusBtn');
    if (confirmBtn) {
        confirmBtn.onclick = updateUserStatus;
    }
    
    const statusModal = new bootstrap.Modal(document.getElementById('statusModal'));
    statusModal.show();
}

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
    
    // Update counter
    updateLyDoKhoaCounter();
}

function updateLyDoKhoaCounter() {
    const lyDoInput = document.getElementById('lyDoKhoaInput');
    const counter = document.getElementById('lyDoKhoaCounter');
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
    if (selectedUsers.size === 0) {
        showToast('error', 'Lỗi', 'Vui lòng chọn ít nhất 1 người dùng để thay đổi trạng thái');
        return;
    }
    
    const bulkStatusCount = document.getElementById('bulkStatusCount');
    const bulkStatusCountBtn = document.getElementById('bulkStatusCountBtn');
    if (bulkStatusCount) bulkStatusCount.textContent = selectedUsers.size;
    if (bulkStatusCountBtn) bulkStatusCountBtn.textContent = selectedUsers.size;
    
    // Reset form
    const bulkLyDoKhoaSection = document.getElementById('bulkLyDoKhoaSection');
    const bulkLyDoKhoaPreset = document.getElementById('bulkLyDoKhoaPreset');
    const bulkLyDoKhoaInput = document.getElementById('bulkLyDoKhoaInput');
    
    bulkLyDoKhoaPreset.value = '';
    bulkLyDoKhoaInput.value = '';
    bulkLyDoKhoaInput.disabled = false;
    bulkLyDoKhoaSection.style.display = 'none';
    
    // Display preview of users to be updated
    const previewContainer = document.getElementById('bulkStatusPreview');
    if (previewContainer) {
        previewContainer.innerHTML = '';
        
        selectedUsers.forEach(userId => {
            const userRow = document.querySelector(`.user-checkbox[value="${userId}"]`);
            if (userRow) {
                const row = userRow.closest('tr');
                const userNameElement = row.querySelector('td:nth-child(4)');
                const userStatusElement = row.querySelector('.status-toggle');
                const userName = userNameElement ? userNameElement.textContent : 'Unknown';
                const currentStatus = userStatusElement ? userStatusElement.textContent.includes('Hoạt động') : true;
                
                const div = document.createElement('div');
                div.className = 'bulk-preview-item';
                div.innerHTML = `
                    <div class="d-flex justify-content-between align-items-center">
                        <div>
                            <i class="fas fa-user me-2"></i>${userName}
                        </div>
                        <span class="badge ${currentStatus ? 'bg-success' : 'bg-danger'}">
                            ${currentStatus ? 'Hoạt động' : 'Đã khóa'}
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
            if (this.value === 'Khóa') {
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
        confirmBtn.onclick = bulkUpdateUserStatus;
    }
    
    const bulkStatusModal = new bootstrap.Modal(document.getElementById('bulkStatusModal'));
    bulkStatusModal.show();
}

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

// ============= API CALLS =============

function deleteUser(userId) {
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
    
    fetch(`/admin/users/delete/${userId}`, {
        method: 'DELETE',
        headers: headers
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showToast('success', 'Thành công', data.message || 'Xóa người dùng thành công');
            setTimeout(() => {
                location.reload();
            }, 1000);
        } else {
            showToast('error', 'Lỗi', data.message || 'Có lỗi xảy ra khi xóa người dùng');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('error', 'Lỗi', 'Có lỗi xảy ra khi xóa người dùng');
    });
}

function bulkDeleteUsers() {
    // Close modal first
    const bulkDeleteModal = bootstrap.Modal.getInstance(document.getElementById('bulkDeleteModal'));
    if (bulkDeleteModal) {
        bulkDeleteModal.hide();
    }
    
    const userIds = Array.from(selectedUsers);
    
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';
    
    // Create form data
    const formData = new FormData();
    formData.append('ids', userIds.join(','));
    
    const headers = {};
    if (csrfToken) {
        headers[csrfHeader] = csrfToken;
    }
    
    fetch('/admin/users/bulk-delete', {
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
            showToast('error', 'Lỗi', data.message || 'Có lỗi xảy ra khi xóa người dùng');
            
            if (data.errors && data.errors.length > 0) {
                data.errors.forEach(error => {
                    console.error(error);
                });
            }
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('error', 'Lỗi', 'Có lỗi xảy ra khi xóa nhiều người dùng');
    });
}

function updateUserStatus() {
    const statusRadio = document.querySelector('input[name="statusRadio"]:checked');
    if (!statusRadio) {
        showToast('error', 'Lỗi', 'Vui lòng chọn trạng thái');
        return;
    }
    
    const newStatus = statusRadio.value;
    let lyDoKhoa = '';
    
    // Nếu là khóa tài khoản, lấy lý do
    if (newStatus === 'Khóa') {
        lyDoKhoa = document.getElementById('lyDoKhoaInput').value.trim();
        if (!lyDoKhoa) {
            showToast('error', 'Lỗi', 'Vui lòng nhập lý do khóa tài khoản');
            return;
        }
    }
    
    // Close modal
    const statusModal = bootstrap.Modal.getInstance(document.getElementById('statusModal'));
    if (statusModal) {
        statusModal.hide();
    }
    
    const csrfToken = getCsrfToken();
    const csrfHeader = getCsrfHeader();
    
    const formData = new FormData();
    formData.append('status', newStatus);
    if (lyDoKhoa) {
        formData.append('lyDoKhoa', lyDoKhoa);
    }
    
    const headers = {};
    if (csrfToken) {
        headers[csrfHeader] = csrfToken;
    }
    
    fetch(`/admin/users/change-status/${currentUserId}`, {
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

function bulkUpdateUserStatus() {
    const statusRadio = document.querySelector('input[name="bulkStatusRadio"]:checked');
    if (!statusRadio) {
        showToast('error', 'Lỗi', 'Vui lòng chọn trạng thái');
        return;
    }
    
    const newStatus = statusRadio.value;
    let lyDoKhoa = '';
    
    // Nếu là khóa tài khoản, lấy lý do
    if (newStatus === 'Khóa') {
        lyDoKhoa = document.getElementById('bulkLyDoKhoaInput').value.trim();
        if (!lyDoKhoa) {
            showToast('error', 'Lỗi', 'Vui lòng nhập lý do khóa tài khoản');
            return;
        }
    }
    
    const userIds = Array.from(selectedUsers);
    
    // Close modal first
    const bulkStatusModal = bootstrap.Modal.getInstance(document.getElementById('bulkStatusModal'));
    if (bulkStatusModal) {
        bulkStatusModal.hide();
    }
    
    const csrfToken = getCsrfToken();
    const csrfHeader = getCsrfHeader();
    
    // Create form data
    const formData = new FormData();
    formData.append('ids', userIds.join(','));
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
    
    fetch('/admin/users/bulk-change-status', {
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

// ============= EXPORT FUNCTIONS =============

function exportSelectedUsers() {
    const userIds = Array.from(selectedUsers);
    
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
    
    const form = document.createElement('form');
    form.method = 'POST';
    
    let successMessage = '';
    
    // Nếu không có người dùng nào được chọn, export tất cả
    if (userIds.length === 0) {
        form.action = '/admin/users/export-all';
        successMessage = 'Đã xuất tất cả người dùng thành công. File sẽ tự động tải về.';
    } else {
        form.action = '/admin/users/export-selected';
        successMessage = `Đã xuất ${userIds.length} người dùng thành công. File sẽ tự động tải về.`;
    }
    
    // Add CSRF token
    const csrfInput = document.createElement('input');
    csrfInput.type = 'hidden';
    csrfInput.name = '_csrf';
    csrfInput.value = csrfToken;
    form.appendChild(csrfInput);
    
    // Chỉ thêm user IDs nếu có người dùng được chọn
    if (userIds.length > 0) {
        // Tạo một input duy nhất với danh sách IDs phân cách bằng dấu phẩy
        const idsInput = document.createElement('input');
        idsInput.type = 'hidden';
        idsInput.name = 'userIds';
        idsInput.value = userIds.join(',');
        form.appendChild(idsInput);
    }
    
    // Gửi form
    document.body.appendChild(form);
    form.submit();
    document.body.removeChild(form);
    
    setTimeout(() => {
        showToast('success', 'Thành công', successMessage); 
    }, 500); 
    
    // Xóa lựa chọn sau khi xuất file (tùy chọn)
    clearSelection();
}

// ============= TOAST NOTIFICATIONS =============

function showToast(type, title, message) {
    const toastContainer = document.getElementById('toastContainer');
    if (!toastContainer) {
        console.error('Toast container not found!');
        return;
    }
    
    const toastId = 'toast-' + Date.now();
    
    const iconClass = type === 'success' ? 'fa-check-circle' : 
                     type === 'error' ? 'fa-exclamation-circle' : 
                     type === 'warning' ? 'fa-exclamation-triangle' : 
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
    
    // Auto remove after 5 seconds
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
            if (toast.parentNode) {
                toast.parentNode.removeChild(toast);
            }
        }, 300);
    }
}

// ============= EVENT LISTENERS =============

document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM loaded - Initializing users management...');
    
    // Initialize selection controls
    updateSelectionControls();
    
    // Add change listeners to all user checkboxes
    const userCheckboxes = document.querySelectorAll('.user-checkbox');
    userCheckboxes.forEach(checkbox => {
        checkbox.addEventListener('change', updateSelectionControls);
    });
	
	// Initialize lý do khóa counters
	    initLyDoKhoaCounter();
	    initBulkLyDoKhoaCounter();
    
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
    
    console.log('Users management system fully initialized');
});

// ============= UTILITY FUNCTIONS =============

function getCsrfToken() {
    return document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
}

function getCsrfHeader() {
    return document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';
}

function formatDate(dateString) {
    if (!dateString) return '';
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('vi-VN').format(date);
}

function initLyDoKhoaCounter() {
    const lyDoInput = document.getElementById('lyDoKhoaInput');
    if (lyDoInput) {
        lyDoInput.addEventListener('input', updateLyDoKhoaCounter);
        updateLyDoKhoaCounter(); // Initialize counter
    }
}

function initBulkLyDoKhoaCounter() {
    const lyDoInput = document.getElementById('bulkLyDoKhoaInput');
    if (lyDoInput) {
        lyDoInput.addEventListener('input', updateBulkLyDoKhoaCounter);
        updateBulkLyDoKhoaCounter(); // Initialize counter
    }
}