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

function showFieldError(fieldName, errorMessage) {
    const field = document.getElementById(fieldName);
    const errorElement = document.getElementById(fieldName + 'Error');

    if (field && errorElement) {
        field.classList.add('is-invalid');
        errorElement.textContent = errorMessage;
    }
}

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

function validateForm() {
    let isValid = true;
    clearErrors();

    const tenDanhMuc = document.getElementById('tenDanhMuc').value.trim();
    
    if (!tenDanhMuc) {
        showFieldError('tenDanhMuc', 'Tên danh mục không được để trống');
        isValid = false;
    }

    return isValid;
}

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
        
        // Validate file size (5MB)
        if (file.size > 5 * 1024 * 1024) {
            showFieldError('hinhAnh', 'Kích thước file không được vượt quá 5MB');
            event.target.value = '';
            return;
        }
        
        const reader = new FileReader();
        reader.onload = function(e) {
            preview.innerHTML = `
                <img src="${e.target.result}" 
                     alt="Preview"
                     style="max-width: 100%; max-height: 300px; border-radius: 12px; object-fit: cover; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);">
            `;
        };
        reader.readAsDataURL(file);
    }
}

function submitForm() {
    console.log('=== SUBMIT FORM START ===');
    
    clearErrors();

    if (!validateForm()) {
        showToast('error', 'Lỗi', 'Vui lòng kiểm tra lại thông tin!');
        return;
    }

    setLoading(true);

    const maDanhMuc = document.getElementById('maDanhMuc');
    const isEdit = !!maDanhMuc;
    const hinhAnh = document.getElementById('hinhAnh');
    const hasImage = hinhAnh.files.length > 0;

    console.log('Form info:', {
        isEdit: isEdit,
        hasImage: hasImage,
        categoryId: isEdit ? maDanhMuc.value : 'new'
    });

    // Always use FormData for file uploads
    const formData = new FormData();
    formData.append('tenDanhMuc', document.getElementById('tenDanhMuc').value.trim());
    formData.append('moTa', document.getElementById('moTa').value.trim());
    formData.append('trangThai', document.getElementById('trangThai').value);

    if (hasImage) {
        formData.append('hinhAnh', hinhAnh.files[0]);
        console.log('Image file:', hinhAnh.files[0].name, 'Size:', hinhAnh.files[0].size, 'bytes');
    }

    const url = isEdit ? `/admin/categories/edit/${maDanhMuc.value}` : '/admin/categories/add';
    
    console.log('Submitting to URL:', url);

    fetch(url, {
        method: 'POST',
        headers: {
            'X-Requested-With': 'XMLHttpRequest'
        },
        body: formData
    })
    .then(handleResponse)
    .then(handleSuccess)
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

function handleSuccess(data) {
    setLoading(false);
    
    if (data.success) {
        showToast('success', 'Thành công', data.message);
        
        setTimeout(() => {
            window.location.href = '/admin/categories';
        }, 1500);
    } else {
        if (data.errors) {
            Object.keys(data.errors).forEach(field => {
                showFieldError(field, data.errors[field]);
            });
        }
        showToast('error', 'Lỗi', data.message || 'Đã có lỗi xảy ra!');
    }
}

// Event listeners
document.addEventListener('DOMContentLoaded', function() {
    const submitBtn = document.getElementById('submitBtn');
    if (submitBtn) {
        submitBtn.addEventListener('click', submitForm);
    }

    // Auto-dismiss alerts
    const alerts = document.querySelectorAll('.alert');
    alerts.forEach(function(alert) {
        setTimeout(function() {
            const bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        }, 5000);
    });
});