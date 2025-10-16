// ===== ACCOUNT LOCKED STATE JAVASCRIPT =====

class AccountLockedManager {
    constructor() {
        this.init();
    }

    init() {
        // Đợi content load xong rồi mới chặn
        this.waitForContent();
    }

    waitForContent() {
        // Kiểm tra định kỳ cho đến khi content được load
        const checkInterval = setInterval(() => {
            const accountContent = document.getElementById('accountContent');
            if (accountContent && accountContent.style.display !== 'none') {
                clearInterval(checkInterval);
                this.disableInteractiveElements();
                this.showLockedStateIndicators();
                this.bindEvents();
            }
        }, 100);
    }

    disableInteractiveElements() {
        // Chỉ disable các nút chức năng, không disable input fields
        document.querySelectorAll('.stats-container button, .stats-container a.btn').forEach(element => {
            // Không chặn nút "Thử lại" trong error state
            if (!element.closest('#errorState') && 
                !element.classList.contains('btn-view')) {
                element.disabled = true;
                element.style.opacity = '0.6';
                element.style.cursor = 'not-allowed';
            }
        });

        // Disable các nút submit và action đặc biệt
        document.querySelectorAll('.stats-container .btn-save, .stats-container .btn-update, .stats-container .btn-change, .stats-container .btn-edit').forEach(button => {
            button.style.pointerEvents = 'none';
            button.style.opacity = '0.5';
        });

        // Disable file upload inputs
        document.querySelectorAll('.stats-container input[type="file"]').forEach(input => {
            input.disabled = true;
            input.style.opacity = '0.6';
            input.style.cursor = 'not-allowed';
        });
    }

    showLockedStateIndicators() {
        // Thêm indicators cho các section có chức năng chỉnh sửa
        this.addLockedIndicator('.profile-form-section', 'Không thể cập nhật thông tin');
        this.addLockedIndicator('.store-form-section', 'Không thể cập nhật thông tin cửa hàng');
        this.addLockedIndicator('.password-form-section', 'Không thể đổi mật khẩu');
        
        // Thêm indicator cho upload ảnh
        const uploadSections = document.querySelectorAll('.avatar-upload, .store-logo-upload');
        uploadSections.forEach(section => {
            this.addLockedIndicator(section, 'Không thể tải lên ảnh');
        });
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
        // Show locked message khi click vào các nút bị chặn
        document.querySelectorAll('.stats-container button, .stats-container a.btn').forEach(element => {
            if ((element.disabled || element.style.pointerEvents === 'none') && 
                !element.closest('#errorState') &&
                !element.classList.contains('btn-view')) {
                element.addEventListener('click', (e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    this.showLockedMessage();
                });
            }
        });

        // Ngăn chặn form submissions
        document.querySelectorAll('.stats-container form').forEach(form => {
            form.addEventListener('submit', (e) => {
                e.preventDefault();
                e.stopPropagation();
                this.showLockedMessage();
            });
        });

        // Ngăn chặn file uploads
        document.querySelectorAll('.stats-container input[type="file"]').forEach(input => {
            input.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                this.showLockedMessage();
            });
            
            input.addEventListener('change', (e) => {
                e.preventDefault();
                e.stopPropagation();
                this.showLockedMessage();
            });
        });

        // Ngăn chặn các modal triggers cho chức năng chỉnh sửa
        document.querySelectorAll('.stats-container [data-bs-toggle="modal"]').forEach(modalTrigger => {
            if (modalTrigger.classList.contains('btn-edit') || 
                modalTrigger.classList.contains('btn-change')) {
                modalTrigger.addEventListener('click', (e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    this.showLockedMessage();
                });
            }
        });
    }

    showLockedMessage() {
        this.showToast('warning', 
            'Cửa hàng của bạn đang bị khóa. Bạn không thể thực hiện thao tác cập nhật thông tin.', 
            5000
        );
    }

    showToast(type, message, duration = 3000) {
        const toastContainer = document.getElementById('toastContainer');
        if (!toastContainer) return;
        
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
        border-radius: 8px;
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
    
    .btn-save, .btn-update, .btn-change, .btn-edit {
        transition: opacity 0.3s ease;
    }

    /* Specific styles for account page */
    .profile-form-section .locked-indicator,
    .store-form-section .locked-indicator,
    .password-form-section .locked-indicator {
        border-radius: 8px;
    }

    .avatar-upload .locked-indicator,
    .store-logo-upload .locked-indicator {
        border-radius: 8px;
    }

    /* Keep inputs readable but non-interactive */
    input:disabled, select:disabled, textarea:disabled {
        background-color: #f8f9fa !important;
        color: #6c757d !important;
        border-color: #dee2e6 !important;
    }
`;

// Inject styles
const styleSheet = document.createElement('style');
styleSheet.textContent = lockedStyles;
document.head.appendChild(styleSheet);

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    window.accountLockedManager = new AccountLockedManager();
});