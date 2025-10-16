// cart.js - Enhanced Cart Functionality with Modern Features

// Global variables
let itemToRemove = null;
let selectedItems = [];

// JWT Token Management
function getJwtToken() {
    return localStorage.getItem('jwtToken');
}

// =======================================================
// CHECKBOX AND BULK ACTIONS LOGIC
// =======================================================

function toggleSelectAll(checkbox) {
    const itemCheckboxes = document.querySelectorAll('.item-checkbox-input');
    const bulkActions = document.getElementById('bulkActions');
    
    itemCheckboxes.forEach(itemCheckbox => {
        itemCheckbox.checked = checkbox.checked;
    });
    
    updateBulkActions();
    
    if (checkbox.checked) {
        bulkActions.style.display = 'flex';
    } else {
        bulkActions.style.display = 'none';
    }
}

function updateBulkActions() {
    const itemCheckboxes = document.querySelectorAll('.item-checkbox-input');
    const selectAllCheckbox = document.getElementById('selectAll');
    const bulkActions = document.getElementById('bulkActions');
    const selectedCount = document.getElementById('selectedCount');
    
    selectedItems = Array.from(itemCheckboxes)
        .filter(checkbox => checkbox.checked)
        .map(checkbox => parseInt(checkbox.value));
    
    if (selectedCount) {
        selectedCount.textContent = `${selectedItems.length} sản phẩm được chọn`;
    }
    
    const allChecked = itemCheckboxes.length > 0 && 
                      Array.from(itemCheckboxes).every(checkbox => checkbox.checked);
    const someChecked = Array.from(itemCheckboxes).some(checkbox => checkbox.checked);
    
    if (selectAllCheckbox) {
        selectAllCheckbox.checked = allChecked;
        selectAllCheckbox.indeterminate = someChecked && !allChecked;
    }
    
    if (bulkActions) {
        if (someChecked) {
            bulkActions.style.display = 'flex';
        } else {
            bulkActions.style.display = 'none';
        }
    }
}

// =======================================================
// MODAL MANAGEMENT
// =======================================================

function showDeleteModal(button) {
    itemToRemove = button.closest('.cart-item');
    const modal = document.getElementById('deleteModal');
    
    if (!itemToRemove || !modal) return;
    
    const productName = itemToRemove.getAttribute('data-name');
    const productImage = itemToRemove.getAttribute('data-image');
    
    const modalProductName = document.getElementById('modalProductName');
    const modalProductImage = document.getElementById('modalProductImage');
    
    if (modalProductName) modalProductName.textContent = productName;
    if (modalProductImage) modalProductImage.src = productImage;
    
    modal.classList.add('show');
    document.body.style.overflow = 'hidden';
}

function hideDeleteModal() {
    const modal = document.getElementById('deleteModal');
    if (modal) {
        modal.classList.remove('show');
        itemToRemove = null;
        document.body.style.overflow = '';
    }
}

function showBulkDeleteModal() {
    const modal = document.getElementById('bulkDeleteModal');
    const countElement = document.getElementById('bulkDeleteCount');
    const countTextElement = document.getElementById('bulkDeleteCountText');
    
    if (!modal || selectedItems.length === 0) return;
    
    if (countElement) countElement.textContent = selectedItems.length;
    if (countTextElement) countTextElement.textContent = selectedItems.length;
    
    modal.classList.add('show');
    document.body.style.overflow = 'hidden';
}

function hideBulkDeleteModal() {
    const modal = document.getElementById('bulkDeleteModal');
    if (modal) {
        modal.classList.remove('show');
        document.body.style.overflow = '';
    }
}

// MODAL XÓA TOÀN BỘ GIỎ HÀNG
function showClearCartModal() {
    const modal = document.getElementById('clearCartModal');
    if (!modal) return;
    
    modal.classList.add('show');
    document.body.style.overflow = 'hidden';
}

function hideClearCartModal() {
    const modal = document.getElementById('clearCartModal');
    if (modal) {
        modal.classList.remove('show');
        document.body.style.overflow = '';
    }
}

