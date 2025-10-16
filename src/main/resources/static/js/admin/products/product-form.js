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

// ========== LOẠI SẢN PHẨM THEO DANH MỤC ==========
let currentLoaiSanPham = '';
let availableLoaiSanPham = [];

// Khởi tạo khi trang load
function initLoaiSanPham() {
    // Lấy giá trị ban đầu của loại sản phẩm
    const initialLoaiSanPham = document.getElementById('loaiSanPham').value;
    if (initialLoaiSanPham) {
        currentLoaiSanPham = initialLoaiSanPham;
        updateLoaiSanPhamDisplay();
    }
    
    // Lắng nghe sự kiện thay đổi danh mục
    const danhMucSelect = document.getElementById('maDanhMuc');
    if (danhMucSelect) {
        danhMucSelect.addEventListener('change', function() {
            loadLoaiSanPhamByDanhMuc(this.value);
        });
        
        // Load loại sản phẩm cho danh mục hiện tại (nếu có)
        if (danhMucSelect.value) {
            loadLoaiSanPhamByDanhMuc(danhMucSelect.value);
        }
    }
}

// Load loại sản phẩm theo danh mục
function loadLoaiSanPhamByDanhMuc(maDanhMuc) {
    if (!maDanhMuc) {
        // Reset về danh sách rỗng nếu không có danh mục
        availableLoaiSanPham = [];
        updateLoaiSanPhamSelect();
        return;
    }
    
    console.log('Loading loai san pham for category:', maDanhMuc);
    
    fetch(`/admin/products/loai-san-pham/${maDanhMuc}`)
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

// Cập nhật select box với danh sách mới
function updateLoaiSanPhamSelect() {
    const select = document.getElementById('loaiSanPhamSelect');
    const customInput = document.getElementById('customLoaiSanPham');
    const currentValue = select ? select.value : '';
    
    if (select) {
        // Giữ lại option đầu tiên và option custom
        const firstOption = select.options[0];
        const customOption = select.options[select.options.length - 1];
        
        // Xóa các option cũ (trừ option đầu và custom)
        while (select.options.length > 2) {
            select.remove(1);
        }
        
        // Thêm các option mới
        availableLoaiSanPham.forEach(loai => {
            const option = document.createElement('option');
            option.value = loai;
            option.textContent = loai;
            select.insertBefore(option, customOption);
        });
        
        // Khôi phục giá trị đã chọn nếu vẫn tồn tại
        if (currentLoaiSanPham && availableLoaiSanPham.includes(currentLoaiSanPham)) {
            select.value = currentLoaiSanPham;
        } else {
            select.selectedIndex = 0;
            currentLoaiSanPham = '';
            updateLoaiSanPhamDisplay();
        }
    }
}

// Xử lý khi chọn từ select
function onLoaiSanPhamSelectChange() {
    const select = document.getElementById('loaiSanPhamSelect');
    const customGroup = document.getElementById('customLoaiSanPhamGroup');
    const customInput = document.getElementById('customLoaiSanPham');
    
    if (select.value === 'custom') {
        // Hiển thị input để nhập loại mới
        customGroup.style.display = 'flex';
        customInput.focus();
        customInput.value = currentLoaiSanPham || '';
    } else if (select.value) {
        // Sử dụng giá trị từ select
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

// Xử lý khi nhập vào input custom
function onCustomLoaiSanPhamInput() {
    const customInput = document.getElementById('customLoaiSanPham');
    currentLoaiSanPham = customInput.value.trim();
    updateLoaiSanPhamDisplay();
}

// Sử dụng giá trị custom
function useCustomLoaiSanPham() {
    const customInput = document.getElementById('customLoaiSanPham');
    const customGroup = document.getElementById('customLoaiSanPhamGroup');
    const select = document.getElementById('loaiSanPhamSelect');
    
    if (currentLoaiSanPham) {
        // Ẩn input custom
        customGroup.style.display = 'none';
        customInput.value = '';
        
        // Reset select về option đầu tiên
        select.selectedIndex = 0;
        
        updateLoaiSanPhamDisplay();
    }
}

// Cập nhật hiển thị và giá trị ẩn
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

// ========== VALIDATION & FORM SUBMISSION ==========

function validateForm() {
    let isValid = true;
    clearErrors();

    const requiredFields = [
        'tenSanPham',
        'maCuaHang',
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

    // Validate giaBan
    const giaBan = parseFloat(document.getElementById('giaBan').value);
    if (giaBan && giaBan <= 0) {
        showFieldError('giaBan', 'Giá bán phải lớn hơn 0');
        isValid = false;
    }

    // Validate soLuongConLai
    const soLuong = parseInt(document.getElementById('soLuongConLai').value);
    if (soLuong && soLuong < 0) {
        showFieldError('soLuongConLai', 'Số lượng không được âm');
        isValid = false;
    }

    // Validate loaiSanPham
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

    const maSanPham = document.getElementById('maSanPham');
    const isEdit = !!maSanPham;
    const hinhAnh = document.getElementById('hinhAnh');
    const hasImage = hinhAnh.files.length > 0;

    console.log('Form info:', {
        isEdit: isEdit,
        hasImage: hasImage,
        productId: isEdit ? maSanPham.value : 'new'
    });

    // Always use FormData for file uploads
    const formData = new FormData();
    formData.append('tenSanPham', document.getElementById('tenSanPham').value.trim());
    formData.append('maCuaHang', document.getElementById('maCuaHang').value);
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

    const url = isEdit ? `/admin/products/edit/${maSanPham.value}` : '/admin/products/add';
    
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
            window.location.href = '/admin/products';
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

    // Khởi tạo tính năng LoaiSanPham theo danh mục
    initLoaiSanPham();

    // Auto-dismiss alerts
    const alerts = document.querySelectorAll('.alert');
    alerts.forEach(function(alert) {
        setTimeout(function() {
            const bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        }, 5000);
    });
});