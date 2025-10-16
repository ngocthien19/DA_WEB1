// ============= IMAGE OVERLAY FUNCTIONALITY =============

function setupImageOverlays() {
    const imageContainers = document.querySelectorAll('.image-container');
    
    imageContainers.forEach(container => {
        const image = container.querySelector('.review-media');
        const overlay = container.querySelector('.image-overlay');
        
        if (!image || !overlay) return;
        
        // Hiển thị overlay khi hover vào ảnh
        container.addEventListener('mouseenter', function() {
            overlay.style.opacity = '1';
            overlay.style.visibility = 'visible';
        });
        
        // Ẩn overlay khi rời chuột khỏi ảnh
        container.addEventListener('mouseleave', function() {
            overlay.style.opacity = '0';
            overlay.style.visibility = 'hidden';
        });
        
        // Đảm bảo overlay ẩn khi load trang
        overlay.style.opacity = '0';
        overlay.style.visibility = 'hidden';
        
        // Thêm hiệu ứng transition mượt mà
        overlay.style.transition = 'all 0.3s ease-in-out';
        
        console.log('Image overlay setup for container:', container);
    });
}

// ============= ENHANCED IMAGE MODAL FUNCTIONALITY =============

function setupImageModal() {
    const imageModal = document.getElementById('imageModal');
    if (!imageModal) return;
    
    // Get the image element in modal
    const modalImage = imageModal.querySelector('img');
    if (!modalImage) return;
    
    // Get the original image source from Thymeleaf
    const originalImageSrc = modalImage.getAttribute('th:src');
    if (originalImageSrc) {
        // Remove Thymeleaf syntax and get actual path
        const imagePath = originalImageSrc.replace('@{', '').replace('}', '');
        modalImage.src = imagePath;
        console.log('Modal image source set to:', imagePath);
    }
    
    // Setup download button in modal
    const modalDownloadBtn = imageModal.querySelector('a[download]');
    if (modalDownloadBtn && originalImageSrc) {
        const imagePath = originalImageSrc.replace('@{', '').replace('}', '');
        modalDownloadBtn.href = imagePath;
        
        // Set filename for download
        const filename = imagePath.split('/').pop() || 'review_image.jpg';
        modalDownloadBtn.setAttribute('download', filename);
        
        // Thêm sự kiện click để hiển thị toast
        modalDownloadBtn.addEventListener('click', function(e) {
            e.preventDefault();
            const downloadUrl = this.href;
            const fileName = this.getAttribute('download') || 'review_image.jpg';
            downloadFile(downloadUrl, fileName);
        });
    }
    
    // Setup click events for all review images to open modal with correct image
    const reviewImages = document.querySelectorAll('.review-media[data-bs-toggle="modal"]');
    reviewImages.forEach(img => {
        img.addEventListener('click', function(e) {
            e.preventDefault();
            
            const imageSrc = this.getAttribute('src') || this.getAttribute('th:src');
            if (imageSrc) {
                // Clean Thymeleaf syntax if present
                const cleanSrc = imageSrc.replace('@{', '').replace('}', '');
                modalImage.src = cleanSrc;
                
                // Update download link in modal
                if (modalDownloadBtn) {
                    modalDownloadBtn.href = cleanSrc;
                    const filename = cleanSrc.split('/').pop() || 'review_image.jpg';
                    modalDownloadBtn.setAttribute('download', filename);
                }
                
                console.log('Modal opened with image:', cleanSrc);
            }
            
            // Show the modal
            const modal = new bootstrap.Modal(imageModal);
            modal.show();
        });
    });
}

// ============= DOWNLOAD FUNCTIONALITY =============

function setupDownloadButtons() {
    // Setup download buttons outside modal
    const downloadButtons = document.querySelectorAll('a[download]');
    downloadButtons.forEach(button => {
        // Skip modal download button (already handled in setupImageModal)
        if (button.closest('#imageModal')) return;
        
        button.addEventListener('click', function(e) {
            e.preventDefault();
            const downloadUrl = this.href;
            const fileName = this.getAttribute('download') || this.getAttribute('th:download') || 'review_media';
            downloadFile(downloadUrl, fileName);
        });
    });
}