function confirmClearCart() {
    const token = getJwtToken();
    
    fetch('/api/cart/clear', {
        method: 'POST',
        headers: {
            'Authorization': 'Bearer ' + token
        }
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.json();
    })
    .then(data => {
        if (data.success) {
            // Ẩn modal
            hideClearCartModal();
            
            // Hiệu ứng xóa tất cả sản phẩm
            const cartItems = document.querySelectorAll('.cart-item');
            cartItems.forEach((item, index) => {
                setTimeout(() => {
                    item.style.opacity = '0';
                    item.style.transform = 'translateX(-100%)';
                }, index * 100);
            });
            
            // Reload trang sau khi hiệu ứng hoàn tất
            setTimeout(() => {
                location.reload();
            }, cartItems.length * 100 + 500);
            
            showToast('success', 'Thành công', 'Đã xóa toàn bộ giỏ hàng.');
        } else {
            showToast('error', 'Lỗi', data.message || 'Có lỗi xảy ra khi xóa giỏ hàng.');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('error', 'Lỗi kết nối', 'Có lỗi xảy ra khi xóa giỏ hàng.');
    });
}

// =======================================================
// QUANTITY MANAGEMENT
// =======================================================

function increaseQuantity(button) {
    const cartItem = button.closest('.cart-item');
    const input = cartItem.querySelector('.quantity-input');
    let quantity = parseInt(input.value);
    const maxQuantity = parseInt(input.getAttribute('max'));
    
    if (quantity < maxQuantity) {
        quantity++;
        input.value = quantity;
        updateCartItem(cartItem, quantity);
    } else {
        showToast('warning', 'Cảnh báo', 'Rất tiếc, số lượng sản phẩm không đủ.');
    }
}

function decreaseQuantity(button) {
    const cartItem = button.closest('.cart-item');
    const input = cartItem.querySelector('.quantity-input');
    let quantity = parseInt(input.value);
    
    if (quantity > 1) {
        quantity--;
        input.value = quantity;
        updateCartItem(cartItem, quantity);
    }
}

function updateCartItem(cartItem, quantity) {
    const maMatHang = cartItem.getAttribute('data-id');
    const input = cartItem.querySelector('.quantity-input');
    const oldQuantity = parseInt(input.value);
    const token = getJwtToken();
    
    fetch('/api/cart/update', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + token
        },
        body: JSON.stringify({
            maMatHang: parseInt(maMatHang),
            soLuong: quantity
        })
    })
    .then(response => {
        if (!response.ok) {
            if (response.status === 400) {
                 return response.json().then(errorData => { throw new Error(errorData.message); });
            }
            throw new Error('Network response was not ok');
        }
        return response.json();
    })
    .then(data => {
        if (data.success) {
            const totalElement = cartItem.querySelector('.total-price');
            totalElement.textContent = '₫' + formatNumber(data.data.thanhTien);
            
            updateCartSummary(data.data.tongTien);
            updateCartCount(data.data.totalQuantity);
            
            showToast('success', 'Thành công', 'Đã cập nhật giỏ hàng thành công.');
        } else {
            showToast('error', 'Lỗi', data.message || 'Có lỗi xảy ra khi cập nhật giỏ hàng.');
            input.value = oldQuantity;
        }
    })
    .catch(error => {
        console.error('Error:', error);
        const errorMessage = error.message.includes('Sản phẩm không đủ số lượng') 
            ? error.message : 'Có lỗi xảy ra khi cập nhật giỏ hàng.';
        showToast('error', 'Lỗi kết nối', errorMessage);
        input.value = oldQuantity;
    });
}

// =======================================================
// DELETE OPERATIONS
// =======================================================

function confirmRemove() {
    if (itemToRemove) {
        removeItem(itemToRemove.querySelector('.btn-remove'));
        hideDeleteModal();
    }
}

function removeItem(button) {
    const cartItem = button.closest('.cart-item');
    const maMatHang = cartItem.getAttribute('data-id');
    const token = getJwtToken();
    
    fetch('/api/cart/remove', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + token
        },
        body: JSON.stringify({
            maMatHang: parseInt(maMatHang)
        })
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.json();
    })
    .then(data => {
        if (data.success) {
            cartItem.style.opacity = '0';
            cartItem.style.transform = 'translateX(-100%)';
            
            setTimeout(() => {
                cartItem.remove();
                updateBulkActions();
                updateCartCount(data.data.totalQuantity);
                
                // CẬP NHẬT SỐ TIỀN KHI XÓA THÀNH CÔNG
                updateCartSummary(data.data.tongTien);
                
                // Cập nhật số lượng sản phẩm trong header
                updateItemCountInHeader();
                
                const remainingItems = document.querySelectorAll('.cart-item');
                if (remainingItems.length === 0) {
                    location.reload();
                }
                
                showToast('success', 'Thành công', 'Đã xóa sản phẩm khỏi giỏ hàng.');
            }, 300);
        } else {
            showToast('error', 'Lỗi', data.message || 'Có lỗi xảy ra khi xóa sản phẩm.');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('error', 'Lỗi kết nối', 'Có lỗi xảy ra khi xóa sản phẩm.');
    });
}

