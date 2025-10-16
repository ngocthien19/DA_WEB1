let selectedOrderId = null;
let selectedReason = null;
const ITEMS_PER_PAGE = 10;
let currentActiveTab = 'all';
let currentFilteredPage = 1;
let allOrders = []; // Lưu trữ tất cả đơn hàng

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    initializeTabs();
    initializeCancelReasons();
    saveOriginalPagination();
    collectAllOrders(); // Thu thập tất cả đơn hàng khi trang tải
    initializeScrollToTop(); 
    initializeToast(); // Khởi tạo toast
});

// ========== TOAST SYSTEM ==========
function initializeToast() {
    // Tạo container cho toast nếu chưa có
    if (!document.getElementById('toastContainer')) {
        const toastContainer = document.createElement('div');
        toastContainer.id = 'toastContainer';
        document.body.appendChild(toastContainer);
    }

    // Thêm CSS cho toast
    if (!document.getElementById('toast-styles')) {
        const style = document.createElement('style');
        style.id = 'toast-styles';
        style.textContent = `
            #toastContainer {
                position: fixed;
                top: 100px;
                right: 20px;
                z-index: 10000;
                display: flex;
                flex-direction: column;
                gap: 10px;
            }

            .toast {
                min-width: 300px;
                background: white;
                padding: 16px 20px;
                border-radius: 8px;
                box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
                display: flex;
                align-items: center;
                gap: 12px;
                animation: slideInRight 0.3s ease;
                border-left: 4px solid;
                transform: translateX(400px);
                opacity: 0;
                transition: all 0.3s ease;
            }

            .toast.show {
                transform: translateX(0);
                opacity: 1;
            }

            .toast.hide {
                transform: translateX(400px);
                opacity: 0;
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
                font-size: 20px;
                flex-shrink: 0;
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
                margin-bottom: 4px;
                color: #2c3e50;
                font-size: 14px;
            }

            .toast-message {
                color: #5a6c7d;
                font-size: 13px;
                line-height: 1.4;
            }

            .toast-close {
                background: none;
                border: none;
                font-size: 14px;
                color: #95a5a6;
                cursor: pointer;
                padding: 4px;
                border-radius: 4px;
                transition: color 0.2s ease, background-color 0.2s ease;
                flex-shrink: 0;
            }

            .toast-close:hover {
                color: #7f8c8d;
                background-color: #f8f9fa;
            }

            @keyframes slideInRight {
                from {
                    transform: translateX(400px);
                    opacity: 0;
                }
                to {
                    transform: translateX(0);
                    opacity: 1;
                }
            }

            @keyframes slideOutRight {
                from {
                    transform: translateX(0);
                    opacity: 1;
                }
                to {
                    transform: translateX(400px);
                    opacity: 0;
                }
            }
        `;
        document.head.appendChild(style);
    }
}

// Hàm hiển thị toast
function showToast(type, title, message, duration = 3000) {
    const toastContainer = document.getElementById('toastContainer');
    
    // Tạo toast element
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    
    // Chọn icon dựa trên type
    let icon;
    switch(type) {
        case 'success':
            icon = '<i class="fa-solid fa-check-circle toast-icon"></i>';
            break;
        case 'error':
            icon = '<i class="fa-solid fa-exclamation-circle toast-icon"></i>';
            break;
        case 'warning':
            icon = '<i class="fa-solid fa-triangle-exclamation toast-icon"></i>';
            break;
        case 'info':
            icon = '<i class="fa-solid fa-info-circle toast-icon"></i>';
            break;
        default:
            icon = '<i class="fa-solid fa-bell toast-icon"></i>';
    }
    
    toast.innerHTML = `
        ${icon}
        <div class="toast-content">
            <div class="toast-title">${title}</div>
            <div class="toast-message">${message}</div>
        </div>
        <button class="toast-close" onclick="this.parentElement.remove()">
            <i class="fa-solid fa-times"></i>
        </button>
    `;
    
    // Thêm toast vào container
    toastContainer.appendChild(toast);
    
    // Hiển thị toast với animation
    setTimeout(() => {
        toast.classList.add('show');
    }, 10);
    
    // Tự động ẩn sau duration (nếu duration > 0)
    if (duration > 0) {
        setTimeout(() => {
            hideToast(toast);
        }, duration);
    }
    
    return toast;
}

// Hàm ẩn toast
function hideToast(toast) {
    toast.classList.remove('show');
    toast.classList.add('hide');
    
    setTimeout(() => {
        if (toast.parentElement) {
            toast.parentElement.removeChild(toast);
        }
    }, 300);
}

