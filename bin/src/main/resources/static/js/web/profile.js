// Global variables
let currentUser = null;
let isEditMode = false;

// Load user profile when page loads
document.addEventListener('DOMContentLoaded', function() {
    loadUserProfile();
    initProfileEventHandlers();
});

// Initialize event handlers
function initProfileEventHandlers() {
    // Tab navigation
    document.addEventListener('click', function(e) {
        if (e.target.closest('.nav-item')) {
            const navItem = e.target.closest('.nav-item');
            const tabName = navItem.getAttribute('data-tab');
            switchTab(tabName);
        }
    });

    // Password toggle
    document.addEventListener('click', function(e) {
        if (e.target.classList.contains('toggle-password')) {
            togglePasswordVisibility(e.target);
        }
    });

    // Avatar upload
    document.addEventListener('change', function(e) {
        if (e.target.id === 'avatarInput') {
            handleAvatarUpload(e.target);
        }
    });

    // Form submissions
    document.addEventListener('submit', function(e) {
        if (e.target.id === 'profileForm') {
            e.preventDefault();
            saveProfile();
        } else if (e.target.id === 'passwordForm') {
            e.preventDefault();
            changePassword();
        }
    });
}

// Load user profile from API
async function loadUserProfile() {
    const token = localStorage.getItem('jwtToken');

    // Show loading state
    showLoadingState();

    if (!token) {
        showError('Vui lòng đăng nhập để xem trang cá nhân');
        setTimeout(() => {
            window.location.href = '/login';
        }, 2000);
        return;
    }

    try {
        const response = await fetch('/profile/api/user/profile', {
            headers: {
                'Authorization': 'Bearer ' + token,
                'Content-Type': 'application/json'
            }
        });

        if (response.status === 401) {
            localStorage.removeItem('jwtToken');
            localStorage.removeItem('user');
            showError('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
            setTimeout(() => {
                window.location.href = '/login';
            }, 3000);
            return;
        }

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const result = await response.json();

        if (result.success) {
            currentUser = result.data;
            displayUserProfile(currentUser);
            showProfileContent();
        } else {
            showError('Lỗi: ' + result.message);
        }

    } catch (error) {
        console.error('Profile loading error:', error);
        showError('Lỗi kết nối: ' + error.message);
    }
}

// Display user profile
function displayUserProfile(user) {
    if (!user) {
        showError('Không có dữ liệu người dùng');
        return;
    }

    try {
        // Safely handle user data with proper escaping
        const tenNguoiDung = escapeHtml(user.tenNguoiDung || 'Chưa có tên');
        const email = escapeHtml(user.email || '');
        const sdt = escapeHtml(user.sdt || 'Chưa cập nhật');
        const diaChi = escapeHtml(user.diaChi || '');
        const trangThai = escapeHtml(user.trangThai || 'Hoạt động');
        const vaiTroTen = escapeHtml(user.vaiTro?.tenVaiTro || 'USER');

        // Determine status badge color
        const statusColor = user.trangThai === 'Hoạt động' ? '#28a745' :
            user.trangThai === 'Tạm khóa' ? '#ffc107' : '#dc3545';

        // Construct avatar image path
        const avatarPath = user.hinhAnh ? `/uploads/images/${user.hinhAnh}` : '/images/default-avatar.jpg';

        const profileHTML = `
            <div class="profile-content">
                <!-- Sidebar -->
                <div class="profile-sidebar">
                    <div class="user-avatar">
                        <img src="${avatarPath}" 
                             alt="Avatar" class="avatar-image" id="avatarPreview"
                             onerror="this.src='/images/default-avatar.jpg'">
                        <div class="avatar-upload">
                            <label for="avatarInput" class="avatar-upload-label">
                                <i class="fas fa-camera"></i> Đổi ảnh
                            </label>
                            <input type="file" id="avatarInput" accept="image/*" style="display: none;">
                        </div>
                    </div>
                    
                    <div class="user-info-sidebar">
                        <h3>${tenNguoiDung}</h3>
                        <p><i class="fas fa-envelope"></i> ${email}</p>
                        <p><i class="fas fa-phone"></i> ${sdt}</p>
                        <div class="user-role">${vaiTroTen}</div>
                        <div class="user-status" style="background-color: ${statusColor}">
                            <i class="fas fa-circle"></i> ${trangThai}
                        </div>
                    </div>

                    <div class="profile-nav">
                        <div class="nav-item active" data-tab="profile">
                            <i class="fas fa-user"></i>
                            <span>Thông tin cá nhân</span>
                        </div>
                        <div class="nav-item" data-tab="security">
                            <i class="fas fa-shield-alt"></i>
                            <span>Đổi mật khẩu</span>
                        </div>
                    </div>
                </div>

                <!-- Main Content -->
                <div class="profile-main">
                    <!-- Profile Tab -->
                    <div class="profile-tab active" id="profileTab">
                        <h2 class="section-title">Thông tin cá nhân</h2>
                        <form id="profileForm">
                            <div class="form-grid">
                                <div class="form-group">
                                    <label class="form-label">Họ và tên *</label>
                                    <input type="text" class="form-input" id="tenNguoiDung" name="tenNguoiDung" 
                                           value="${tenNguoiDung}" disabled required>
                                    <div class="form-error" id="tenNguoiDungError">Vui lòng nhập họ và tên</div>
                                </div>
                                
                                <div class="form-group">
                                    <label class="form-label">Email *</label>
                                    <input type="email" class="form-input" value="${email}" readonly>
                                    <small style="color: #666; font-size: 12px;">Email không thể thay đổi</small>
                                </div>
                                
                                <div class="form-group">
                                    <label class="form-label">Số điện thoại</label>
                                    <input type="tel" class="form-input" id="sdt" name="sdt" 
                                           value="${sdt === 'Chưa cập nhật' ? '' : sdt}" 
                                           pattern="^0[0-9]{9}$"
                                           placeholder="Ví dụ: 0123456789" disabled>
                                    <div class="form-error" id="sdtError">Số điện thoại phải bắt đầu bằng số 0 và có đúng 10 chữ số</div>
                                </div>
                                
                                <div class="form-group">
                                    <label class="form-label">Vai trò</label>
                                    <input type="text" class="form-input" 
                                           value="${vaiTroTen}" readonly>
                                    <small style="color: #666; font-size: 12px;">Vai trò được quản lý bởi hệ thống</small>
                                </div>

                                <div class="form-group full-width">
                                    <label class="form-label">Địa chỉ</label>
                                    <input type="text" class="form-input" id="diaChi" name="diaChi" 
                                           value="${diaChi}" 
                                           placeholder="Nhập địa chỉ của bạn" disabled>
                                </div>

                                <div class="form-group full-width">
                                    <label class="form-label">Trạng thái tài khoản</label>
                                    <input type="text" class="form-input" 
                                           value="${trangThai}" readonly>
                                    <small style="color: #666; font-size: 12px;">Trạng thái được quản lý bởi hệ thống</small>
                                </div>
                            </div>
                            
                            <div class="form-actions">
                                <button type="button" class="btn btn-primary" id="editProfileBtn" onclick="toggleEditMode()">
                                    <i class="fas fa-edit"></i> Cập nhật thông tin
                                </button>
                                <button type="submit" class="btn btn-success" id="saveProfileBtn" style="display: none;">
                                    <i class="fas fa-save"></i> Lưu
                                </button>
                                <button type="button" class="btn btn-secondary" id="cancelProfileBtn" style="display: none;" onclick="cancelEdit()">
                                    <i class="fas fa-times"></i> Hủy
                                </button>
                            </div>
                        </form>
                    </div>

                    <!-- Security Tab -->
                    <div class="profile-tab" id="securityTab">
                        <h2 class="section-title">Đổi mật khẩu</h2>
                        <form id="passwordForm">
                            <div class="form-grid">
                                <div class="form-group full-width">
                                    <label class="form-label">Mật khẩu hiện tại *</label>
                                    <div class="password-toggle">
                                        <input type="password" class="form-input" id="currentPassword" name="currentPassword" required>
                                        <i class="fas fa-eye toggle-password"></i>
                                    </div>
                                    <div class="form-error" id="currentPasswordError">Vui lòng nhập mật khẩu hiện tại</div>
                                </div>
                                
                                <div class="form-group">
                                    <label class="form-label">Mật khẩu mới *</label>
                                    <div class="password-toggle">
                                        <input type="password" class="form-input" id="newPassword" name="newPassword" 
                                               minlength="6" required>
                                        <i class="fas fa-eye toggle-password"></i>
                                    </div>
                                    <div class="form-error" id="newPasswordError">Mật khẩu phải có ít nhất 6 ký tự</div>
                                </div>
                                
                                <div class="form-group">
                                    <label class="form-label">Xác nhận mật khẩu *</label>
                                    <div class="password-toggle">
                                        <input type="password" class="form-input" id="confirmPassword" name="confirmPassword" required>
                                        <i class="fas fa-eye toggle-password"></i>
                                    </div>
                                    <div class="form-error" id="confirmPasswordError">Mật khẩu xác nhận không khớp</div>
                                </div>
                            </div>
                            
                            <div class="form-actions">
                                <button type="submit" class="btn btn-primary">
                                    <i class="fas fa-key"></i> Đổi mật khẩu
                                </button>
                                <button type="button" class="btn btn-secondary" onclick="resetPasswordForm()">
                                    <i class="fas fa-undo"></i> Đặt lại
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        `;

        const profileContainer = document.getElementById('profileContent');
        if (profileContainer) {
            profileContainer.innerHTML = profileHTML;
        }

    } catch (error) {
        console.error('Error displaying profile:', error);
        showError('Lỗi hiển thị thông tin profile: ' + error.message);
    }
}

// Utility function to escape HTML
function escapeHtml(text) {
    if (typeof text !== 'string') return '';
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, function(m) { return map[m]; });
}