function confirmBulkDelete() {
    if (selectedItems.length === 0) return;
    
    const token = getJwtToken();
    const deletePromises = selectedItems.map(maMatHang => {
        return fetch('/api/cart/remove', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + token
            },
            body: JSON.stringify({ maMatHang: maMatHang })
        });
    });
    
    Promise.all(deletePromises)
        .then(responses => {
            const allSuccessful = responses.every(response => response.ok);
            
            if (allSuccessful) {
                // Lấy tổng tiền từ response đầu tiên (giả sử tất cả response đều có cùng tổng tiền)
                return responses[0].json();
            } else {
                throw new Error('Một số sản phẩm không thể xóa');
            }
        })
        .then(data => {
            if (data.success) {
                selectedItems.forEach(maMatHang => {
                    const itemToRemove = document.querySelector(`.cart-item[data-id="${maMatHang}"]`);
                    if (itemToRemove) {
                        itemToRemove.style.opacity = '0';
                        itemToRemove.style.transform = 'translateX(-100%)';
                        setTimeout(() => {
                            itemToRemove.remove();
                        }, 300);
                    }
                });
                
                selectedItems = [];
                const selectAllCheckbox = document.getElementById('selectAll');
                if (selectAllCheckbox) {
                    selectAllCheckbox.checked = false;
                }
                
                const bulkActions = document.getElementById('bulkActions');
                if (bulkActions) {
                    bulkActions.style.display = 'none';
                }
                
                // CẬP NHẬT SỐ TIỀN VÀ SỐ LƯỢNG KHI XÓA HÀNG LOẠT THÀNH CÔNG
                updateCartSummary(data.data.tongTien);
                updateCartCount(data.data.totalQuantity);
                updateItemCountInHeader();
                
                const remainingItems = document.querySelectorAll('.cart-item');
                if (remainingItems.length === 0) {
                    setTimeout(() => {
                        location.reload();
                    }, 500);
                }
                
                showToast('success', 'Thành công', `Đã xóa ${selectedItems.length} sản phẩm khỏi giỏ hàng.`);
                hideBulkDeleteModal();
            } else {
                throw new Error('Một số sản phẩm không thể xóa');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showToast('error', 'Lỗi', 'Có lỗi xảy ra khi xóa các sản phẩm.');
        });
}

// =======================================================
// UTILITY FUNCTIONS
// =======================================================

function updateCartSummary(tongTien) {
    const subtotal = document.querySelector('.subtotal');
    const grandTotal = document.querySelector('.grand-total');
    const itemCountElement = document.querySelector('.item-count');
    
    if (subtotal && grandTotal) {
        subtotal.textContent = '₫' + formatNumber(tongTien);
        grandTotal.textContent = '₫' + formatNumber(tongTien);
    }
    
    // Cập nhật số lượng sản phẩm trong phần header của giỏ hàng
    if (itemCountElement) {
        const remainingItems = document.querySelectorAll('.cart-item').length;
        itemCountElement.textContent = remainingItems + ' sản phẩm';
    }
}

function formatNumber(number) {
    return new Intl.NumberFormat('vi-VN', { 
        minimumFractionDigits: 0, 
        maximumFractionDigits: 0 
    }).format(number);
}

function updateCartCount(count) {
    const cartQtyElement = document.getElementById('cartQty');
    if (cartQtyElement) {
        cartQtyElement.textContent = count;
        cartQtyElement.style.transform = 'scale(1.3)';
        setTimeout(() => {
            cartQtyElement.style.transform = 'scale(1)';
        }, 200);
    }
}

function updateItemCountInHeader() {
    // Cập nhật số lượng item trong phần header của trang giỏ hàng
    const remainingItems = document.querySelectorAll('.cart-item').length;
    const itemCountElement = document.querySelector('.item-count');
    if (itemCountElement) {
        itemCountElement.textContent = remainingItems + ' sản phẩm';
    }
}

function updateCartCountFromServer() {
    const token = getJwtToken();
    
    fetch('/api/cart/quantity', {
        headers: {
            'Authorization': 'Bearer ' + token
        }
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            updateCartCount(data.data);
        }
    })
    .catch(error => {
        console.error('Error fetching cart quantity:', error);
    });
}

function showToast(type, title, message) {
    const existingToast = document.querySelector('.toast');
    if (existingToast) {
        existingToast.remove();
    }
    
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    
    let iconClass = '';
    if (type === 'success') {
        iconClass = 'fa-circle-check';
    } else if (type === 'error') {
        iconClass = 'fa-circle-xmark';
    } else if (type === 'warning') {
        iconClass = 'fa-circle-exclamation';
    }
    
    toast.innerHTML = `
        <div class="toast-icon">
            <i class="fa-solid ${iconClass}"></i>
        </div>
        <div class="toast-content">
            <div class="toast-title">${title}</div>
            <div class="toast-message">${message}</div>
        </div>
        <button class="toast-close" onclick="this.closest('.toast').classList.add('hide')">
            <i class="fa-solid fa-xmark"></i>
        </button>
    `;
    
    document.body.appendChild(toast);
    
    setTimeout(() => {
        toast.classList.add('show');
    }, 100);
    
    setTimeout(() => {
        toast.classList.add('hide');
        setTimeout(() => {
            toast.remove();
        }, 300);
    }, 3000);
}