// Thu thập tất cả đơn hàng từ DOM
function collectAllOrders() {
    allOrders = Array.from(document.querySelectorAll('.order-card'));
}

// Lưu phân trang gốc từ server
function saveOriginalPagination() {
    const originalPagination = document.querySelector('.pagination');
    if (originalPagination) {
        originalPagination.setAttribute('id', 'original-pagination');
    }
}

function initializeScrollToTop() {
    const scrollToTopBtn = document.getElementById('scrollToTopBtn');

    if (!scrollToTopBtn) return;

    // Thiết lập trạng thái ban đầu là ẩn
    scrollToTopBtn.classList.add('hidden');

    // Hiển thị/Ẩn nút khi cuộn trang
    window.addEventListener('scroll', function() {
        // Nút sẽ hiển thị khi cuộn xuống 300px
        if (document.body.scrollTop > 300 || document.documentElement.scrollTop > 300) {
            // Khi cần hiển thị: Đảm bảo nút ở trạng thái hiển thị
            scrollToTopBtn.style.display = 'block';
            
            // Dùng setTimeout để đảm bảo 'display: block' được áp dụng trước khi thêm class 'visible'
            setTimeout(() => {
                scrollToTopBtn.classList.remove('hidden');
                scrollToTopBtn.classList.add('visible');
            }, 10); // Độ trễ nhỏ 10ms
        } else {
            // Khi cần ẩn: Áp dụng trạng thái ẩn (kích hoạt animation)
            scrollToTopBtn.classList.remove('visible');
            scrollToTopBtn.classList.add('hidden');
            
            // Sau khi animation kết thúc (0.4s), ẩn hoàn toàn nút
            setTimeout(() => {
                // Chỉ ẩn display nếu nút vẫn ở trạng thái 'hidden'
                if (scrollToTopBtn.classList.contains('hidden')) {
                    scrollToTopBtn.style.display = 'none';
                }
            }, 400); 
        }
    });

    // Cuộn lên đầu trang khi click
    scrollToTopBtn.addEventListener('click', function() {
        window.scrollTo({
            top: 0,
            behavior: 'smooth' // Cuộn mượt mà
        });
    });
}

// Tab switching functionality
function initializeTabs() {
    const tabButtons = document.querySelectorAll('.tab-button');
    
    tabButtons.forEach(button => {
        button.addEventListener('click', function() {
            const tabType = this.getAttribute('data-tab');
            currentActiveTab = tabType;

            // Update active tab
            tabButtons.forEach(btn => btn.classList.remove('active'));
            this.classList.add('active');

            // Nếu là tab "all", khôi phục phân trang gốc
            if (tabType === 'all') {
                restoreOriginalPagination();
            } else {
                // Filter orders và ẩn phân trang gốc
                filterOrders(tabType);
                hideOriginalPagination();
            }
        });
    });
}

// Khôi phục phân trang gốc từ server
function restoreOriginalPagination() {
    const originalPagination = document.getElementById('original-pagination');
    const clientPagination = document.querySelector('.pagination:not(#original-pagination)');
    
    // Ẩn phân trang client-side nếu có
    if (clientPagination) {
        clientPagination.style.display = 'none';
    }
    
    // Hiện phân trang gốc
    if (originalPagination) {
        originalPagination.style.display = 'flex';
    }
    
    // Hiện tất cả orders và áp dụng phân trang server-side
    const allOrders = document.querySelectorAll('.order-card');
    allOrders.forEach(order => {
        order.style.display = 'block';
    });
    
    // Ẩn thông báo no orders nếu có
    const noOrdersDiv = document.querySelector('.no-orders');
    if (noOrdersDiv) {
        noOrdersDiv.style.display = 'none';
    }
    
    // Reset trang hiện tại
    currentFilteredPage = 1;
}

// Ẩn phân trang gốc
function hideOriginalPagination() {
    const originalPagination = document.getElementById('original-pagination');
    if (originalPagination) {
        originalPagination.style.display = 'none';
    }
}

