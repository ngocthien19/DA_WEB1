// ============= VENDOR DELIVERY MANAGEMENT JS =============

let currentOrderId = null;
let currentDeliveryId = null;

// Tab Management
function switchTab(tabName) {
    const url = new URL(window.location.href);
    url.searchParams.set('tab', tabName);
    url.searchParams.set('page', '0'); // Reset to first page when switching tabs
    window.location.href = url.toString();
}

// Assign Shipper Modal
function showAssignShipperModal(button) {
    currentOrderId = button.getAttribute('data-order-id');
    
    const orderIdElement = document.getElementById('orderIdToAssign');
    if (orderIdElement) {
        orderIdElement.textContent = `#${currentOrderId}`;
    }
    
    // Reset form
    const shipperSelect = document.getElementById('shipperSelect');
    const shipperRadios = document.querySelectorAll('input[name="shipperRadio"]');
    
    if (shipperSelect) shipperSelect.value = '';
    shipperRadios.forEach(radio => radio.checked = false);
    
    const confirmBtn = document.getElementById('confirmAssignBtn');
    if (confirmBtn) {
        confirmBtn.onclick = assignShipperToOrder;
    }
    
    const assignModal = new bootstrap.Modal(document.getElementById('assignShipperModal'));
    assignModal.show();
}

// Update Status Modal với lý do hủy
function showUpdateStatusModal(button) {
    currentDeliveryId = button.getAttribute('data-delivery-id');
    const orderId = button.getAttribute('data-order-id');
    
    const orderIdElement = document.getElementById('orderIdForStatus');
    if (orderIdElement) {
        orderIdElement.textContent = `#${orderId}`;
    }
    
    // Reset form và ẩn lý do hủy
    const lyDoHuyContainer = document.getElementById('lyDoHuyDeliveryContainer');
    const lyDoHuyInput = document.getElementById('lyDoHuyDeliveryInput');
    const lyDoHuyCounter = document.getElementById('lyDoHuyDeliveryCounter');
    
    if (lyDoHuyContainer) {
        lyDoHuyContainer.style.display = 'none';
        lyDoHuyContainer.classList.remove('show');
    }
    if (lyDoHuyInput) {
        lyDoHuyInput.value = '';
    }
    if (lyDoHuyCounter) {
        lyDoHuyCounter.textContent = '0/500';
        lyDoHuyCounter.className = 'char-counter text-muted';
    }
    
    // Reset radio buttons
    const statusShipping = document.getElementById('statusShipping');
    if (statusShipping) {
        statusShipping.checked = true;
    }
    
    const confirmBtn = document.getElementById('confirmStatusBtn');
    if (confirmBtn) {
        confirmBtn.onclick = updateDeliveryStatus;
    }
    
    const statusModal = new bootstrap.Modal(document.getElementById('updateStatusModal'));
    statusModal.show();
}


// Unassign Shipper Modal
function showUnassignModal(button) {
    currentDeliveryId = button.getAttribute('data-delivery-id');
    
    const orderIdElement = document.getElementById('orderIdToUnassign');
    if (orderIdElement) {
        orderIdElement.textContent = `#${currentDeliveryId}`;
    }
    
    const confirmBtn = document.getElementById('confirmUnassignBtn');
    if (confirmBtn) {
        confirmBtn.onclick = unassignShipper;
    }
    
    const unassignModal = new bootstrap.Modal(document.getElementById('unassignModal'));
    unassignModal.show();
}

// Get CSRF Token (Sửa cách lấy CSRF token)
function getCsrfToken() {
    const csrfTokenElement = document.getElementById('csrfToken');
    const csrfHeaderElement = document.getElementById('csrfHeader');
    
    return {
        token: csrfTokenElement ? csrfTokenElement.value : '',
        headerName: csrfHeaderElement ? csrfHeaderElement.value : 'X-CSRF-TOKEN'
    };
}

