// ===== VENDOR REVIEWS MANAGEMENT JS =====

// Global variables
let currentReviewId = null;
let currentUserName = null;
let selectedReviews = new Set();

// ============= MEDIA MODAL FUNCTIONALITY =============

// ============= MEDIA MODAL FUNCTIONALITY =============

function setupMediaModal() {
    const mediaModal = document.getElementById('mediaModal');
    if (!mediaModal) return;
    
    mediaModal.addEventListener('show.bs.modal', function(event) {
        const button = event.relatedTarget;
        const mediaUrl = button.getAttribute('data-media-url');
        const mediaType = button.getAttribute('data-media-type');
        
        const mediaContent = document.getElementById('mediaContent');
        const noMediaPlaceholder = document.getElementById('noMediaPlaceholder');
        const downloadLink = document.getElementById('downloadMedia');
        const modalTitle = document.getElementById('mediaModalLabel');
        
        if (!mediaContent || !noMediaPlaceholder || !downloadLink || !modalTitle) return;
        
        // Ẩn tất cả nội dung trước
        mediaContent.style.display = 'none';
        noMediaPlaceholder.style.display = 'none';
        downloadLink.style.display = 'none';
        
        // Clear previous media content
        mediaContent.innerHTML = '';
        
        // Check if mediaUrl exists and is not empty
        if (!mediaUrl || mediaUrl.trim() === '') {
            // Show no media placeholder
            noMediaPlaceholder.style.display = 'block';
            modalTitle.innerHTML = '<i class="fas fa-image me-2"></i>Không có media';
            return;
        }
        
        // Construct full URL path
        const fullMediaUrl = `${mediaUrl}`;
        
        // Show media content
        mediaContent.style.display = 'block';
        
        if (mediaType === 'video') {
            modalTitle.innerHTML = '<i class="fas fa-video me-2"></i>Xem video';
            
            // Create video element
            const video = document.createElement('video');
            video.controls = true;
            video.className = 'img-fluid';
            video.style.maxHeight = '60vh';
            video.style.objectFit = 'contain';
            
            const source = document.createElement('source');
            source.src = fullMediaUrl;
            
            // Set video type based on file extension
            if (mediaUrl.includes('.mp4')) {
                source.type = 'video/mp4';
            } else if (mediaUrl.includes('.avi')) {
                source.type = 'video/x-msvideo';
            } else if (mediaUrl.includes('.mov')) {
                source.type = 'video/quicktime';
            } else {
                source.type = 'video/mp4'; // default
            }
            
            video.appendChild(source);
            video.innerHTML += 'Trình duyệt của bạn không hỗ trợ video.';
            mediaContent.appendChild(video);
            
            // Load the video
            video.load();
            
        } else {
            modalTitle.innerHTML = '<i class="fas fa-image me-2"></i>Xem hình ảnh';
            
            // Create image element
            const img = document.createElement('img');
            img.src = fullMediaUrl;
            img.alt = 'Hình ảnh đánh giá';
            img.className = 'img-fluid';
            img.style.maxHeight = '60vh';
            img.style.objectFit = 'contain';
            
            // Add error handling for broken images
            img.onerror = function() {
                mediaContent.innerHTML = `
                    <div class="text-center py-5">
                        <i class="fas fa-exclamation-triangle fa-3x text-warning mb-3"></i>
                        <h5 class="text-warning">Không thể tải media</h5>
                        <p class="text-muted">File không tồn tại hoặc đã bị xóa</p>
                        <small class="text-muted">URL: ${mediaUrl}</small>
                    </div>
                `;
            };
            
            mediaContent.appendChild(img);
        }
        
        // Setup download link
        downloadLink.href = fullMediaUrl;
        const filename = mediaUrl.split('/').pop() || 
                        (mediaType === 'video' ? 'review_video.mp4' : 'review_image.jpg');
        downloadLink.setAttribute('download', filename);
        downloadLink.style.display = 'inline-block';
        
        console.log('Media modal setup:', { mediaUrl, mediaType, fullMediaUrl, filename });
    });
    
    // Reset modal when hidden
    mediaModal.addEventListener('hidden.bs.modal', function() {
        // Có thể reset một số state nếu cần
    });
}