// Filter orders based on status - SỬA LẠI: lọc từ tất cả đơn hàng
function filterOrders(status) {
    const noOrdersDiv = document.querySelector('.no-orders');
    let visibleOrders = [];

    // QUAN TRỌNG: Lọc từ tất cả đơn hàng (allOrders) thay vì chỉ đơn hàng hiển thị
    allOrders.forEach(order => {
        const orderStatus = order.querySelector('.order-status').textContent.trim();
        const normalizedStatus = normalizeStatus(orderStatus);

        if (status === 'all' || normalizedStatus === status) {
            visibleOrders.push(order);
        }
    });

    // Ẩn tất cả đơn hàng trước
    allOrders.forEach(order => {
        order.style.display = 'none';
    });

    // Show/hide no orders message
    if (visibleOrders.length === 0) {
        if (!noOrdersDiv) {
            createNoOrdersMessage(status);
        } else {
            noOrdersDiv.style.display = 'block';
            updateNoOrdersMessage(status);
        }
        
        // Ẩn phân trang client-side nếu có
        const clientPagination = document.querySelector('.pagination:not(#original-pagination)');
        if (clientPagination) {
            clientPagination.style.display = 'none';
        }
    } else {
        if (noOrdersDiv) {
            noOrdersDiv.style.display = 'none';
        }

        // Reset về trang 1 khi filter
        currentFilteredPage = 1;
        
        // Show first page of orders
        showPage(visibleOrders, currentFilteredPage);
        
        // Chỉ tạo phân trang client-side nếu có nhiều hơn ITEMS_PER_PAGE orders
        if (visibleOrders.length > ITEMS_PER_PAGE) {
            updatePagination(visibleOrders, status);
        } else {
            // Ẩn phân trang client-side nếu có ít hơn hoặc bằng ITEMS_PER_PAGE orders
            const clientPagination = document.querySelector('.pagination:not(#original-pagination)');
            if (clientPagination) {
                clientPagination.style.display = 'none';
            }
        }
    }
}

// Show specific page of orders
function showPage(orders, page) {
    const startIndex = (page - 1) * ITEMS_PER_PAGE;
    const endIndex = startIndex + ITEMS_PER_PAGE;

    orders.forEach((order, index) => {
        if (index >= startIndex && index < endIndex) {
            order.style.display = 'block';
            order.style.animation = 'fadeIn 0.5s ease';
        } else {
            order.style.display = 'none';
        }
    });
}

// Update pagination based on filtered orders
function updatePagination(visibleOrders, currentStatus) {
    let paginationDiv = document.querySelector('.pagination:not(#original-pagination)');
    const totalPages = Math.ceil(visibleOrders.length / ITEMS_PER_PAGE);

    // Create pagination if it doesn't exist
    if (!paginationDiv) {
        const tabContent = document.getElementById('tab-all');
        paginationDiv = document.createElement('div');
        paginationDiv.className = 'pagination';
        tabContent.appendChild(paginationDiv);
    }

    paginationDiv.style.display = 'flex';
    paginationDiv.innerHTML = ''; // Clear existing pagination

    // Previous button
    const prevLink = document.createElement('a');
    prevLink.href = '#';
    prevLink.className = 'page-link';
    if (currentFilteredPage === 1) {
        prevLink.classList.add('disabled');
    }
    prevLink.innerHTML = '<i class="fa-solid fa-chevron-left"></i>';
    prevLink.addEventListener('click', function(e) {
        e.preventDefault();
        if (currentFilteredPage > 1) {
            currentFilteredPage--;
            showPage(visibleOrders, currentFilteredPage);
            updatePaginationButtons();
        }
    });
    paginationDiv.appendChild(prevLink);

    // Page number buttons
    const pageButtons = [];
    for (let i = 1; i <= totalPages; i++) {
        const pageLink = document.createElement('a');
        pageLink.href = '#';
        pageLink.className = 'page-link';
        if (i === currentFilteredPage) {
            pageLink.classList.add('current');
        }
        pageLink.textContent = i;
        pageLink.setAttribute('data-page', i);
        pageLink.addEventListener('click', function(e) {
            e.preventDefault();
            currentFilteredPage = parseInt(this.getAttribute('data-page'));
            showPage(visibleOrders, currentFilteredPage);
            updatePaginationButtons();
        });
        paginationDiv.appendChild(pageLink);
        pageButtons.push(pageLink);
    }

    // Next button
    const nextLink = document.createElement('a');
    nextLink.href = '#';
    nextLink.className = 'page-link';
    if (currentFilteredPage === totalPages) {
        nextLink.classList.add('disabled');
    }
    nextLink.innerHTML = '<i class="fa-solid fa-chevron-right"></i>';
    nextLink.addEventListener('click', function(e) {
        e.preventDefault();
        if (currentFilteredPage < totalPages) {
            currentFilteredPage++;
            showPage(visibleOrders, currentFilteredPage);
            updatePaginationButtons();
        }
    });
    paginationDiv.appendChild(nextLink);

    // Function to update button states
    function updatePaginationButtons() {
        // Update page number buttons
        pageButtons.forEach((btn, index) => {
            if (index + 1 === currentFilteredPage) {
                btn.classList.add('current');
            } else {
                btn.classList.remove('current');
            }
        });

        // Update prev/next buttons
        if (currentFilteredPage === 1) {
            prevLink.classList.add('disabled');
        } else {
            prevLink.classList.remove('disabled');
        }

        if (currentFilteredPage === totalPages) {
            nextLink.classList.add('disabled');
        } else {
            nextLink.classList.remove('disabled');
        }

        // Scroll to top of orders container
        const ordersContainer = document.querySelector('.orders-container');
        if (ordersContainer) {
            ordersContainer.scrollIntoView({ 
                behavior: 'smooth', 
                block: 'start' 
            });
        }
    }
}

