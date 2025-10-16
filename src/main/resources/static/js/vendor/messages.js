// Vendor Messages JavaScript - FIXED VERSION
let stompClient = null;
let vendorId = null;
let storeId = null;
let currentCustomerId = null;
let isTyping = false;
let typingTimeout = null;
let sentMessageIds = new Set(); // Track tin nhắn đã gửi để tránh duplicate

// Khởi tạo khi trang load
document.addEventListener('DOMContentLoaded', function() {
    vendorId = document.getElementById('vendorId').value;
    storeId = document.getElementById('storeId').value;
    
    console.log('🔧 Vendor ID:', vendorId, 'Store ID:', storeId);
    
    // Kết nối WebSocket
    connect();
    
    // Load danh sách người chat
    loadChatList();
    
    // Xử lý gửi tin nhắn
    const sendButton = document.getElementById('sendButton');
    const messageInput = document.getElementById('messageInput');
    
    if (sendButton) {
        sendButton.addEventListener('click', sendMessage);
    }
    
    if (messageInput) {
        messageInput.addEventListener('keydown', function(e) {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                sendMessage();
            }
            handleTyping();
        });
        
        // Auto-resize textarea
        messageInput.addEventListener('input', function() {
            this.style.height = 'auto';
            this.style.height = (this.scrollHeight) + 'px';
        });
    }
    
    // Xử lý upload file
    const fileInput = document.getElementById('fileInput');
    const btnAttachment = document.querySelector('.btn-attachment');
    
    if (btnAttachment) {
        btnAttachment.addEventListener('click', function() {
            fileInput.click();
        });
    }
    
    if (fileInput) {
        fileInput.addEventListener('change', handleFileUpload);
    }
    
    // Search chat
    const searchChat = document.getElementById('searchChat');
    if (searchChat) {
        searchChat.addEventListener('input', function() {
            const keyword = this.value.toLowerCase();
            const chatItems = document.querySelectorAll('.chat-list-item');
            
            chatItems.forEach(item => {
                const name = item.querySelector('.name').textContent.toLowerCase();
                if (name.includes(keyword)) {
                    item.style.display = 'flex';
                } else {
                    item.style.display = 'none';
                }
            });
        });
    }
});

