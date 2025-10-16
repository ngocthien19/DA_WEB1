// Clear error khi người dùng nhập
document.querySelectorAll('input').forEach(input => {
	input.addEventListener('input', function() {
		clearError(this.id);

		// Tự động format số điện thoại
		if (this.id === 'sdt') {
			formatPhoneNumber(this);
		}
	});

	input.addEventListener('focus', function() {
		this.closest('.input-with-icon').classList.add('focused');
	});

	input.addEventListener('blur', function() {
		if (!this.value) {
			this.closest('.input-with-icon').classList.remove('focused');
		}
	});
});

// Format số điện thoại - chỉ cho phép nhập số và giới hạn 10 ký tự
function formatPhoneNumber(input) {
	// Chỉ giữ lại các ký tự số
	let value = input.value.replace(/\D/g, '');

	// Giới hạn độ dài 10 số
	if (value.length > 10) {
		value = value.substring(0, 10);
	}

	input.value = value;
}

function clearError(fieldId) {
	const input = document.getElementById(fieldId);
	const errorDiv = document.getElementById(fieldId + 'Error');

	if (input) input.classList.remove('error');
	if (errorDiv) {
		errorDiv.textContent = '';
		errorDiv.style.display = 'none';
	}
}

function showError(fieldId, message) {
	const input = document.getElementById(fieldId);
	const errorDiv = document.getElementById(fieldId + 'Error');

	if (input) input.classList.add('error');
	if (errorDiv) {
		errorDiv.textContent = message;
		errorDiv.style.display = 'block';
	}
}

function showSuccess(message) {
	const successDiv = document.getElementById('successMessage');
	successDiv.textContent = message;
	successDiv.style.display = 'block';
}

function validateForm() {
	let isValid = true;

	// Clear all errors
	document.querySelectorAll('.error-text').forEach(el => {
		el.style.display = 'none';
		el.textContent = '';
	});
	document.querySelectorAll('input').forEach(el => el.classList.remove('error'));

	// Validate Họ và tên
	const tenNguoiDung = document.getElementById('tenNguoiDung').value.trim();
	if (!tenNguoiDung) {
		showError('tenNguoiDung', 'Tên người dùng không được để trống');
		isValid = false;
	} else if (tenNguoiDung.length < 2 || tenNguoiDung.length > 100) {
		showError('tenNguoiDung', 'Tên người dùng phải từ 2 đến 100 ký tự');
		isValid = false;
	}

	// Validate Email
	const email = document.getElementById('email').value.trim();
	const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
	if (!email) {
		showError('email', 'Email không được để trống');
		isValid = false;
	} else if (!emailRegex.test(email)) {
		showError('email', 'Email không đúng định dạng');
		isValid = false;
	}

	// Validate Mật khẩu
	const matKhau = document.getElementById('matKhau').value;
	if (!matKhau) {
		showError('matKhau', 'Mật khẩu không được để trống');
		isValid = false;
	} else if (matKhau.length < 6) {
		showError('matKhau', 'Mật khẩu phải có ít nhất 6 ký tự');
		isValid = false;
	}

	// Validate Xác nhận mật khẩu
	const xacNhanMatKhau = document.getElementById('xacNhanMatKhau').value;
	if (!xacNhanMatKhau) {
		showError('xacNhanMatKhau', 'Vui lòng xác nhận mật khẩu');
		isValid = false;
	} else if (matKhau !== xacNhanMatKhau) {
		showError('xacNhanMatKhau', 'Mật khẩu xác nhận không khớp');
		isValid = false;
	}

	// Validate Số điện thoại (BẮT BUỘC)
	const sdt = document.getElementById('sdt').value.trim();
	if (!sdt) {
		showError('sdt', 'Số điện thoại không được để trống');
		isValid = false;
	} else {
		const phoneRegex = /^(0[3|5|7|8|9])+([0-9]{8})$/;
		if (!phoneRegex.test(sdt)) {
			showError('sdt', 'Số điện thoại phải bắt đầu bằng 0 và có đúng 10 chữ số (VD: 0912345678)');
			isValid = false;
		} else if (sdt.length !== 10) {
			showError('sdt', 'Số điện thoại phải có đúng 10 chữ số');
			isValid = false;
		}
	}

	// Validate Địa chỉ (BẮT BUỘC)
	const diaChi = document.getElementById('diaChi').value.trim();
	if (!diaChi) {
		showError('diaChi', 'Địa chỉ không được để trống');
		isValid = false;
	} else if (diaChi.length < 5) {
		showError('diaChi', 'Địa chỉ phải có ít nhất 5 ký tự');
		isValid = false;
	} else if (diaChi.length > 255) {
		showError('diaChi', 'Địa chỉ không được vượt quá 255 ký tự');
		isValid = false;
	}

	// Validate Terms checkbox
	const agreeTerms = document.getElementById('agreeTerms');
	if (!agreeTerms.checked) {
		showError('agreeTerms', 'Bạn phải đồng ý với điều khoản dịch vụ');
		isValid = false;
	}

	return isValid;
}