// Show loading state
function showLoadingState() {
    const loadingElement = document.getElementById('loadingState');
    const errorElement = document.getElementById('errorState');
    const contentElement = document.getElementById('profileContent');
    
    if (loadingElement) loadingElement.style.display = 'block';
    if (errorElement) errorElement.style.display = 'none';
    if (contentElement) contentElement.style.display = 'none';
}

// Show profile content
function showProfileContent() {
    const loadingElement = document.getElementById('loadingState');
    const errorElement = document.getElementById('errorState');
    const contentElement = document.getElementById('profileContent');
    
    if (loadingElement) loadingElement.style.display = 'none';
    if (errorElement) errorElement.style.display = 'none';
    if (contentElement) contentElement.style.display = 'block';
}

// Show error message
function showError(message) {
    const loadingElement = document.getElementById('loadingState');
    const errorElement = document.getElementById('errorState');
    const contentElement = document.getElementById('profileContent');
    const errorMessageElement = document.getElementById('errorMessage');
    
    if (loadingElement) loadingElement.style.display = 'none';
    if (contentElement) contentElement.style.display = 'none';
    if (errorElement) errorElement.style.display = 'block';
    if (errorMessageElement) errorMessageElement.textContent = message;
}

// Switch between tabs
function switchTab(tabName) {
    // Remove active class from all nav items and tabs
    document.querySelectorAll('.nav-item').forEach(item => {
        item.classList.remove('active');
    });
    document.querySelectorAll('.profile-tab').forEach(tab => {
        tab.classList.remove('active');
    });

    // Add active class to selected nav item and tab
    const selectedNavItem = document.querySelector(`.nav-item[data-tab="${tabName}"]`);
    const selectedTab = document.getElementById(`${tabName}Tab`);
    
    if (selectedNavItem) selectedNavItem.classList.add('active');
    if (selectedTab) selectedTab.classList.add('active');
}

