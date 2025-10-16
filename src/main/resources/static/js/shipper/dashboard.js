// Shipper Dashboard JavaScript
let currentDeliveryId = null;
let currentOrderId = null;
let selectedCancelReason = '';
let currentTab = 'pending'; // Thêm biến để lưu tab hiện tại

// Các hằng số phân loại lý do
const REASON_CATEGORIES = {
    SHIPPER: 'shipper',      // Chuyển về trạng thái cũ
    SYSTEM: 'system'         // Hủy thật sự
};

const CANCEL_REASONS = {
    // Lý do thuộc về Shipper - Chuyển về "Đã xác nhận"
    SHIPPER_UNAVAILABLE: "Tôi không thể giao hàng vào thời gian này",
    VEHICLE_ISSUE: "Xe hỏng/Phương tiện có vấn đề",
    HEALTH_ISSUE: "Sức khỏe không tốt",
    PERSONAL_REASON: "Lý do cá nhân khác",
    
    // Lý do thuộc về hệ thống - Hủy thật sự
    CUSTOMER_NO_ANSWER: "Khách hàng không nghe máy",
    CUSTOMER_REFUSED: "Khách hàng từ chối nhận hàng",
    WRONG_ADDRESS: "Địa chỉ giao hàng không chính xác",
    CUSTOMER_REQUEST: "Khách hàng yêu cầu hủy đơn",
    DAMAGED_GOODS: "Hàng hóa bị hư hỏng",
    BAD_WEATHER: "Thời tiết xấu không thể giao"
};

document.addEventListener('DOMContentLoaded', function() {
    console.log('Shipper Dashboard loaded');
    initializeEventListeners();
    initLogoutHandler();
    detectCurrentTab(); // Thêm hàm phát hiện tab hiện tại
	checkAccountStatus();
	    
	    // Đóng modal khi click bên ngoài
	    window.addEventListener('click', function(event) {
	        const modal = document.getElementById('accountLockedModal');
	        if (event.target === modal) {
	            closeAccountLockedModal();
	        }
	    });
	    
	    // Đóng modal bằng phím ESC
	    document.addEventListener('keydown', function(event) {
	        if (event.key === 'Escape') {
	            const modal = document.getElementById('accountLockedModal');
	            if (modal && modal.style.display === 'block') {
	                closeAccountLockedModal();
	            }
	        }
	    });
});

function detectCurrentTab() {
    // Lấy tab hiện tại từ URL hoặc từ active tab
    const urlParams = new URLSearchParams(window.location.search);
    currentTab = urlParams.get('tab') || 'pending';
    console.log('Current tab detected:', currentTab);
}

function initializeEventListeners() {
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

    // Xử lý nhập lý do tùy chỉnh
	const customReasonTextarea = document.getElementById('customReason');
	if (customReasonTextarea) {
	    customReasonTextarea.addEventListener('input', function() {
	        const charCount = this.value.length;
	        updateCharCounter(charCount, document.getElementById('reasonCharCounter'));
	        
	        // Nếu có nội dung, cập nhật lý do đã chọn
	        if (this.value.trim() !== '') {
	            selectedCancelReason = this.value.trim();
	            updateSelectedReason(selectedCancelReason);
	            
	            // Bỏ chọn các nút lý do phổ biến
	            document.querySelectorAll('.reason-option').forEach(btn => {
	                btn.classList.remove('active');
	            });
	            
	            // THÊM: Nếu đang ở tab Chờ xác nhận, luôn hiển thị thông tin shipper
	            if (currentTab === 'pending') {
	                const categoryInfo = document.getElementById('reasonCategoryInfo');
	                const shipperInfo = document.querySelector('.shipper-info');
	                const systemInfo = document.querySelector('.system-info');
	                
	                // Ẩn system info và hiển thị shipper info
	                shipperInfo.style.display = 'flex';
	                systemInfo.style.display = 'none';
	                categoryInfo.style.display = 'block';
	            }
	        } else if (selectedCancelReason === '') {
	            // Nếu không có lý do nào được chọn
	            resetSelectedReason();
	        }
	    });
	}
}

