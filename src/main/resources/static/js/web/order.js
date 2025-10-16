// order.js - Fixed version
document.addEventListener('DOMContentLoaded', function() {
    console.log('Order.js loaded');
    
    // Form validation
    const orderForm = document.querySelector('.order-form');
    if (orderForm) {
        orderForm.addEventListener('submit', function(e) {
            if (!validateForm()) {
                e.preventDefault();
                showToast('error', 'Lỗi', 'Vui lòng kiểm tra lại thông tin đơn hàng');
            }
        });
    }
    
	// Thêm vào hàm validateForm() trong order.js
	function validateForm() {
	    let isValid = true;
	    
	    const diaChi = document.getElementById('diaChiGiaoHang');
	    const soDienThoai = document.getElementById('soDienThoai');
	    const paymentMethods = document.querySelectorAll('input[name="phuongThucThanhToan"]');
	    
	    // Validate địa chỉ
	    if (!diaChi.value.trim()) {
	        showValidationError(diaChi, 'Vui lòng nhập địa chỉ giao hàng');
	        isValid = false;
	    } else {
	        clearValidationError(diaChi);
	    }
	    
	    // Validate số điện thoại
	    if (!soDienThoai.value.trim()) {
	        showValidationError(soDienThoai, 'Vui lòng nhập số điện thoại');
	        isValid = false;
	    } else if (!isValidPhone(soDienThoai.value)) {
	        showValidationError(soDienThoai, 'Số điện thoại không hợp lệ');
	        isValid = false;
	    } else {
	        clearValidationError(soDienThoai);
	    }
	    
	    // Validate phương thức thanh toán
	    let paymentSelected = false;
	    paymentMethods.forEach(method => {
	        if (method.checked) {
	            paymentSelected = true;
	        }
	    });
	    
	    if (!paymentSelected) {
	        showToast('error', 'Lỗi', 'Vui lòng chọn phương thức thanh toán');
	        isValid = false;
	    }
	    
	    return isValid;
	}
    
    // Hiển thị lỗi validation
    function showValidationError(input, message) {
        const formGroup = input.closest('.form-group');
        let errorElement = formGroup.querySelector('.validation-error');
        
        if (!errorElement) {
            errorElement = document.createElement('div');
            errorElement.className = 'validation-error';
            formGroup.appendChild(errorElement);
        }
        
        errorElement.textContent = message;
        errorElement.style.color = '#e94560';
        errorElement.style.fontSize = '12px';
        errorElement.style.marginTop = '5px';
        
        input.style.borderColor = '#e94560';
    }
    
    // Xóa lỗi validation
    function clearValidationError(input) {
        const formGroup = input.closest('.form-group');
        const errorElement = formGroup.querySelector('.validation-error');
        
        if (errorElement) {
            errorElement.remove();
        }
        
        input.style.borderColor = '#e0e0e0';
    }
    
    // Kiểm tra số điện thoại
    function isValidPhone(phone) {
        const phoneRegex = /^(0|\+84)(\d{9,10})$/;
        return phoneRegex.test(phone.replace(/\s/g, ''));
    }
    
    // Hiển thị toast notification
    function showToast(type, title, message) {
        const existingToast = document.querySelector('.toast');
        if (existingToast) {
            existingToast.remove();
        }
        
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        
        let iconClass = '';
        if (type === 'success') iconClass = 'fa-circle-check';
        else if (type === 'error') iconClass = 'fa-circle-xmark';
        else if (type === 'warning') iconClass = 'fa-circle-exclamation';
        
        toast.innerHTML = `
            <div class="toast-icon">
                <i class="fa-solid ${iconClass}"></i>
            </div>
            <div class="toast-content">
                <div class="toast-title">${title}</div>
                <div class="toast-message">${message}</div>
            </div>
            <button class="toast-close">
                <i class="fa-solid fa-xmark"></i>
            </button>
        `;
        
        document.body.appendChild(toast);
        
        // Xử lý sự kiện đóng toast
        toast.querySelector('.toast-close').addEventListener('click', function() {
            toast.remove();
        });
        
        // Tự động ẩn sau 5 giây
        setTimeout(() => {
            if (toast.parentNode) {
                toast.remove();
            }
        }, 5000);
    }
    
    // Thêm sự kiện input để clear validation
    const diaChiInput = document.getElementById('diaChiGiaoHang');
    const soDienThoaiInput = document.getElementById('soDienThoai');
    
    if (diaChiInput) {
        diaChiInput.addEventListener('input', function() {
            clearValidationError(this);
        });
    }
    
    if (soDienThoaiInput) {
        soDienThoaiInput.addEventListener('input', function() {
            clearValidationError(this);
        });
    }
});

// Thêm vào order.js
function showQRPayment(method, amount, description) {
    const qrText = `${method}://payment?amount=${amount}&description=${encodeURIComponent(description)}`;
    
    // Gọi API tạo QR code
    fetch(`/qrcode/generate?text=${encodeURIComponent(qrText)}&width=300&height=300`)
        .then(response => response.json())
        .then(data => {
            if (data.status === 'success') {
                // Hiển thị modal với QR code
                showQRModal(data.qrCode, amount, description, method);
            } else {
                showToast('error', 'Lỗi', 'Không thể tạo mã QR');
            }
        })
        .catch(error => {
            console.error('Error generating QR code:', error);
            showToast('error', 'Lỗi', 'Không thể tạo mã QR');
        });
}

function showQRModal(qrCodeBase64, amount, description, method) {
    const modalHtml = `
        <div class="qr-modal" style="position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 10000;">
            <div style="background: white; padding: 30px; border-radius: 15px; text-align: center; max-width: 400px;">
                <h3>Quét mã để thanh toán</h3>
                <img src="data:image/png;base64,${qrCodeBase64}" alt="QR Code" style="width: 250px; height: 250px; margin: 15px 0;">
                <p><strong>Số tiền:</strong> ₫${formatCurrency(amount)}</p>
                <p><strong>Nội dung:</strong> ${description}</p>
                <button onclick="this.closest('.qr-modal').remove()" style="background: #e94560; color: white; border: none; padding: 10px 20px; border-radius: 5px; cursor: pointer;">
                    Đóng
                </button>
            </div>
        </div>
    `;
    
    document.body.insertAdjacentHTML('beforeend', modalHtml);
}

// Thêm CSS
const orderStyle = document.createElement('style');
orderStyle.textContent = `
    .toast {
        position: fixed;
        top: 20px;
        right: 20px;
        min-width: 300px;
        background: white;
        padding: 20px;
        border-radius: 10px;
        box-shadow: 0 10px 30px rgba(0,0,0,0.2);
        display: flex;
        align-items: center;
        gap: 15px;
        z-index: 9999;
        border-left: 4px solid;
        animation: slideInRight 0.3s ease-out;
    }
    
    .toast-success { border-left-color: #28a745; }
    .toast-error { border-left-color: #e94560; }
    .toast-warning { border-left-color: #ffc107; }
    
    .toast-icon { font-size: 24px; }
    .toast-success .toast-icon { color: #28a745; }
    .toast-error .toast-icon { color: #e94560; }
    .toast-warning .toast-icon { color: #ffc107; }
    
    .toast-content { flex: 1; }
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
    }
    
    .toast-close:hover { color: #666; }
    
    @keyframes slideInRight {
        from { transform: translateX(100%); opacity: 0; }
        to { transform: translateX(0); opacity: 1; }
    }
    
    .validation-error {
        color: #e94560;
        font-size: 12px;
        margin-top: 5px;
    }
`;
document.head.appendChild(orderStyle);