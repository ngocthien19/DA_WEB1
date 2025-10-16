document.addEventListener('DOMContentLoaded', function() {

	// Xử lý upload ảnh (code giữ nguyên)
	const uploadArea = document.getElementById('uploadArea');
	const fileInput = document.getElementById('storeImage');
	const uploadPreview = document.getElementById('uploadPreview');
	const previewImage = document.getElementById('previewImage');
	const changeImageBtn = document.getElementById('changeImage');
	const removeImageBtn = document.getElementById('removeImage');
	const hinhAnhInput = document.getElementById('hinhAnh');
	const submitBtn = document.getElementById('submitBtn');
	const btnText = submitBtn.querySelector('.btn-text');
	const loading = submitBtn.querySelector('.loading');
	const form = document.getElementById('storeRegisterForm');

	// Hàm helper để hiển thị lỗi validation
	function showFieldError(fieldId, message) {
		const input = document.getElementById(fieldId);
		const group = document.getElementById('group-' + fieldId);
		const errorDiv = document.getElementById('error-' + fieldId);

		if (group) {
			group.classList.add('has-error');
		}
		if (errorDiv) {
			errorDiv.textContent = message;
		}
	}

	// Hàm helper để xóa lỗi validation
	function clearFieldError(fieldId) {
		const group = document.getElementById('group-' + fieldId);
		const errorDiv = document.getElementById('error-' + fieldId);

		if (group) {
			group.classList.remove('has-error');
		}
		if (errorDiv) {
			errorDiv.textContent = '';
		}
	}

	// --- VALIDATION LOGIC ---

	// 1. Gắn sự kiện "input" cho các trường cần validate realtime
	const soDienThoaiInput = document.getElementById('soDienThoai');
	const emailInput = document.getElementById('email');

	if (soDienThoaiInput) {
		soDienThoaiInput.addEventListener('input', function() {
			validateSingleField(this);
		});
	}

	if (emailInput) {
		emailInput.addEventListener('input', function() {
			validateSingleField(this);
		});
	}

	// Hàm validate cho một trường cụ thể
	function validateSingleField(field) {
		const fieldId = field.id;

		// Xóa lỗi cũ
		clearFieldError(fieldId);

		// Kiểm tra trường bắt buộc (check HTML required attribute)
		if (field.hasAttribute('required') && !field.value.trim()) {
			showFieldError(fieldId, 'Trường này là bắt buộc.');
			return false;
		}

		// Kiểm tra SĐT: Phải bắt đầu bằng 0 và có đủ 10 chữ số
		if (fieldId === 'soDienThoai') {
			// Pattern: Bắt đầu bằng 0, theo sau là 9 chữ số
			const pattern = /^0[0-9]{9}$/;
			if (field.value.trim() && !pattern.test(field.value.trim())) {
				showFieldError(fieldId, 'Số điện thoại phải bắt đầu bằng 0 và có đủ 10 chữ số.');
				return false;
			}
		}

		// Kiểm tra Email (sử dụng HTML5 validity check)
		if (fieldId === 'email' && field.value.trim() && !field.validity.valid) {
			showFieldError(fieldId, 'Email không hợp lệ.');
			return false;
		}

		// Nếu không có lỗi
		return true;
	}

	// Xử lý form submit
	form.addEventListener('submit', function(e) {
		e.preventDefault();

		let isFormValid = true;
		let firstInvalidField = null;

		// 1. Validate tất cả các trường input chính
		const allFields = form.querySelectorAll('input[required], textarea[required]');

		allFields.forEach(field => {
			const isValid = validateSingleField(field);
			if (!isValid) {
				isFormValid = false;
				if (!firstInvalidField) {
					firstInvalidField = field;
				}
			}
		});

		// 2. Kiểm tra điều khoản
		const agreeTerms = document.getElementById('agreeTerms');
		if (!agreeTerms.checked) {
			isFormValid = false;

			if (typeof showToast === 'function') {
				showToast('error', 'Lỗi', 'Vui lòng đồng ý với Điều khoản dịch vụ và Chính sách bảo mật');
			} else {
				alert('Vui lòng đồng ý với Điều khoản dịch vụ và Chính sách bảo mật');
			}
		}

		if (!isFormValid) {
			if (firstInvalidField) {
				firstInvalidField.focus();
			}
			if (typeof showToast === 'function') {
				showToast('error', 'Lỗi', 'Vui lòng điền đầy đủ và chính xác các trường bắt buộc');
			}
			return;
		}

		// Nếu form hợp lệ, tiến hành submit
		showLoading();
		submitBtn.disabled = true;
		btnText.textContent = 'Đang xử lý...';
		form.submit();
	});

	// Xử lý file upload
	function handleFileUpload(file) {
		// Kiểm tra loại file
		if (!file.type.match('image.*')) {
			if (typeof showToast === 'function') {
				showToast('error', 'Lỗi', 'Vui lòng chọn file ảnh (JPG, PNG, GIF)');
			} else {
				alert('Vui lòng chọn file ảnh (JPG, PNG, GIF)');
			}
			return;
		}

		// Kiểm tra kích thước file (tối đa 5MB)
		if (file.size > 5 * 1024 * 1024) {
			if (typeof showToast === 'function') {
				showToast('error', 'Lỗi', 'Kích thước file không được vượt quá 5MB');
			} else {
				alert('Kích thước file không được vượt quá 5MB');
			}
			return;
		}

		const formData = new FormData();
		formData.append('file', file);

		showLoading();

		fetch('/files/api/upload/store-image', {
			method: 'POST',
			body: formData,
			headers: {
				'X-Requested-With': 'XMLHttpRequest'
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
					hinhAnhInput.value = data.filePath;

					const reader = new FileReader();
					reader.onload = function(e) {
						previewImage.src = e.target.result;
						uploadPreview.style.display = 'block';
						uploadArea.style.display = 'none';
					};
					reader.readAsDataURL(file);

					if (typeof showToast === 'function') {
						showToast('success', 'Thành công', 'Upload ảnh thành công', 3000);
					}
				} else {
					if (typeof showToast === 'function') {
						showToast('error', 'Lỗi', 'Lỗi khi upload ảnh: ' + (data.message || 'Unknown error'));
					}
				}
			})
			.catch(error => {
				console.error('Error:', error);
				if (typeof showToast === 'function') {
					showToast('error', 'Lỗi', 'Lỗi khi upload ảnh. Vui lòng thử lại.');
				}
			})
			.finally(() => {
				hideLoading();
			});
	}

	function showLoading() {
		loading.style.display = 'inline-block';
	}

	function hideLoading() {
		loading.style.display = 'none';
	}

	// Click vào upload area
	uploadArea.addEventListener('click', function() {
		fileInput.click();
	});

	// Kéo thả file
	uploadArea.addEventListener('dragover', function(e) {
		e.preventDefault();
		uploadArea.classList.add('dragover');
	});

	uploadArea.addEventListener('dragleave', function() {
		uploadArea.classList.remove('dragover');
	});

	uploadArea.addEventListener('drop', function(e) {
		e.preventDefault();
		uploadArea.classList.remove('dragover');

		if (e.dataTransfer.files.length) {
			handleFileUpload(e.dataTransfer.files[0]);
		}
	});

	// Thay đổi file input
	fileInput.addEventListener('change', function() {
		if (this.files.length) {
			handleFileUpload(this.files[0]);
		}
	});

	// Thay đổi ảnh
	changeImageBtn.addEventListener('click', function() {
		fileInput.click();
	});

	// Xóa ảnh
	removeImageBtn.addEventListener('click', function() {
		fileInput.value = '';
		hinhAnhInput.value = '';
		uploadPreview.style.display = 'none';
		uploadArea.style.display = 'block';
	});

	// Reset form khi page load (phòng trường hợp back từ success)
	if (window.performance && window.performance.navigation.type === window.performance.navigation.TYPE_BACK) {
		form.reset();
		uploadPreview.style.display = 'none';
		uploadArea.style.display = 'block';
	}
});