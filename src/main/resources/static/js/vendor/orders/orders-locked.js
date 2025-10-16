// ===== ORDERS LOCKED STATE JAVASCRIPT =====

class OrdersLockedManager {
    constructor() {
        this.init();
    }

    init() {
        this.disableInteractiveElements();
        this.showLockedStateIndicators();
        this.bindEvents();
    }

    disableInteractiveElements() {
        // Only disable elements in the main content area (stats-container)
        document.querySelectorAll('.stats-container input, .stats-container select, .stats-container textarea, .stats-container button, .stats-container a.btn').forEach(element => {
            // Keep search input functional for viewing
            if (!element.closest('.search-box') && 
                !element.closest('.vendor-store-card') &&
                !element.classList.contains('btn-view')) {
                element.disabled = true;
                element.style.opacity = '0.6';
                element.style.cursor = 'not-allowed';
            }
        });

        // Remove click events from action buttons in orders section only
        document.querySelectorAll('.stats-container .action-btn, .stats-container .status-toggle, .stats-container .btn-search, .stats-container .btn-bulk-status, .stats-container .btn-bulk-delete').forEach(button => {
            // Keep view buttons functional
            if (!button.classList.contains('btn-view')) {
                button.style.pointerEvents = 'none';
                button.style.opacity = '0.5';
            }
        });

        // Disable status display clicks in orders section only
        document.querySelectorAll('.stats-container .status-display').forEach(statusElement => {
            statusElement.style.pointerEvents = 'none';
            statusElement.style.cursor = 'default';
        });

        // Disable pagination in orders section only
        document.querySelectorAll('.stats-container .pagination .page-link').forEach(link => {
            link.style.pointerEvents = 'none';
            link.style.opacity = '0.5';
        });

        // Keep search button functional but disable form submission
        document.querySelectorAll('.stats-container .btn-search').forEach(button => {
            button.style.pointerEvents = 'none';
            button.style.opacity = '0.5';
        });
    }

    showLockedStateIndicators() {
        // Add locked indicator to action areas only in orders section
        this.addLockedIndicator('.stats-container .table-container', 'Chỉ có thể xem đơn hàng');
        this.addLockedIndicator('.stats-container .filter-section', 'Bộ lọc chỉ để xem');
        this.addLockedIndicator('.stats-container .status-summary-cards', 'Thống kê chỉ để xem');
        
        // Add specific indicators for bulk operations
        this.addLockedIndicator('.stats-container .selection-controls', 'Không thể thực hiện thao tác hàng loạt');
    }

    addLockedIndicator(selector, message) {
        const element = document.querySelector(selector);
        if (element) {
            const indicator = document.createElement('div');
            indicator.className = 'locked-indicator';
            indicator.innerHTML = `
                <div class="locked-overlay">
                    <i class="fas fa-lock"></i>
                    <span>${message}</span>
                </div>
            `;
            element.style.position = 'relative';
            element.appendChild(indicator);
        }
    }

    bindEvents() {
        // Show locked message when trying to interact with disabled elements in orders section only
        document.querySelectorAll('.stats-container button, .stats-container a, .stats-container input, .stats-container select').forEach(element => {
            if ((element.disabled || element.style.pointerEvents === 'none') && 
                !element.classList.contains('btn-view')) {
                element.addEventListener('click', (e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    this.showLockedMessage();
                });
            }
        });

        // Special handling for view-only links - keep them functional
        document.querySelectorAll('.stats-container a.btn-view').forEach(link => {
            link.style.pointerEvents = 'auto';
            link.style.opacity = '1';
        });

        // Prevent status display interactions in orders section only
        document.querySelectorAll('.stats-container .status-display').forEach(statusElement => {
            statusElement.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                this.showLockedMessage();
            });
        });

        // Prevent pagination in orders section only
        document.querySelectorAll('.stats-container .pagination .page-link').forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                this.showLockedMessage();
            });
        });

        // Prevent search form submission in orders section only
        document.querySelectorAll('.stats-container form').forEach(form => {
            form.addEventListener('submit', (e) => {
                e.preventDefault();
                e.stopPropagation();
                this.showLockedMessage();
            });
        });

        // Prevent bulk operations
        document.querySelectorAll('.stats-container .btn-bulk-status, .stats-container .btn-bulk-delete').forEach(button => {
            button.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                this.showLockedMessage();
            });
        });
    }

    showLockedMessage() {
        this.showToast('warning', 
            'Cửa hàng của bạn đang bị khóa. Bạn không thể thực hiện thao tác quản lý đơn hàng.', 
            5000
        );
    }

    showToast(type, message, duration = 3000) {
        const toastContainer = document.getElementById('toastContainer');
        const toastId = 'toast-' + Date.now();
        
        const toastHtml = `
            <div id="${toastId}" class="toast align-items-center text-white bg-${type} border-0" role="alert">
                <div class="d-flex">
                    <div class="toast-body">
                        <i class="fas fa-exclamation-triangle me-2"></i>
                        ${message}
                    </div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
                </div>
            </div>
        `;
        
        toastContainer.insertAdjacentHTML('beforeend', toastHtml);
        
        const toastElement = document.getElementById(toastId);
        const toast = new bootstrap.Toast(toastElement, {
            autohide: true,
            delay: duration
        });
        
        toast.show();
        
        // Remove toast from DOM after hide
        toastElement.addEventListener('hidden.bs.toast', () => {
            toastElement.remove();
        });
    }
}

// Add CSS for locked state
const lockedStyles = `
    .locked-indicator {
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
    
    .locked-overlay {
        text-align: center;
        color: #6c757d;
    }
    
    .locked-overlay i {
        font-size: 2rem;
        margin-bottom: 10px;
        display: block;
        color: #dc3545;
    }
    
    .locked-overlay span {
        font-weight: 600;
        font-size: 0.9rem;
    }
    
    .action-btn, .status-display, .btn-search, .btn-bulk-status, .btn-bulk-delete {
        transition: opacity 0.3s ease;
    }

    /* Additional styles for order-specific elements */
    .status-summary-cards .locked-indicator {
        border-radius: 8px;
    }

    .table-container .locked-indicator {
        border-radius: 8px;
    }

    /* Selection controls specific styles */
    .selection-controls .locked-indicator {
        border-radius: 8px;
        background: rgba(255, 255, 255, 0.95);
    }

    /* Disabled pagination appearance */
    .pagination .page-link:not(.active) {
        opacity: 0.6;
    }
`;

// Inject styles
const styleSheet = document.createElement('style');
styleSheet.textContent = lockedStyles;
document.head.appendChild(styleSheet);

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    window.ordersLockedManager = new OrdersLockedManager();
});