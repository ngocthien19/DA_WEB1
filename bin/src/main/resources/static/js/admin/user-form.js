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
	const matKhau = document.getElementById('matKhau');
	if (matKhau && !matKhau.value.trim()) {
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

function submitForm() {
	// Clear previous errors
	clearErrors();

	// Client-side validation
	if (!validateForm()) {
		showToast('error', 'Lỗi', 'Vui lòng kiểm tra lại thông tin!');
		return;
	}

	setLoading(true);

	const formData = {
		tenNguoiDung: document.getElementById('tenNguoiDung').value.trim(),
		email: document.getElementById('email').value.trim(),
		sdt: document.getElementById('sdt').value.trim(),
		diaChi: document.getElementById('diaChi').value.trim(),
		maVaiTro: document.getElementById('maVaiTro').value,
		trangThai: document.getElementById('trangThai').value
	};

	// Add password for new users
	const matKhau = document.getElementById('matKhau');
	if (matKhau) {
		formData.matKhau = matKhau.value;
	}

	// Add user ID for edit mode
	const maNguoiDung = document.getElementById('maNguoiDung');
	if (maNguoiDung) {
		formData.maNguoiDung = parseInt(maNguoiDung.value);
	}

	const isEdit = !!maNguoiDung;
	const url = isEdit ? `/admin/users/edit/${maNguoiDung.value}` : '/admin/users/add';

	fetch(url, {
		method: 'POST',
		headers: {
			'Content-Type': 'application/json',
			'X-Requested-With': 'XMLHttpRequest'
		},
		body: JSON.stringify(formData)
	})
		.then(response => {
			if (!response.ok) {
				return response.json().then(error => {
					throw error;
				});
			}
			return response.json();
		})
		.then(data => {
			setLoading(false);
			if (data.success) {
				showToast('success', 'Thành công', data.message);
				setTimeout(() => {
					window.location.href = '/admin/users';
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
		})
		.catch(error => {
			setLoading(false);
			console.error('Error:', error);
			if (error.errors) {
				Object.keys(error.errors).forEach(field => {
					showFieldError(field, error.errors[field]);
				});
			}
			showToast('error', 'Lỗi', error.message || 'Đã có lỗi xảy ra khi xử lý yêu cầu!');
		});
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