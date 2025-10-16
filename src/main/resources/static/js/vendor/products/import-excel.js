let selectedFiles = [];
const MAX_FILES = 5;

// File upload handling
document.getElementById('uploadArea').addEventListener('click', function(e) {
	// Chỉ click vào input file khi không click vào nút xóa
	if (!e.target.closest('.btn') && !e.target.closest('.file-item')) {
		document.getElementById('excelFile').click();
	}
});

document.getElementById('excelFile').addEventListener('change', function(e) {
	if (e.target.files.length > 0) {
		handleFileSelect(Array.from(e.target.files));
	}
});

// Drag and drop functionality
const uploadArea = document.getElementById('uploadArea');
uploadArea.addEventListener('dragover', function(e) {
	e.preventDefault();
	uploadArea.style.background = '#e3f2fd';
	uploadArea.style.borderColor = '#e94560';
});

uploadArea.addEventListener('dragleave', function(e) {
	e.preventDefault();
	if (!uploadArea.classList.contains('has-files')) {
		uploadArea.style.background = '#f8fafc';
		uploadArea.style.borderColor = '#667eea';
	}
});

uploadArea.addEventListener('drop', function(e) {
	e.preventDefault();
	uploadArea.style.background = '#f8fafc';
	uploadArea.style.borderColor = '#667eea';

	const files = e.dataTransfer.files;
	if (files.length > 0) {
		handleFileSelect(Array.from(files));
	}
});

function handleFileSelect(files) {
	const validFiles = files.filter(file => {
		const isValidType = file.name.endsWith('.xlsx');
		const isNotDuplicate = !selectedFiles.some(f => f.name === file.name && f.size === file.size);
		const hasSpace = selectedFiles.length < MAX_FILES;

		if (!isValidType) {
			showToast('error', 'Lỗi', `File "${file.name}" không phải là file Excel (.xlsx)`);
			return false;
		}

		if (!hasSpace) {
			showToast('error', 'Lỗi', `Chỉ được phép upload tối đa ${MAX_FILES} file`);
			return false;
		}

		// Kiểm tra xem đã đủ MAX_FILES hay chưa
		if (selectedFiles.length >= MAX_FILES) {
			return false;
		}

		if (isNotDuplicate) {
			return true;
		} else {
			showToast('warning', 'Cảnh báo', `File "${file.name}" đã được chọn`);
			return false;
		}
	});

	if (validFiles.length > 0) {
		// Chỉ thêm các file hợp lệ và không vượt quá giới hạn
		const remainingSpace = MAX_FILES - selectedFiles.length;
		const filesToAddToAdd = validFiles.slice(0, remainingSpace);

		selectedFiles = [...selectedFiles, ...filesToAddToAdd];
		updateFileList();
		document.getElementById('importBtn').disabled = false;

		// Cảnh báo nếu có file bị bỏ qua do giới hạn
		if (validFiles.length > remainingSpace) {
			showToast('warning', 'Cảnh báo', `Đã bỏ qua ${validFiles.length - remainingSpace} file do giới hạn tối đa ${MAX_FILES} file.`);
		}
	}
}

function updateFileList() {
	const filesList = document.getElementById('filesList');
	const filesCount = document.getElementById('filesCount');
	const uploadArea = document.getElementById('uploadArea');

	filesList.innerHTML = '';

	if (selectedFiles.length > 0) {
		uploadArea.classList.add('has-files');
		filesCount.textContent = selectedFiles.length;
		filesCount.style.display = 'inline-flex';

		selectedFiles.forEach((file, index) => {
			const fileSize = formatFileSize(file.size);
			const fileItem = document.createElement('div');
			fileItem.className = 'file-item';
			fileItem.innerHTML = `
                        <div class="file-info-content">
                            <i class="fas fa-file-excel text-success"></i>
                            <div>
                                <div class="file-name">${file.name}</div>
                                <div class="file-size">${fileSize}</div>
                            </div>
                        </div>
                        <div class="file-actions">
                            <button type="button" class="btn btn-sm btn-outline-danger" onclick="removeFile(${index})">
                                <i class="fas fa-times"></i>
                            </button>
                        </div>
                    `;
			filesList.appendChild(fileItem);
		});
	} else {
		uploadArea.classList.remove('has-files');
		filesCount.style.display = 'none';
		document.getElementById('importBtn').disabled = true;
	}
}

