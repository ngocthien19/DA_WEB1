// ===== MESSAGES LOCKED STATE JAVASCRIPT =====

class MessagesLockedManager {
    constructor() {
        this.init();
    }

    init() {
        this.disableInteractiveElements();
        this.showLockedStateIndicators();
        this.bindEvents();
        this.modifyChatInterface();
    }

    disableInteractiveElements() {
        // Disable chat input
        const messageInput = document.getElementById('messageInput');
        if (messageInput) {
            messageInput.disabled = true;
            messageInput.placeholder = 'Cửa hàng đang bị khóa. Không thể gửi tin nhắn.';
            messageInput.style.opacity = '0.6';
            messageInput.style.cursor = 'not-allowed';
        }

        // Disable send button
        const sendButton = document.getElementById('sendButton');
        if (sendButton) {
            sendButton.disabled = true;
            sendButton.style.opacity = '0.5';
            sendButton.style.cursor = 'not-allowed';
        }

        // Disable file attachment
        const fileInput = document.getElementById('fileInput');
        const attachmentButton = document.querySelector('.btn-attachment');
        if (fileInput && attachmentButton) {
            fileInput.disabled = true;
            attachmentButton.style.opacity = '0.5';
            attachmentButton.style.cursor = 'not-allowed';
        }

        // Disable search chat
        const searchChat = document.getElementById('searchChat');
        if (searchChat) {
            searchChat.disabled = true;
            searchChat.placeholder = 'Không thể tìm kiếm khi cửa hàng bị khóa';
            searchChat.style.opacity = '0.6';
        }
    }

    showLockedStateIndicators() {
        // Thêm indicator cho khu vực chat input
        this.addLockedIndicator('.chat-input-area', 'Cửa hàng bị khóa - Chỉ có thể đọc tin nhắn');
        
        // Thêm indicator cho search
        this.addLockedIndicator('.search-chat', 'Tìm kiếm bị vô hiệu hóa');
        
        // Thêm thông báo trong chat area
        this.addChatLockedMessage();
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

    addChatLockedMessage() {
        const chatActivePanel = document.getElementById('chatActivePanel');
        if (chatActivePanel) {
            const lockedMessage = document.createElement('div');
            lockedMessage.className = 'chat-locked-message alert alert-warning';
            lockedMessage.innerHTML = `
                <i class="fas fa-exclamation-triangle me-2"></i>
                <strong>Cửa hàng đang bị khóa:</strong> Bạn chỉ có thể đọc tin nhắn, không thể gửi tin nhắn mới hoặc đính kèm file.
            `;
            
            // Chèn thông báo vào trước chat messages
            const chatMessages = document.getElementById('chatMessages');
            if (chatMessages && chatMessages.parentNode) {
                chatMessages.parentNode.insertBefore(lockedMessage, chatMessages);
            }
        }
    }

    modifyChatInterface() {
        // Thêm CSS class để chỉ ra trạng thái read-only
        const chatInputArea = document.querySelector('.chat-input-area');
        if (chatInputArea) {
            chatInputArea.classList.add('chat-input-locked');
        }

        const chatListPanel = document.querySelector('.chat-list-panel');
        if (chatListPanel) {
            chatListPanel.classList.add('chat-list-locked');
        }
    }

    bindEvents() {
        // Ngăn chặn gửi tin nhắn
        const sendButton = document.getElementById('sendButton');
        if (sendButton) {
            sendButton.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                this.showLockedMessage();
            });
        }

        // Ngăn chặn nhập tin nhắn
        const messageInput = document.getElementById('messageInput');
        if (messageInput) {
            messageInput.addEventListener('keydown', (e) => {
                if (e.key === 'Enter' || e.keyCode === 13) {
                    e.preventDefault();
                    e.stopPropagation();
                    this.showLockedMessage();
                }
            });

            messageInput.addEventListener('focus', (e) => {
                e.preventDefault();
                this.showLockedMessage();
            });
        }

        // Ngăn chặn đính kèm file
        const attachmentButton = document.querySelector('.btn-attachment');
        if (attachmentButton) {
            attachmentButton.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                this.showLockedMessage();
            });
        }

        const fileInput = document.getElementById('fileInput');
        if (fileInput) {
            fileInput.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
            });
        }

        // Ngăn chặn tìm kiếm
        const searchChat = document.getElementById('searchChat');
        if (searchChat) {
            searchChat.addEventListener('input', (e) => {
                e.preventDefault();
                e.stopPropagation();
            });

            searchChat.addEventListener('focus', (e) => {
                this.showLockedMessage();
            });
        }
    }

    showLockedMessage() {
        this.showToast('warning', 
            'Cửa hàng của bạn đang bị khóa. Bạn chỉ có thể đọc tin nhắn, không thể gửi tin nhắn mới.', 
            4000
        );
    }

    showToast(type, message, duration = 3000) {
        // Tạo toast container nếu chưa có
        let toastContainer = document.getElementById('toastContainer');
        if (!toastContainer) {
            toastContainer = document.createElement('div');
            toastContainer.id = 'toastContainer';
            toastContainer.className = 'toast-container position-fixed top-0 end-0 p-3';
            document.body.appendChild(toastContainer);
        }
        
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
        font-size: 1.5rem;
        margin-bottom: 8px;
        display: block;
        color: #dc3545;
    }
    
    .locked-overlay span {
        font-weight: 600;
        font-size: 0.8rem;
    }

    /* Chat locked specific styles */
    .chat-input-locked {
        background-color: #f8f9fa;
        border-top: 2px solid #e9ecef;
    }

    .chat-locked-message {
        margin: 10px;
        padding: 12px;
        border-radius: 8px;
        font-size: 0.9rem;
    }

    .chat-list-locked {
        opacity: 0.8;
    }

    /* Disabled input styles */
    #messageInput:disabled {
        background-color: #f8f9fa;
        color: #6c757d;
    }

    .btn-attachment:disabled, 
    .btn-send:disabled {
        background-color: #e9ecef;
        border-color: #dee2e6;
    }

    /* Search disabled state */
    .search-chat input:disabled {
        background-color: #f8f9fa;
        color: #6c757d;
    }
`;

// Inject styles
const styleSheet = document.createElement('style');
styleSheet.textContent = lockedStyles;
document.head.appendChild(styleSheet);

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    window.messagesLockedManager = new MessagesLockedManager();
});