// Hàm hiển thị modal tài khoản bị khóa
function showAccountLockedModal(lyDoKhoa) {
    const modal = document.getElementById('accountLockedModal');
    const reasonText = document.getElementById('lockedReasonText');
    
    if (reasonText) {
        reasonText.textContent = lyDoKhoa || 'Không có lý do cụ thể.';
    }
    
    // Hiển thị modal
    modal.style.display = 'block';
    
    // Tạo backdrop nếu chưa có
    let backdrop = document.querySelector('.modal-backdrop');
    if (!backdrop) {
        backdrop = document.createElement('div');
        backdrop.className = 'modal-backdrop';
        backdrop.style.cssText = `
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0, 0, 0, 0.5);
            z-index: 1040;
        `;
        // Thêm sự kiện click để đóng modal
        backdrop.onclick = closeAccountLockedModal;
        document.body.appendChild(backdrop);
    }
    
    // Ngăn scroll body
    document.body.style.overflow = 'hidden';
}


// Hàm đóng modal tài khoản bị khóa
function closeAccountLockedModal() {
    const modal = document.getElementById('accountLockedModal');
    const backdrop = document.querySelector('.modal-backdrop');
    
    if (modal) {
        modal.style.display = 'none';
    }
    
    if (backdrop) {
        backdrop.remove();
    }
    
    // Khôi phục scroll body
    document.body.style.overflow = 'auto';
}

// Hàm kiểm tra trạng thái tài khoản
// Hàm kiểm tra trạng thái tài khoản
function checkAccountStatus() {
    console.log('Checking shipper account status...');
    
    fetch('/shipper/check-account-status', {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json'
        }
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.json();
    })
    .then(data => {
        if (data.success && data.isLocked) {
            // Hiển thị modal ngay lập tức
            showAccountLockedModal(data.lyDoKhoa);
        }
    })
    .catch(error => {
        console.error('Error checking account status:', error);
    });
}

// Hàm phân loại lý do
function getReasonCategory(reason) {
    const shipperReasons = [
        CANCEL_REASONS.SHIPPER_UNAVAILABLE,
        CANCEL_REASONS.VEHICLE_ISSUE, 
        CANCEL_REASONS.HEALTH_ISSUE,
        CANCEL_REASONS.PERSONAL_REASON
    ];
    
    return shipperReasons.includes(reason) ? REASON_CATEGORIES.SHIPPER : REASON_CATEGORIES.SYSTEM;
}

function updateCharCounter(count, counterElement) {
    if (!counterElement) return;
    
    counterElement.textContent = `${count}/500`;
    
    if (count > 450) {
        counterElement.className = 'char-counter text-danger';
    } else if (count > 400) {
        counterElement.className = 'char-counter text-warning';
    } else {
        counterElement.className = 'char-counter text-muted';
    }
}

// Khởi tạo xử lý đăng xuất
function initLogoutHandler() {
    const logoutBtn = document.querySelector('.logout-btn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', function(e) {
            e.preventDefault();
            performLogout();
        });
    }
}

function performLogout() {
    console.log('Performing shipper logout...');
    
    showLoading();
    
    // Clear authentication data
    clearShipperAuthData();
    
    // Chuyển hướng đến trang đăng nhập
    setTimeout(() => {
        window.location.href = '/login?logout=true';
    }, 500);
}

// Xóa dữ liệu xác thực của shipper
function clearShipperAuthData() {
    console.log('Clearing shipper auth data...');

    // Set flag for fresh login for next time
    localStorage.setItem('freshLogin', 'true');
    
    // Clear localStorage
    localStorage.removeItem('jwtToken');
    localStorage.removeItem('user');
    localStorage.removeItem('activeSidebarPage');

    // Clear sessionStorage
    sessionStorage.clear();

    // Xóa cookies
    document.cookie = "jwtToken=; path=/; expires=Thu, 01 Jan 1970 00:00:01 GMT;";
    document.cookie = "JSESSIONID=; path=/; expires=Thu, 01 Jan 1970 00:00:01 GMT;";

    console.log('Shipper auth data cleared successfully');
}

// Modal Functions
function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.style.display = 'block';
        modal.classList.add('show');
        document.body.style.overflow = 'hidden';
        document.body.classList.add('modal-open');
    }
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.style.display = 'none';
        modal.classList.remove('show');
        document.body.style.overflow = 'auto';
        document.body.classList.remove('modal-open');
    }
}

// Hàm lấy thông tin đơn hàng từ DOM
function getOrderInfo(orderId) {
    const orderCard = document.querySelector(`.order-card[data-delivery-id="${orderId}"]`);
    if (orderCard) {
        const address = orderCard.querySelector('.info-row:nth-child(3) span').textContent;
        const phone = orderCard.querySelector('.info-row:nth-child(4) span').textContent;
        const noteElement = orderCard.querySelector('.order-note');
        const note = noteElement ? noteElement.textContent : 'Không có ghi chú';
        
        return { address, phone, note };
    }
    return { 
        address: 'Không tìm thấy', 
        phone: 'Không tìm thấy', 
        note: 'Không có ghi chú'
    };
}

