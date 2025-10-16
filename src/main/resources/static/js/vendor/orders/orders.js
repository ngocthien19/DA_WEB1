// ============= VENDOR ORDERS MANAGEMENT JS =============

// Global variables
let currentOrderId = null;
let currentCustomerName = null;
let selectedOrders = new Set();

// ============= FILTER MANAGEMENT =============

function submitFilterForm() {
    document.getElementById('mainFilterForm').submit();
}

function clearFilter(filterType) {
    const urlParams = new URLSearchParams(window.location.search);
    
    switch (filterType) {
        case 'keyword':
            urlParams.delete('keyword');
            break;
        case 'date':
            urlParams.delete('startDate');
            urlParams.delete('endDate');
            break;
        case 'status':
            urlParams.delete('trangThai');
            break;
        case 'all':
            // Xóa tất cả các tham số filter
            urlParams.delete('keyword');
            urlParams.delete('startDate');
            urlParams.delete('endDate');
            urlParams.delete('trangThai');
            urlParams.set('page', '0'); 
            break;
    }
    
    window.location.href = `${window.location.pathname}?${urlParams.toString()}`;
}

// ============= STATUS MODAL FUNCTIONS =============

function showStatusModal(element) {
    // Lấy orderId và currentStatus từ data attributes của phần tử
    const orderId = element.getAttribute('data-order-id');
    const currentStatus = element.getAttribute('data-current-status');
    
    // KIỂM TRA: Nếu đơn hàng đã hoàn thành hoặc đã hủy thì không cho phép cập nhật
    if (currentStatus === 'Hoàn thành' || currentStatus === 'Hủy') {
        showToast('warning', 'Thông báo', `Đơn hàng đã ${currentStatus.toLowerCase()}, không thể thay đổi trạng thái`);
        return;
    }
    
    const customerNameElement = element.closest('tr').querySelector('.customer-info strong');
    const customerName = customerNameElement ? customerNameElement.textContent : 'Unknown';
    
    currentOrderId = orderId;
    currentCustomerName = customerName;
    
    const customerNameElementModal = document.getElementById('customerNameForStatus');
    if (customerNameElementModal) {
        customerNameElementModal.textContent = customerName;
    }

    // Hiển thị trạng thái hiện tại
    const currentStatusInfo = document.getElementById('currentStatusInfo');
    if (currentStatusInfo) {
        currentStatusInfo.textContent = `Trạng thái hiện tại: ${currentStatus}`;
    }
    
    // Set radio button based on current status và vô hiệu hóa các tùy chọn không hợp lệ
    const statusRadios = document.getElementsByName('statusRadio');
    const validationAlert = document.getElementById('statusValidationAlert');
    const validationMessage = document.getElementById('validationMessage');
    
    // Ẩn cảnh báo ban đầu
    if (validationAlert) {
        validationAlert.style.display = 'none';
    }
    
    statusRadios.forEach(radio => {
        // Reset trạng thái
        radio.disabled = false;
        radio.parentElement.classList.remove('text-muted');
        
        // Vô hiệu hóa các tùy chọn không hợp lệ khi đơn hàng đang giao
        if (currentStatus === 'Đang giao') {
            if (radio.value === 'Chờ xác nhận' || radio.value === 'Đã xác nhận') {
                radio.disabled = true;
                radio.parentElement.classList.add('text-muted');
                
                // Hiển thị cảnh báo
                if (validationAlert && validationMessage) {
                    validationAlert.style.display = 'block';
                    validationMessage.textContent = 'Không thể chuyển đơn hàng đang giao về trạng thái trước đó';
                }
            }
        }
        
        // Set checked state
        if (radio.value === currentStatus) {
            radio.checked = true;
        } else {
            radio.checked = false;
        }
    });
    
    const confirmBtn = document.getElementById('confirmStatusBtn');
    if (confirmBtn) {
        confirmBtn.onclick = updateOrderStatusModal;
    }
    
    const statusModal = new bootstrap.Modal(document.getElementById('statusModal'));
    statusModal.show();
}

