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

// Global variables to store user data for deletion
let currentUserId = null;
let currentUserName = null;

// Function to show delete confirmation modal
function showDeleteModal(button) {
    currentUserId = button.getAttribute('data-user-id');
    currentUserName = button.getAttribute('data-user-name');
    
    document.getElementById('userNameToDelete').textContent = currentUserName;
    
    const deleteModal = new bootstrap.Modal(document.getElementById('deleteModal'));
    deleteModal.show();
}

// Function to handle user deletion
function deleteUser() {
    if (!currentUserId) return;
    
    // Close modal
    const deleteModal = bootstrap.Modal.getInstance(document.getElementById('deleteModal'));
    deleteModal.hide();
    
    // Show loading state
    const confirmBtn = document.getElementById('confirmDeleteBtn');
    const originalText = confirmBtn.innerHTML;
    confirmBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i>Đang xóa...';
    confirmBtn.disabled = true;
    
    fetch(`/admin/users/delete/${currentUserId}`, {
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
        showToast('error', 'Lỗi', error.message || 'Đã có lỗi xảy ra khi xóa người dùng!');
    })
    .finally(() => {
        // Reset current user data
        currentUserId = null;
        currentUserName = null;
    });
}

// Function to initialize status dropdowns
function initStatusDropdowns() {
    const statusToggles = document.querySelectorAll('.status-toggle');
    
    statusToggles.forEach(toggle => {
        toggle.addEventListener('click', function(e) {
            e.stopPropagation();
            const dropdown = this.closest('.status-dropdown');
            const isActive = dropdown.classList.contains('active');
            
            // Close all other dropdowns
            document.querySelectorAll('.status-dropdown').forEach(d => {
                if (d !== dropdown) {
                    d.classList.remove('active');
                }
            });
            
            // Toggle current dropdown
            dropdown.classList.toggle('active', !isActive);
        });
        
        // Add event listeners to options
        const dropdownMenu = toggle.nextElementSibling;
        const options = dropdownMenu.querySelectorAll('.status-option');
        
        options.forEach(option => {
            option.addEventListener('click', function() {
                const newStatus = this.getAttribute('data-status');
                const userId = toggle.getAttribute('data-user-id');
                const currentStatus = toggle.getAttribute('data-current-status');
                
                // Only update if status changed
                if (newStatus !== currentStatus) {
                    updateUserStatus(userId, newStatus, toggle, dropdownMenu);
                }
                
                // Close dropdown
                toggle.closest('.status-dropdown').classList.remove('active');
            });
        });
    });
    
    // Close dropdowns when clicking outside
    document.addEventListener('click', function() {
        document.querySelectorAll('.status-dropdown').forEach(dropdown => {
            dropdown.classList.remove('active');
        });
    });
}

// Function to update user status
function updateUserStatus(userId, newStatus, toggleElement, dropdownMenu) {
    // Show loading state
    const originalText = toggleElement.innerHTML;
    toggleElement.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
    toggleElement.disabled = true;
    
    fetch(`/admin/users/change-status/${userId}?status=${newStatus}`, {
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
	    if (data.success) {
	        showToast('success', 'Thành công', data.message);
	        // Reload page after 1 second
	        setTimeout(() => {
	            window.location.reload();
	        }, 1000);
	    }
	})
    .catch(error => {
        console.error('Error:', error);
        showToast('error', 'Lỗi', error.message || 'Đã có lỗi xảy ra khi thay đổi trạng thái!');
        // Restore original state
        toggleElement.innerHTML = originalText;
        toggleElement.disabled = false;
    });
}

// Function to handle search form submission
function handleSearch(event) {
    event.preventDefault();
    const form = event.target;
    const keyword = form.querySelector('input[name="keyword"]').value.trim();
    
    if (keyword.length > 0 && keyword.length < 2) {
        showToast('error', 'Lỗi', 'Vui lòng nhập ít nhất 2 ký tự để tìm kiếm');
        return;
    }
    
    form.submit();
}

// Utility function to refresh page
function refreshPage() {
    window.location.reload();
}

// Event listeners
document.addEventListener('DOMContentLoaded', function() {
    // Confirm delete button event
    const confirmDeleteBtn = document.getElementById('confirmDeleteBtn');
    if (confirmDeleteBtn) {
        confirmDeleteBtn.addEventListener('click', deleteUser);
    }
    
    // Reset modal data when hidden
    const deleteModal = document.getElementById('deleteModal');
    if (deleteModal) {
        deleteModal.addEventListener('hidden.bs.modal', function() {
            currentUserId = null;
            currentUserName = null;
            
            // Reset confirm button state
            const confirmBtn = document.getElementById('confirmDeleteBtn');
            if (confirmBtn) {
                confirmBtn.innerHTML = '<i class="fas fa-trash me-1"></i>Xóa';
                confirmBtn.disabled = false;
            }
        });
    }
    
    // Initialize status dropdowns
    initStatusDropdowns();
    
    // Search form enhancement
    const searchForm = document.querySelector('form[th\\:action="@{/admin/users}"]');
    if (searchForm) {
        searchForm.addEventListener('submit', handleSearch);
    }
    
    // Add keyboard event to close dropdowns on Escape key
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            document.querySelectorAll('.status-dropdown').forEach(dropdown => {
                dropdown.classList.remove('active');
            });
        }
    });
});

// Prevent dropdown from closing when clicking inside
document.addEventListener('click', function(e) {
    if (e.target.closest('.status-dropdown-menu')) {
        e.stopPropagation();
    }
});