// ============= DOWNLOAD FUNCTIONALITY =============

function handleDownload(url, filename) {
    try {
        // Create a temporary anchor element
        const a = document.createElement('a');
        a.href = url;
        a.download = filename || 'download';
        a.style.display = 'none';
        
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        
        // Show success toast
        showToast('success', 'Thành công', 'Đang tải xuống...');
        
        console.log('Download initiated:', { url, filename });
    } catch (error) {
        console.error('Download error:', error);
        showToast('error', 'Lỗi', 'Không thể tải xuống file');
    }
}

function setupDownloadButtons() {
    // Add download functionality to modal download button
    const modalDownloadBtn = document.getElementById('downloadMedia');
    if (modalDownloadBtn) {
        modalDownloadBtn.addEventListener('click', function(e) {
            e.preventDefault();
            
            const url = this.getAttribute('href');
            let filename = this.getAttribute('download');
            
            if (!filename || filename === 'true') {
                filename = url.split('/').pop() || 'review_media';
            }
            
            console.log('Modal download clicked:', { url, filename });
            handleDownload(url, filename);
        });
    }
    
    // Add download functionality to any other download buttons in the page
    const downloadButtons = document.querySelectorAll('a[download]');
    downloadButtons.forEach(button => {
        if (button.id !== 'downloadMedia') { // Skip modal button as we already handled it
            button.addEventListener('click', function(e) {
                e.preventDefault();
                
                const url = this.getAttribute('href');
                let filename = this.getAttribute('download');
                
                if (!filename || filename === 'true') {
                    filename = url.split('/').pop() || 'review_media';
                }
                
                console.log('Download clicked:', { url, filename });
                handleDownload(url, filename);
            });
        }
    });
}

// ============= SELECTION MANAGEMENT =============

function toggleSelectAll(checkbox) {
    const reviewCheckboxes = document.querySelectorAll('.review-checkbox');
    reviewCheckboxes.forEach(cb => {
        cb.checked = checkbox.checked;
        if (checkbox.checked) {
            selectedReviews.add(cb.value);
        } else {
            selectedReviews.delete(cb.value);
        }
    });
    updateSelectionControls();
}

function updateSelectionControls() {
    const selectedCheckboxes = document.querySelectorAll('.review-checkbox:checked');
    selectedReviews = new Set(Array.from(selectedCheckboxes).map(cb => cb.value));
    
    const selectionControls = document.getElementById('selectionControls');
    const selectedCount = document.getElementById('selectedCount');
    const bulkDeleteBtn = document.querySelector('.btn-bulk-delete');
    
    if (selectedReviews.size > 0) {
        if (selectionControls) selectionControls.style.display = 'flex';
        if (selectedCount) selectedCount.textContent = selectedReviews.size;
        if (bulkDeleteBtn) bulkDeleteBtn.style.display = 'inline-block';
    } else {
        if (selectionControls) selectionControls.style.display = 'none';
        if (bulkDeleteBtn) bulkDeleteBtn.style.display = 'none';
    }
    
    // Update select all checkbox state
    const totalCheckboxes = document.querySelectorAll('.review-checkbox').length;
    const selectAllCheckbox = document.getElementById('selectAllCheckbox');
    if (selectAllCheckbox && totalCheckboxes > 0) {
        selectAllCheckbox.checked = selectedReviews.size === totalCheckboxes;
        selectAllCheckbox.indeterminate = selectedReviews.size > 0 && selectedReviews.size < totalCheckboxes;
    }
}