// =======================================================
// EVENT LISTENERS AND INITIALIZATION
// =======================================================

document.addEventListener('DOMContentLoaded', function() {
    // Modal event listeners
    const confirmDeleteBtn = document.getElementById('confirmDeleteButton');
    const confirmBulkDeleteBtn = document.getElementById('confirmBulkDeleteButton');
    const confirmClearCartBtn = document.getElementById('confirmClearCartButton');
    const checkoutBtn = document.querySelector('.btn-checkout');
    
    if (confirmDeleteBtn) {
        confirmDeleteBtn.addEventListener('click', confirmRemove);
    }
    
    if (confirmBulkDeleteBtn) {
        confirmBulkDeleteBtn.addEventListener('click', confirmBulkDelete);
    }
    
    if (confirmClearCartBtn) {
        confirmClearCartBtn.addEventListener('click', confirmClearCart);
    }
    
    if (checkoutBtn) {
        checkoutBtn.addEventListener('click', function() {
            window.location.href = '/orders';
        });
    }
    
    // Checkbox event listeners
    const itemCheckboxes = document.querySelectorAll('.item-checkbox-input');
    itemCheckboxes.forEach(checkbox => {
        checkbox.addEventListener('change', updateBulkActions);
    });
    
    // Close modals when clicking outside
    document.addEventListener('click', function(event) {
        const modals = document.querySelectorAll('.modal-overlay');
        modals.forEach(modal => {
            if (event.target === modal) {
                modal.classList.remove('show');
                document.body.style.overflow = '';
            }
        });
    });
    
    // Add CSS for dynamic elements
    if (!document.getElementById('cart-dynamic-style')) {
        const style = document.createElement('style');
        style.id = 'cart-dynamic-style';
        style.textContent = `
            .modal-overlay {
                display: none;
                position: fixed;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                background: rgba(0, 0, 0, 0.5);
                z-index: 1000;
                align-items: center;
                justify-content: center;
            }
            
            .modal-overlay.show {
                display: flex;
            }
            
            .modal-content {
                background: white;
                border-radius: 8px;
                width: 90%;
                max-width: 400px;
                animation: modalSlideIn 0.3s ease;
            }
            
            @keyframes modalSlideIn {
                from {
                    opacity: 0;
                    transform: translateY(-50px);
                }
                to {
                    opacity: 1;
                    transform: translateY(0);
                }
            }
            
            .modal-header {
                display: flex;
                justify-content: between;
                align-items: center;
                padding: 1rem;
                border-bottom: 1px solid #e5e5e5;
            }
            
            .modal-header h3 {
                margin: 0;
                flex: 1;
            }
            
            .modal-close {
                background: none;
                border: none;
                font-size: 1.2rem;
                cursor: pointer;
                padding: 0.25rem;
            }
            
            .modal-body {
                padding: 1rem;
            }
            
            .modal-product-info {
                display: flex;
                align-items: center;
                gap: 1rem;
            }
            
            .modal-product-image {
                width: 60px;
                height: 60px;
                object-fit: cover;
                border-radius: 4px;
            }
            
            .modal-icon {
                text-align: center;
                font-size: 3rem;
                color: #dc3545;
                margin-bottom: 1rem;
            }
            
            .modal-text {
                text-align: center;
            }
            
            .modal-footer {
                display: flex;
                gap: 0.5rem;
                padding: 1rem;
                border-top: 1px solid #e5e5e5;
                justify-content: flex-end;
            }
            
            .btn-primary {
                background: #dc3545;
                color: white;
                border: none;
                padding: 0.5rem 1rem;
                border-radius: 4px;
                cursor: pointer;
            }
            
            .btn-secondary {
                background: #6c757d;
                color: white;
                border: none;
                padding: 0.5rem 1rem;
                border-radius: 4px;
                cursor: pointer;
            }
            
            .toast {
                position: fixed;
                top: 20px;
                right: 20px;
                background: white;
                border-radius: 8px;
                box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
                padding: 1rem;
                display: flex;
                align-items: center;
                gap: 0.75rem;
                max-width: 400px;
                z-index: 1100;
                transform: translateX(150%);
                transition: transform 0.3s ease;
            }
            
            .toast.show {
                transform: translateX(0);
            }
            
            .toast.hide {
                transform: translateX(150%);
            }
            
            .toast-success .toast-icon {
                color: #28a745;
            }
            
            .toast-error .toast-icon {
                color: #dc3545;
            }
            
            .toast-warning .toast-icon {
                color: #ffc107;
            }
            
            .toast-close {
                background: none;
                border: none;
                cursor: pointer;
                padding: 0.25rem;
            }
        `;
        document.head.appendChild(style);
    }
});