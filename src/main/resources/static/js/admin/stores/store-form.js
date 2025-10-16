// Function to show toast notification
function showToast(type, title, message) {
    const toastContainer = document.getElementById('toastContainer');
    const toastId = 'toast-' + Date.now();

    const toastHTML = `
        <div id="${toastId}" class="toast toast-${type}">
            <div class="toast-icon">
                <i class="fas ${type === 'success' ? 'fa-check-circle' : 'fa-exclamation-circle'}"></i>
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

    // Auto close after 5 seconds
    setTimeout(() => {
        closeToast(toastId);
    }, 5000);
}

// Function to close toast
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

// Clear error messages and styling
function clearErrors() {
    const errorElements = document.querySelectorAll('.error-message');
    errorElements.forEach(element => {
        element.textContent = '';
    });

    const formControls = document.querySelectorAll('.form-control, .form-select');
    formControls.forEach(control => {
        control.classList.remove('is-invalid');
    });
}

// Show error for specific field
function showFieldError(fieldName, errorMessage) {
    const field = document.getElementById(fieldName);
    const errorElement = document.getElementById(fieldName + 'Error');

    if (field && errorElement) {
        field.classList.add('is-invalid');
        errorElement.textContent = errorMessage;
    }
}

// Show loading state
function setLoading(isLoading) {
    const submitBtn = document.getElementById('submitBtn');
    const submitText = document.getElementById('submitText');
    const loadingSpinner = document.getElementById('loadingSpinner');

    if (isLoading) {
        submitBtn.disabled = true;
        submitText.style.display = 'none';
        loadingSpinner.style.display = 'inline-block';
    } else {
        submitBtn.disabled = false;
        submitText.style.display = 'inline';
        loadingSpinner.style.display = 'none';
    }
}

// Check if there are available vendors
function hasAvailableVendors() {
    const vendorSelect = document.getElementById('maNguoiDung');
    const vendors = vendorSelect.querySelectorAll('option');
    // Trừ option đầu tiên "Chọn chủ cửa hàng"
    return vendors.length > 1;
}

// Client-side validation
function validateForm() {
    let isValid = true;
    clearErrors();

    // Kiểm tra xem có vendor nào khả dụng không (chỉ khi thêm mới)
    const isEdit = !!document.getElementById('maCuaHang');
    if (!isEdit && !hasAvailableVendors()) {
        showToast('error', 'Lỗi', 'Không có người dùng nào khả dụng. Vui lòng tạo người dùng VENDOR trước.');
        return false;
    }

    // Validate required fields
    const requiredFields = [
        'tenCuaHang',
        'maNguoiDung',
        'soDienThoai',
        'email',
        'diaChi',
        'trangThai'
    ];

    requiredFields.forEach(field => {
        const value = document.getElementById(field).value.trim();
        if (!value) {
            showFieldError(field, 'Trường này là bắt buộc');
            isValid = false;
        }
    });

    // Validate email format
    const email = document.getElementById('email').value;
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (email && !emailRegex.test(email)) {
        showFieldError('email', 'Email không đúng định dạng');
        isValid = false;
    }

    // Validate phone number (10 digits, starts with 0)
    const soDienThoai = document.getElementById('soDienThoai').value;
    const phoneRegex = /^0[0-9]{9}$/;
    if (soDienThoai && !phoneRegex.test(soDienThoai)) {
        showFieldError('soDienThoai', 'Số điện thoại phải có 10 chữ số và bắt đầu bằng 0');
        isValid = false;
    }

    // Validate year (between 1900 and current year + 1)
    const namThanhLap = document.getElementById('namThanhLap').value;
    const currentYear = new Date().getFullYear();
    if (namThanhLap && (namThanhLap < 1900 || namThanhLap > currentYear + 1)) {
        showFieldError('namThanhLap', `Năm thành lập phải từ 1900 đến ${currentYear + 1}`);
        isValid = false;
    }

    return isValid;
}

// Handle form submission
document.getElementById('submitBtn').addEventListener('click', function() {
    submitForm();
});

// Restrict phone input to numbers only and max 10 digits
document.getElementById('soDienThoai').addEventListener('input', function(e) {
    // Remove non-numeric characters
    this.value = this.value.replace(/[^0-9]/g, '');

    // Limit to 10 digits
    if (this.value.length > 10) {
        this.value = this.value.slice(0, 10);
    }
});

// Function to preview selected image
function previewImage(event) {
    const file = event.target.files[0];
    const preview = document.getElementById('imagePreview');
    
    if (file) {
        // Validate file type
        if (!file.type.startsWith('image/')) {
            showFieldError('hinhAnh', 'Vui lòng chọn file ảnh hợp lệ');
            event.target.value = '';
            return;
        }
        
        // Validate file size (10MB)
        if (file.size > 10 * 1024 * 1024) {
            showFieldError('hinhAnh', 'Kích thước file không được vượt quá 10MB');
            event.target.value = '';
            return;
        }
        
        const reader = new FileReader();
        reader.onload = function(e) {
            preview.innerHTML = `
                <img src="${e.target.result}" 
                     style="max-width: 120px; max-height: 120px; border-radius: 8px; object-fit: cover;" 
                     alt="Preview">
            `;
        };
        reader.readAsDataURL(file);
    } else {
        preview.innerHTML = `
            <div class="text-muted">
                <i class="fas fa-image fa-3x mb-2" style="color: #dee2e6;"></i>
                <p>Không có ảnh</p>
            </div>
        `;
    }
}

function submitForm() {
    console.log('=== SUBMIT FORM START ===');
    
    // Clear previous errors
    clearErrors();

    // Client-side validation
    if (!validateForm()) {
        showToast('error', 'Lỗi', 'Vui lòng kiểm tra lại thông tin!');
        return;
    }

    // Kiểm tra lại có vendor khả dụng không (phòng trường hợp bypass client-side)
    const isEdit = !!document.getElementById('maCuaHang');
    if (!isEdit && !hasAvailableVendors()) {
        showToast('error', 'Lỗi', 'Không có người dùng nào khả dụng. Vui lòng tạo người dùng VENDOR trước.');
        return;
    }

    setLoading(true);

    const maCuaHang = document.getElementById('maCuaHang');
    const isEditMode = !!maCuaHang;
    const hinhAnh = document.getElementById('hinhAnh');
    const hasImage = hinhAnh.files.length > 0;

    console.log('Form info:', {
        isEdit: isEditMode,
        hasImage: hasImage,
        storeId: isEditMode ? maCuaHang.value : 'new',
        hasAvailableVendors: hasAvailableVendors()
    });

    // Use FormData for both add and edit operations (since we have image upload)
    console.log('Using FormData submission');
    
    const formData = new FormData();
    formData.append('tenCuaHang', document.getElementById('tenCuaHang').value.trim());
    formData.append('maNguoiDung', document.getElementById('maNguoiDung').value);
    formData.append('soDienThoai', document.getElementById('soDienThoai').value.trim());
    formData.append('email', document.getElementById('email').value.trim());
    formData.append('diaChi', document.getElementById('diaChi').value.trim());
    formData.append('trangThai', document.getElementById('trangThai').value);

    // Optional fields
    const moTa = document.getElementById('moTa').value.trim();
    const namThanhLap = document.getElementById('namThanhLap').value;
    
    if (moTa) formData.append('moTa', moTa);
    if (namThanhLap) formData.append('namThanhLap', namThanhLap);

    // Add image if provided
    if (hasImage) {
        formData.append('hinhAnh', hinhAnh.files[0]);
        console.log('Image file:', hinhAnh.files[0].name, 'Size:', hinhAnh.files[0].size, 'bytes');
    }

    // Choose correct endpoint
    const url = isEditMode ? `/admin/stores/edit/${maCuaHang.value}` : '/admin/stores/add';
    
    console.log('Submitting to URL:', url);

    fetch(url, {
        method: 'POST',
        headers: {
            'X-Requested-With': 'XMLHttpRequest'
        },
        body: formData
    })
    .then(handleResponse)
    .then(data => handleSuccess(data))
    .catch(handleError);
}

function handleResponse(response) {
    if (!response.ok) {
        return response.json().then(error => {
            throw error;
        });
    }
    return response.json();
}

function handleError(error) {
    setLoading(false);
    console.error('Error:', error);
    if (error.errors) {
        Object.keys(error.errors).forEach(field => {
            showFieldError(field, error.errors[field]);
        });
    }
    showToast('error', 'Lỗi', error.message || 'Đã có lỗi xảy ra khi xử lý yêu cầu!');
}

// Handle successful response
function handleSuccess(data) {
    setLoading(false);
    
    if (data.success) {
        showToast('success', 'Thành công', data.message);
        
        // Redirect to stores list after success
        setTimeout(() => {
            window.location.href = '/admin/stores';
        }, 1000);
    } else {
        // Show validation errors from server
        if (data.errors) {
            Object.keys(data.errors).forEach(field => {
                showFieldError(field, data.errors[field]);
            });
        }
        showToast('error', 'Lỗi', data.message || 'Đã có lỗi xảy ra!');
    }
}

// Auto-dismiss alerts after 5 seconds
document.addEventListener('DOMContentLoaded', function() {
    const alerts = document.querySelectorAll('.alert');
    alerts.forEach(function(alert) {
        setTimeout(function() {
            const bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        }, 5000);
    });

    // Kiểm tra và hiển thị thông báo nếu không có vendor khả dụng
    const isEdit = !!document.getElementById('maCuaHang');
    if (!isEdit && !hasAvailableVendors()) {
        console.warn('No available vendors found for store creation');
        
        // Vô hiệu hóa các trường nhập liệu quan trọng
        const importantFields = ['tenCuaHang', 'soDienThoai', 'email', 'diaChi', 'hinhAnh'];
        importantFields.forEach(field => {
            const element = document.getElementById(field);
            if (element) {
                element.disabled = true;
            }
        });
    }
});

// Thêm hàm để kiểm tra trạng thái form khi load
function checkFormState() {
    const isEdit = !!document.getElementById('maCuaHang');
    const vendorSelect = document.getElementById('maNguoiDung');
    
    if (!isEdit && vendorSelect) {
        const options = vendorSelect.querySelectorAll('option');
        if (options.length <= 1) {
            console.log('No vendors available - form should be disabled');
        } else {
            console.log(`Found ${options.length - 1} available vendors`);
        }
    }
}

// Gọi hàm kiểm tra khi DOM loaded
document.addEventListener('DOMContentLoaded', checkFormState);