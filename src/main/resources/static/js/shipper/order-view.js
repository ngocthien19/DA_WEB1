// Khai báo biến toàn cục
let currentDeliveryId, currentOrderId;

// Hàm khởi tạo - lấy dữ liệu từ hidden inputs khi DOM ready
function initializeOrderData() {
    const deliveryInput = document.getElementById('deliveryId');
    const orderInput = document.getElementById('orderId');
    
    currentDeliveryId = deliveryInput ? deliveryInput.value : null;
    currentOrderId = orderInput ? orderInput.value : null;
    
    console.log('Delivery ID:', currentDeliveryId);
    console.log('Order ID:', currentOrderId);
    
    // Kiểm tra nếu không lấy được dữ liệu
    if (!currentDeliveryId || !currentOrderId) {
        console.warn('Không thể lấy dữ liệu đơn hàng từ hidden inputs');
    }
}

// Hàm lấy thông tin đơn hàng từ DOM - ĐƠN GIẢN HÓA
function getOrderInfo() {
    // Lấy thông tin trực tiếp từ các phần tử hiển thị
    const addressElement = document.querySelector('.info-row:nth-child(3) .info-value');
    const phoneElement = document.querySelector('.info-row:nth-child(4) .info-value');
    const noteElement = document.querySelector('.note-text');
    
    const address = addressElement ? addressElement.textContent.trim() : 'Chưa có địa chỉ';
    const phone = phoneElement ? phoneElement.textContent.trim() : 'Chưa có SĐT';
    const note = noteElement ? noteElement.textContent.trim() : 'Không có ghi chú';
    
    console.log('Order Info:', { address, phone, note });
    
    return { address, phone, note };
}

// Hàm mở modal với thông tin đầy đủ
function showConfirmModal() {
    // Đảm bảo dữ liệu đã được khởi tạo
    if (!currentDeliveryId || !currentOrderId) {
        initializeOrderData();
    }
    
    const orderInfo = getOrderInfo();
    
    // Cập nhật thông tin trong modal
    const confirmOrderId = document.getElementById('confirmOrderId');
    if (confirmOrderId) {
        confirmOrderId.innerHTML = '#' + currentOrderId;
    }
    
    const confirmOrderAddress = document.getElementById('confirmOrderAddress');
    if (confirmOrderAddress) {
        confirmOrderAddress.textContent = orderInfo.address;
    }
    
    const confirmOrderPhone = document.getElementById('confirmOrderPhone');
    if (confirmOrderPhone) {
        confirmOrderPhone.textContent = orderInfo.phone;
    }
    
    const confirmOrderNote = document.getElementById('confirmOrderNote');
    if (confirmOrderNote) {
        confirmOrderNote.textContent = orderInfo.note;
    }

    openModal('confirmModal');
}

function showCompleteModal() {
    if (!currentDeliveryId || !currentOrderId) {
        initializeOrderData();
    }
    
    const orderInfo = getOrderInfo();
    
    // Cập nhật thông tin trong modal
    const completeOrderId = document.getElementById('completeOrderId');
    if (completeOrderId) {
        completeOrderId.innerHTML = '#' + currentOrderId;
    }
    
    const completeOrderAddress = document.getElementById('completeOrderAddress');
    if (completeOrderAddress) {
        completeOrderAddress.textContent = orderInfo.address;
    }
    
    const completeOrderPhone = document.getElementById('completeOrderPhone');
    if (completeOrderPhone) {
        completeOrderPhone.textContent = orderInfo.phone;
    }

    openModal('completeModal');
}

function showCancelModal() {
    if (!currentDeliveryId || !currentOrderId) {
        initializeOrderData();
    }
    
    const orderInfo = getOrderInfo();
    
    // Cập nhật thông tin trong modal
    const cancelOrderId = document.getElementById('cancelOrderId');
    if (cancelOrderId) {
        cancelOrderId.innerHTML = '#' + currentOrderId;
    }
    
    const cancelOrderAddress = document.getElementById('cancelOrderAddress');
    if (cancelOrderAddress) {
        cancelOrderAddress.textContent = orderInfo.address;
    }
    
    const cancelOrderPhone = document.getElementById('cancelOrderPhone');
    if (cancelOrderPhone) {
        cancelOrderPhone.textContent = orderInfo.phone;
    }

    openModal('cancelModal');
}