// Show Confirm Modal
function showConfirmModal(deliveryId, orderId) {
    currentDeliveryId = deliveryId;
    currentOrderId = orderId;
    
    const orderInfo = getOrderInfo(deliveryId);
    
    document.getElementById('confirmOrderId').textContent = '#' + orderId;
    document.getElementById('confirmOrderAddress').textContent = orderInfo.address;
    document.getElementById('confirmOrderPhone').textContent = orderInfo.phone;
    document.getElementById('confirmOrderNote').textContent = orderInfo.note;
    
    openModal('confirmModal');
}

// Show Complete Modal
function showCompleteModal(deliveryId, orderId) {
    currentDeliveryId = deliveryId;
    currentOrderId = orderId;
    
    const orderInfo = getOrderInfo(deliveryId);
    
    document.getElementById('completeOrderId').textContent = '#' + orderId;
    document.getElementById('completeOrderAddress').textContent = orderInfo.address;
    document.getElementById('completeOrderPhone').textContent = orderInfo.phone;
    
    openModal('completeModal');
}

// Hàm tương thích với HTML hiện tại
function showCancelModal(deliveryId, orderId) {
    currentTab = 'active'; // Đang giao
    showCancelReasonModal(deliveryId, orderId);
}

// Show Cancel Reason Modal
function showCancelReasonModal(deliveryId, orderId) {
    currentDeliveryId = deliveryId;
    currentOrderId = orderId;
    
    // Reset các trường
    selectedCancelReason = '';
    const customReasonTextarea = document.getElementById('customReason');
    if (customReasonTextarea) {
        customReasonTextarea.value = '';
    }
    resetSelectedReason();
    updateCharCounter(0, document.getElementById('reasonCharCounter'));
    
    // Reset trạng thái các nút lý do
    document.querySelectorAll('.reason-option').forEach(btn => {
        btn.classList.remove('active');
    });
    
    document.getElementById('cancelReasonOrderId').textContent = '#' + orderId;
    
    // HIỂN THỊ/ẨN CÁC LOẠI LÝ DO THEO TAB
    updateReasonCategoriesVisibility();
    
    openModal('cancelReasonModal');
}

// Cập nhật hiển thị các loại lý do theo tab
function updateReasonCategoriesVisibility() {
    const shipperCategory = document.querySelector('.shipper-category');
    const systemCategory = document.querySelector('.system-category');
    
    if (!shipperCategory || !systemCategory) {
        console.error('Không tìm thấy các category element');
        return;
    }
    
    if (currentTab === 'pending') {
        // Tab Chờ xác nhận: Chỉ hiển thị lý do cá nhân
        shipperCategory.style.display = 'block';
        systemCategory.style.display = 'none';
        console.log('Tab Chờ xác nhận: Chỉ hiển thị lý do cá nhân');
    } else if (currentTab === 'active') {
        // Tab Đang giao: Hiển thị cả 2 loại lý do
        shipperCategory.style.display = 'block';
        systemCategory.style.display = 'block';
        console.log('Tab Đang giao: Hiển thị cả 2 loại lý do');
    }
}

// Show Confirm All Modal
function showConfirmAllModal() {
    const pendingDeliveryIds = getPendingDeliveryIds();
    const count = pendingDeliveryIds.length;
    
    if (count === 0) {
        showNotification('warning', 'Không có đơn hàng nào để xác nhận');
        return;
    }
    
    document.getElementById('confirmAllCount').textContent = count;
    openModal('confirmAllModal');
}

// Cập nhật lý do đã chọn
function updateSelectedReason(reason) {
    selectedCancelReason = reason;
    document.getElementById('selectedReasonText').textContent = reason;
    document.getElementById('selectedReasonContainer').style.display = 'block';
    document.getElementById('confirmCancelBtn').disabled = false;
    
    // Hiển thị thông tin phân loại
    showReasonCategoryInfo(reason);
}

// Reset lý do đã chọn
function resetSelectedReason() {
    selectedCancelReason = '';
    document.getElementById('selectedReasonContainer').style.display = 'none';
    document.getElementById('confirmCancelBtn').disabled = true;
    document.getElementById('reasonCategoryInfo').style.display = 'none';
}