// Create no orders message dynamically
function createNoOrdersMessage(status) {
    const tabContent = document.getElementById('tab-all');
    const noOrdersDiv = document.createElement('div');
    noOrdersDiv.className = 'no-orders';
    
    const statusMessages = {
        'all': 'Bạn chưa có đơn hàng nào',
        'cho-xac-nhan': 'Không có đơn hàng chờ xác nhận',
        'da-xac-nhan': 'Không có đơn hàng đã xác nhận',
        'dang-giao': 'Không có đơn hàng đang giao',
        'hoan-thanh': 'Không có đơn hàng hoàn thành',
        'da-huy': 'Không có đơn hàng đã hủy'
    };
    
    noOrdersDiv.innerHTML = `
        <div class="no-orders-icon">
            <i class="fa-solid fa-box-open"></i>
        </div>
        <h3>${statusMessages[status] || 'Không có đơn hàng'}</h3>
        <p>Hãy bắt đầu mua sắm để xem đơn hàng của bạn tại đây</p>
        <a href="/products" class="btn-shopping">
            <i class="fa-solid fa-bag-shopping"></i>
            Mua sắm ngay
        </a>
    `;
    
    tabContent.appendChild(noOrdersDiv);
}

// Update no orders message based on filter
function updateNoOrdersMessage(status) {
    const noOrdersDiv = document.querySelector('.no-orders');
    if (!noOrdersDiv) return;
    
    const statusMessages = {
        'all': 'Bạn chưa có đơn hàng nào',
        'cho-xac-nhan': 'Không có đơn hàng chờ xác nhận',
        'da-xac-nhan': 'Không có đơn hàng đã xác nhận',
        'dang-giao': 'Không có đơn hàng đang giao',
        'hoan-thanh': 'Không có đơn hàng hoàn thành',
        'da-huy': 'Không có đơn hàng đã hủy'
    };
    
    const h3 = noOrdersDiv.querySelector('h3');
    if (h3) {
        h3.textContent = statusMessages[status] || 'Không có đơn hàng';
    }
}

// Normalize status for comparison
function normalizeStatus(status) {
    const statusMap = {
        'Chờ xác nhận': 'cho-xac-nhan',
        'Đã xác nhận': 'da-xac-nhan',
        'Đang giao': 'dang-giao',
        'Hoàn thành': 'hoan-thanh',
        'Hủy': 'da-huy'
    };
    return statusMap[status] || status.toLowerCase().replace(/ /g, '-');
}

// Initialize cancel reasons
function initializeCancelReasons() {
    const radioButtons = document.querySelectorAll('input[name="cancelReason"]');
    const otherReasonContainer = document.getElementById('otherReasonContainer');
    const otherReasonText = document.getElementById('otherReasonText');
    
    radioButtons.forEach(radio => {
        radio.addEventListener('change', function() {
            selectedReason = this.value;
            
            if (this.value === 'other') {
                otherReasonContainer.style.display = 'block';
                otherReasonText.focus();
            } else {
                otherReasonContainer.style.display = 'none';
                otherReasonText.value = '';
            }
        });
    });
    
    // Character counter for other reason
    if (otherReasonText) {
        otherReasonText.addEventListener('input', function() {
            const charCount = this.value.length;
            const charCountElement = otherReasonContainer.querySelector('.char-count');
            if (charCountElement) {
                charCountElement.textContent = `${charCount}/500 ký tự`;
                
                if (charCount > 450) {
                    charCountElement.style.color = '#e94560';
                } else {
                    charCountElement.style.color = '#999';
                }
            }
        });
    }
}

