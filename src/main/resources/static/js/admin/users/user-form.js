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

// Client-side validation
function validateForm() {
    let isValid = true;
    clearErrors();

    // Validate required fields
    const requiredFields = [
        'tenNguoiDung',
        'email',
        'sdt',
        'diaChi',
        'maVaiTro',
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
    const sdt = document.getElementById('sdt').value;
    const phoneRegex = /^0[0-9]{9}$/;
    if (sdt && !phoneRegex.test(sdt)) {
        showFieldError('sdt', 'Số điện thoại phải có 10 chữ số và bắt đầu bằng 0');
        isValid = false;
    }

    // Validate password for new users
    const isEdit = document.getElementById('maNguoiDung') !== null;
    const matKhau = document.getElementById('matKhau');
    if (!isEdit && matKhau && !matKhau.value.trim()) {
        showFieldError('matKhau', 'Mật khẩu là bắt buộc');
        isValid = false;
    }

    return isValid;
}

// Handle form submission
document.getElementById('submitBtn').addEventListener('click', function() {
    submitForm();
});

// Restrict phone input to numbers only and max 10 digits
document.getElementById('sdt').addEventListener('input', function(e) {
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

    setLoading(true);

    const maNguoiDung = document.getElementById('maNguoiDung');
    const isEdit = !!maNguoiDung;
    const hinhAnh = document.getElementById('hinhAnh');
    const hasImage = hinhAnh.files.length > 0;

    console.log('Form info:', {
        isEdit: isEdit,
        hasImage: hasImage,
        userId: isEdit ? maNguoiDung.value : 'new'
    });

    // Use FormData endpoints when uploading images
    if (hasImage) {
        console.log('Using FormData submission with image');
        
        // Create FormData for both add and edit operations
        const formData = new FormData();
        formData.append('tenNguoiDung', document.getElementById('tenNguoiDung').value.trim());
        formData.append('email', document.getElementById('email').value.trim());
        formData.append('sdt', document.getElementById('sdt').value.trim());
        formData.append('diaChi', document.getElementById('diaChi').value.trim());
        formData.append('maVaiTro', document.getElementById('maVaiTro').value);
        formData.append('trangThai', document.getElementById('trangThai').value);
        formData.append('hinhAnh', hinhAnh.files[0]);

        // Only include password for new users or when password is provided
        const matKhauField = document.getElementById('matKhau');
        if (!isEdit || (matKhauField && matKhauField.value.trim() !== '')) {
            formData.append('matKhau', matKhauField.value);
        }

        // Choose correct endpoint
        const url = isEdit ? `/admin/users/edit-with-image/${maNguoiDung.value}` : '/admin/users/add-with-image';
        
        console.log('Submitting to URL:', url);
        console.log('Image file:', hinhAnh.files[0].name, 'Size:', hinhAnh.files[0].size, 'bytes');

        fetch(url, {
            method: 'POST',
            headers: {
                'X-Requested-With': 'XMLHttpRequest'
            },
            body: formData
        })
        .then(handleResponse)
        .then(data => handleSuccess(data, true))
        .catch(handleError);
        
    } else {
        console.log('Using JSON submission without image');
        
        // For operations without image, use JSON
        const userData = {
            tenNguoiDung: document.getElementById('tenNguoiDung').value.trim(),
            email: document.getElementById('email').value.trim(),
            sdt: document.getElementById('sdt').value.trim(),
            diaChi: document.getElementById('diaChi').value.trim(),
            maVaiTro: document.getElementById('maVaiTro').value,
            trangThai: document.getElementById('trangThai').value
        };

        // Only include password for new users or when password is provided
        const matKhauField = document.getElementById('matKhau');
        if (!isEdit || (matKhauField && matKhauField.value.trim() !== '')) {
            userData.matKhau = matKhauField.value;
        }

        const url = isEdit ? `/admin/users/edit/${maNguoiDung.value}` : '/admin/users/add';
        
        console.log('Submitting to URL:', url);
        console.log('User data:', userData);

        fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
            },
            body: JSON.stringify(userData)
        })
        .then(handleResponse)
        .then(data => handleSuccess(data, false))
        .catch(handleError);
    }
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

// Handle successful response - FIXED VERSION
function handleSuccess(data, isFormData = false) {
    setLoading(false);
    
    if (data.success) {
        showToast('success', 'Thành công', data.message);
        
        // Redirect to users list after success
        setTimeout(() => {
            window.location.href = '/admin/users';
        }, 1500);
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
});