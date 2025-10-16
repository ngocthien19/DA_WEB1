// Chat WebSocket functionality - FIXED VERSION
let stompClient = null;
let currentUserId = null;
let vendorId = null;
let storeId = null;
let isTyping = false;
let typingTimeout = null;
let sentMessageIds = new Set();

// Khởi tạo khi trang load
document.addEventListener('DOMContentLoaded', function() {
    // Lấy thông tin từ hidden inputs
    currentUserId = document.getElementById('currentUserId').value;
    vendorId = document.getElementById('vendorId').value;
    storeId = document.getElementById('storeId').value;
    
    console.log('🔧 User ID:', currentUserId, 'Vendor ID:', vendorId, 'Store ID:', storeId);
    
    // Kết nối WebSocket
    connect();
    
    // Load lịch sử chat
    loadChatHistory();
    
    // Xử lý gửi tin nhắn
    const sendButton = document.getElementById('sendButton');
    const messageInput = document.getElementById('messageInput');
    
    sendButton.addEventListener('click', sendMessage);
    
    messageInput.addEventListener('keydown', function(e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
        
        // Thông báo đang gõ
        handleTyping();
    });
    
    // Auto-resize textarea
    messageInput.addEventListener('input', function() {
        this.style.height = 'auto';
        this.style.height = (this.scrollHeight) + 'px';
    });
    
    // Xử lý upload file
    const fileInput = document.getElementById('fileInput');
    const btnAttachment = document.querySelector('.btn-attachment');
    
    btnAttachment.addEventListener('click', function() {
        fileInput.click();
    });
    
    fileInput.addEventListener('change', handleFileUpload);
});

// Kết nối WebSocket
function connect() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    
    stompClient.connect({}, function(frame) {
        console.log('✅ WebSocket Connected:', frame);
        
        // Subscribe để nhận tin nhắn - FIXED LOGIC
        stompClient.subscribe('/user/' + currentUserId + '/queue/messages', function(message) {
            const chatMessage = JSON.parse(message.body);
            console.log('📩 Received message:', chatMessage);
            
            // Tạo unique ID cho tin nhắn để tránh duplicate
            const msgId = chatMessage.maTinNhan || `${chatMessage.maNguoiGui}-${Date.now()}`;
            
            // Kiểm tra đã hiển thị chưa
            if (sentMessageIds.has(msgId)) {
                console.log('   ⚠️ Message already displayed, skipping...');
                return;
            }
            
            // So sánh với String để tránh lỗi type mismatch
            const currentUserIdStr = String(currentUserId);
            const nguoiGuiStr = String(chatMessage.maNguoiGui);
            const vendorIdStr = String(vendorId);
            
            console.log('   Current User:', currentUserIdStr, 'Sender:', nguoiGuiStr, 'Vendor:', vendorIdStr);
            
            // HIỂN THỊ TẤT CẢ TIN NHẮN LIÊN QUAN ĐẾN CUỘC HỘI THOẠI NÀY
            // Tin nhắn từ vendor gửi cho user hoặc từ user gửi cho vendor
            const isRelevantMessage = 
                (nguoiGuiStr === vendorIdStr && chatMessage.maNguoiNhan == currentUserId) ||
                (nguoiGuiStr === currentUserIdStr && chatMessage.maNguoiNhan == vendorId);
            
            if (isRelevantMessage) {
                console.log('   ✅ Displaying relevant message');
                displayMessage(chatMessage, true);
                scrollToBottom();
                sentMessageIds.add(msgId);
                
                // Phát âm thanh thông báo nếu là tin nhắn từ vendor
                if (nguoiGuiStr === vendorIdStr) {
                    console.log('   🔔 Playing notification sound');
                    playNotificationSound();
                }
            } else {
                console.log('   ❌ Message not relevant to this conversation');
            }
        });
        
        // Subscribe để nhận thông báo đang gõ
        stompClient.subscribe('/user/' + currentUserId + '/queue/typing', function(message) {
            const typingInfo = JSON.parse(message.body);
            console.log('⌨️ Received typing event:', typingInfo);
            showTypingIndicator(typingInfo.isTyping);
        });
        
        // Subscribe để nhận lỗi
        stompClient.subscribe('/user/' + currentUserId + '/queue/errors', function(message) {
            const error = JSON.parse(message.body);
            showToast('error', error.error);
        });
        
    }, function(error) {
        console.error('❌ WebSocket connection error:', error);
        showToast('error', 'Không thể kết nối đến server chat. Đang thử kết nối lại...');
        setTimeout(connect, 5000);
    });
}