// Toggle edit mode
function toggleEditMode() {
    isEditMode = !isEditMode;
    
    const editableFields = ['tenNguoiDung', 'sdt', 'diaChi'];
    const editBtn = document.getElementById('editProfileBtn');
    const saveBtn = document.getElementById('saveProfileBtn');
    const cancelBtn = document.getElementById('cancelProfileBtn');
    
    editableFields.forEach(fieldId => {
        const field = document.getElementById(fieldId);
        if (field) {
            field.disabled = !isEditMode;
        }
    });
    
    if (isEditMode) {
        if (editBtn) editBtn.style.display = 'none';
        if (saveBtn) saveBtn.style.display = 'inline-block';
        if (cancelBtn) cancelBtn.style.display = 'inline-block';
    } else {
        if (editBtn) editBtn.style.display = 'inline-block';
        if (saveBtn) saveBtn.style.display = 'none';
        if (cancelBtn) cancelBtn.style.display = 'none';
    }
}

// Cancel edit mode
function cancelEdit() {
    if (currentUser) {
        // Reset form values to original data
        const tenField = document.getElementById('tenNguoiDung');
        const sdtField = document.getElementById('sdt');
        const diaChiField = document.getElementById('diaChi');
        
        if (tenField) tenField.value = currentUser.tenNguoiDung || '';
        if (sdtField) sdtField.value = currentUser.sdt || '';
        if (diaChiField) diaChiField.value = currentUser.diaChi || '';
        
        // Clear errors
        clearFormErrors();
        
        // Exit edit mode
        isEditMode = false;
        toggleEditMode();
    }
}