// API Calls
function assignShipperToOrder() {
    const shipperSelect = document.getElementById('shipperSelect');
    const shipperRadios = document.querySelector('input[name="shipperRadio"]:checked');
    
    let shipperId = null;
    if (shipperSelect && shipperSelect.value) {
        shipperId = shipperSelect.value;
    } else if (shipperRadios) {
        shipperId = shipperRadios.value;
    }
    
    if (!shipperId) {
        showToast('error', 'Lỗi', 'Vui lòng chọn shipper');
        return;
    }
    
    // Close modal first
    const assignModal = bootstrap.Modal.getInstance(document.getElementById('assignShipperModal'));
    if (assignModal) {
        assignModal.hide();
    }
    
    const csrf = getCsrfToken();
    
    const headers = {
        'Content-Type': 'application/x-www-form-urlencoded'
    };
    if (csrf.token) {
        headers[csrf.headerName] = csrf.token;
    }
    
    const url = `/vendor/delivery/assign-shipper`;
    const body = `orderId=${encodeURIComponent(currentOrderId)}&shipperId=${encodeURIComponent(shipperId)}`;
    
    showToast('info', 'Đang xử lý', 'Đang gán shipper...');
    
    fetch(url, {
        method: 'POST',
        headers: headers,
        body: body
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.json();
    })
    .then(data => {
        if (data.success) {
            showToast('success', 'Thành công', data.message);
            setTimeout(() => {
                location.reload();
            }, 1500);
        } else {
            showToast('error', 'Lỗi', data.message || 'Có lỗi xảy ra khi gán shipper');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('error', 'Lỗi', 'Lỗi kết nối khi gán shipper');
    });
}

// Hàm cập nhật trạng thái với lý do hủy
function updateDeliveryStatus() {
    const statusRadio = document.querySelector('input[name="statusRadio"]:checked');
    if (!statusRadio) {
        showToast('error', 'Lỗi', 'Vui lòng chọn trạng thái');
        return;
    }
    
    const newStatus = statusRadio.value;
    let lyDoHuy = '';
    
    // Kiểm tra lý do hủy nếu chọn trạng thái Hủy
    if (newStatus === 'Hủy') {
        const lyDoHuyInput = document.getElementById('lyDoHuyDeliveryInput');
        if (lyDoHuyInput) {
            lyDoHuy = lyDoHuyInput.value.trim();
            if (!lyDoHuy) {
                showToast('error', 'Lỗi', 'Vui lòng nhập lý do hủy đơn hàng');
                lyDoHuyInput.focus();
                return;
            }
        }
    }
    
    // Close modal first
    const statusModal = bootstrap.Modal.getInstance(document.getElementById('updateStatusModal'));
    if (statusModal) {
        statusModal.hide();
    }
    
    const csrf = getCsrfToken();
    
    const headers = {
        'Content-Type': 'application/x-www-form-urlencoded'
    };
    if (csrf.token) {
        headers[csrf.headerName] = csrf.token;
    }
    
    const url = `/vendor/delivery/update-status`;
    const body = `deliveryId=${encodeURIComponent(currentDeliveryId)}&newStatus=${encodeURIComponent(newStatus)}&lyDoHuy=${encodeURIComponent(lyDoHuy)}`;
    
    showToast('info', 'Đang xử lý', 'Đang cập nhật trạng thái...');
    
    fetch(url, {
        method: 'POST',
        headers: headers,
        body: body
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.json();
    })
    .then(data => {
        if (data.success) {
            let successMessage = data.message;
            
            // THÊM: Hiển thị thông báo đặc biệt nếu đơn hàng sẽ biến mất
            if (data.willDisappear) {
                successMessage += `. ${data.disappearMessage || 'Đơn hàng sẽ không hiển thị trong danh sách vận chuyển nữa'}`;
            }
            
            showToast('success', 'Thành công', successMessage);
            setTimeout(() => {
                location.reload();
            }, 1500);
        } else {
            showToast('error', 'Lỗi', data.message || 'Có lỗi xảy ra khi cập nhật trạng thái');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('error', 'Lỗi', 'Lỗi kết nối khi cập nhật trạng thái');
    });
}

// Hàm khởi tạo xử lý lý do hủy
function initializeLyDoHuyDelivery() {
    const statusRadios = document.querySelectorAll('input[name="statusRadio"]');
    const lyDoHuyContainer = document.getElementById('lyDoHuyDeliveryContainer');
    const lyDoHuyInput = document.getElementById('lyDoHuyDeliveryInput');
    const lyDoHuyCounter = document.getElementById('lyDoHuyDeliveryCounter');
    
    if (!lyDoHuyInput || !lyDoHuyCounter) return;
    
    // Xử lý thay đổi radio button
    statusRadios.forEach(radio => {
        radio.addEventListener('change', function() {
            if (this.value === 'Hủy') {
                lyDoHuyContainer.style.display = 'block';
                setTimeout(() => {
                    lyDoHuyContainer.classList.add('show');
                }, 10);
                lyDoHuyInput.focus();
            } else {
                lyDoHuyContainer.style.display = 'none';
                lyDoHuyContainer.classList.remove('show');
                lyDoHuyInput.value = '';
                updateCharCounter(0, lyDoHuyCounter);
            }
        });
    });
    
    // Xử lý input character counter
    lyDoHuyInput.addEventListener('input', function() {
        const charCount = this.value.length;
        updateCharCounter(charCount, lyDoHuyCounter);
    });
    
    function updateCharCounter(count, counterElement) {
        counterElement.textContent = `${count}/500`;
        
        if (count > 450) {
            counterElement.className = 'char-counter danger';
        } else if (count > 400) {
            counterElement.className = 'char-counter warning';
        } else {
            counterElement.className = 'char-counter text-muted';
        }
    }
}


function unassignShipper() {
    // Close modal first
    const unassignModal = bootstrap.Modal.getInstance(document.getElementById('unassignModal'));
    if (unassignModal) {
        unassignModal.hide();
    }
    
    const csrf = getCsrfToken();
    
    const headers = {
        'Content-Type': 'application/x-www-form-urlencoded'
    };
    if (csrf.token) {
        headers[csrf.headerName] = csrf.token;
    }
    
    const url = `/vendor/delivery/unassign-shipper`;
    const body = `deliveryId=${encodeURIComponent(currentDeliveryId)}`;
    
    showToast('info', 'Đang xử lý', 'Đang hủy gán shipper...');
    
    fetch(url, {
        method: 'POST',
        headers: headers,
        body: body
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.json();
    })
    .then(data => {
        if (data.success) {
            showToast('success', 'Thành công', data.message);
            setTimeout(() => {
                location.reload();
            }, 1500);
        } else {
            showToast('error', 'Lỗi', data.message || 'Có lỗi xảy ra khi hủy gán shipper');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('error', 'Lỗi', 'Lỗi kết nối khi hủy gán shipper');
    });
}

// Toast Notifications
function showToast(type, title, message) {
    const toastContainer = document.getElementById('toastContainer');
    if (!toastContainer) return;
    
    const toastId = 'toast-' + Date.now();
    
    const iconClass = type === 'success' ? 'fa-check-circle' : 
                     type === 'error' ? 'fa-exclamation-circle' : 
                     type === 'warning' ? 'fa-exclamation-triangle' :
                     type === 'info' ? 'fa-info-circle' : 'fa-bell';
    
    const toastHTML = `
        <div id="${toastId}" class="toast" role="alert" aria-live="assertive" aria-atomic="true">
            <div class="toast-header">
                <i class="fas ${iconClass} me-2 text-${type}"></i>
                <strong class="me-auto">${title}</strong>
                <small class="text-muted">just now</small>
                <button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
            <div class="toast-body">
                ${message}
            </div>
        </div>
    `;
    
    toastContainer.insertAdjacentHTML('beforeend', toastHTML);
    
    const toastElement = document.getElementById(toastId);
    const toast = new bootstrap.Toast(toastElement, {
        autohide: true,
        delay: 5000
    });
    toast.show();
    
    // Auto remove after hide
    toastElement.addEventListener('hidden.bs.toast', () => {
        toastElement.remove();
    });
}

// Initialize
document.addEventListener('DOMContentLoaded', function() {
    console.log('Delivery management system initialized');
    
    // Sync radio buttons với select
    const shipperSelect = document.getElementById('shipperSelect');
    const shipperRadios = document.querySelectorAll('input[name="shipperRadio"]');
    
    if (shipperSelect) {
        shipperSelect.addEventListener('change', function() {
            shipperRadios.forEach(radio => {
                radio.checked = radio.value === this.value;
            });
        });
    }
    
    shipperRadios.forEach(radio => {
        radio.addEventListener('change', function() {
            if (this.checked && shipperSelect) {
                shipperSelect.value = this.value;
            }
        });
    });
    
    // Khởi tạo xử lý lý do hủy
    initializeLyDoHuyDelivery();
    
    // Export functions to global scope
    window.showAssignShipperModal = showAssignShipperModal;
    window.showUpdateStatusModal = showUpdateStatusModal;
    window.showUnassignModal = showUnassignModal;
    window.switchTab = switchTab;
    
    console.log('Delivery system ready.');
});