function removeFile(index) {
	selectedFiles.splice(index, 1);
	updateFileList();
}

function clearAllFiles() {
	selectedFiles = [];
	document.getElementById('excelFile').value = '';
	updateFileList();
	document.getElementById('importResult').style.display = 'none';
}

function formatFileSize(bytes) {
	if (bytes === 0) return '0 Bytes';
	const k = 1024;
	const sizes = ['Bytes', 'KB', 'MB', 'GB'];
	const i = Math.floor(Math.log(bytes) / Math.log(k));
	return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function importExcel() {
	if (selectedFiles.length === 0) {
		showToast('error', 'Lỗi', 'Vui lòng chọn ít nhất 1 file Excel để import');
		return;
	}

	const formData = new FormData();
	selectedFiles.forEach((file, index) => {
		formData.append('excelFiles', file);
	});

	const importBtn = document.getElementById('importBtn');
	const originalText = importBtn.innerHTML;
	importBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Đang import...';
	importBtn.disabled = true;

	// Đổi API endpoint sang /vendor/products/import
	fetch('/vendor/products/import', {
		method: 'POST',
		body: formData
	})
		.then(response => response.json())
		.then(data => {
			importBtn.innerHTML = originalText;
			importBtn.disabled = false;

			const resultDiv = document.getElementById('importResult');
			const resultAlert = document.getElementById('resultAlert');
			const resultTitle = document.getElementById('resultTitle');
			const resultDetails = document.getElementById('resultDetails');
			const errorList = document.getElementById('errorList');

			resultDiv.style.display = 'block';
			errorList.innerHTML = '';

			if (data.success) {
				resultAlert.className = 'alert alert-success';
				resultTitle.innerHTML = '<i class="fas fa-check-circle me-2"></i>Import thành công!';
				resultDetails.innerHTML = `
                        <p><strong>Tổng file:</strong> ${selectedFiles.length}</p>
                        <p><strong>Tổng bản ghi:</strong> ${data.totalRecords}</p>
                        <p><strong>Thành công:</strong> ${data.successCount}</p>
                        <p><strong>Lỗi:</strong> ${data.errorCount}</p>
                        <p>${data.message}</p>
                    `;

				if (data.errors && data.errors.length > 0) {
					resultDetails.innerHTML += '<p class="mb-2"><strong>Chi tiết lỗi:</strong></p>';
					data.errors.forEach(error => {
						const li = document.createElement('li');
						li.className = 'text-danger';
						li.textContent = error;
						errorList.appendChild(li);
					});
				}

				showToast('success', 'Thành công', data.message);

				// Reload trang sau 1 giây nếu import thành công
				setTimeout(() => {
					window.location.href = '/vendor/products';
				}, 1000);

			} else {
				resultAlert.className = 'alert alert-danger';
				resultTitle.innerHTML = '<i class="fas fa-exclamation-circle me-2"></i>Import thất bại!';
				resultDetails.innerHTML = `<p>${data.message}</p>`;

				if (data.errors && data.errors.length > 0) {
					resultDetails.innerHTML += '<p class="mb-2"><strong>Chi tiết lỗi:</strong></p>';
					data.errors.forEach(error => {
						const li = document.createElement('li');
						li.className = 'text-danger';
						li.textContent = error;
						errorList.appendChild(li);
					});
				}

				showToast('error', 'Lỗi', data.message);
			}
		})
		.catch(error => {
			importBtn.innerHTML = originalText;
			importBtn.disabled = false;
			console.error('Error:', error);
			showToast('error', 'Lỗi', 'Đã có lỗi xảy ra khi import file');
		});
}

// Toast function
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