// Save profile
async function saveProfile() {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
        showError('Phiên đăng nhập đã hết hạn');
        return;
    }

    // Validate form
    if (!validateProfileForm()) {
        return;
    }

    try {
        const formData = {
            tenNguoiDung: document.getElementById('tenNguoiDung').value.trim(),
            sdt: document.getElementById('sdt').value.trim(),
            diaChi: document.getElementById('diaChi').value.trim()
        };

        const response = await fetch('/profile/api/user/update', {
            method: 'PUT',
            headers: {
                'Authorization': 'Bearer ' + token,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(formData)
        });

        const result = await response.json();

        if (result.success) {
            currentUser = result.data;
            isEditMode = false;
            toggleEditMode();
            showSuccessMessage('Cập nhật thông tin thành công!');
            
            // Update localStorage user data
            const userData = localStorage.getItem('user');
            if (userData) {
                const user = JSON.parse(userData);
                user.username = formData.tenNguoiDung;
                localStorage.setItem('user', JSON.stringify(user));
            }
        } else {
            showError('Lỗi: ' + result.message);
        }

    } catch (error) {
        console.error('Save profile error:', error);
        showError('Lỗi khi lưu thông tin: ' + error.message);
    }
}

// Change password
async function changePassword() {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
        showError('Phiên đăng nhập đã hết hạn');
        return;
    }

    // Validate password form
    if (!validatePasswordForm()) {
        return;
    }

    try {
        const formData = {
            currentPassword: document.getElementById('currentPassword').value,
            newPassword: document.getElementById('newPassword').value
        };

        const response = await fetch('/profile/api/user/change-password', {
            method: 'PUT',
            headers: {
                'Authorization': 'Bearer ' + token,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(formData)
        });

        const result = await response.json();

        if (result.success) {
            resetPasswordForm();
            showSuccessMessage('Đổi mật khẩu thành công!');
        } else {
            showError('Lỗi: ' + result.message);
        }

    } catch (error) {
        console.error('Change password error:', error);
        showError('Lỗi khi đổi mật khẩu: ' + error.message);
    }
}

// Validate profile form
function validateProfileForm() {
    let isValid = true;
    clearFormErrors();

    const tenNguoiDung = document.getElementById('tenNguoiDung').value.trim();
    const sdt = document.getElementById('sdt').value.trim();

    if (!tenNguoiDung) {
        showFieldError('tenNguoiDung', 'Vui lòng nhập họ và tên');
        isValid = false;
    }

    if (sdt && !/^0[0-9]{9}$/.test(sdt)) {
        showFieldError('sdt', 'Số điện thoại phải bắt đầu bằng số 0 và có đúng 10 chữ số');
        isValid = false;
    }

    return isValid;
}