// Kết nối WebSocket
function connect() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    
    stompClient.connect({}, function(frame) {
        console.log('✅ WebSocket Connected:', frame);
        
        // Subscribe để nhận tin nhắn - FIXED LOGIC
        stompClient.subscribe('/user/' + vendorId + '/queue/messages', function(message) {
            const chatMessage = JSON.parse(message.body);
            console.log('📩 Received message:', chatMessage);
            console.log('   Current customer ID:', currentCustomerId);
            
            // Tạo unique ID cho tin nhắn để tránh duplicate
            const msgId = chatMessage.maTinNhan || `${chatMessage.maNguoiGui}-${Date.now()}`;
            
            // Kiểm tra đã hiển thị chưa
            if (sentMessageIds.has(msgId)) {
                console.log('   ⚠️ Message already displayed, skipping...');
                return;
            }
            
            // So sánh với String để tránh lỗi type mismatch
            const currentCustIdStr = String(currentCustomerId);
            const nguoiGuiStr = String(chatMessage.maNguoiGui);
            const nguoiNhanStr = String(chatMessage.maNguoiNhan);
            const vendorIdStr = String(vendorId);
            
            console.log('   Current Customer:', currentCustIdStr, 'Sender:', nguoiGuiStr, 'Receiver:', nguoiNhanStr);
            
            // HIỂN THỊ TIN NHẮN NẾU:
            // 1. Tin nhắn từ customer hiện tại gửi cho vendor
            // 2. Tin nhắn từ vendor gửi cho customer hiện tại
            const isRelevantToCurrentChat = currentCustomerId && 
                ((nguoiGuiStr === currentCustIdStr && nguoiNhanStr === vendorIdStr) ||
                 (nguoiGuiStr === vendorIdStr && nguoiNhanStr === currentCustIdStr));
            
            if (isRelevantToCurrentChat) {
                console.log('   ✅ Displaying message in current chat');
                displayMessage(chatMessage, true);
                scrollToBottom();
                sentMessageIds.add(msgId);
                
                // Đánh dấu đã đọc nếu là tin nhắn từ customer
                if (nguoiGuiStr === currentCustIdStr) {
                    console.log('   📖 Marking as read');
                    markAsRead(currentCustomerId);
                }
                
                // Phát âm thanh thông báo nếu không phải tin nhắn của mình
                if (nguoiGuiStr !== vendorIdStr) {
                    console.log('   🔔 Playing notification sound');
                    playNotificationSound();
                }
            } else {
                console.log('   ℹ️ Message not for current chat, updating list');
                // Vẫn phát âm thanh nếu là tin nhắn mới từ customer khác
                if (nguoiGuiStr !== vendorIdStr) {
                    playNotificationSound();
                }
            }
            
            // Luôn cập nhật danh sách chat khi có tin nhắn mới
            const customerIdToUpdate = nguoiGuiStr === vendorIdStr ? chatMessage.maNguoiNhan : chatMessage.maNguoiGui;
            updateChatListItem(customerIdToUpdate);
        });
        
        // Subscribe để nhận thông báo đang gõ
        stompClient.subscribe('/user/' + vendorId + '/queue/typing', function(message) {
            const typingInfo = JSON.parse(message.body);
            console.log('⌨️ Received typing event:', typingInfo);
            if (currentCustomerId && typingInfo.nguoiGuiId == currentCustomerId) {
                showTypingIndicator(typingInfo.isTyping);
            }
        });
        
        // Subscribe để nhận lỗi
        stompClient.subscribe('/user/' + vendorId + '/queue/errors', function(message) {
            const error = JSON.parse(message.body);
            showToast('error', error.error);
        });
        
    }, function(error) {
        console.error('❌ WebSocket connection error:', error);
        showToast('error', 'Không thể kết nối đến server chat. Đang thử kết nối lại...');
        setTimeout(connect, 5000);
    });
}

// Load danh sách người đã chat
function loadChatList() {
    const chatList = document.getElementById('chatList');
    chatList.innerHTML = '<div class="loading-chat-list"><i class="fa-solid fa-spinner fa-spin"></i> Đang tải danh sách...</div>';
    
    fetch(`/chat/partners/${storeId}`)
        .then(response => response.json())
        .then(partners => {
            chatList.innerHTML = '';
            
            if (partners.length === 0) {
                chatList.innerHTML = '<div class="loading-chat-list">Chưa có tin nhắn nào</div>';
            } else {
                partners.forEach(partner => {
                    const chatItem = createChatListItem(partner);
                    chatList.appendChild(chatItem);
                });
            }
        })
        .catch(error => {
            console.error('Error loading chat list:', error);
            chatList.innerHTML = '<div class="loading-chat-list" style="color: #ef4444;">Không thể tải danh sách</div>';
        });
}

// Tạo item trong danh sách chat
function createChatListItem(partner) {
    const div = document.createElement('div');
    div.className = 'chat-list-item';
    div.dataset.userId = partner.maNguoiDung;
    
    const avatar = partner.hinhAnh ? `/uploads/users/${partner.hinhAnh}` : '/images/default-avatar.png';
    const lastMessage = partner.lastMessage || 'Chưa có tin nhắn';
    const lastTime = partner.lastMessageTime ? formatTime(new Date(partner.lastMessageTime)) : '';
    const unreadBadge = partner.unreadCount > 0 ? `<span class="unread-badge">${partner.unreadCount}</span>` : '';
    
    div.innerHTML = `
        <img src="${avatar}" alt="${partner.hoTen}" class="avatar" onerror="this.onerror=null; this.src='/images/default-avatar.png';">
        <div class="chat-info">
            <div class="name">
                <span>${partner.hoTen}</span>
                ${unreadBadge}
            </div>
            <div class="last-message">${lastMessage}</div>
        </div>
        <div class="time">${lastTime}</div>
    `;
    
    div.addEventListener('click', function() {
        selectChat(partner.maNguoiDung, partner.hoTen, avatar);
    });
    
    return div;
}

