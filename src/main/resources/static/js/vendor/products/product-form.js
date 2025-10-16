// ============= VENDOR PRODUCT FORM JS =============

// Global variables
let currentLoaiSanPham = '';
let availableLoaiSanPham = [];

// ============= TOAST FUNCTIONS =============

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

// ============= ERROR HANDLING =============

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

// ============= LOẠI SẢN PHẨM FUNCTIONS =============

function initLoaiSanPham() {
    const initialLoaiSanPham = document.getElementById('loaiSanPham').value;
    if (initialLoaiSanPham) {
        currentLoaiSanPham = initialLoaiSanPham;
        updateLoaiSanPhamDisplay();
    }
    
    const danhMucSelect = document.getElementById('maDanhMuc');
    if (danhMucSelect) {
        danhMucSelect.addEventListener('change', function() {
            loadLoaiSanPhamByDanhMuc(this.value);
        });
        
        if (danhMucSelect.value) {
            loadLoaiSanPhamByDanhMuc(danhMucSelect.value);
        }
    }
}

function loadLoaiSanPhamByDanhMuc(maDanhMuc) {
    if (!maDanhMuc) {
        availableLoaiSanPham = [];
        updateLoaiSanPhamSelect();
        return;
    }
    
    console.log('Loading loai san pham for category:', maDanhMuc);
    
    fetch(`/vendor/products/loai-san-pham/${maDanhMuc}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            availableLoaiSanPham = data;
            updateLoaiSanPhamSelect();
            console.log('Loaded loai san pham:', data);
        })
        .catch(error => {
            console.error('Error loading loai san pham:', error);
            availableLoaiSanPham = [];
            updateLoaiSanPhamSelect();
        });
}

function updateLoaiSanPhamSelect() {
    const select = document.getElementById('loaiSanPhamSelect');
    const currentValue = select ? select.value : '';
    
    if (select) {
        const firstOption = select.options[0];
        const customOption = select.options[select.options.length - 1];
        
        while (select.options.length > 2) {
            select.remove(1);
        }
        
        availableLoaiSanPham.forEach(loai => {
            const option = document.createElement('option');
            option.value = loai;
            option.textContent = loai;
            select.insertBefore(option, customOption);
        });
        
        if (currentLoaiSanPham && availableLoaiSanPham.includes(currentLoaiSanPham)) {
            select.value = currentLoaiSanPham;
        } else {
            select.selectedIndex = 0;
            currentLoaiSanPham = '';
            updateLoaiSanPhamDisplay();
        }
    }
}

function onLoaiSanPhamSelectChange() {
    const select = document.getElementById('loaiSanPhamSelect');
    const customGroup = document.getElementById('customLoaiSanPhamGroup');
    const customInput = document.getElementById('customLoaiSanPham');
    
    if (select.value === 'custom') {
        customGroup.style.display = 'flex';
        customInput.focus();
        customInput.value = currentLoaiSanPham || '';
    } else if (select.value) {
        currentLoaiSanPham = select.value;
        updateLoaiSanPhamDisplay();
        customGroup.style.display = 'none';
        customInput.value = '';
    } else {
        currentLoaiSanPham = '';
        updateLoaiSanPhamDisplay();
        customGroup.style.display = 'none';
        customInput.value = '';
    }
}

function onCustomLoaiSanPhamInput() {
    const customInput = document.getElementById('customLoaiSanPham');
    currentLoaiSanPham = customInput.value.trim();
    updateLoaiSanPhamDisplay();
}

function useCustomLoaiSanPham() {
    const customInput = document.getElementById('customLoaiSanPham');
    const customGroup = document.getElementById('customLoaiSanPhamGroup');
    const select = document.getElementById('loaiSanPhamSelect');
    
    if (currentLoaiSanPham) {
        customGroup.style.display = 'none';
        customInput.value = '';
        select.selectedIndex = 0;
        updateLoaiSanPhamDisplay();
    }
}

function updateLoaiSanPhamDisplay() {
    const hiddenInput = document.getElementById('loaiSanPham');
    const currentDisplay = document.getElementById('currentLoaiSanPham');
    const noDisplay = document.getElementById('noLoaiSanPham');
    
    hiddenInput.value = currentLoaiSanPham;
    
    if (currentLoaiSanPham) {
        if (currentDisplay) {
            currentDisplay.innerHTML = `Loại hiện tại: <strong>${currentLoaiSanPham}</strong>`;
            currentDisplay.style.display = 'block';
        }
        if (noDisplay) {
            noDisplay.style.display = 'none';
        }
    } else {
        if (currentDisplay) {
            currentDisplay.style.display = 'none';
        }
        if (noDisplay) {
            noDisplay.style.display = 'block';
        }
    }
}

// ============= VALIDATION & FORM SUBMISSION =============

function validateForm() {
    let isValid = true;
    clearErrors();

    const requiredFields = [
        'tenSanPham',
        'maDanhMuc',
        'giaBan',
        'soLuongConLai',
        'loaiSanPham'
    ];

    requiredFields.forEach(field => {
        const element = document.getElementById(field);
        const value = element.value.trim();
        if (!value) {
            showFieldError(field, 'Trường này là bắt buộc');
            isValid = false;
        }
    });

    const giaBan = parseFloat(document.getElementById('giaBan').value);
    if (giaBan && giaBan <= 0) {
        showFieldError('giaBan', 'Giá bán phải lớn hơn 0');
        isValid = false;
    }

    const soLuong = parseInt(document.getElementById('soLuongConLai').value);
    if (soLuong && soLuong < 0) {
        showFieldError('soLuongConLai', 'Số lượng không được âm');
        isValid = false;
    }

    const loaiSanPham = document.getElementById('loaiSanPham').value.trim();
    if (!loaiSanPham) {
        showFieldError('loaiSanPham', 'Vui lòng chọn hoặc nhập loại sản phẩm');
        isValid = false;
    }

    return isValid;
}

function previewImage(event) {
    const file = event.target.files[0];
    const preview = document.getElementById('imagePreview');
    
    if (file) {
        if (!file.type.startsWith('image/')) {
            showFieldError('hinhAnh', 'Vui lòng chọn file ảnh hợp lệ');
            event.target.value = '';
            return;
        }
        
        if (file.size > 10 * 1024 * 1024) {
            showFieldError('hinhAnh', 'Kích thước file không được vượt quá 10MB');
            event.target.value = '';
            return;
        }
        
        const reader = new FileReader();
        reader.onload = function(e) {
            preview.innerHTML = `
                <img src="${e.target.result}" 
                     alt="Preview"
                     class="image-preview">
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

    const maSanPham = document.getElementById('maSanPham');
    const isEdit = !!maSanPham;
    const hinhAnh = document.getElementById('hinhAnh');
    const hasImage = hinhAnh.files.length > 0;

    console.log('Form info:', {
        isEdit: isEdit,
        hasImage: hasImage,
        productId: isEdit ? maSanPham.value : 'new'
    });

    const formData = new FormData();
    formData.append('tenSanPham', document.getElementById('tenSanPham').value.trim());
    formData.append('maDanhMuc', document.getElementById('maDanhMuc').value);
    formData.append('moTaSanPham', document.getElementById('moTaSanPham').value.trim());
    formData.append('giaBan', document.getElementById('giaBan').value);
    formData.append('soLuongConLai', document.getElementById('soLuongConLai').value);
    formData.append('loaiSanPham', document.getElementById('loaiSanPham').value);
    formData.append('trangThai', document.getElementById('trangThai').value);

    if (hasImage) {
        formData.append('hinhAnh', hinhAnh.files[0]);
        console.log('Image file:', hinhAnh.files[0].name, 'Size:', hinhAnh.files[0].size, 'bytes');
    }

    const url = isEdit ? `/vendor/products/edit/${maSanPham.value}` : '/vendor/products/add';
    
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
            window.location.href = '/vendor/products';
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

// ============= EVENT LISTENERS =============

document.addEventListener('DOMContentLoaded', function() {
    const submitBtn = document.getElementById('submitBtn');
    if (submitBtn) {
        submitBtn.addEventListener('click', submitForm);
    }

    initLoaiSanPham();

    const alerts = document.querySelectorAll('.alert');
    alerts.forEach(function(alert) {
        setTimeout(function() {
            const bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        }, 5000);
    });
});