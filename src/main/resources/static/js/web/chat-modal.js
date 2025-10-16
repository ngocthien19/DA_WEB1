// Chat Modal Functionality
class ChatModal {
    constructor() {
        this.modal = document.getElementById('chatModal');
        this.toggleBtn = document.getElementById('chatToggleBtn');
        this.minimizedChat = document.getElementById('minimizedChat');
        this.currentStoreId = null;
        this.stompClient = null;
        this.sentMessageIds = new Set();
        this.totalUnreadCount = 0;
        
        this.init();
    }
    
    init() {
        this.bindEvents();
        this.loadStores();
        this.connectWebSocket();
    }
    
    bindEvents() {
        // Toggle button
        this.toggleBtn.addEventListener('click', () => this.toggleModal());
        
        // Modal controls
        document.querySelector('.chat-btn-close').addEventListener('click', () => this.closeModal());
        document.querySelector('.chat-btn-minimize').addEventListener('click', () => this.minimizeModal());
        document.querySelector('.btn-close-minimized').addEventListener('click', () => this.closeModal());
        document.querySelector('.btn-restore-chat').addEventListener('click', () => this.restoreModal());
        
        // Chat functionality
        document.getElementById('chatSendButton').addEventListener('click', () => this.sendMessage());
        document.getElementById('chatMessageInput').addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                this.sendMessage();
            }
        });
        
        // File upload
        document.getElementById('chatFileInput').addEventListener('change', (e) => this.handleFileUpload(e));
        document.querySelector('.btn-attachment').addEventListener('click', () => {
            document.getElementById('chatFileInput').click();
        });
        
        // Store search
        document.getElementById('chatStoreSearch').addEventListener('input', (e) => {
            this.filterStores(e.target.value);
        });
        
        // Auto-resize textarea
        document.getElementById('chatMessageInput').addEventListener('input', function() {
            this.style.height = 'auto';
            this.style.height = (this.scrollHeight) + 'px';
        });
    }
    
    toggleModal() {
        if (this.modal.style.display === 'block') {
            this.closeModal();
        } else {
            this.openModal();
        }
    }
    
    openModal() {
        this.modal.style.display = 'block';
        this.minimizedChat.style.display = 'none';
        this.toggleBtn.style.display = 'none';
    }
    
    closeModal() {
        this.modal.style.display = 'none';
        this.minimizedChat.style.display = 'none';
        this.toggleBtn.style.display = 'flex';
    }
    
    minimizeModal() {
        this.modal.style.display = 'none';
        this.minimizedChat.style.display = 'block';
        this.toggleBtn.style.display = 'none';
    }
    
    restoreModal() {
        this.openModal();
    }
    
	async loadStores() {
	    const storeList = document.getElementById('storeList');
	    
	    try {
	        // Hiển thị loading
	        storeList.innerHTML = `
	            <div class="loading-stores">
	                <i class="fa-solid fa-spinner fa-spin"></i>
	                <p>Đang tải cửa hàng...</p>
	            </div>
	        `;

	        const response = await fetch('/chat/api/stores/chat-list');
	        
	        if (!response.ok) {
	            throw new Error(`HTTP error! status: ${response.status}`);
	        }
	        
	        const stores = await response.json();
	        
	        storeList.innerHTML = '';
	        
	        if (!stores || stores.length === 0) {
	            storeList.innerHTML = `
	                <div class="loading-stores">
	                    <i class="fa-solid fa-store-slash"></i>
	                    <p>Không có cửa hàng nào để chat</p>
	                </div>
	            `;
	            return;
	        }
	        
	        stores.forEach(store => {
	            const storeItem = this.createStoreItem(store);
	            storeList.appendChild(storeItem);
	        });
	        
	    } catch (error) {
	        console.error('Error loading stores:', error);
	        storeList.innerHTML = `
	            <div class="loading-stores" style="color: #e53e3e;">
	                <i class="fa-solid fa-exclamation-triangle"></i>
	                <p>Không thể tải danh sách cửa hàng</p>
	                <small>Lỗi: ${error.message}</small>
	            </div>
	        `;
	    }
	}
    
	createStoreItem(store) {
	    const div = document.createElement('div');
	    div.className = 'store-item';
	    div.dataset.storeId = store.maCuaHang;
	    div.dataset.vendorId = store.vendorId; // Thêm vendorId vào data attribute
	    
	    const avatar = store.hinhAnh ? `/uploads/stores/${store.hinhAnh}` : '/images/default-store.png';
	    const lastMessage = store.lastMessage || 'Chưa có tin nhắn';
	    const lastTime = store.lastMessageTime ? this.formatTime(store.lastMessageTime) : '';
	    const unreadBadge = store.unreadCount > 0 ? 
	        `<span class="unread-count">${store.unreadCount}</span>` : '';
	    
	    div.innerHTML = `
	        <img src="${avatar}" alt="${store.tenCuaHang}" class="store-avatar" 
	             onerror="this.src='/images/default-store.png'">
	        <div class="store-info">
	            <div class="store-name">${store.tenCuaHang}</div>
	            <div class="last-message">${lastMessage}</div>
	            <div class="store-meta">
	                <div class="store-time">${lastTime}</div>
	                ${unreadBadge}
	            </div>
	        </div>
	    `;
	    
	    div.addEventListener('click', () => {
	        this.selectStore(store);
	    });
	    
	    return div;
	}
    
    selectStore(store) {
        this.currentStoreId = store.maCuaHang;
        
        // Update active state
        document.querySelectorAll('.store-item').forEach(item => {
            item.classList.remove('active');
        });
        document.querySelector(`[data-store-id="${store.maCuaHang}"]`).classList.add('active');
        
        // Update UI
        document.getElementById('noStoreSelected').style.display = 'none';
        document.getElementById('chatActiveArea').style.display = 'flex';
        
        // Update store info
        document.getElementById('currentStoreName').textContent = store.tenCuaHang;
        document.getElementById('currentStoreAvatar').src = store.hinhAnh ? 
            `/uploads/stores/${store.hinhAnh}` : '/images/default-store.png';
        document.getElementById('typingStoreName').textContent = store.tenCuaHang;
        document.getElementById('minimizedStoreName').textContent = store.tenCuaHang;
        
        // Load chat history
        this.loadChatHistory(store.maCuaHang);
        
        // Clear unread count for this store
        this.clearStoreUnread(store.maCuaHang);
    }
    
    async loadChatHistory(storeId) {
        const chatMessages = document.getElementById('chatMessages');
        chatMessages.innerHTML = '<div class="loading-stores"><i class="fa-solid fa-spinner fa-spin"></i> Đang tải tin nhắn...</div>';
        
        try {
            const response = await fetch(`/chat/history/${storeId}/vendor`);
            const messages = await response.json();
            
            chatMessages.innerHTML = '';
            
            if (messages.length === 0) {
                chatMessages.innerHTML = `
                    <div class="welcome-message">
                        <i class="fa-solid fa-comments"></i>
                        <h4>Bắt đầu cuộc trò chuyện</h4>
                        <p>Chào bạn! Chúng tôi có thể giúp gì cho bạn?</p>
                    </div>
                `;
            } else {
                messages.forEach(message => {
                    this.displayMessage(message, false);
                });
                this.scrollToBottom();
            }
        } catch (error) {
            console.error('Error loading chat history:', error);
            chatMessages.innerHTML = '<div class="loading-stores" style="color: #e53e3e;">Không thể tải tin nhắn</div>';
        }
    }
    
    displayMessage(message, animate = true) {
        const chatMessages = document.getElementById('chatMessages');
        const isSent = message.maNguoiGui === this.getCurrentUserId();
        
        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${isSent ? 'sent' : 'received'}`;
        if (animate) {
            messageDiv.style.animation = 'messageSlideIn 0.3s ease';
        }
        
        const time = new Date(message.thoiGian);
        const timeString = time.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
        
        messageDiv.innerHTML = `
            <div class="message-content">
                <div class="message-bubble">${message.noiDung}</div>
                <div class="message-time">${timeString}</div>
            </div>
        `;
        
        chatMessages.appendChild(messageDiv);
    }
    
    sendMessage() {
        if (!this.currentStoreId) {
            this.showToast('Vui lòng chọn cửa hàng để chat', 'error');
            return;
        }
        
        const messageInput = document.getElementById('chatMessageInput');
        const messageContent = messageInput.value.trim();
        
        if (messageContent === '' || !this.stompClient) {
            return;
        }
        
        const chatMessage = {
            maNguoiGui: this.getCurrentUserId(),
            maNguoiNhan: this.getVendorId(this.currentStoreId),
            maCuaHang: this.currentStoreId,
            noiDung: messageContent,
            thoiGian: new Date(),
            type: 'text'
        };
        
        // Display message immediately
        this.displayMessage(chatMessage, true);
        this.scrollToBottom();
        
        // Send via WebSocket
        this.stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(chatMessage));
        
        messageInput.value = '';
        messageInput.style.height = 'auto';
        messageInput.focus();
    }
    
    connectWebSocket() {
        const socket = new SockJS('/ws');
        this.stompClient = Stomp.over(socket);
        
        this.stompClient.connect({}, (frame) => {
            console.log('Chat Modal WebSocket Connected');
            
            // Subscribe to messages
            this.stompClient.subscribe(`/user/${this.getCurrentUserId()}/queue/messages`, (message) => {
                const chatMessage = JSON.parse(message.body);
                this.handleIncomingMessage(chatMessage);
            });
            
            // Subscribe to typing notifications
            this.stompClient.subscribe(`/user/${this.getCurrentUserId()}/queue/typing`, (message) => {
                const typingInfo = JSON.parse(message.body);
                this.showTypingIndicator(typingInfo.isTyping);
            });
            
        }, (error) => {
            console.error('WebSocket connection error:', error);
            setTimeout(() => this.connectWebSocket(), 5000);
        });
    }
    
    handleIncomingMessage(chatMessage) {
        const msgId = chatMessage.maTinNhan || `${chatMessage.maNguoiGui}-${Date.now()}`;
        
        if (this.sentMessageIds.has(msgId)) {
            return;
        }
        
        // If message is for current store, display it
        if (this.currentStoreId && chatMessage.maCuaHang === this.currentStoreId) {
            this.displayMessage(chatMessage, true);
            this.scrollToBottom();
            this.sentMessageIds.add(msgId);
        }
        
        // Update unread count
        if (chatMessage.maNguoiGui !== this.getCurrentUserId()) {
            this.incrementUnreadCount();
            this.updateStoreUnread(chatMessage.maCuaHang);
        }
    }
    
    showTypingIndicator(show) {
        const typingIndicator = document.getElementById('typingIndicator');
        typingIndicator.style.display = show ? 'block' : 'none';
        if (show) {
            this.scrollToBottom();
        }
    }
    
    async handleFileUpload(event) {
        const file = event.target.files[0];
        if (!file) return;
        
        if (!this.currentStoreId) {
            this.showToast('Vui lòng chọn cửa hàng để gửi file', 'error');
            return;
        }
        
        const formData = new FormData();
        formData.append('file', file);
        
        try {
            const response = await fetch('/files/api/upload/chat', {
                method: 'POST',
                body: formData
            });
            
            const data = await response.json();
            
            if (data.success) {
                const chatMessage = {
                    maNguoiGui: this.getCurrentUserId(),
                    maNguoiNhan: this.getVendorId(this.currentStoreId),
                    maCuaHang: this.currentStoreId,
                    noiDung: file.name,
                    fileUrl: data.fileName,
                    thoiGian: new Date(),
                    type: 'file'
                };
                
                this.stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(chatMessage));
                this.showToast('Upload file thành công', 'success');
            } else {
                this.showToast('Upload file thất bại', 'error');
            }
        } catch (error) {
            console.error('Upload error:', error);
            this.showToast('Không thể upload file', 'error');
        }
        
        event.target.value = '';
    }
    
    filterStores(keyword) {
        const storeItems = document.querySelectorAll('.store-item');
        const searchTerm = keyword.toLowerCase();
        
        storeItems.forEach(item => {
            const storeName = item.querySelector('.store-name').textContent.toLowerCase();
            if (storeName.includes(searchTerm)) {
                item.style.display = 'flex';
            } else {
                item.style.display = 'none';
            }
        });
    }
    
    incrementUnreadCount() {
        this.totalUnreadCount++;
        this.updateUnreadBadge();
    }
    
    updateUnreadBadge() {
        const badge = document.getElementById('chatUnreadBadge');
        const minimizedBadge = document.getElementById('minimizedUnread');
        const minimizedCount = document.getElementById('minimizedUnreadCount');
        
        if (this.totalUnreadCount > 0) {
            badge.textContent = this.totalUnreadCount;
            badge.style.display = 'block';
            
            minimizedCount.textContent = this.totalUnreadCount;
            minimizedBadge.style.display = 'block';
            
            // Update page title
            const originalTitle = document.title.replace(/^\(\d+\)\s*/, '');
            document.title = `(${this.totalUnreadCount}) ${originalTitle}`;
        } else {
            badge.style.display = 'none';
            minimizedBadge.style.display = 'none';
            document.title = document.title.replace(/^\(\d+\)\s*/, '');
        }
    }
    
    updateStoreUnread(storeId) {
        const storeItem = document.querySelector(`[data-store-id="${storeId}"]`);
        if (storeItem) {
            let unreadBadge = storeItem.querySelector('.unread-count');
            if (!unreadBadge) {
                unreadBadge = document.createElement('span');
                unreadBadge.className = 'unread-count';
                storeItem.querySelector('.store-meta').appendChild(unreadBadge);
            }
            
            const currentCount = parseInt(unreadBadge.textContent) || 0;
            unreadBadge.textContent = currentCount + 1;
        }
    }
    
    clearStoreUnread(storeId) {
        const storeItem = document.querySelector(`[data-store-id="${storeId}"]`);
        if (storeItem) {
            const unreadBadge = storeItem.querySelector('.unread-count');
            if (unreadBadge) {
                const removedCount = parseInt(unreadBadge.textContent) || 0;
                this.totalUnreadCount = Math.max(0, this.totalUnreadCount - removedCount);
                unreadBadge.remove();
                this.updateUnreadBadge();
            }
        }
    }
    
    scrollToBottom() {
        const container = document.querySelector('.chat-messages-container');
        if (container) {
            container.scrollTop = container.scrollHeight;
        }
    }
    
    formatTime(dateString) {
        if (!dateString) return '';
        
        const date = new Date(dateString);
        const now = new Date();
        const diff = now - date;
        const minutes = Math.floor(diff / 60000);
        const hours = Math.floor(diff / 3600000);
        const days = Math.floor(diff / 86400000);
        
        if (minutes < 1) return 'Vừa xong';
        if (minutes < 60) return `${minutes} phút`;
        if (hours < 24) return `${hours} giờ`;
        if (days < 7) return `${days} ngày`;
        
        return date.toLocaleDateString('vi-VN');
    }
    
    showToast(message, type = 'info') {
        // Simple toast implementation
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        toast.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            background: white;
            padding: 12px 20px;
            border-radius: 8px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            z-index: 10001;
            border-left: 4px solid ${type === 'error' ? '#e53e3e' : type === 'success' ? '#38a169' : '#667eea'};
        `;
        toast.textContent = message;
        
        document.body.appendChild(toast);
        
        setTimeout(() => {
            toast.remove();
        }, 3000);
    }
    
	getCurrentUserId() {
	    // Cách 1: Lấy từ hidden input (nếu có)
	    const currentUserIdInput = document.getElementById('currentUserId');
	    if (currentUserIdInput) {
	        return parseInt(currentUserIdInput.value);
	    }
	    
	    // Cách 2: Lấy từ sessionStorage/localStorage (nếu bạn lưu thông tin đăng nhập)
	    const userData = localStorage.getItem('userData');
	    if (userData) {
	        const user = JSON.parse(userData);
	        return user.maNguoiDung;
	    }
	    
	    // Cách 3: Nếu không có, trả về null (sẽ cần xử lý đăng nhập)
	    console.error('Không tìm thấy thông tin người dùng');
	    return null;
	}
    
	getVendorId(storeId) {
	    // Lấy vendorId từ data attribute của store item
	    const storeItem = document.querySelector(`[data-store-id="${storeId}"]`);
	    return storeItem ? parseInt(storeItem.dataset.vendorId) : null;
	}
}

// Initialize chat modal when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.chatModal = new ChatModal();
});