// Chọn một cuộc trò chuyện
function selectChat(customerId, customerName, avatar) {
    currentCustomerId = customerId;
    console.log('💬 Selected chat with customer:', customerId, customerName);
    
    // Cập nhật active state
    document.querySelectorAll('.chat-list-item').forEach(item => {
        item.classList.remove('active');
    });
    const selectedItem = document.querySelector(`[data-user-id="${customerId}"]`);
    if (selectedItem) {
        selectedItem.classList.add('active');
    }
    
    // Ẩn no-chat-selected, hiện chat panel
    document.getElementById('noChatSelected').style.display = 'none';
    document.getElementById('chatActivePanel').style.display = 'flex';
    
    // Cập nhật thông tin khách hàng
    document.getElementById('customerName').textContent = customerName;
    document.getElementById('customerAvatar').src = avatar;
    document.getElementById('typingUserName').textContent = customerName;
    
    // Clear sentMessageIds khi chuyển chat để load lại tin nhắn
    sentMessageIds.clear();
    
    // Load lịch sử chat
    loadChatHistory(customerId);
    
    // Đánh dấu đã đọc
    markAsRead(customerId);
}

// Load lịch sử chat với khách hàng
function loadChatHistory(customerId) {
    const chatMessages = document.getElementById('chatMessages');
    chatMessages.innerHTML = '<div class="loading-messages"><i class="fa-solid fa-spinner fa-spin"></i> Đang tải tin nhắn...</div>';
    
    fetch(`/chat/history/${storeId}/${customerId}`)
        .then(response => response.json())
        .then(messages => {
            chatMessages.innerHTML = '';
            
            if (messages.length === 0) {
                chatMessages.innerHTML = '<div class="loading-messages">Chưa có tin nhắn nào</div>';
            } else {
                let lastDate = null;
                messages.forEach(message => {
                    const messageDate = new Date(message.thoiGian).toLocaleDateString('vi-VN');
                    if (messageDate !== lastDate) {
                        chatMessages.innerHTML += createDateDivider(messageDate);
                        lastDate = messageDate;
                    }
                    
                    // Thêm message ID vào Set để tránh duplicate khi nhận real-time
                    const msgId = message.maTinNhan || `${message.maNguoiGui}-${message.thoiGian}`;
                    sentMessageIds.add(msgId);
                    
                    displayMessage(message, false);
                });
                scrollToBottom();
            }
        })
        .catch(error => {
            console.error('Error loading chat history:', error);
            chatMessages.innerHTML = '<div class="loading-messages" style="color: #ef4444;">Không thể tải tin nhắn</div>';
        });
}

// Gửi tin nhắn
function sendMessage() {
    if (!currentCustomerId) {
        showToast('error', 'Vui lòng chọn khách hàng để chat');
        return;
    }
    
    const messageInput = document.getElementById('messageInput');
    const messageContent = messageInput.value.trim();
    
    if (messageContent === '' || !stompClient) {
        return;
    }
    
    const chatMessage = {
        maNguoiGui: parseInt(vendorId),
        maNguoiNhan: parseInt(currentCustomerId),
        maCuaHang: parseInt(storeId),
        noiDung: messageContent,
        thoiGian: new Date(),
        type: 'text'
    };
    
    console.log('📤 Sending message:', chatMessage);
    
    // Hiển thị tin nhắn ngay lập tức (optimistic UI)
    const tempId = `temp-${Date.now()}`;
    sentMessageIds.add(tempId);
    displayMessage(chatMessage, true);
    scrollToBottom();
    
    // Gửi qua WebSocket
    stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(chatMessage));
    
    messageInput.value = '';
    messageInput.style.height = 'auto';
    messageInput.focus();
}

