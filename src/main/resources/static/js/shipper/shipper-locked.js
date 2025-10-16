// ===== SHIPPER LOCKED STATE JAVASCRIPT =====

class ShipperLockedManager {
    constructor() {
        this.init();
    }

    init() {
        this.showLockedStateIndicators();
        this.disableAllFunctionality();
        this.bindEvents();
    }

    showLockedStateIndicators() {
        // Hiển thị ổ khóa trên tất cả các tab
        this.addLockedIndicator('.tabs-container', 'Tài khoản đã bị khóa - Chỉ được xem');
        
        // Hiển thị ổ khóa trên danh sách đơn hàng
        this.addLockedIndicator('.orders-container', 'Tài khoản đã bị khóa - Chỉ được xem');
        
        // Hiển thị ổ khóa trên từng tab
        document.querySelectorAll('.tab-button').forEach(tab => {
            this.addTabLockedIndicator(tab);
        });
        
        // Hiển thị ổ khóa trên statistics
        this.addLockedIndicator('.stats-container', 'Tài khoản đã bị khóa');
    }

    addLockedIndicator(selector, message) {
        const element = document.querySelector(selector);
        if (element) {
            const indicator = document.createElement('div');
            indicator.className = 'shipper-locked-indicator';
            indicator.innerHTML = `
                <div class="shipper-locked-overlay">
                    <i class="fas fa-lock"></i>
                    <span>${message}</span>
                </div>
            `;
            element.style.position = 'relative';
            element.appendChild(indicator);
        }
    }

    addTabLockedIndicator(tab) {
        const indicator = document.createElement('div');
        indicator.className = 'tab-locked-indicator';
        indicator.innerHTML = `
            <div class="tab-locked-overlay">
                <i class="fas fa-lock"></i>
            </div>
        `;
        tab.style.position = 'relative';
        tab.appendChild(indicator);
    }

    disableAllFunctionality() {
        // Vô hiệu hóa tất cả các nút hành động
        document.querySelectorAll('.tab-button, .pagination-btn, .page-number').forEach(element => {
            element.style.pointerEvents = 'none';
            element.style.opacity = '0.6';
            element.style.cursor = 'not-allowed';
        });

        // Vô hiệu hóa tất cả các input, select
        document.querySelectorAll('input, select, textarea').forEach(element => {
            element.disabled = true;
            element.style.opacity = '0.6';
            element.style.cursor = 'not-allowed';
        });

        // Vô hiệu hóa tất cả các link (trừ logout)
        document.querySelectorAll('a:not(.logout-btn)').forEach(link => {
            link.style.pointerEvents = 'none';
            link.style.opacity = '0.6';
            link.style.cursor = 'not-allowed';
        });
    }

    bindEvents() {
        // Chặn tất cả sự kiện click
        document.addEventListener('click', (e) => {
            if (!e.target.closest('.logout-btn')) {
                e.preventDefault();
                e.stopPropagation();
                this.showLockedMessage();
            }
        });

        // Chặn sự kiện form
        document.querySelectorAll('form').forEach(form => {
            form.addEventListener('submit', (e) => {
                e.preventDefault();
                e.stopPropagation();
                this.showLockedMessage();
            });
        });
    }

    showLockedMessage() {
        this.showToast('warning', 
            'Tài khoản của bạn đã bị khóa. Chỉ có thể xem thông tin.', 
            3000
        );
    }
}

// CSS cho trạng thái bị khóa - MỜ như promotions-locked.js
const shipperLockedStyles = `
    .shipper-locked-indicator {
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(255, 255, 255, 0.9);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 10;
        border-radius: inherit;
    }
    
    .shipper-locked-overlay {
        text-align: center;
        color: #6c757d;
    }
    
    .shipper-locked-overlay i {
        font-size: 2rem;
        margin-bottom: 10px;
        display: block;
        color: #dc3545;
    }
    
    .shipper-locked-overlay span {
        font-weight: 600;
        font-size: 0.9rem;
    }

    .tab-locked-indicator {
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(255, 255, 255, 0.8);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 10;
        border-radius: inherit;
    }
    
    .tab-locked-overlay {
        text-align: center;
        color: #6c757d;
    }
    
    .tab-locked-overlay i {
        font-size: 1.2rem;
        color: #dc3545;
    }

    /* Style cho các phần tử bị vô hiệu hóa */
    .tab-button, .pagination-btn, .page-number {
        opacity: 0.6 !important;
        pointer-events: none !important;
        cursor: not-allowed !important;
    }

    input, select, textarea {
        opacity: 0.6 !important;
        cursor: not-allowed !important;
    }

    a:not(.logout-btn) {
        opacity: 0.6 !important;
        pointer-events: none !important;
        cursor: not-allowed !important;
    }

    /* Đảm bảo logout vẫn hoạt động */
    .logout-btn {
        opacity: 1 !important;
        pointer-events: auto !important;
        cursor: pointer !important;
    }

    /* Hiệu ứng cho các khu vực bị khóa */
    .tabs-container, .orders-container, .stats-container {
        position: relative;
    }

    .stats-container .stat-card {
        opacity: 0.7;
    }

    /* Additional styles for shipper-specific elements */
    .tabs-container .shipper-locked-indicator {
        border-radius: 8px;
    }

    .orders-container .shipper-locked-indicator {
        border-radius: 8px;
    }

    .stats-container .shipper-locked-indicator {
        border-radius: 8px;
    }

    /* Disabled pagination appearance */
    .pagination .page-link:not(.active) {
        opacity: 0.6;
    }

    /* Transition effects */
    .action-btn, .status-toggle, .btn-add, .btn-confirm, .btn-cancel, .btn-complete {
        transition: opacity 0.3s ease;
    }
`;

// Tiêm styles vào DOM
const styleSheet = document.createElement('style');
styleSheet.textContent = shipperLockedStyles;
document.head.appendChild(styleSheet);

// Khởi tạo khi DOM được tải
document.addEventListener('DOMContentLoaded', function() {
    window.shipperLockedManager = new ShipperLockedManager();
});