// Hiển thị thông tin phân loại
function showReasonCategoryInfo(reason) {
    const categoryInfo = document.getElementById('reasonCategoryInfo');
    const shipperInfo = document.querySelector('.shipper-info');
    const systemInfo = document.querySelector('.system-info');
    
    // Ẩn tất cả thông tin trước
    shipperInfo.style.display = 'none';
    systemInfo.style.display = 'none';
    
    const reasonCategory = getReasonCategory(reason);
    
    // Hiển thị thông tin phù hợp
    if (reasonCategory === REASON_CATEGORIES.SHIPPER) {
        shipperInfo.style.display = 'flex';
        categoryInfo.style.display = 'block';
    } else {
        systemInfo.style.display = 'flex';
        categoryInfo.style.display = 'block';
    }
}

// Chọn lý do từ danh sách
function selectReason(reason) {
    // KIỂM TRA XEM LÝ DO CÓ PHÙ HỢP VỚI TAB HIỆN TẠI KHÔNG
    const reasonCategory = getReasonCategory(reason);
    
    if (currentTab === 'pending' && reasonCategory === REASON_CATEGORIES.SYSTEM) {
        showNotification('error', 'Tab "Chờ xác nhận" chỉ cho phép chọn lý do cá nhân');
        return;
    }
    
    updateSelectedReason(reason);
    
    // Xóa nội dung textarea nếu có
    const customReasonTextarea = document.getElementById('customReason');
    if (customReasonTextarea) {
        customReasonTextarea.value = '';
        updateCharCounter(0, document.getElementById('reasonCharCounter'));
    }
    
    // Đánh dấu nút được chọn
    document.querySelectorAll('.reason-option').forEach(btn => {
        btn.classList.remove('active');
    });
    event.target.classList.add('active');
}

// Xóa lý do đã chọn
function removeSelectedReason() {
    resetSelectedReason();
    
    // Reset trạng thái các nút lý do
    document.querySelectorAll('.reason-option').forEach(btn => {
        btn.classList.remove('active');
    });
    
    // Xóa nội dung textarea
    const customReasonTextarea = document.getElementById('customReason');
    if (customReasonTextarea) {
        customReasonTextarea.value = '';
        updateCharCounter(0, document.getElementById('reasonCharCounter'));
    }
}