// Hiển thị tin nhắn
function displayMessage(message, animate = true) {
    const chatMessages = document.getElementById('chatMessages');
    const isSent = message.maNguoiGui == vendorId;
    
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${isSent ? 'sent' : 'received'}`;
    if (animate) {
        messageDiv.style.animation = 'slideIn 0.3s ease';
    }
    
    const time = new Date(message.thoiGian);
    const timeString = time.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
    
    let fileHtml = '';
    if (message.fileUrl) {
        const fileExt = message.fileUrl.split('.').pop().toLowerCase();
        if (['jpg', 'jpeg', 'png', 'gif'].includes(fileExt)) {
            fileHtml = `
                <div class="message-file">
                    <img src="/uploads/chats/${message.fileUrl}" alt="Hình ảnh" onclick="openImage(this.src)">
                </div>
            `;
        } else {
            fileHtml = `
                <div class="message-file">
                    <a href="/uploads/chats/${message.fileUrl}" download>
                        <i class="fa-solid fa-file"></i>
                        <span>Tải xuống file</span>
                    </a>
                </div>
            `;
        }
    }
    
    messageDiv.innerHTML = `
        <div class="message-content">
            ${!isSent ? `<div class="message-sender">${message.tenNguoiGui || 'Khách hàng'}</div>` : ''}
            <div class="message-bubble">
                ${message.noiDung}
                ${fileHtml}
            </div>
            <div class="message-time">${timeString}</div>
        </div>
    `;
    
    chatMessages.appendChild(messageDiv);
}

// Tạo date divider
function createDateDivider(dateString) {
    return `
        <div class="date-divider">
            <span>${dateString}</span>
        </div>
    `;
}

// Xử lý đang gõ
function handleTyping() {
    if (!currentCustomerId) return;
    
    if (!isTyping) {
        isTyping = true;
        sendTypingStatus(true);
    }
    
    clearTimeout(typingTimeout);
    typingTimeout = setTimeout(function() {
        isTyping = false;
        sendTypingStatus(false);
    }, 1000);
}

// Gửi trạng thái đang gõ
function sendTypingStatus(typing) {
    if (stompClient && currentCustomerId) {
        const typingInfo = {
            nguoiGuiId: parseInt(vendorId),
            nguoiNhanId: parseInt(currentCustomerId),
            isTyping: typing
        };
        stompClient.send("/app/chat.typing", {}, JSON.stringify(typingInfo));
    }
}

// Hiển thị typing indicator
function showTypingIndicator(show) {
    const typingIndicator = document.getElementById('typingIndicator');
    if (typingIndicator) {
        typingIndicator.style.display = show ? 'block' : 'none';
        if (show) {
            scrollToBottom();
        }
    }
}

// Xử lý upload file
function handleFileUpload(event) {
    const file = event.target.files[0];
    if (!file) return;
    
    if (!currentCustomerId) {
        showToast('error', 'Vui lòng chọn khách hàng để gửi file');
        return;
    }
    
    if (file.size > 10 * 1024 * 1024) {
        showToast('error', 'File quá lớn. Vui lòng chọn file nhỏ hơn 10MB');
        return;
    }
    
    const formData = new FormData();
    formData.append('file', file);
    
    showToast('info', 'Đang upload file...');
    
    fetch('/files/api/upload/chat', {
        method: 'POST',
        body: formData
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            const chatMessage = {
                maNguoiGui: parseInt(vendorId),
                maNguoiNhan: parseInt(currentCustomerId),
                maCuaHang: parseInt(storeId),
                noiDung: file.name,
                fileUrl: data.fileName,
                thoiGian: new Date(),
                type: 'file'
            };
            
            stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(chatMessage));
            showToast('success', 'Upload file thành công');
        } else {
            showToast('error', 'Upload file thất bại: ' + data.message);
        }
    })
    .catch(error => {
        console.error('Upload error:', error);
        showToast('error', 'Không thể upload file. Vui lòng thử lại.');
    });
    
    event.target.value = '';
}

// Đánh dấu đã đọc
function markAsRead(customerId) {
    // Gọi API đánh dấu đã đọc
    fetch(`/chat/mark-read/${storeId}/${customerId}`, {
        method: 'POST'
    }).catch(error => console.error('Error marking as read:', error));
    
    // Xóa unread badge trong danh sách
    const chatItem = document.querySelector(`[data-user-id="${customerId}"]`);
    if (chatItem) {
        const badge = chatItem.querySelector('.unread-badge');
        if (badge) {
            badge.remove();
        }
    }
}

// Cập nhật chat list item khi có tin nhắn mới
function updateChatListItem(customerId) {
    // Reload lại danh sách chat
    loadChatList();
}

// Format time
function formatTime(date) {
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

// Scroll to bottom
function scrollToBottom() {
    const chatMessages = document.getElementById('chatMessages');
    if (chatMessages) {
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }
}

// Show toast notification
function showToast(type, message) {
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    
    const icon = type === 'success' ? 'fa-check-circle' : 
                 type === 'error' ? 'fa-exclamation-circle' : 
                 'fa-info-circle';
    
    toast.innerHTML = `
        <i class="fa-solid ${icon}"></i>
        <span>${message}</span>
    `;
    
    document.body.appendChild(toast);
    
    setTimeout(() => {
        toast.style.animation = 'slideInRight 0.3s ease reverse';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

// Play notification sound
function playNotificationSound() {
    const audio = new Audio('data:audio/wav;base64,UklGRnoGAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQoGAACBhYqFbF1fdJivrJBhNjVgodDbq2EcBj+a2/LDciUFLIHO8tiJNwgZaLvt559NEAxQp+PwtmMcBjiR1/LMeSwFJHfH8N2QQAoUXrTp66hVFApGn+DyvmwhBSuBzvLZiTYIG2m98OScTgwOUKzn77RgGwU7k9n0yHUqBSh+zPLaizsKGGS56+mmUxELTKXh8bllHAU2jdXyyXswBSh+zPLaizsKGGS56+mmUxELTKXh8bllHAU2jdXyyXswBSh+zPLaizsKGGS56+mmUxELTKXh8bllHAU2jdXyyXswBSh+zPLaizsKGGS56+mmUxELTKXh8bllHAU2jdXyyXswBSh+zPLaizsKGGS56+mmUxELTKXh8bllHAU2jdXyyXswBSh+zPLaizsKGGS56+mmUxELTKXh8bllHAU2jdXyyXswBSh+zPLaizsKGGS56+mmUxELTKXh8bllHAU2jdXyyXswBSh+zPLaizsKGGS56+mmUxELTKXh8bllHAU2jdXyyXswBSh+zPLaizsKGGS56+mmUxELTKXh8bllHAU2jdXyyXswBSh+zPLaizsKGGS56+mmUxELTKXh8bllHAU2jdXyyXswBQ==');
    audio.volume = 0.3;
    audio.play().catch(e => console.log('Audio play failed:', e));
}

// Mở ảnh trong modal (improved version)
function openImage(src) {
    const modal = document.createElement('div');
    modal.className = 'image-modal';
    modal.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0,0,0,0.95);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 10000;
    `;
    
    modal.innerHTML = `
        <button class="image-modal-close">
            <i class="fa-solid fa-times"></i>
        </button>
        <img src="${src}" style="max-width: 90%; max-height: 90%; border-radius: 12px; box-shadow: 0 10px 30px rgba(0,0,0,0.3);">
    `;
    
    const closeModal = function() {
        modal.style.animation = 'fadeOut 0.3s ease';
        setTimeout(() => modal.remove(), 300);
    };
    
    modal.querySelector('.image-modal-close').onclick = closeModal;
    modal.onclick = function(e) {
        if (e.target === modal) {
            closeModal();
        }
    };
    
    // Close with ESC key
    const escHandler = function(e) {
        if (e.key === 'Escape') {
            closeModal();
            document.removeEventListener('keydown', escHandler);
        }
    };
    document.addEventListener('keydown', escHandler);
    
    document.body.appendChild(modal);
    
    // Add fade in animation
    modal.style.animation = 'fadeIn 0.3s ease';
}
// Thêm keyframe animation cho modal
const style = document.createElement('style');
style.textContent = `
    @keyframes fadeIn {
        from { opacity: 0; }
        to { opacity: 1; }
    }
    @keyframes fadeOut {
        from { opacity: 1; }
        to { opacity: 0; }
    }
`;
document.head.appendChild(style);

// Disconnect khi đóng trang
window.addEventListener('beforeunload', function() {
    if (stompClient) {
        stompClient.disconnect();
    }
});