function clearSelection() {
    const checkboxes = document.querySelectorAll('.review-checkbox:checked');
    checkboxes.forEach(cb => cb.checked = false);
    selectedReviews.clear();
    updateSelectionControls();
}

// ============= DELETE MODAL FUNCTIONS =============

function showDeleteModal(button) {
    currentReviewId = button.getAttribute('data-review-id');
    currentUserName = button.getAttribute('data-user-name');
    
    const userNameElement = document.getElementById('userNameToDelete');
    if (userNameElement) {
        userNameElement.textContent = currentUserName;
    }
    
    const confirmBtn = document.getElementById('confirmDeleteBtn');
    if (confirmBtn) {
        confirmBtn.onclick = function() {
            deleteReview(currentReviewId);
        };
    }
    
    const deleteModal = new bootstrap.Modal(document.getElementById('deleteModal'));
    deleteModal.show();
}

function showBulkDeleteModal() {
    if (selectedReviews.size === 0) {
        showToast('error', 'Lỗi', 'Vui lòng chọn ít nhất 1 đánh giá để xóa');
        return;
    }
    
    const bulkDeleteCount = document.getElementById('bulkDeleteCount');
    const bulkDeleteCountBtn = document.getElementById('bulkDeleteCountBtn');
    if (bulkDeleteCount) bulkDeleteCount.textContent = selectedReviews.size;
    if (bulkDeleteCountBtn) bulkDeleteCountBtn.textContent = selectedReviews.size;
    
    // Display preview of reviews to be deleted
    const previewContainer = document.getElementById('bulkDeletePreview');
    if (previewContainer) {
        previewContainer.innerHTML = '';
        
        selectedReviews.forEach(reviewId => {
            const reviewRow = document.querySelector(`.review-checkbox[value="${reviewId}"]`);
            if (reviewRow) {
                const row = reviewRow.closest('tr');
                const userNameElement = row.querySelector('.user-name');
                const productNameElement = row.querySelector('.product-name');
                const userName = userNameElement ? userNameElement.textContent : 'Unknown';
                const productName = productNameElement ? productNameElement.textContent : 'Unknown';
                
                const div = document.createElement('div');
                div.className = 'preview-item mb-2';
                div.innerHTML = `
                    <div class="d-flex justify-content-between">
                        <span><i class="fas fa-user me-2"></i>${userName}</span>
                        <small class="text-muted">${productName}</small>
                    </div>
                `;
                previewContainer.appendChild(div);
            }
        });
    }
    
    const confirmBtn = document.getElementById('confirmBulkDeleteBtn');
    if (confirmBtn) {
        confirmBtn.onclick = bulkDeleteReviews;
    }
    
    const bulkDeleteModal = new bootstrap.Modal(document.getElementById('bulkDeleteModal'));
    bulkDeleteModal.show();
}

// ============= API CALLS =============

function deleteReview(reviewId) {
    // Close modal first
    const deleteModal = bootstrap.Modal.getInstance(document.getElementById('deleteModal'));
    if (deleteModal) {
        deleteModal.hide();
    }
    
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';
    
    const headers = {
        'Content-Type': 'application/json'
    };
    
    if (csrfToken) {
        headers[csrfHeader] = csrfToken;
    }
    
    fetch(`/vendor/reviews/delete/${reviewId}`, {
        method: 'DELETE',
        headers: headers
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showToast('success', 'Thành công', data.message || 'Xóa đánh giá thành công');
            setTimeout(() => {
                location.reload();
            }, 1000);
        } else {
            showToast('error', 'Lỗi', data.message || 'Có lỗi xảy ra khi xóa đánh giá');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('error', 'Lỗi', 'Có lỗi xảy ra khi xóa đánh giá');
    });
}