// Hàm xử lý xác nhận đơn hàng
function processConfirmDelivery() {
    closeModal('confirmModal');
    showLoading();

    // Kiểm tra lại dữ liệu trước khi gửi
    if (!currentDeliveryId) {
        hideLoading();
        showNotification('error', 'Lỗi: Không tìm thấy thông tin đơn hàng');
        return;
    }

    fetch('/shipper/confirm-delivery', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: 'deliveryId=' + currentDeliveryId
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.json();
    })
    .then(data => {
        hideLoading();
        if (data.success) {
            showNotification('success', data.message);
            setTimeout(() => {
                window.location.href = '/shipper/dashboard';
            }, 1500);
        } else {
            showNotification('error', data.message);
        }
    })
    .catch(error => {
        hideLoading();
        showNotification('error', 'Lỗi hệ thống: ' + error.message);
        console.error('Error:', error);
    });
}

// Hàm xử lý hoàn thành đơn hàng
function processCompleteDelivery() {
    closeModal('completeModal');
    showLoading();

    if (!currentDeliveryId) {
        hideLoading();
        showNotification('error', 'Lỗi: Không tìm thấy thông tin đơn hàng');
        return;
    }

    fetch('/shipper/complete-delivery', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: 'deliveryId=' + currentDeliveryId
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.json();
    })
    .then(data => {
        hideLoading();
        if (data.success) {
            showNotification('success', data.message);
            setTimeout(() => {
                window.location.href = '/shipper/dashboard';
            }, 1500);
        } else {
            showNotification('error', data.message);
        }
    })
    .catch(error => {
        hideLoading();
        showNotification('error', 'Lỗi hệ thống: ' + error.message);
        console.error('Error:', error);
    });
}

// Hàm xử lý hủy đơn hàng
function processCancelDelivery() {
    closeModal('cancelModal');
    showLoading();

    if (!currentDeliveryId) {
        hideLoading();
        showNotification('error', 'Lỗi: Không tìm thấy thông tin đơn hàng');
        return;
    }

    fetch('/shipper/cancel-delivery', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: 'deliveryId=' + currentDeliveryId
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.json();
    })
    .then(data => {
        hideLoading();
        if (data.success) {
            showNotification('success', data.message);
            setTimeout(() => {
                window.location.href = '/shipper/dashboard';
            }, 1500);
        } else {
            showNotification('error', data.message);
        }
    })
    .catch(error => {
        hideLoading();
        showNotification('error', 'Lỗi hệ thống: ' + error.message);
        console.error('Error:', error);
    });
}

// Khởi tạo event listeners khi trang load
document.addEventListener('DOMContentLoaded', function() {
    console.log('Order View JS loaded');
    
    // Khởi tạo dữ liệu khi DOM ready
    initializeOrderData();
    
    // Debug: kiểm tra xem các nút có được gán event không
    const confirmBtn = document.querySelector('.btn-confirm');
    const completeBtn = document.querySelector('.btn-complete');
    const cancelBtn = document.querySelector('.btn-cancel');
    
    console.log('Confirm button:', confirmBtn);
    console.log('Complete button:', completeBtn);
    console.log('Cancel button:', cancelBtn);
    
    // Đóng modal khi click bên ngoài
    window.addEventListener('click', function(event) {
        const modals = document.querySelectorAll('.modal');
        modals.forEach(modal => {
            if (event.target === modal) {
                closeModal(modal.id);
            }
        });
    });

    // Đóng modal bằng phím ESC
    document.addEventListener('keydown', function(event) {
        if (event.key === 'Escape') {
            const openModal = document.querySelector('.modal[style*="display: block"]');
            if (openModal) {
                closeModal(openModal.id);
            }
        }
    });
});

// Export functions to global scope
window.showConfirmModal = showConfirmModal;
window.showCompleteModal = showCompleteModal;
window.showCancelModal = showCancelModal;
window.closeModal = closeModal;
window.processConfirmDelivery = processConfirmDelivery;
window.processCompleteDelivery = processCompleteDelivery;
window.processCancelDelivery = processCancelDelivery;