// Validate password form
function validatePasswordForm() {
    let isValid = true;
    clearFormErrors();

    const currentPassword = document.getElementById('currentPassword').value;
    const newPassword = document.getElementById('newPassword').value;
    const confirmPassword = document.getElementById('confirmPassword').value;

    if (!currentPassword) {
        showFieldError('currentPassword', 'Vui lòng nhập mật khẩu hiện tại');
        isValid = false;
    }

    if (!newPassword) {
        showFieldError('newPassword', 'Vui lòng nhập mật khẩu mới');
        isValid = false;
    } else if (newPassword.length < 6) {
        showFieldError('newPassword', 'Mật khẩu phải có ít nhất 6 ký tự');
        isValid = false;
    }

    if (newPassword !== confirmPassword) {
        showFieldError('confirmPassword', 'Mật khẩu xác nhận không khớp');
        isValid = false;
    }

    return isValid;
}

// Show field error
function showFieldError(fieldId, message) {
    const field = document.getElementById(fieldId);
    const errorDiv = document.getElementById(fieldId + 'Error');
    
    if (field) {
        field.classList.add('error');
    }
    if (errorDiv) {
        errorDiv.textContent = message;
        errorDiv.style.display = 'block';
    }
}

// Clear form errors
function clearFormErrors() {
    document.querySelectorAll('.form-error').forEach(error => {
        error.style.display = 'none';
        error.textContent = '';
    });
    document.querySelectorAll('.form-input').forEach(input => {
        input.classList.remove('error');
    });
}

// Reset password form
function resetPasswordForm() {
    const passwordForm = document.getElementById('passwordForm');
    if (passwordForm) {
        passwordForm.reset();
        clearFormErrors();
    }
}

// Toggle password visibility
function togglePasswordVisibility(toggleElement) {
    const passwordInput = toggleElement.parentElement.querySelector('input');
    if (passwordInput) {
        if (passwordInput.type === 'password') {
            passwordInput.type = 'text';
            toggleElement.className = 'fas fa-eye-slash toggle-password';
        } else {
            passwordInput.type = 'password';
            toggleElement.className = 'fas fa-eye toggle-password';
        }
    }
}

// Handle avatar upload
function handleAvatarUpload(input) {
    const file = input.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = function(e) {
            const preview = document.getElementById('avatarPreview');
            if (preview) {
                preview.src = e.target.result;
            }
        };
        reader.readAsDataURL(file);
        
        // Upload avatar
        uploadAvatar(file);
    }
}

// Upload avatar
async function uploadAvatar(file) {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
        showError('Phiên đăng nhập đã hết hạn');
        return;
    }

    try {
        const formData = new FormData();
        formData.append('avatar', file);

        const response = await fetch('/profile/api/user/upload-avatar', {
            method: 'POST',
            headers: {
                'Authorization': 'Bearer ' + token
            },
            body: formData
        });

        const result = await response.json();

        if (result.success) {
            showSuccessMessage('Cập nhật ảnh đại diện thành công!');
            // Refresh profile data
            await loadUserProfile();
        } else {
            showError('Lỗi: ' + result.message);
        }

    } catch (error) {
        console.error('Upload avatar error:', error);
        showError('Lỗi khi tải ảnh lên: ' + error.message);
    }
}

// Show success message
function showSuccessMessage(message) {
    // Create or update success message element
    let successDiv = document.getElementById('successMessage');
    if (!successDiv) {
        successDiv = document.createElement('div');
        successDiv.id = 'successMessage';
        successDiv.className = 'alert alert-success';
        successDiv.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            background-color: #d4edda;
            color: #155724;
            padding: 12px 20px;
            border: 1px solid #c3e6cb;
            border-radius: 4px;
            z-index: 1000;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        `;
        document.body.appendChild(successDiv);
    }
    
    successDiv.textContent = message;
    successDiv.style.display = 'block';
    
    // Auto hide after 3 seconds
    setTimeout(() => {
        successDiv.style.display = 'none';
    }, 3000);
}