function bulkDeleteReviews() {
    // Close modal first
    const bulkDeleteModal = bootstrap.Modal.getInstance(document.getElementById('bulkDeleteModal'));
    if (bulkDeleteModal) {
        bulkDeleteModal.hide();
    }
    
    const reviewIds = Array.from(selectedReviews);
    
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';
    
    // Create form data
    const formData = new FormData();
    formData.append('ids', reviewIds.join(','));
    
    const headers = {};
    if (csrfToken) {
        headers[csrfHeader] = csrfToken;
    }
    
    fetch('/vendor/reviews/bulk-delete', {
        method: 'POST',
        headers: headers,
        body: formData
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showToast('success', 'Thành công', data.message);
            
            if (data.errorCount > 0 && data.errors && data.errors.length > 0) {
                data.errors.forEach(error => {
                    showToast('warning', 'Cảnh báo', error);
                });
            }
            
            setTimeout(() => {
                location.reload();
            }, 1000);
        } else {
            showToast('error', 'Lỗi', data.message || 'Có lỗi xảy ra khi xóa đánh giá');
            
            if (data.errors && data.errors.length > 0) {
                data.errors.forEach(error => {
                    console.error(error);
                });
            }
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('error', 'Lỗi', 'Có lỗi xảy ra khi xóa nhiều đánh giá');
    });
}

// ============= TOAST NOTIFICATIONS =============

function showToast(type, title, message) {
    const toastContainer = document.getElementById('toastContainer');
    if (!toastContainer) return;
    
    const toastId = 'toast-' + Date.now();
    
    const iconClass = type === 'success' ? 'fa-check-circle' : 
                     type === 'error' ? 'fa-exclamation-circle' : 
                     'fa-info-circle';
    
    const toastHTML = `
        <div id="${toastId}" class="toast toast-${type}">
            <div class="toast-icon">
                <i class="fas ${iconClass}"></i>
            </div>
            <div class="toast-content">
                <div class="toast-title">${title}</div>
                <div class="toast-message">${message}</div>
            </div>
            <button class="toast-close" onclick="closeToast('${toastId}')">
                <i class="fas fa-times"></i>
            </button>
        </div>
    `;
    
    toastContainer.insertAdjacentHTML('beforeend', toastHTML);
    
    const toast = document.getElementById(toastId);
    setTimeout(() => {
        toast.classList.add('show');
    }, 100);
    
    setTimeout(() => {
        closeToast(toastId);
    }, 5000);
}

function closeToast(toastId) {
    const toast = document.getElementById(toastId);
    if (toast) {
        toast.classList.remove('show');
        toast.classList.add('hide');
        setTimeout(() => {
            toast.remove();
        }, 300);
    }
}

// ============= EVENT LISTENERS =============

document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM loaded - Initializing reviews management...');
    
    // Initialize all functionality
    setupMediaModal();
    setupDownloadButtons();
    updateSelectionControls();
    
    // Add change listeners to all review checkboxes
    const reviewCheckboxes = document.querySelectorAll('.review-checkbox');
    reviewCheckboxes.forEach(checkbox => {
        checkbox.addEventListener('change', updateSelectionControls);
    });
    
    // Delete buttons
    const deleteButtons = document.querySelectorAll('.btn-delete');
    deleteButtons.forEach(button => {
        button.addEventListener('click', function() {
            showDeleteModal(this);
        });
    });
    
    // Fix modal backdrop issue
    const modals = document.querySelectorAll('.modal');
    modals.forEach(modal => {
        modal.addEventListener('hidden.bs.modal', function() {
            const backdrops = document.querySelectorAll('.modal-backdrop');
            backdrops.forEach(backdrop => backdrop.remove());
            document.body.classList.remove('modal-open');
            document.body.style.removeProperty('padding-right');
            document.body.style.removeProperty('overflow');
        });
    });
    
    // Auto-dismiss alerts
    const alerts = document.querySelectorAll('.alert');
    alerts.forEach(function(alert) {
        setTimeout(function() {
            const bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        }, 5000);
    });
    
    console.log('Reviews management system fully initialized with media and download functionality');
});