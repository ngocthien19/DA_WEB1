// Global variables for deletion and status change
let currentCategoryId = null;
let currentCategoryName = null;
let currentCategoryStatus = null;

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
    currentCategoryId = button.getAttribute('data-category-id');
    currentCategoryName = button.getAttribute('data-category-name');
    
    document.getElementById('categoryNameToDelete').textContent = currentCategoryName;
    
    const deleteModal = new bootstrap.Modal(document.getElementById('deleteModal'));
    deleteModal.show();
}

// Function to show status change modal
function showStatusModal(categoryId, categoryName, currentStatus) {
    currentCategoryId = categoryId;
    currentCategoryName = categoryName;
    currentCategoryStatus = currentStatus;
    
    document.getElementById('categoryNameForStatus').textContent = categoryName;
    
    // Pre-select current status
    if (currentStatus) {
        document.getElementById('statusActive').checked = true;
    } else {
        document.getElementById('statusInactive').checked = true;
    }
    
    const statusModal = new bootstrap.Modal(document.getElementById('statusModal'));
    statusModal.show();
}

// Function to delete category
function deleteCategory() {
    if (!currentCategoryId) return;
    
    const deleteModal = bootstrap.Modal.getInstance(document.getElementById('deleteModal'));
    deleteModal.hide();
    
    const confirmBtn = document.getElementById('confirmDeleteBtn');
    const originalText = confirmBtn.innerHTML;
    confirmBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i>Đang xóa...';
    confirmBtn.disabled = true;
    
    fetch(`/admin/categories/delete/${currentCategoryId}`, {
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
        showToast('error', 'Lỗi', error.message || 'Đã có lỗi xảy ra khi xóa danh mục!');
    })
    .finally(() => {
        currentCategoryId = null;
        currentCategoryName = null;
    });
}

// Function to initialize status buttons
function initStatusButtons() {
    const statusToggles = document.querySelectorAll('.status-toggle');
    
    statusToggles.forEach(toggle => {
        toggle.addEventListener('click', function(e) {
            e.stopPropagation();
            
            // Get category info
            const categoryId = this.getAttribute('data-category-id');
            const currentStatus = this.getAttribute('data-current-status') === 'true';
            
            // Find category name from table row
            const row = this.closest('tr');
            const categoryName = row.querySelector('.category-name').textContent;
            
            // Show status modal
            showStatusModal(categoryId, categoryName, currentStatus);
        });
    });
}

// Function to update category status
function updateCategoryStatus() {
    if (!currentCategoryId) return;
    
    const newStatus = document.getElementById('statusActive').checked;
    
    // If status hasn't changed, just close modal
    if (newStatus === currentCategoryStatus) {
        const statusModal = bootstrap.Modal.getInstance(document.getElementById('statusModal'));
        statusModal.hide();
        return;
    }
    
    const statusModal = bootstrap.Modal.getInstance(document.getElementById('statusModal'));
    statusModal.hide();
    
    const confirmBtn = document.getElementById('confirmStatusBtn');
    const originalText = confirmBtn.innerHTML;
    
    // Thêm thông báo về việc sẽ cập nhật sản phẩm
    const statusText = newStatus ? 'Hoạt động' : 'Ngừng hoạt động';
    const productUpdateText = newStatus ? 
        'Các sản phẩm trong danh mục sẽ được kích hoạt.' : 
        'Các sản phẩm trong danh mục sẽ được chuyển sang trạng thái Ngừng bán.';
    
    confirmBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i>Đang cập nhật...';
    confirmBtn.disabled = true;
    
    fetch(`/admin/categories/change-status/${currentCategoryId}?status=${newStatus}`, {
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
            let message = data.message;
            if (!newStatus) {
                message += ' Tất cả sản phẩm trong danh mục đã được chuyển sang trạng thái Ngừng bán.';
            } else {
                message += ' Các sản phẩm trong danh mục đã được kích hoạt.';
            }
            
            showToast('success', 'Thành công', message);
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
        showToast('error', 'Lỗi', error.message || 'Đã có lỗi xảy ra khi thay đổi trạng thái!');
    })
    .finally(() => {
        currentCategoryId = null;
        currentCategoryName = null;
        currentCategoryStatus = null;
    });
}

// Event listeners
document.addEventListener('DOMContentLoaded', function() {
    // Delete confirmation
    const confirmDeleteBtn = document.getElementById('confirmDeleteBtn');
    if (confirmDeleteBtn) {
        confirmDeleteBtn.addEventListener('click', deleteCategory);
    }
    
    // Status confirmation
    const confirmStatusBtn = document.getElementById('confirmStatusBtn');
    if (confirmStatusBtn) {
        confirmStatusBtn.addEventListener('click', updateCategoryStatus);
    }
    
    // Reset delete modal on close
    const deleteModal = document.getElementById('deleteModal');
    if (deleteModal) {
        deleteModal.addEventListener('hidden.bs.modal', function() {
            currentCategoryId = null;
            currentCategoryName = null;
            
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
            currentCategoryId = null;
            currentCategoryName = null;
            currentCategoryStatus = null;
            
            const confirmBtn = document.getElementById('confirmStatusBtn');
            if (confirmBtn) {
                confirmBtn.innerHTML = '<i class="fas fa-save me-1"></i>Lưu thay đổi';
                confirmBtn.disabled = false;
            }
        });
    }
    
    // Initialize status buttons
    initStatusButtons();
});