function togglePassword(fieldId) {
	const passwordInput = document.getElementById(fieldId);
	const toggleIcon = passwordInput.parentElement.querySelector('.toggle-password i');

	if (passwordInput.type === 'password') {
		passwordInput.type = 'text';
		toggleIcon.className = 'fas fa-eye-slash';
	} else {
		passwordInput.type = 'password';
		toggleIcon.className = 'fas fa-eye';
	}
}

// UPDATED: Xử lý submit form với OTP
document.getElementById('registerForm').addEventListener('submit', function(e) {
	e.preventDefault();

	if (!validateForm()) {
		return;
	}

	const formData = {
		tenNguoiDung: document.getElementById('tenNguoiDung').value.trim(),
		email: document.getElementById('email').value.trim(),
		matKhau: document.getElementById('matKhau').value,
		sdt: document.getElementById('sdt').value.trim(),
		diaChi: document.getElementById('diaChi').value.trim()
	};

	const submitBtn = document.querySelector('.btn-register');
	const originalText = submitBtn.innerHTML;
	submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang gửi mã OTP...';
	submitBtn.disabled = true;

	// Gửi request tạo OTP
	fetch('/api/auth/send-otp', {
		method: 'POST',
		headers: {
			'Content-Type': 'application/json',
		},
		body: JSON.stringify(formData)
	})
		.then(response => response.json())
		.then(data => {
			if (data.success) {
				showSuccess(data.message + ' Đang chuyển hướng...');
				submitBtn.innerHTML = '<i class="fas fa-check"></i> Đã gửi OTP!';

				// Lưu email và thông tin đăng ký vào sessionStorage
				sessionStorage.setItem('registerEmail', formData.email);
				sessionStorage.setItem('registrationData', JSON.stringify(formData));

				// Chuyển đến trang xác thực OTP
				setTimeout(() => {
					window.location.href = '/verify-otp';
				}, 1500);
			} else {
				if (data.message.includes('Email')) {
					showError('email', data.message);
				} else if (data.message.includes('số điện thoại') || data.message.includes('SĐT')) {
					showError('sdt', data.message);
				} else {
					showError('email', data.message);
				}
				submitBtn.innerHTML = originalText;
				submitBtn.disabled = false;
			}
		})
		.catch(error => {
			showError('email', 'Đã xảy ra lỗi khi gửi mã OTP. Vui lòng thử lại.');
			submitBtn.innerHTML = originalText;
			submitBtn.disabled = false;
			console.error('Error:', error);
		});
});

// Ngăn chặn nhập ký tự không phải số vào trường số điện thoại
document.getElementById('sdt').addEventListener('keypress', function(e) {
	const charCode = e.which ? e.which : e.keyCode;
	if (charCode < 48 || charCode > 57) {
		e.preventDefault();
	}
});

// Kiểm tra nếu đã đăng nhập thì chuyển hướng
if (localStorage.getItem('jwtToken')) {
	window.location.href = '/';
}