// Load lịch sử chat
function loadChatHistory() {
    const chatMessages = document.getElementById('chatMessages');
    chatMessages.innerHTML = '<div class="loading-messages"><i class="fa-solid fa-spinner fa-spin"></i> Đang tải tin nhắn...</div>';
    
    fetch(`/chat/history/${storeId}/${vendorId}`)
        .then(response => response.json())
        .then(messages => {
            chatMessages.innerHTML = '';
            
            if (messages.length === 0) {
                chatMessages.innerHTML = `
                    <div class="welcome-message">
                        <i class="fa-solid fa-comments"></i>
                        <h3>Chào mừng đến với cửa hàng</h3>
                        <p>Bắt đầu cuộc trò chuyện với chúng tôi!</p>
                    </div>
                `;
            } else {
                let lastDate = null;
                messages.forEach(message => {
                    // Thêm date divider nếu ngày thay đổi
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
            chatMessages.innerHTML = '<div class="error-message">Không thể tải lịch sử chat. Vui lòng thử lại.</div>';
        });
}

// Gửi tin nhắn
function sendMessage() {
    const messageInput = document.getElementById('messageInput');
    const messageContent = messageInput.value.trim();
    
    if (messageContent === '' || !stompClient) {
        return;
    }
    
    // Ẩn welcome message nếu có
    const welcomeMessage = document.querySelector('.welcome-message');
    if (welcomeMessage) {
        welcomeMessage.remove();
    }
    
    const chatMessage = {
        maNguoiGui: parseInt(currentUserId),
        maNguoiNhan: parseInt(vendorId),
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
    const isSent = message.maNguoiGui == currentUserId;
    
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
            ${!isSent ? `<div class="message-sender">${message.tenNguoiGui || 'Vendor'}</div>` : ''}
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
    if (stompClient) {
        const typingInfo = {
            nguoiGuiId: parseInt(currentUserId),
            nguoiNhanId: parseInt(vendorId),
            isTyping: typing
        };
        stompClient.send("/app/chat.typing", {}, JSON.stringify(typingInfo));
    }
}

// Hiển thị typing indicator
function showTypingIndicator(show) {
    const typingIndicator = document.getElementById('typingIndicator');
    typingIndicator.style.display = show ? 'block' : 'none';
    if (show) {
        scrollToBottom();
    }
}

// Xử lý upload file
function handleFileUpload(event) {
    const file = event.target.files[0];
    if (!file) return;
    
    // Kiểm tra kích thước file (max 10MB)
    if (file.size > 10 * 1024 * 1024) {
        showToast('error', 'File quá lớn. Vui lòng chọn file nhỏ hơn 10MB');
        return;
    }
    
    const formData = new FormData();
    formData.append('file', file);
    
    // Hiển thị loading
    showToast('info', 'Đang upload file...');
    
    // Upload file
    fetch('/files/api/upload/chat', {
        method: 'POST',
        body: formData
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            // Gửi tin nhắn với file
            const chatMessage = {
                maNguoiGui: parseInt(currentUserId),
                maNguoiNhan: parseInt(vendorId),
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
    
    // Reset input
    event.target.value = '';
}

// Scroll to bottom
function scrollToBottom() {
    const chatMessages = document.getElementById('chatMessages');
    chatMessages.scrollTop = chatMessages.scrollHeight;
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

// Mở ảnh trong modal
function openImage(src) {
    const modal = document.createElement('div');
    modal.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0,0,0,0.9);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 10000;
        cursor: pointer;
    `;
    
    modal.innerHTML = `
        <img src="${src}" style="max-width: 90%; max-height: 90%; border-radius: 10px;">
    `;
    
    modal.onclick = function() {
        modal.remove();
    };
    
    document.body.appendChild(modal);
}

// Disconnect khi đóng trang
window.addEventListener('beforeunload', function() {
    if (stompClient) {
        stompClient.disconnect();
    }
});