function downloadFile(url, fileName) {
    // Tạo thẻ a ẩn để tải file
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    link.style.display = 'none';
    
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    
    // Hiển thị toast thông báo
    const fileType = getFileType(fileName);
    const message = getDownloadMessage(fileType, fileName);
    
    showToast('success', 'Tải xuống thành công', message);
}

function getFileType(fileName) {
    const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp'];
    const videoExtensions = ['.mp4', '.avi', '.mov', '.mkv', '.webm'];
    
    const ext = fileName.toLowerCase().substring(fileName.lastIndexOf('.'));
    
    if (imageExtensions.includes(ext)) {
        return 'image';
    } else if (videoExtensions.includes(ext)) {
        return 'video';
    } else {
        return 'file';
    }
}

function getDownloadMessage(fileType, fileName) {
    switch (fileType) {
        case 'image':
            return `Hình ảnh "${fileName}" đã được tải xuống thành công.`;
        case 'video':
            return `Video "${fileName}" đã được tải xuống thành công.`;
        default:
            return `File "${fileName}" đã được tải xuống thành công.`;
    }
}

// ============= DELETE REVIEW FUNCTIONALITY =============

function showDeleteModal(button) {
    const deleteModal = document.getElementById('deleteModal');
    if (!deleteModal) return;
    
    const reviewId = button.getAttribute('th:data-review-id') || button.getAttribute('data-review-id');
    const userName = button.getAttribute('th:data-user-name') || button.getAttribute('data-user-name');
    
    // Update modal content
    const userNameElement = document.getElementById('userNameToDelete');
    if (userNameElement) {
        userNameElement.textContent = userName;
    }
    
    // Setup confirm delete button
    const confirmDeleteBtn = document.getElementById('confirmDeleteBtn');
    if (confirmDeleteBtn) {
        // Remove existing event listeners
        const newConfirmDeleteBtn = confirmDeleteBtn.cloneNode(true);
        confirmDeleteBtn.parentNode.replaceChild(newConfirmDeleteBtn, confirmDeleteBtn);
        
        // Add new event listener
        newConfirmDeleteBtn.addEventListener('click', function() {
            deleteReview(reviewId, userName);
            
            // Hide modal
            const modal = bootstrap.Modal.getInstance(deleteModal);
            modal.hide();
        });
    }
    
    // Show modal
    const modal = new bootstrap.Modal(deleteModal);
    modal.show();
}

function deleteReview(reviewId, userName) {
    // Hiển thị toast loading
    showToast('info', 'Đang xử lý', 'Đang xóa đánh giá...');
    
    // Gửi request xóa đánh giá
    fetch(`/vendor/reviews/delete/${reviewId}`, {
        method: 'DELETE',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': getCsrfToken()
        }
    })
    .then(response => {
        if (response.ok) {
            showToast('success', 'Xóa thành công', `Đánh giá của ${userName} đã được xóa thành công.`);
            
            // Chuyển hướng về trang danh sách sau 2 giây
            setTimeout(() => {
                window.location.href = '/vendor/reviews';
            }, 2000);
        } else {
            throw new Error('Xóa thất bại');
        }
    })
    .catch(error => {
        console.error('Error deleting review:', error);
        showToast('error', 'Lỗi', 'Đã xảy ra lỗi khi xóa đánh giá. Vui lòng thử lại.');
    });
}

// ============= TOAST NOTIFICATION FUNCTIONALITY =============

function showToast(type, title, message) {
    const toastContainer = document.getElementById('toastContainer');
    if (!toastContainer) {
        // Tạo toast container nếu chưa có
        const container = document.createElement('div');
        container.id = 'toastContainer';
        container.style.cssText = 'position: fixed; top: 20px; right: 20px; z-index: 9999;';
        document.body.appendChild(container);
    }
    
    const finalContainer = document.getElementById('toastContainer');
    const toastId = 'toast-' + Date.now();
    
    const icon = type === 'success' ? 'fa-check-circle' : 
                 type === 'error' ? 'fa-exclamation-circle' : 
                 type === 'warning' ? 'fa-exclamation-triangle' : 'fa-info-circle';
    
    const toastClass = type === 'success' ? 'toast-success' : 
                      type === 'error' ? 'toast-error' : 
                      type === 'warning' ? 'toast-warning' : 'toast-info';
    
    const toastHTML = `
        <div id="${toastId}" class="toast ${toastClass}">
            <div class="toast-icon">
                <i class="fas ${icon}"></i>
            </div>
            <div class="toast-content">
                <div class="toast-title">${title}</div>
                <div class="toast-message">${message}</div>
            </div>
            <button type="button" class="toast-close" onclick="hideToast('${toastId}')">
                <i class="fas fa-times"></i>
            </button>
        </div>
    `;
    
    finalContainer.insertAdjacentHTML('beforeend', toastHTML);
    
    // Thêm class show để hiển thị animation
    setTimeout(() => {
        const toastElement = document.getElementById(toastId);
        if (toastElement) {
            toastElement.classList.add('show');
        }
    }, 10);
    
    // Tự động ẩn toast sau 5 giây
    setTimeout(() => {
        hideToast(toastId);
    }, 5000);
}

