// ============= ADMIN REVIEW VIEW FUNCTIONALITY =============

// Delete Review Functionality
function showDeleteModal(button) {
    const deleteModal = document.getElementById('deleteModal');
    if (!deleteModal) return;
    
    const reviewId = button.getAttribute('th:data-review-id') || button.getAttribute('data-review-id');
    const userName = button.getAttribute('th:data-user-name') || button.getAttribute('data-user-name');
    
    // Update modal content
    const userNameElement = document.getElementById('deleteUserName');
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
    fetch(`/admin/reviews/delete/${reviewId}`, {
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
                window.location.href = '/admin/reviews';
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

// Media Preview Enhancement
function setupMediaPreview() {
    // Setup image click to view larger - CHỈ áp dụng cho ảnh không có data-bs-toggle
    const reviewImages = document.querySelectorAll('.review-image:not([data-bs-toggle])');
    reviewImages.forEach(img => {
        img.style.cursor = 'pointer';
        img.addEventListener('click', function() {
            // Create modal for image preview
            showImageModal(this.src);
        });
    });
    
    // Setup video controls enhancement
    const reviewVideos = document.querySelectorAll('.review-video');
    reviewVideos.forEach(video => {
        video.addEventListener('click', function() {
            if (this.paused) {
                this.play();
            } else {
                this.pause();
            }
        });
    });
}

function showImageModal(imageSrc) {
    // Create modal HTML
    const modalHTML = `
        <div class="modal fade" id="imagePreviewModal" tabindex="-1" aria-hidden="true">
            <div class="modal-dialog modal-lg modal-dialog-centered">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">
                            <i class="fas fa-expand me-2"></i>
                            Xem hình ảnh
                        </h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body text-center">
                        <img src="${imageSrc}" alt="Hình ảnh đánh giá" class="img-fluid" style="max-height: 70vh; object-fit: contain;">
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
                            <i class="fas fa-times me-1"></i>Đóng
                        </button>
                        <a href="${imageSrc}" class="btn btn-primary" download="review_image.jpg">
                            <i class="fas fa-download me-1"></i>Tải xuống
                        </a>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    // Remove existing modal if any
    const existingModal = document.getElementById('imagePreviewModal');
    if (existingModal) {
        existingModal.remove();
    }
    
    // Add modal to body
    document.body.insertAdjacentHTML('beforeend', modalHTML);
    
    // Show modal
    const modal = new bootstrap.Modal(document.getElementById('imagePreviewModal'));
    modal.show();
    
    // Remove modal when hidden
    document.getElementById('imagePreviewModal').addEventListener('hidden.bs.modal', function() {
        this.remove();
    });
}

// Toast Notification Functionality
function showToast(type, title, message) {
    const toastContainer = document.getElementById('toastContainer');
    if (!toastContainer) {
        // Create toast container if not exists
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
    
    // Add show class for animation
    setTimeout(() => {
        const toastElement = document.getElementById(toastId);
        if (toastElement) {
            toastElement.classList.add('show');
        }
    }, 10);
    
    // Auto hide toast after 5 seconds
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

// Utility Functions
function getCsrfToken() {
    // Get CSRF token from meta tag (if exists)
    const metaTag = document.querySelector('meta[name="_csrf"]');
    return metaTag ? metaTag.getAttribute('content') : '';
}

// Enhanced Media Download
function setupDownloadButtons() {
    const downloadButtons = document.querySelectorAll('a[download]');
    downloadButtons.forEach(button => {
        button.addEventListener('click', function(e) {
            e.preventDefault();
            const downloadUrl = this.href;
            const fileName = this.getAttribute('download') || 'review_media';
            downloadFile(downloadUrl, fileName);
        });
    });
}

function downloadFile(url, fileName) {
    // Create hidden anchor tag for download
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    link.style.display = 'none';
    
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    
    // Show success toast
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

// Page Initialization
document.addEventListener('DOMContentLoaded', function() {
    console.log('Admin Review View page loaded');
    
    // Setup all functionality
    setupMediaPreview();
    setupDownloadButtons();
    
    // Add loading state to buttons
    const buttons = document.querySelectorAll('.btn');
    buttons.forEach(btn => {
        btn.addEventListener('click', function() {
            if (this.classList.contains('btn-danger')) {
                this.classList.add('loading');
                setTimeout(() => {
                    this.classList.remove('loading');
                }, 2000);
            }
        });
    });
    
    console.log('Admin Review View page fully initialized');
});

// Error handling for media loading
window.addEventListener('error', function(e) {
    if (e.target.tagName === 'IMG' || e.target.tagName === 'VIDEO') {
        console.error('Media loading error:', e.target.src);
        showToast('error', 'Lỗi tải media', 'Không thể tải hình ảnh/video. Vui lòng kiểm tra đường dẫn.');
    }
}, true);