function updateOrderStatusModal() {
    const statusRadio = document.querySelector('input[name="statusRadio"]:checked');
    if (!statusRadio) {
        showToast('error', 'Lỗi', 'Vui lòng chọn trạng thái');
        return;
    }
    
    const newStatus = statusRadio.value;
    
    // Thêm input cho lý do hủy nếu trạng thái là "Hủy"
    let lyDoHuy = '';
    if (newStatus === 'Hủy') {
        const lyDoHuyInput = document.getElementById('lyDoHuyInput');
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
    
    const url = `/vendor/orders/update-status/${currentOrderId}`;
    const body = `newStatus=${encodeURIComponent(newStatus)}&lyDoHuy=${encodeURIComponent(lyDoHuy)}`;
    
    fetch(url, {
        method: 'POST',
        headers: headers,
        body: body
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            let successMessage = data.message || 'Cập nhật trạng thái thành công';
            
            // Thêm thông báo về đồng bộ hóa vận chuyển nếu có
            if (data.deliverySynced) {
                successMessage += `. ${data.deliveryMessage || 'Trạng thái vận chuyển đã được tự động đồng bộ'}`;
            }
            
            // Thêm thông báo về cập nhật thanh toán nếu có (cả completed và cancelled)
            if (data.paymentUpdated) {
                successMessage += `. ${data.paymentMessage || 'Trạng thái thanh toán đã được tự động cập nhật'}`;
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

// ============= SELECTION MANAGEMENT =============

function toggleSelectAll(checkbox) {
    const orderCheckboxes = document.querySelectorAll('.order-checkbox');
    orderCheckboxes.forEach(cb => {
        cb.checked = checkbox.checked;
        if (checkbox.checked) {
            selectedOrders.add(cb.value);
        } else {
            selectedOrders.delete(cb.value);
        }
    });
    updateSelectionControls();
}

function updateSelectionControls() {
    const selectedCheckboxes = document.querySelectorAll('.order-checkbox:checked');
    selectedOrders = new Set(Array.from(selectedCheckboxes).map(cb => cb.value));
    
    const selectionControls = document.getElementById('selectionControls');
    const selectedCount = document.getElementById('selectedCount');
    
    // Toggle visibility based on selection size
    if (selectedOrders.size > 0) {
        if (selectionControls) selectionControls.style.display = 'flex';
        if (selectedCount) selectedCount.textContent = selectedOrders.size;
    } else {
        if (selectionControls) selectionControls.style.display = 'none';
    }
    
    // Update select all checkbox state
    const totalCheckboxes = document.querySelectorAll('.order-checkbox').length;
    const selectAllCheckbox = document.getElementById('selectAllCheckbox');
    if (selectAllCheckbox && totalCheckboxes > 0) {
        selectAllCheckbox.checked = selectedOrders.size === totalCheckboxes;
        selectAllCheckbox.indeterminate = selectedOrders.size > 0 && selectedOrders.size < totalCheckboxes;
    }
}

function clearSelection() {
    const checkboxes = document.querySelectorAll('.order-checkbox:checked');
    checkboxes.forEach(cb => cb.checked = false);
    selectedOrders.clear();
    updateSelectionControls();
}

// ============= DELETE MODAL FUNCTIONS =============

function showDeleteModal(button) {
    currentOrderId = button.getAttribute('data-order-id');
    currentCustomerName = button.getAttribute('data-customer-name');
    
    const customerNameElement = document.getElementById('customerNameToDelete');
    if (customerNameElement) {
        customerNameElement.textContent = `đơn hàng #${currentOrderId} của ${currentCustomerName}`;
    }
    
    const confirmBtn = document.getElementById('confirmDeleteBtn');
    if (confirmBtn) {
        confirmBtn.onclick = function() {
            deleteOrder(currentOrderId);
        };
    }
    
    const deleteModal = new bootstrap.Modal(document.getElementById('deleteModal'));
    deleteModal.show();
}
function bulkUpdateOrderStatus() {
    const newStatus = document.getElementById('bulkStatusSelect').value;
    const orderIds = Array.from(selectedOrders);
    
    // Close modal first
    const bulkStatusModal = bootstrap.Modal.getInstance(document.getElementById('bulkStatusModal'));
    if (bulkStatusModal) {
        bulkStatusModal.hide();
    }
    
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';
    
    // Create form data 
    const formData = new FormData();
    formData.append('ids', orderIds.join(','));
    formData.append('newStatus', newStatus);
    
    const headers = {};
    if (csrfToken) {
        headers[csrfHeader] = csrfToken;
    }
    
    fetch('/vendor/orders/bulk-update-status', {
        method: 'POST',
        headers: headers,
        body: formData
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            // Nếu không có đơn hàng nào được cập nhật thành công, chỉ hiển thị lỗi
            if (data.successCount === 0 && data.errorCount > 0) {
                // Hiển thị cảnh báo nếu có lỗi
                if (data.errors && data.errors.length > 0) {
                    data.errors.forEach(error => {
                        showToast('error', 'Lỗi', error);
                    });
                } else {
                    showToast('error', 'Lỗi', 'Không có đơn hàng nào được cập nhật');
                }
            } 
            // Nếu có ít nhất 1 đơn hàng được cập nhật thành công
            else if (data.successCount > 0) {
                let successMessage = data.message;
                
                // Thêm thông báo về cập nhật thanh toán nếu có (completed)
                if (data.paymentUpdatedCount > 0) {
                    successMessage += `. ${data.paymentMessage || 'Đã tự động cập nhật trạng thái thanh toán sang completed cho ' + data.paymentUpdatedCount + ' đơn hàng'}`;
                }
                
                // THÊM: Thông báo về cập nhật thanh toán khi hủy (cancelled)
                if (data.paymentCancelledCount > 0) {
                    successMessage += `. ${data.paymentCancelledMessage || 'Đã tự động cập nhật trạng thái thanh toán sang cancelled cho ' + data.paymentCancelledCount + ' đơn hàng'}`;
                }
                
                showToast('success', 'Thành công', successMessage);
                
                // Hiển thị cảnh báo nếu có lỗi kèm theo
                if (data.errorCount > 0 && data.errors && data.errors.length > 0) {
                    data.errors.forEach(error => {
                        showToast('warning', 'Cảnh báo', error);
                    });
                }
            }
            
            setTimeout(() => {
                location.reload();
            }, 1500);
        } else {
            showToast('error', 'Lỗi', data.message || 'Có lỗi xảy ra khi cập nhật trạng thái hàng loạt');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('error', 'Lỗi', 'Lỗi kết nối khi cập nhật trạng thái hàng loạt');
    });
}

// ============= BULK STATUS MODAL FUNCTIONS =============

function showBulkStatusModal() {
	if (selectedOrders.size === 0) {
	        showToast('error', 'Lỗi', 'Vui lòng chọn ít nhất 1 đơn hàng để cập nhật trạng thái');
	        return;
	    }
	    
	    // Lọc ra các đơn hàng không phải trạng thái "Hoàn thành" hoặc "Hủy"
	    const validOrders = Array.from(selectedOrders).filter(orderId => {
	        const orderRow = document.querySelector(`.order-checkbox[value="${orderId}"]`);
	        if (orderRow) {
	            const statusElement = orderRow.closest('tr').querySelector('.status-display');
	            const currentStatus = statusElement ? statusElement.getAttribute('data-current-status') : '';
	            return currentStatus !== 'Hoàn thành' && currentStatus !== 'Hủy';
	        }
	        return false;
	    });
	    
	    if (validOrders.length === 0) {
	        showToast('warning', 'Thông báo', 'Các đơn hàng đã chọn đều đã hoàn thành hoặc đã hủy, không thể cập nhật trạng thái');
	        return;
	    }
    
    // Cập nhật selectedOrders chỉ bao gồm các đơn hàng hợp lệ
    selectedOrders = new Set(validOrders);
    
    // Đếm số đơn hàng đang giao
    let shippingOrdersCount = 0;
    selectedOrders.forEach(orderId => {
        const orderRow = document.querySelector(`.order-checkbox[value="${orderId}"]`);
        if (orderRow) {
            const statusElement = orderRow.closest('tr').querySelector('.status-display');
            const currentStatus = statusElement ? statusElement.getAttribute('data-current-status') : '';
            if (currentStatus === 'Đang giao') {
                shippingOrdersCount++;
            }
        }
    });
    
    const bulkStatusCount = document.getElementById('bulkStatusCount');
    const bulkStatusCountBtn = document.getElementById('bulkStatusCountBtn');
    if (bulkStatusCount) bulkStatusCount.textContent = selectedOrders.size;
    if (bulkStatusCountBtn) bulkStatusCountBtn.textContent = selectedOrders.size;
    
    // Display preview of orders to be updated
    const previewContainer = document.getElementById('bulkStatusPreview');
    if (previewContainer) {
        previewContainer.innerHTML = '';
        
        // Thêm cảnh báo nếu có đơn hàng đang giao
        if (shippingOrdersCount > 0) {
            const warningDiv = document.createElement('div');
            warningDiv.className = 'alert alert-warning small mb-2';
            warningDiv.innerHTML = `<i class="fas fa-exclamation-triangle me-1"></i> 
                <strong>Lưu ý:</strong> ${shippingOrdersCount} đơn hàng đang giao sẽ không thể chuyển về trạng thái "Chờ xác nhận" hoặc "Đã xác nhận"`;
            previewContainer.appendChild(warningDiv);
        }
        
        selectedOrders.forEach(orderId => {
            const orderRow = document.querySelector(`.order-checkbox[value="${orderId}"]`);
            if (orderRow) {
                const row = orderRow.closest('tr');
                const customerNameElement = row.querySelector('.customer-info strong');
                const customerName = customerNameElement ? customerNameElement.textContent : 'Unknown';
                const statusElement = row.querySelector('.status-display');
                const currentStatus = statusElement ? statusElement.getAttribute('data-current-status') : '';
                
                const div = document.createElement('div');
                div.className = 'preview-item d-flex justify-content-between align-items-center';
                div.innerHTML = `
                    <div>
                        <i class="fas fa-shopping-cart me-2"></i>Mã ĐH #${orderId} - Khách: ${customerName}
                    </div>
                    <span class="badge ${getStatusBadgeClass(currentStatus)}">${currentStatus}</span>
                `;
                previewContainer.appendChild(div);
            }
        });
    }
    
    const confirmBtn = document.getElementById('confirmBulkStatusBtn');
    if (confirmBtn) {
        confirmBtn.onclick = bulkUpdateOrderStatus;
    }
    
    const bulkStatusModal = new bootstrap.Modal(document.getElementById('bulkStatusModal'));
    bulkStatusModal.show();
}

// Hàm hỗ trợ để lấy class badge cho trạng thái
function getStatusBadgeClass(status) {
    switch (status) {
        case 'Chờ xác nhận': return 'bg-warning';
        case 'Đã xác nhận': return 'bg-primary';
        case 'Đang giao': return 'bg-purple';
        case 'Hoàn thành': return 'bg-success';
        case 'Hủy': return 'bg-danger';
        default: return 'bg-secondary';
    }
}

// ============= API CALLS - SINGLE ACTIONS =============

function updateOrderStatusSelect(selectElement) {
    // Chức năng này đã được thay thế bằng showStatusModal/updateOrderStatusModal
    // Giữ lại nếu bạn có nơi nào khác gọi nó.
}

function deleteOrder(orderId) {
    // Close modal first
    const deleteModal = bootstrap.Modal.getInstance(document.getElementById('deleteModal'));
    if (deleteModal) {
        deleteModal.hide();
    }
    
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';
    
    const headers = {};
    if (csrfToken) {
        headers[csrfHeader] = csrfToken;
    }
    
    fetch(`/vendor/orders/delete/${orderId}`, {
        method: 'DELETE',
        headers: headers
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showToast('success', 'Thành công', data.message || 'Xóa đơn hàng thành công');
            setTimeout(() => {
                location.reload();
            }, 1000);
        } else {
            showToast('error', 'Lỗi', data.message || 'Có lỗi xảy ra khi xóa đơn hàng');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('error', 'Lỗi', 'Có lỗi xảy ra khi xóa đơn hàng');
    });
}

// ============= API CALLS - BULK ACTIONS =============

function bulkDeleteOrders() {
    // Close modal first
    const bulkDeleteModal = bootstrap.Modal.getInstance(document.getElementById('bulkDeleteModal'));
    if (bulkDeleteModal) {
        bulkDeleteModal.hide();
    }
    
    const orderIds = Array.from(selectedOrders);
    
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';
    
    // Create form data 
    const formData = new FormData();
    formData.append('ids', orderIds.join(','));
    
    const headers = {};
    if (csrfToken) {
        headers[csrfHeader] = csrfToken;
    }
    
    fetch('/vendor/orders/bulk-delete', {
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
            showToast('error', 'Lỗi', data.message || 'Có lỗi xảy ra khi xóa đơn hàng');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('error', 'Lỗi', 'Có lỗi xảy ra khi xóa nhiều đơn hàng');
    });
}

// ============= EXPORT FUNCTIONS =============

function exportSelectedToExcel() {
    const orderIds = Array.from(selectedOrders);
    
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
    
    const form = document.createElement('form');
    form.method = 'POST';
    
    // Nếu không có đơn hàng nào được chọn, export tất cả
    if (orderIds.length === 0) {
        form.action = '/vendor/orders/export-all';
        showToast('info', 'Thông báo', 'Đang xuất tất cả đơn hàng...');
    } else {
        form.action = '/vendor/orders/export-selected';
        showToast('info', 'Thông báo', `Đang xuất ${orderIds.length} đơn hàng được chọn...`);
    }
    
    // Add CSRF token
    const csrfInput = document.createElement('input');
    csrfInput.type = 'hidden';
    csrfInput.name = '_csrf';
    csrfInput.value = csrfToken;
    form.appendChild(csrfInput);
    
    // Chỉ thêm order IDs nếu có đơn hàng được chọn
    if (orderIds.length > 0) {
        const idsInput = document.createElement('input');
        idsInput.type = 'hidden';
        idsInput.name = 'orderIds';
        idsInput.value = orderIds.join(',');
        form.appendChild(idsInput);
    }
    
    document.body.appendChild(form);
    form.submit();
    document.body.removeChild(form);
}

// ============= TOAST NOTIFICATIONS (Giữ nguyên) =============
function showToast(type, title, message) {
    const toastContainer = document.getElementById('toastContainer');
    if (!toastContainer) return;
    
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

// ============= UTILITY FUNCTIONS (Giữ nguyên) =============

function isValidDate(dateString) {
    if (!dateString) return false;
    const date = new Date(dateString);
    return date instanceof Date && !isNaN(date);
}

// ============= INITIALIZATION =============

function initializeFilters() {
    // Gán sự kiện submit form cho nút áp dụng (applyFilterBtn)
    const applyFilterBtn = document.getElementById('applyFilterBtn');
    if (applyFilterBtn) {
        applyFilterBtn.addEventListener('click', function(e) {
            e.preventDefault();
            document.getElementById('mainFilterForm').submit();
        });
    }

    // Gán sự kiện submit form cho select trạng thái (để người dùng có thể lọc nhanh)
    const statusSelect = document.getElementById('statusFilterSelect');
    const mainFilterForm = document.getElementById('mainFilterForm');

    if (statusSelect && mainFilterForm) {
        statusSelect.addEventListener('change', function() {
            mainFilterForm.submit();
        });
    }

    // Gán sự kiện cho form search riêng (đã có trong HTML)
}

// Thêm vào orders.js
function initializeLyDoHuy() {
    const lyDoHuyContainer = document.getElementById('lyDoHuyContainer');
    const lyDoHuyInput = document.getElementById('lyDoHuyInput');
    const lyDoHuyCounter = document.getElementById('lyDoHuyCounter');
    const statusRadios = document.querySelectorAll('input[name="statusRadio"]');
    
    if (!lyDoHuyInput || !lyDoHuyCounter) return;
    
    // Xử lý thay đổi trạng thái radio
    statusRadios.forEach(radio => {
        radio.addEventListener('change', function() {
            if (this.value === 'Hủy') {
                lyDoHuyContainer.style.display = 'block';
                // Thêm hiệu ứng
                lyDoHuyContainer.classList.add('show');
                lyDoHuyInput.focus();
            } else {
                lyDoHuyContainer.style.display = 'none';
                lyDoHuyContainer.classList.remove('show');
                lyDoHuyInput.value = '';
                updateCharCounter(0);
            }
        });
    });
    
    // Xử lý input character counter
    lyDoHuyInput.addEventListener('input', function() {
        const charCount = this.value.length;
        updateCharCounter(charCount);
    });
    
    // Xử lý focus/blur cho textarea
    lyDoHuyInput.addEventListener('focus', function() {
        this.parentElement.classList.add('focused');
    });
    
    lyDoHuyInput.addEventListener('blur', function() {
        this.parentElement.classList.remove('focused');
    });
    
    function updateCharCounter(count) {
        lyDoHuyCounter.textContent = `${count}/500`;
        
        // Thay đổi màu sắc dựa trên số ký tự
        if (count > 450) {
            lyDoHuyCounter.className = 'char-counter danger';
        } else if (count > 400) {
            lyDoHuyCounter.className = 'char-counter warning';
        } else {
            lyDoHuyCounter.className = 'char-counter';
        }
    }
}

// ============= EVENT LISTENERS =============

document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM loaded - Initializing orders management...');
    
    // Initialize filters (chủ yếu là gán event cho select trạng thái)
    initializeFilters();
    
    // Initialize selection controls
    updateSelectionControls();
	
	initializeLyDoHuy();
	
	const statusRadios = document.querySelectorAll('input[name="statusRadio"]');
	    const lyDoHuyContainer = document.getElementById('lyDoHuyContainer');
	    
	    statusRadios.forEach(radio => {
	        radio.addEventListener('change', function() {
	            if (this.value === 'Hủy') {
	                lyDoHuyContainer.style.display = 'block';
	            } else {
	                lyDoHuyContainer.style.display = 'none';
	                const lyDoHuyInput = document.getElementById('lyDoHuyInput');
	                if (lyDoHuyInput) {
	                    lyDoHuyInput.value = '';
	                }
	            }
	        });
	    });
	
	// Initialize tooltips
	    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
	    const tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
	        return new bootstrap.Tooltip(tooltipTriggerEl);
	    });
    
    // Add change listeners to all order checkboxes
    const orderCheckboxes = document.querySelectorAll('.order-checkbox');
    orderCheckboxes.forEach(checkbox => {
        checkbox.addEventListener('change', updateSelectionControls);
    });
    
    // Status display buttons
    const statusDisplays = document.querySelectorAll('.status-display');
    statusDisplays.forEach(display => {
        display.addEventListener('click', function() {
            showStatusModal(this);
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

    // Gán hàm đóng toast vào window để các nút close trong HTML gọi được
    window.closeToast = closeToast;
    
    // Export functions to global scope
    window.showStatusModal = showStatusModal;
    window.clearFilter = clearFilter;
    window.toggleSelectAll = toggleSelectAll;
    window.updateSelectionControls = updateSelectionControls;
    window.clearSelection = clearSelection;
    window.showDeleteModal = showDeleteModal;
    window.showBulkDeleteModal = showBulkDeleteModal;
    window.showBulkStatusModal = showBulkStatusModal;
    window.exportSelectedToExcel = exportSelectedToExcel;
    
    console.log('Orders management system fully initialized');
});