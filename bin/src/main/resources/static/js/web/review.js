let currentPage = 1;
let totalPages = 1;
let productId = null;

// Khởi tạo khi DOM loaded
document.addEventListener('DOMContentLoaded', function() {
    // Lấy productId từ URL hoặc từ thẻ meta
    productId = getProductIdFromUrl();
    
    // Load trang đầu tiên
    if (productId) {
        loadReviewsPage(1);
    }
});

// Lấy productId từ URL
function getProductIdFromUrl() {
    const pathSegments = window.location.pathname.split('/');
    for (let i = 0; i < pathSegments.length; i++) {
        if (pathSegments[i] === 'view' && i + 1 < pathSegments.length) {
            const id = pathSegments[i + 1];
            return !isNaN(id) ? parseInt(id) : null;
        }
    }
    return null;
}

async function loadReviewsPage(page) {
    if (!productId) {
        console.error('Không tìm thấy productId');
        return;
    }

    const reviewsList = document.getElementById('reviewsList');
    const paginationContainer = document.getElementById('paginationContainer');
    
    if (!reviewsList) {
        console.error('Không tìm thấy reviewsList container');
        return;
    }

    // Hiển thị loading
    reviewsList.innerHTML = '<div class="loading-reviews" style="text-align: center; padding: 20px;"><i class="fa-solid fa-spinner fa-spin"></i> Đang tải...</div>';

    try {
        console.log(`Loading reviews page ${page} for product ${productId}`);
        
        const response = await fetch(`/view/${productId}/reviews?page=${page}&size=4`);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        
        console.log('Response data:', data);

        if (data.success === false) {
            throw new Error(data.error || 'Lỗi không xác định từ server');
        }

        if (data.danhGias && data.danhGias.length > 0) {
            // Xóa nội dung cũ
            reviewsList.innerHTML = '';
            
            // Thêm các review mới
            data.danhGias.forEach(review => {
                const reviewItem = createReviewItem(review);
                reviewsList.appendChild(reviewItem);
            });

            // Cập nhật thông tin phân trang - FIX: Đảm bảo có giá trị mặc định
            currentPage = data.currentPage || page;
            totalPages = data.totalPages || Math.ceil((data.totalDanhGias || 0) / 4);
            
            console.log(`Updated pagination: currentPage=${currentPage}, totalPages=${totalPages}, totalDanhGias=${data.totalDanhGias}`);
            
            // Tạo pagination controls
            createPaginationControls({
                currentPage: currentPage,
                totalPages: totalPages,
                totalDanhGias: data.totalDanhGias || 0,
                hasNext: data.hasNext || false,
                hasPrevious: data.hasPrevious || false
            });
            
            console.log(`Loaded page ${currentPage}/${totalPages}`);
        } else {
            reviewsList.innerHTML = '<div class="no-reviews"><p>Chưa có đánh giá nào.</p></div>';
            if (paginationContainer) {
                paginationContainer.style.display = 'none';
            }
        }
    } catch (error) {
        console.error('Lỗi khi tải bình luận:', error);
        reviewsList.innerHTML = '<div class="error-message">Có lỗi xảy ra khi tải bình luận. Vui lòng thử lại.</div>';
        if (paginationContainer) {
            paginationContainer.style.display = 'none';
        }
    }
}