// Show cancel modal
function showCancelModal(orderId) {
    selectedOrderId = orderId;
    selectedReason = null;
    
    // Reset form
    const radioButtons = document.querySelectorAll('input[name="cancelReason"]');
    radioButtons.forEach(radio => radio.checked = false);
    
    const otherReasonContainer = document.getElementById('otherReasonContainer');
    const otherReasonText = document.getElementById('otherReasonText');
    
    if (otherReasonContainer) {
        otherReasonContainer.style.display = 'none';
    }
    
    if (otherReasonText) {
        otherReasonText.value = '';
    }
    
    // Show modal
    document.getElementById('cancelModal').style.display = 'block';
    document.body.style.overflow = 'hidden';
}

// Close cancel modal
function closeCancelModal() {
    selectedOrderId = null;
    selectedReason = null;
    document.getElementById('cancelModal').style.display = 'none';
    document.body.style.overflow = 'auto';
}

// Confirm cancel order - SỬA LẠI để dùng toast của trang
function confirmCancelOrder() {
    if (!selectedOrderId) {
        showToast('error', 'Lỗi', 'Không tìm thấy đơn hàng');
        return;
    }
    
    // Get selected reason
    const radioButtons = document.querySelectorAll('input[name="cancelReason"]');
    let reason = null;
    
    radioButtons.forEach(radio => {
        if (radio.checked) {
            reason = radio.value;
        }
    });
    
    if (!reason) {
        showToast('error', 'Lỗi', 'Vui lòng chọn lý do hủy đơn hàng');
        return;
    }
    
    // If "other" is selected, get the text input
    if (reason === 'other') {
        const otherReasonText = document.getElementById('otherReasonText');
        if (!otherReasonText || !otherReasonText.value.trim()) {
            showToast('error', 'Lỗi', 'Vui lòng nhập lý do hủy đơn hàng');
            otherReasonText.focus();
            return;
        }
        reason = otherReasonText.value.trim();
    }
    
    // Disable button to prevent double submission
    const confirmButton = document.querySelector('.btn-confirm');
    confirmButton.disabled = true;
    confirmButton.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Đang xử lý...';
    
    // Get JWT token
    const token = localStorage.getItem('jwtToken');
    if (!token) {
        showToast('error', 'Lỗi', 'Vui lòng đăng nhập lại');
        confirmButton.disabled = false;
        confirmButton.innerHTML = '<i class="fa-solid fa-check"></i> Xác nhận hủy';
        return;
    }
    
    console.log('🟡 Gửi yêu cầu hủy đơn hàng:', {
        orderId: selectedOrderId,
        reason: reason
    });
    
    // Send cancel request
    fetch(`/api/orders/${selectedOrderId}/cancels`, {
        method: 'POST',
        headers: {
            'Authorization': 'Bearer ' + token,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            reason: reason
        })
    })
    .then(response => {
        console.log('🟡 Response status:', response.status);
        if (!response.ok) {
            // Nếu response không ok, thử parse lỗi từ body
            return response.json().then(errorData => {
                throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
            }).catch(() => {
                throw new Error(`HTTP error! status: ${response.status}`);
            });
        }
        return response.json();
    })
    .then(data => {
        console.log('🟡 Response data:', data);
        if (data.success) {
            closeCancelModal();
            showToast('success', 'Thành công', 'Đã hủy đơn hàng thành công', 3000);
            
            // Reload page after 1.5 seconds
            setTimeout(() => {
                window.location.reload();
            }, 1500);
        } else {
            throw new Error(data.message || 'Không thể hủy đơn hàng');
        }
    })
    .catch(error => {
        console.error('❌ Error:', error);
        showToast('error', 'Lỗi', error.message, 3000);
        
        // Re-enable button
        confirmButton.disabled = false;
        confirmButton.innerHTML = '<i class="fa-solid fa-check"></i> Xác nhận hủy';
    });
}

// Close modal when clicking outside
window.onclick = function(event) {
    const modal = document.getElementById('cancelModal');
    if (event.target === modal) {
        closeCancelModal();
    }
}

// Close modal with Escape key
document.addEventListener('keydown', function(event) {
    if (event.key === 'Escape') {
        const modal = document.getElementById('cancelModal');
        if (modal && modal.style.display === 'block') {
            closeCancelModal();
        }
    }
});

// Add CSS animations dynamically
const style = document.createElement('style');
style.textContent = `
    @keyframes slideInRight {
        from {
            transform: translateX(100%);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }
    
    @keyframes slideOutRight {
        from {
            transform: translateX(0);
            opacity: 1;
        }
        to {
            transform: translateX(100%);
            opacity: 0;
        }
    }
    
    @keyframes fadeIn {
        from {
            opacity: 0;
            transform: translateY(10px);
        }
        to {
            opacity: 1;
            transform: translateY(0);
        }
    }
`;
document.head.appendChild(style);