function hideToast(toastId) {
    const toastElement = document.getElementById(toastId);
    if (toastElement) {
        toastElement.classList.remove('show');
        toastElement.classList.add('hide');
        setTimeout(() => {
            if (toastElement.parentNode) {
                toastElement.parentNode.removeChild(toastElement);
            }
        }, 300);
    }
}

// ============= UTILITY FUNCTIONS =============

function getCsrfToken() {
    // Lấy CSRF token từ meta tag (nếu có)
    const metaTag = document.querySelector('meta[name="_csrf"]');
    return metaTag ? metaTag.getAttribute('content') : '';
}

// ============= ENHANCED CSS FOR IMAGE OVERLAY =============

function injectOverlayStyles() {
    const style = document.createElement('style');
    style.textContent = `
        .image-container {
            position: relative;
            display: inline-block;
            overflow: hidden;
            border-radius: 8px;
            cursor: pointer;
        }
        
        .image-container .review-media {
            transition: transform 0.3s ease, filter 0.3s ease;
            width: 100%;
            height: auto;
        }
        
        .image-container:hover .review-media {
            transform: scale(1.05);
            filter: brightness(0.8);
        }
        
        .image-overlay {
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: rgba(0, 0, 0, 0.7);
            color: white;
            display: flex;
            align-items: center;
            justify-content: center;
            opacity: 0;
            visibility: hidden;
            transition: all 0.3s ease-in-out;
            border-radius: 8px;
        }
        
        .image-overlay i {
            font-size: 2rem;
            color: white;
        }
        
        .image-container:hover .image-overlay {
            opacity: 1;
            visibility: visible;
        }
        
        .image-overlay:hover {
            background: rgba(0, 0, 0, 0.8);
        }
        
        /* Toast styles */
        #toastContainer {
            position: fixed;
            top: 20px;
            right: 20px;
            z-index: 9999;
        }
        
        .toast {
            min-width: 300px;
            background: white;
            padding: 20px;
            border-radius: 10px;
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
            display: flex;
            align-items: center;
            gap: 15px;
            margin-bottom: 15px;
            border-left: 4px solid;
            opacity: 0;
            transform: translateX(100%);
            transition: all 0.3s ease-out;
        }
        
        .toast.show {
            opacity: 1;
            transform: translateX(0);
        }
        
        .toast.hide {
            opacity: 0;
            transform: translateX(100%);
        }
        
        .toast.toast-success {
            border-left-color: #28a745;
        }
        
        .toast.toast-error {
            border-left-color: #e94560;
        }
        
        .toast.toast-warning {
            border-left-color: #ffc107;
        }
        
        .toast.toast-info {
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
    `;
    document.head.appendChild(style);
    console.log('Image overlay and toast styles injected');
}

// ============= INITIALIZATION =============

document.addEventListener('DOMContentLoaded', function() {
    console.log('Review view page loaded');
    
    // Inject overlay styles
    injectOverlayStyles();
    
    // Setup all functionality
    setupDownloadButtons();
    setupImageOverlays();
    setupImageModal();
    
    // Log media information for debugging
    const mediaElement = document.querySelector('.review-media');
    if (mediaElement) {
        console.log('Media element found:', mediaElement);
    }
    
    const overlayElements = document.querySelectorAll('.image-overlay');
    console.log('Image overlay elements found:', overlayElements.length);
    
    console.log('Review view page fully initialized');
});