// Process Confirm Delivery
function processConfirmDelivery() {
    closeModal('confirmModal');
    
    showLoading();
    
    const csrfToken = getCsrfToken();
    const csrfHeader = getCsrfHeader();
    
    const headers = {
        'Content-Type': 'application/x-www-form-urlencoded'
    };
    if (csrfToken) {
        headers[csrfHeader] = csrfToken;
    }
    
    fetch('/shipper/confirm-delivery', {
        method: 'POST',
        headers: headers,
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
                location.reload();
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

// Process Complete Delivery
function processCompleteDelivery() {
    closeModal('completeModal');
    
    showLoading();
    
    const csrfToken = getCsrfToken();
    const csrfHeader = getCsrfHeader();
    
    const headers = {
        'Content-Type': 'application/x-www-form-urlencoded'
    };
    if (csrfToken) {
        headers[csrfHeader] = csrfToken;
    }
    
    fetch('/shipper/complete-delivery', {
        method: 'POST',
        headers: headers,
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
                location.reload();
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

// Process Cancel với lý do và phân loại - BỎ CONFIRM ALERT
function processCancelWithReason() {
    if (!selectedCancelReason || selectedCancelReason.trim() === '') {
        showNotification('error', 'Vui lòng chọn hoặc nhập lý do hủy đơn');
        return;
    }
    
    // XÁC ĐỊNH PHÂN LOẠI LÝ DO
    let reasonCategory;
    
    if (currentTab === 'pending') {
        // Tab Chờ xác nhận: LUÔN là lý do shipper
        reasonCategory = REASON_CATEGORIES.SHIPPER;
        console.log('Tab Chờ xác nhận: Lý do tự động phân loại là SHIPPER');
    } else {
        // Tab Đang giao: Phân loại theo nội dung lý do
        reasonCategory = getReasonCategory(selectedCancelReason);
    }
    
    // KIỂM TRA LÝ DO CÓ PHÙ HỢP VỚI TAB HIỆN TẠI KHÔNG
    if (currentTab === 'pending' && reasonCategory === REASON_CATEGORIES.SYSTEM) {
        showNotification('error', 'Tab "Chờ xác nhận" chỉ cho phép chọn lý do cá nhân');
        return;
    }
    
    // BỎ phần confirm alert và hủy trực tiếp
    closeModal('cancelReasonModal');
    showLoading();
    
    const csrfToken = getCsrfToken();
    const csrfHeader = getCsrfHeader();
    
    const headers = {
        'Content-Type': 'application/x-www-form-urlencoded'
    };
    if (csrfToken) {
        headers[csrfHeader] = csrfToken;
    }
    
    // Gửi cả lý do và phân loại
    fetch('/shipper/cancel-delivery', {
        method: 'POST',
        headers: headers,
        body: 'deliveryId=' + currentDeliveryId + 
              '&lyDoHuy=' + encodeURIComponent(selectedCancelReason) +
              '&reasonCategory=' + reasonCategory
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
                location.reload();
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

// Process Confirm All Deliveries
function processConfirmAllDeliveries() {
    closeModal('confirmAllModal');
    
    const pendingDeliveryIds = getPendingDeliveryIds();
    
    if (pendingDeliveryIds.length === 0) {
        showNotification('warning', 'Không có đơn hàng nào để xác nhận');
        return;
    }

    showLoading();
    
    const csrfToken = getCsrfToken();
    const csrfHeader = getCsrfHeader();
    
    const headers = {
        'Content-Type': 'application/x-www-form-urlencoded'
    };
    if (csrfToken) {
        headers[csrfHeader] = csrfToken;
    }
    
    fetch('/shipper/confirm-all-deliveries', {
        method: 'POST',
        headers: headers,
        body: 'deliveryIds=' + pendingDeliveryIds.join(',')
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
                location.reload();
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

// Chuyển tab
function changeTab(tab) {
    currentTab = tab; // Cập nhật tab hiện tại
    const url = new URL(window.location);
    url.searchParams.set('tab', tab);
    url.searchParams.set('page', '0');
    window.location.href = url.toString();
}

// Chuyển trang
function changePage(page) {
    const url = new URL(window.location);
    url.searchParams.set('page', page);
    window.location.href = url.toString();
}

// Lấy danh sách delivery IDs từ các đơn hàng chờ xác nhận
function getPendingDeliveryIds() {
    const deliveryIds = [];
    const orderCards = document.querySelectorAll('.order-card');
    
    orderCards.forEach(card => {
        const statusBadge = card.querySelector('.status-badge');
        if (statusBadge && statusBadge.textContent.includes('Đã bàn giao')) {
            const deliveryId = card.getAttribute('data-delivery-id');
            if (deliveryId) {
                deliveryIds.push(deliveryId);
            }
        }
    });
    
    return deliveryIds;
}

// Lấy CSRF Token
function getCsrfToken() {
    const csrfTokenElement = document.querySelector('meta[name="_csrf"]');
    return csrfTokenElement ? csrfTokenElement.getAttribute('content') : '';
}

// Lấy CSRF Header
function getCsrfHeader() {
    const csrfHeaderElement = document.querySelector('meta[name="_csrf_header"]');
    return csrfHeaderElement ? csrfHeaderElement.getAttribute('content') : 'X-CSRF-TOKEN';
}

// Hiển thị loading
function showLoading() {
    document.body.style.cursor = 'wait';
    const loadingOverlay = document.createElement('div');
    loadingOverlay.id = 'loading-overlay';
    loadingOverlay.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0,0,0,0.1);
        display: flex;
        justify-content: center;
        align-items: center;
        z-index: 9999;
    `;
    loadingOverlay.innerHTML = `
        <div style="background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
            <i class="fas fa-spinner fa-spin" style="font-size: 24px; color: #007bff;"></i>
            <p style="margin: 10px 0 0 0;">Đang xử lý...</p>
        </div>
    `;
    document.body.appendChild(loadingOverlay);
}

// Ẩn loading
function hideLoading() {
    document.body.style.cursor = 'default';
    const loadingOverlay = document.getElementById('loading-overlay');
    if (loadingOverlay) {
        loadingOverlay.remove();
    }
}

// Hiển thị thông báo
function showNotification(type, message) {
    const toastContainer = document.getElementById('toast-container') || createToastContainer();
    
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    
    let icon, title;
    switch(type) {
        case 'success':
            icon = 'fa-check-circle';
            title = 'Thành công';
            break;
        case 'error':
            icon = 'fa-exclamation-circle';
            title = 'Lỗi';
            break;
        case 'warning':
            icon = 'fa-exclamation-triangle';
            title = 'Cảnh báo';
            break;
        default:
            icon = 'fa-info-circle';
            title = 'Thông tin';
    }
    
    toast.innerHTML = `
        <div class="toast-icon">
            <i class="fas ${icon}"></i>
        </div>
        <div class="toast-content">
            <div class="toast-title">${title}</div>
            <div class="toast-message">${message}</div>
        </div>
        <button class="toast-close" onclick="this.parentElement.remove()">
            <i class="fas fa-times"></i>
        </button>
    `;
    
    toastContainer.appendChild(toast);
    
    setTimeout(() => {
        toast.classList.add('show');
    }, 10);
    
    setTimeout(() => {
        hideNotification(toast);
    }, 5000);
}

// Ẩn thông báo
function hideNotification(toast) {
    toast.classList.remove('show');
    toast.classList.add('hide');
    setTimeout(() => {
        if (toast.parentElement) {
            toast.remove();
        }
    }, 300);
}

// Tạo container cho toast notifications
function createToastContainer() {
    const container = document.createElement('div');
    container.id = 'toast-container';
    container.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        z-index: 10000;
        max-width: 400px;
    `;
    document.body.appendChild(container);
    return container;
}

// Thêm CSS cho toast và modal
const customStyle = document.createElement('style');
customStyle.textContent = `
    @keyframes slideInRight {
        from {
            opacity: 0;
            transform: translateX(400px);
        }
        to {
            opacity: 1;
            transform: translateX(0);
        }
    }
    
    @keyframes slideOutRight {
        from {
            opacity: 1;
            transform: translateX(0);
        }
        to {
            opacity: 0;
            transform: translateX(400px);
        }
    }
    
    .toast {
        position: relative;
        min-width: 300px;
        background: white;
        padding: 20px;
        border-radius: 10px;
        box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
        display: flex;
        align-items: center;
        gap: 15px;
        margin-bottom: 10px;
        animation: slideInRight 0.3s ease-out;
        opacity: 0;
        transform: translateX(400px);
        border-left: 4px solid;
    }

    .toast.show {
        opacity: 1;
        transform: translateX(0);
    }

    .toast.hide {
        animation: slideOutRight 0.3s ease-out forwards;
    }

    .toast-success {
        border-left-color: #28a745;
    }

    .toast-error {
        border-left-color: #e94560;
    }

    .toast-warning {
        border-left-color: #ffc107;
    }

    .toast-info {
        border-left-color: #17a2b8;
    }

    .toast-icon {
        font-size: 24px;
    }

    .toast-success .toast-icon {
        color: #28a745;
    }

    .toast-error .toast-icon {
        color: #e94560;
    }

    .toast-warning .toast-icon {
        color: #ffc107;
    }

    .toast-info .toast-icon {
        color: #17a2b8;
    }

    .toast-content {
        flex: 1;
    }

    .toast-title {
        font-weight: 600;
        margin-bottom: 5px;
        color: #0f3460;
    }

    .toast-message {
        color: #666;
        font-size: 14px;
    }

    .toast-close {
        background: none;
        border: none;
        font-size: 16px;
        color: #999;
        cursor: pointer;
        padding: 5px;
        transition: color 0.3s ease;
    }

    .toast-close:hover {
        color: #666;
    }

    .char-counter {
        font-size: 12px;
    }
`;
document.head.appendChild(customStyle);

// Thêm hàm processCancelDelivery để modal cancelModal hoạt động
function processCancelDelivery() {
    // Đóng modal cũ và mở modal chọn lý do
    closeModal('cancelModal');
    showCancelReasonModal(currentDeliveryId, currentOrderId);
}

// Export functions to global scope
window.changeTab = changeTab;
window.changePage = changePage;
window.showConfirmModal = showConfirmModal;
window.showCompleteModal = showCompleteModal;
window.showCancelModal = showCancelModal;
window.showCancelReasonModal = showCancelReasonModal;
window.showConfirmAllModal = showConfirmAllModal;
window.closeModal = closeModal;
window.processConfirmDelivery = processConfirmDelivery;
window.processCompleteDelivery = processCompleteDelivery;
window.processCancelWithReason = processCancelWithReason;
window.processConfirmAllDeliveries = processConfirmAllDeliveries;
window.selectReason = selectReason;
window.removeSelectedReason = removeSelectedReason;
window.showLoading = showLoading;
window.hideLoading = hideLoading;
window.showNotification = showNotification;
window.performLogout = performLogout;
window.processCancelDelivery = processCancelDelivery; // Thêm hàm mới
window.closeAccountLockedModal = closeAccountLockedModal;
window.showAccountLockedModal = showAccountLockedModal;
window.checkAccountStatus = checkAccountStatus;