function createPaginationControls(data) {
    let paginationContainer = document.getElementById('paginationContainer');
    
    if (!paginationContainer) {
        // Tạo container phân trang mới
        paginationContainer = document.createElement('div');
        paginationContainer.id = 'paginationContainer';
        paginationContainer.className = 'pagination-container';
        
        // Chèn sau reviewsList
        const reviewsList = document.getElementById('reviewsList');
        reviewsList.parentNode.insertBefore(paginationContainer, reviewsList.nextSibling);
    }
    
    if (data.totalPages <= 1) {
        paginationContainer.style.display = 'none';
        return;
    }
    
    paginationContainer.style.display = 'block';
    paginationContainer.innerHTML = '';
    
    const pagination = document.createElement('div');
    pagination.className = 'pagination';
    
    // Nút Previous
    if (data.hasPrevious) {
        const prevBtn = document.createElement('button');
        prevBtn.className = 'pagination-btn prev-btn';
        prevBtn.innerHTML = '<i class="fa-solid fa-chevron-left"></i> Trước';
        prevBtn.onclick = () => loadReviewsPage(data.currentPage - 1);
        pagination.appendChild(prevBtn);
    }
    
    // Tạo các nút số trang
    const startPage = Math.max(1, data.currentPage - 2);
    const endPage = Math.min(data.totalPages, data.currentPage + 2);
    
    // Nút trang đầu nếu cần
    if (startPage > 1) {
        const firstBtn = createPageButton(1, data.currentPage);
        pagination.appendChild(firstBtn);
        
        if (startPage > 2) {
            const dots = document.createElement('span');
            dots.className = 'pagination-dots';
            dots.textContent = '...';
            pagination.appendChild(dots);
        }
    }
    
    // Các nút trang
    for (let i = startPage; i <= endPage; i++) {
        const pageBtn = createPageButton(i, data.currentPage);
        pagination.appendChild(pageBtn);
    }
    
    // Nút trang cuối nếu cần
    if (endPage < data.totalPages) {
        if (endPage < data.totalPages - 1) {
            const dots = document.createElement('span');
            dots.className = 'pagination-dots';
            dots.textContent = '...';
            pagination.appendChild(dots);
        }
        
        const lastBtn = createPageButton(data.totalPages, data.currentPage);
        pagination.appendChild(lastBtn);
    }
    
    // Nút Next
    if (data.hasNext) {
        const nextBtn = document.createElement('button');
        nextBtn.className = 'pagination-btn next-btn';
        nextBtn.innerHTML = 'Sau <i class="fa-solid fa-chevron-right"></i>';
        nextBtn.onclick = () => loadReviewsPage(data.currentPage + 1);
        pagination.appendChild(nextBtn);
    }
    
    // Thông tin trang
    const pageInfo = document.createElement('div');
    pageInfo.className = 'page-info';
    pageInfo.textContent = `Trang ${data.currentPage} / ${data.totalPages} (${data.totalDanhGias} đánh giá)`;
    
    paginationContainer.appendChild(pagination);
    paginationContainer.appendChild(pageInfo);
}

function createPageButton(pageNum, currentPage) {
    const button = document.createElement('button');
    button.className = `pagination-btn page-btn ${pageNum === currentPage ? 'active' : ''}`;
    button.textContent = pageNum;
    button.onclick = () => loadReviewsPage(pageNum);
    return button;
}

function createReviewItem(review) {
    const reviewItem = document.createElement('div');
    reviewItem.className = 'review-item';
    reviewItem.setAttribute('data-aos', 'fade-up');

    // Xử lý avatar người dùng
    let userAvatar = '';
    if (review.nguoiDung && review.nguoiDung.hinhAnh) {
        let avatarSrc = review.nguoiDung.hinhAnh;
        if (!avatarSrc.startsWith('http') && !avatarSrc.startsWith('/')) {
            avatarSrc = '/' + avatarSrc;
        }
        userAvatar = `<img src="${avatarSrc}" alt="${review.nguoiDung.tenNguoiDung || 'Người dùng'}" class="user-avatar-img" onerror="this.style.display='none'; this.parentNode.innerHTML='<i class=\\'fa-solid fa-user user-avatar-icon\\'></i>'">`;
    } else {
        userAvatar = `<i class="fa-solid fa-user user-avatar-icon"></i>`;
    }

    const stars = Array.from({ length: 5 }, (_, i) =>
        `<i class="fa fa-star ${i < review.soSao ? 'filled' : ''}"></i>`
    ).join('');

    const reviewMedia = review.anhVideo ?
        `<div class="review-media">
            <img src="/images/uploads/reviews/${review.anhVideo}" alt="Media đánh giá" class="review-image">
        </div>` : '';

    const userName = review.nguoiDung && review.nguoiDung.tenNguoiDung ? 
        review.nguoiDung.tenNguoiDung : 'Người dùng ẩn danh';
    
    const reviewDate = review.ngayDanhGia ? 
        formatDateToMatch(new Date(review.ngayDanhGia)) : '';

    reviewItem.innerHTML = `
        <div class="review-header">
            <div class="user-info">
                <div class="user-avatar">
                    ${userAvatar}
                </div>
                <div class="user-details">
                    <span class="user-name">${userName}</span>
                    <div class="review-rating">
                        <div class="stars-small">
                            ${stars}
                        </div>
                        <span class="review-date">${reviewDate}</span>
                    </div>
                </div>
            </div>
        </div>
        <div class="review-content">
            <p class="review-comment">${review.binhLuan || 'Không có bình luận'}</p>
            ${reviewMedia}
        </div>
    `;

    return reviewItem;
}

// Function format date để match với Thymeleaf
function formatDateToMatch(date) {
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    
    return `${day}/${month}/${year} ${hours}:${minutes}`;
}

// Export functions for global access
window.reviewFunctions = {
    loadReviewsPage,
    getProductIdFromUrl
};