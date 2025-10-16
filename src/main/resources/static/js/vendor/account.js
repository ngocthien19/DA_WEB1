// Global variables for vendor account
let currentVendor = null;
let isEditMode = false;
let isStoreEditMode = false;

// Load vendor profile when page loads
document.addEventListener('DOMContentLoaded', function() {
    console.log('Vendor Account page loaded');
    loadVendorProfile();
    initVendorAccountEventHandlers();
});

// Initialize event handlers
function initVendorAccountEventHandlers() {
    // Tab navigation
    document.addEventListener('click', function(e) {
        if (e.target.closest('.nav-item')) {
            const navItem = e.target.closest('.nav-item');
            const tabName = navItem.getAttribute('data-tab');
            switchVendorTab(tabName);
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
            handleVendorAvatarUpload(e.target);
        }
    });

    // Store image upload
    document.addEventListener('change', function(e) {
        if (e.target.id === 'storeImageInput') {
            handleStoreImageUpload(e.target);
        }
    });

    // Form submissions
    document.addEventListener('submit', function(e) {
        if (e.target.id === 'vendorProfileForm') {
            e.preventDefault();
            saveVendorProfile();
        } else if (e.target.id === 'storeInfoForm') {
            e.preventDefault();
            saveStoreInfo();
        } else if (e.target.id === 'vendorPasswordForm') {
            e.preventDefault();
            changeVendorPassword();
        }
    });

    // Real-time validation
    document.addEventListener('input', function(e) {
        if (e.target.id === 'sdt' && isEditMode) {
            validatePhoneNumber(e.target);
        }
        if (e.target.id === 'storePhone' && isStoreEditMode) {
            validateStorePhoneNumber(e.target);
        }
    });
}

// Validate phone number in real-time
function validatePhoneNumber(input) {
    const value = input.value.trim();
    const errorDiv = document.getElementById('sdtError');
    
    if (value === '') {
        input.classList.remove('error');
        errorDiv.style.display = 'none';
        return true;
    }
    
    const phoneRegex = /^0[0-9]{9}$/;
    if (!phoneRegex.test(value)) {
        input.classList.add('error');
        errorDiv.textContent = 'Số điện thoại phải bắt đầu bằng số 0 và có đúng 10 chữ số';
        errorDiv.style.display = 'block';
        return false;
    } else {
        input.classList.remove('error');
        errorDiv.style.display = 'none';
        return true;
    }
}

// Validate store phone number
function validateStorePhoneNumber(input) {
    const value = input.value.trim();
    const errorDiv = document.getElementById('storePhoneError');
    
    if (value === '') {
        input.classList.remove('error');
        errorDiv.style.display = 'none';
        return true;
    }
    
    const phoneRegex = /(84|0[3|5|7|8|9])+([0-9]{8})\b/;
    if (!phoneRegex.test(value)) {
        input.classList.add('error');
        errorDiv.textContent = 'Số điện thoại cửa hàng không hợp lệ';
        errorDiv.style.display = 'block';
        return false;
    } else {
        input.classList.remove('error');
        errorDiv.style.display = 'none';
        return true;
    }
}

// Load vendor profile from API
async function loadVendorProfile() {
    const token = localStorage.getItem('jwtToken');

    showLoadingState();

    if (!token) {
        showError('Vui lòng đăng nhập để xem thông tin tài khoản');
        setTimeout(() => {
            window.location.href = '/login';
        }, 2000);
        return;
    }

    try {
        const response = await fetch('/vendor/api/account/profile', {
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
            currentVendor = result.data;
            displayVendorProfile(currentVendor);
            showAccountContent();
        } else {
            showError('Lỗi: ' + result.message);
        }

    } catch (error) {
        console.error('Vendor profile loading error:', error);
        showError('Lỗi kết nối: ' + error.message);
    }
}

// Display vendor profile
function displayVendorProfile(vendor) {
    if (!vendor) {
        showError('Không có dữ liệu vendor');
        return;
    }

    try {
        
        const tenNguoiDung = escapeHtml(vendor.tenNguoiDung || 'Chưa có tên');
        const email = escapeHtml(vendor.email || '');
        const sdt = escapeHtml(vendor.sdt || 'Chưa cập nhật');
        const diaChi = escapeHtml(vendor.diaChi || '');
        const trangThai = escapeHtml(vendor.trangThai || 'Hoạt động');
        const vaiTroTen = escapeHtml(vendor.vaiTro?.tenVaiTro || 'VENDOR');

        // Determine status badge color
        let statusColor = '#6c757d';
        if (vendor.trangThai === 'Hoạt động') {
            statusColor = '#28a745';
        } else if (vendor.trangThai === 'Tạm khóa') {
            statusColor = '#ffc107';
        } else if (vendor.trangThai === 'Khóa') {
            statusColor = '#dc3545';
        }
        
        let statusBg = '#e9ecef';
        if (vendor.trangThai === 'Hoạt động') {
            statusBg = '#d4edda';
        } else if (vendor.trangThai === 'Tạm khóa') {
            statusBg = '#fff3cd';
        } else if (vendor.trangThai === 'Khóa') {
            statusBg = '#f8d7da';
        }

        const avatarPath = vendor.hinhAnh ? `/uploads/users/${vendor.hinhAnh}` : '/images/default-avatar.jpg';

        // Store information
        const storeInfo = vendor.storeInfo;
        const hasStore = storeInfo != null;

        const accountHTML = `
            <div class="account-content">
                <div class="account-sidebar">
                    <div class="vendor-avatar">
                        <img src="${avatarPath}" 
                             alt="Avatar" class="avatar-image" id="avatarPreview">
                        <div class="avatar-upload">
                            <label for="avatarInput" class="avatar-upload-label">
                                <i class="fas fa-camera"></i> Đổi ảnh
                            </label>
                            <input type="file" id="avatarInput" accept="image/*" style="display: none;">
                        </div>
                    </div>
                    
                    <div class="vendor-info-sidebar">
                        <h3>${tenNguoiDung}</h3>
                        <p><i class="fas fa-envelope"></i> ${email}</p>
                        <p><i class="fas fa-phone"></i> ${sdt}</p>
                        
                        <div class="role-status-container">
                            <div class="vendor-role">${vaiTroTen}</div>
                            <div class="vendor-status" style="background-color: ${statusBg}; color: ${statusColor};">
                                <i class="fas fa-circle" style="color: ${statusColor};"></i> ${trangThai}
                            </div>
                        </div>
                    </div>

                    <div class="account-nav">
                        <div class="nav-item active" data-tab="profile">
                            <i class="fas fa-user"></i>
                            <span>Thông tin cá nhân</span>
                        </div>
                        <div class="nav-item" data-tab="store" ${!hasStore ? 'style="display: none;"' : ''}>
                            <i class="fas fa-store"></i>
                            <span>Thông tin cửa hàng</span>
                        </div>
                        <div class="nav-item" data-tab="security">
                            <i class="fas fa-shield-alt"></i>
                            <span>Đổi mật khẩu</span>
                        </div>
                    </div>
                </div>

                <div class="account-main">
                    <!-- Personal Information Tab -->
                    <div class="account-tab active" id="profileTab">
                        <h2 class="section-title">Thông tin cá nhân</h2>
                        <form id="vendorProfileForm">
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
                                <button type="button" class="btn btn-primary" id="editProfileBtn" onclick="toggleVendorEditMode()">
                                    <i class="fas fa-edit"></i> Cập nhật thông tin
                                </button>
                                <button type="submit" class="btn btn-success" id="saveProfileBtn" style="display: none;">
                                    <i class="fas fa-save"></i> Lưu
                                </button>
                                <button type="button" class="btn btn-secondary" id="cancelProfileBtn" style="display: none;" onclick="cancelVendorEdit()">
                                    <i class="fas fa-times"></i> Hủy
                                </button>
                            </div>
                        </form>
                    </div>

                    <!-- Store Information Tab -->
                    <div class="account-tab" id="storeTab">
                        <h2 class="section-title">Thông tin cửa hàng</h2>
                        ${hasStore ? generateStoreInfoHTML(storeInfo) : '<p class="no-store-message">Bạn chưa có cửa hàng. Vui lòng liên hệ quản trị viên để tạo cửa hàng.</p>'}
                    </div>

                    <!-- Security Tab -->
                    <div class="account-tab" id="securityTab">
                        <h2 class="section-title">Đổi mật khẩu</h2>
                        <form id="vendorPasswordForm">
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
                                <button type="button" class="btn btn-secondary" onclick="resetVendorPasswordForm()">
                                    <i class="fas fa-undo"></i> Đặt lại
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        `;

        const accountContainer = document.getElementById('accountContent');
        if (accountContainer) {
            accountContainer.innerHTML = accountHTML;
        }

    } catch (error) {
        console.error('Error displaying vendor profile:', error);
        showError('Lỗi hiển thị thông tin: ' + error.message);
    }
}

// Generate store information HTML
function generateStoreInfoHTML(storeInfo) {
    const storeImagePath = storeInfo.hinhAnh ? `/uploads/stores/${storeInfo.hinhAnh}` : '/images/default-store.jpg';
    const storeStatus = storeInfo.trangThai ? 'Đang hoạt động' : 'Ngừng hoạt động';
    const statusColor = storeInfo.trangThai ? '#28a745' : '#dc3545';
    const statusBg = storeInfo.trangThai ? '#d4edda' : '#f8d7da';

    return `
        <form id="storeInfoForm">
            <div class="store-header">
                <div class="store-image">
                    <img src="${storeImagePath}" alt="Store Image" class="store-image-preview" id="storeImagePreview">
                    <div class="store-image-upload">
                        <label for="storeImageInput" class="store-upload-label">
                            <i class="fas fa-camera"></i> Đổi ảnh cửa hàng
                        </label>
                        <input type="file" id="storeImageInput" accept="image/*" style="display: none;">
                    </div>
                </div>
                <div class="store-basic-info">
                    <h3>${escapeHtml(storeInfo.tenCuaHang)}</h3>
                    <div class="store-status" style="background-color: ${statusBg}; color: ${statusColor};">
                        <i class="fas fa-circle" style="color: ${statusColor};"></i> ${storeStatus}
                    </div>
                    <p><i class="fas fa-map-marker-alt"></i> ${escapeHtml(storeInfo.diaChi || 'Chưa cập nhật')}</p>
                    <p><i class="fas fa-phone"></i> ${escapeHtml(storeInfo.soDienThoai || 'Chưa cập nhật')}</p>
                </div>
            </div>

            <div class="form-grid">
                <div class="form-group">
                    <label class="form-label">Tên cửa hàng *</label>
                    <input type="text" class="form-input" id="storeName" name="tenCuaHang" 
                           value="${escapeHtml(storeInfo.tenCuaHang)}" disabled required>
                    <div class="form-error" id="storeNameError">Vui lòng nhập tên cửa hàng</div>
                </div>
                
                <div class="form-group">
                    <label class="form-label">Số điện thoại cửa hàng *</label>
                    <input type="tel" class="form-input" id="storePhone" name="soDienThoai" 
                           value="${escapeHtml(storeInfo.soDienThoai || '')}" 
                           pattern="(84|0[3|5|7|8|9])+([0-9]{8})\b"
                           placeholder="Ví dụ: 0123456789" disabled required>
                    <div class="form-error" id="storePhoneError">Số điện thoại cửa hàng không hợp lệ</div>
                </div>
                
                <div class="form-group">
                    <label class="form-label">Email cửa hàng *</label>
                    <input type="email" class="form-input" id="storeEmail" name="email" 
                           value="${escapeHtml(storeInfo.email || '')}" 
                           placeholder="store@example.com" disabled required>
                    <div class="form-error" id="storeEmailError">Email cửa hàng không hợp lệ</div>
                </div>
                
                <div class="form-group">
                    <label class="form-label">Năm thành lập</label>
                    <input type="number" class="form-input" id="storeYear" name="namThanhLap" 
                           value="${storeInfo.namThanhLap || ''}" 
                           min="1900" max="2100" disabled>
                </div>

                <div class="form-group full-width">
                    <label class="form-label">Địa chỉ cửa hàng *</label>
                    <input type="text" class="form-input" id="storeAddress" name="diaChi" 
                           value="${escapeHtml(storeInfo.diaChi || '')}" 
                           placeholder="Nhập địa chỉ cửa hàng" disabled required>
                </div>

                <div class="form-group full-width">
                    <label class="form-label">Mô tả cửa hàng</label>
                    <textarea class="form-input" id="storeDescription" name="moTa" 
                              placeholder="Mô tả về cửa hàng của bạn..." 
                              rows="4" disabled>${escapeHtml(storeInfo.moTa || '')}</textarea>
                </div>

                <div class="form-group">
                    <label class="form-label">Đánh giá trung bình</label>
                    <input type="text" class="form-input" 
                           value="${storeInfo.danhGiaTrungBinh || 0} ⭐ (${storeInfo.soLuongDanhGia || 0} đánh giá)" readonly>
                </div>

                <div class="form-group">
                    <label class="form-label">Ngày tạo</label>
                    <input type="text" class="form-input" 
                           value="${new Date(storeInfo.ngayTao).toLocaleDateString('vi-VN')}" readonly>
                </div>
            </div>
            
            <div class="form-actions">
                <button type="button" class="btn btn-primary" id="editStoreBtn" onclick="toggleStoreEditMode()">
                    <i class="fas fa-edit"></i> Cập nhật thông tin cửa hàng
                </button>
                <button type="submit" class="btn btn-success" id="saveStoreBtn" style="display: none;">
                    <i class="fas fa-save"></i> Lưu thông tin cửa hàng
                </button>
                <button type="button" class="btn btn-secondary" id="cancelStoreBtn" style="display: none;" onclick="cancelStoreEdit()">
                    <i class="fas fa-times"></i> Hủy
                </button>
            </div>
        </form>
    `;
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
    const contentElement = document.getElementById('accountContent');
    
    if (loadingElement) loadingElement.style.display = 'block';
    if (errorElement) errorElement.style.display = 'none';
    if (contentElement) contentElement.style.display = 'none';
}

// Show account content
function showAccountContent() {
    const loadingElement = document.getElementById('loadingState');
    const errorElement = document.getElementById('errorState');
    const contentElement = document.getElementById('accountContent');
    
    if (loadingElement) loadingElement.style.display = 'none';
    if (errorElement) errorElement.style.display = 'none';
    if (contentElement) contentElement.style.display = 'block';
}

// Show error message
function showError(message) {
    const loadingElement = document.getElementById('loadingState');
    const errorElement = document.getElementById('errorState');
    const contentElement = document.getElementById('accountContent');
    const errorMessageElement = document.getElementById('errorMessage');
    
    if (loadingElement) loadingElement.style.display = 'none';
    if (contentElement) contentElement.style.display = 'none';
    if (errorElement) errorElement.style.display = 'block';
    if (errorMessageElement) errorMessageElement.textContent = message;
}

// Switch between vendor tabs
function switchVendorTab(tabName) {
    document.querySelectorAll('.nav-item').forEach(item => {
        item.classList.remove('active');
    });
    document.querySelectorAll('.account-tab').forEach(tab => {
        tab.classList.remove('active');
    });

    const selectedNavItem = document.querySelector(`.nav-item[data-tab="${tabName}"]`);
    const selectedTab = document.getElementById(`${tabName}Tab`);
    
    if (selectedNavItem) selectedNavItem.classList.add('active');
    if (selectedTab) selectedTab.classList.add('active');
}

// SỬA HÀM toggleVendorEditMode - THÊM GHI CHÚ
function toggleVendorEditMode() {
    isEditMode = !isEditMode;
    
    const editableFields = ['tenNguoiDung', 'sdt', 'diaChi'];
    const editBtn = document.getElementById('editProfileBtn');
    const saveBtn = document.getElementById('saveProfileBtn');
    const cancelBtn = document.getElementById('cancelProfileBtn');
    
    // Chỉ thay đổi trạng thái khi người dùng click nút "Cập nhật thông tin"
    // Khi save thành công sẽ tự động disable bằng hàm riêng
    if (isEditMode) {
        editableFields.forEach(fieldId => {
            const field = document.getElementById(fieldId);
            if (field) {
                field.disabled = false;
            }
        });
        
        if (editBtn) editBtn.style.display = 'none';
        if (saveBtn) saveBtn.style.display = 'inline-block';
        if (cancelBtn) cancelBtn.style.display = 'inline-block';
        
        const firstField = document.getElementById('tenNguoiDung');
        if (firstField) {
            setTimeout(() => firstField.focus(), 100);
        }
    } else {
        // Khi cancel thì disable fields và reset buttons
        disableVendorEditFields();
        resetVendorEditButtons();
    }
}

// SỬA HÀM toggleStoreEditMode - THÊM GHI CHÚ
function toggleStoreEditMode() {
    isStoreEditMode = !isStoreEditMode;
    
    const editableFields = ['storeName', 'storePhone', 'storeEmail', 'storeYear', 'storeAddress', 'storeDescription'];
    const editBtn = document.getElementById('editStoreBtn');
    const saveBtn = document.getElementById('saveStoreBtn');
    const cancelBtn = document.getElementById('cancelStoreBtn');
    
    // Chỉ thay đổi trạng thái khi người dùng click nút "Cập nhật thông tin cửa hàng"
    // Khi save thành công sẽ tự động disable bằng hàm riêng
    if (isStoreEditMode) {
        editableFields.forEach(fieldId => {
            const field = document.getElementById(fieldId);
            if (field) {
                field.disabled = false;
            }
        });
        
        if (editBtn) editBtn.style.display = 'none';
        if (saveBtn) saveBtn.style.display = 'inline-block';
        if (cancelBtn) cancelBtn.style.display = 'inline-block';
    } else {
        // Khi cancel thì disable fields và reset buttons
        disableStoreEditFields();
        resetStoreEditButtons();
    }
}

// SỬA HÀM cancelVendorEdit - SỬ DỤNG HÀM MỚI
function cancelVendorEdit() {
    if (currentVendor) {
        const tenField = document.getElementById('tenNguoiDung');
        const sdtField = document.getElementById('sdt');
        const diaChiField = document.getElementById('diaChi');
        
        if (tenField) tenField.value = currentVendor.tenNguoiDung || '';
        if (sdtField) sdtField.value = currentVendor.sdt || '';
        if (diaChiField) diaChiField.value = currentVendor.diaChi || '';
        
        // Sử dụng hàm mới để disable fields và reset buttons
        disableVendorEditFields();
        resetVendorEditButtons();
        isEditMode = false;
    }
}

// SỬA HÀM cancelStoreEdit - SỬ DỤNG HÀM MỚI
function cancelStoreEdit() {
    if (currentVendor && currentVendor.storeInfo) {
        const storeInfo = currentVendor.storeInfo;
        
        const storeNameField = document.getElementById('storeName');
        const storePhoneField = document.getElementById('storePhone');
        const storeEmailField = document.getElementById('storeEmail');
        const storeYearField = document.getElementById('storeYear');
        const storeAddressField = document.getElementById('storeAddress');
        const storeDescriptionField = document.getElementById('storeDescription');
        
        if (storeNameField) storeNameField.value = storeInfo.tenCuaHang || '';
        if (storePhoneField) storePhoneField.value = storeInfo.soDienThoai || '';
        if (storeEmailField) storeEmailField.value = storeInfo.email || '';
        if (storeYearField) storeYearField.value = storeInfo.namThanhLap || '';
        if (storeAddressField) storeAddressField.value = storeInfo.diaChi || '';
        if (storeDescriptionField) storeDescriptionField.value = storeInfo.moTa || '';
        
        // Sử dụng hàm mới để disable fields và reset buttons
        disableStoreEditFields();
        resetStoreEditButtons();
        isStoreEditMode = false;
    }
}

// Save vendor profile - SỬA HÀM NÀY
async function saveVendorProfile() {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
        showError('Phiên đăng nhập đã hết hạn');
        return;
    }

    if (!validateVendorProfileForm()) {
        return;
    }

    try {
        const formData = {
            tenNguoiDung: document.getElementById('tenNguoiDung').value.trim(),
            sdt: document.getElementById('sdt').value.trim(),
            diaChi: document.getElementById('diaChi').value.trim()
        };

        const response = await fetch('/vendor/api/account/update-profile', {
            method: 'PUT',
            headers: {
                'Authorization': 'Bearer ' + token,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(formData)
        });

        const result = await response.json();

        if (result.success) {
            currentVendor = result.data;
            
            // TỰ ĐỘNG DISABLE CÁC TRƯỜNG VÀ TRỞ VỀ TRẠNG THÁI BAN ĐẦU
            disableVendorEditFields();
            resetVendorEditButtons();
            isEditMode = false;
            
            showToast('Thành công!', 'Cập nhật thông tin thành công!', 'success');
            
            // Update localStorage user data
            const userData = localStorage.getItem('user');
            if (userData) {
                const user = JSON.parse(userData);
                user.username = formData.tenNguoiDung;
                localStorage.setItem('user', JSON.stringify(user));
            }
            
            // Update UI without reload
            updateVendorInfoInUI(result.data);
            
        } else {
            showToast('Lỗi!', 'Lỗi: ' + result.message, 'error');
        }

    } catch (error) {
        console.error('Save vendor profile error:', error);
        showToast('Lỗi!', 'Lỗi khi lưu thông tin: ' + error.message, 'error');
    }
}

// Save store information - SỬA HÀM NÀY
async function saveStoreInfo() {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
        showError('Phiên đăng nhập đã hết hạn');
        return;
    }

    if (!validateStoreInfoForm()) {
        return;
    }

    try {
        const formData = {
            tenCuaHang: document.getElementById('storeName').value.trim(),
            moTa: document.getElementById('storeDescription').value.trim(),
            diaChi: document.getElementById('storeAddress').value.trim(),
            soDienThoai: document.getElementById('storePhone').value.trim(),
            email: document.getElementById('storeEmail').value.trim(),
            namThanhLap: document.getElementById('storeYear').value ? parseInt(document.getElementById('storeYear').value) : null
        };

        const response = await fetch('/vendor/api/account/update-store', {
            method: 'PUT',
            headers: {
                'Authorization': 'Bearer ' + token,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(formData)
        });

        const result = await response.json();

        if (result.success) {
            currentVendor = result.data;
            
            // TỰ ĐỘNG DISABLE CÁC TRƯỜNG VÀ TRỞ VỀ TRẠNG THÁI BAN ĐẦU
            disableStoreEditFields();
            resetStoreEditButtons();
            isStoreEditMode = false;
            
            // FIX: Chỉ cập nhật dữ liệu, không reload trang
            updateStoreInfoInUI(result.data.storeInfo);
            showToast('Thành công!', 'Cập nhật thông tin cửa hàng thành công!', 'success');
            
        } else {
            showToast('Lỗi!', 'Lỗi: ' + result.message, 'error');
        }

    } catch (error) {
        console.error('Save store info error:', error);
        showToast('Lỗi!', 'Lỗi khi lưu thông tin cửa hàng: ' + error.message, 'error');
    }
}

// THÊM HÀM MỚI: Disable các trường edit vendor
function disableVendorEditFields() {
    const editableFields = ['tenNguoiDung', 'sdt', 'diaChi'];
    editableFields.forEach(fieldId => {
        const field = document.getElementById(fieldId);
        if (field) {
            field.disabled = true;
        }
    });
}

// THÊM HÀM MỚI: Reset nút edit vendor về trạng thái ban đầu
function resetVendorEditButtons() {
    const editBtn = document.getElementById('editProfileBtn');
    const saveBtn = document.getElementById('saveProfileBtn');
    const cancelBtn = document.getElementById('cancelProfileBtn');
    
    if (editBtn) editBtn.style.display = 'inline-block';
    if (saveBtn) saveBtn.style.display = 'none';
    if (cancelBtn) cancelBtn.style.display = 'none';
    
    clearFormErrors();
}

// THÊM HÀM MỚI: Disable các trường edit store
function disableStoreEditFields() {
    const editableFields = ['storeName', 'storePhone', 'storeEmail', 'storeYear', 'storeAddress', 'storeDescription'];
    editableFields.forEach(fieldId => {
        const field = document.getElementById(fieldId);
        if (field) {
            field.disabled = true;
        }
    });
}

// THÊM HÀM MỚI: Reset nút edit store về trạng thái ban đầu
function resetStoreEditButtons() {
    const editBtn = document.getElementById('editStoreBtn');
    const saveBtn = document.getElementById('saveStoreBtn');
    const cancelBtn = document.getElementById('cancelStoreBtn');
    
    if (editBtn) editBtn.style.display = 'inline-block';
    if (saveBtn) saveBtn.style.display = 'none';
    if (cancelBtn) cancelBtn.style.display = 'none';
    
    clearFormErrors();
}

// Hàm mới: Cập nhật thông tin cửa hàng trong UI mà không cần reload
function updateStoreInfoInUI(storeInfo) {
    if (!storeInfo) return;
    
    // Cập nhật thông tin cơ bản trong store header
    const storeNameElement = document.querySelector('.store-basic-info h3');
    if (storeNameElement && storeInfo.tenCuaHang) {
        storeNameElement.textContent = storeInfo.tenCuaHang;
    }
    
    const storeAddressElement = document.querySelector('.store-basic-info p:nth-child(3)');
    if (storeAddressElement && storeInfo.diaChi) {
        storeAddressElement.innerHTML = `<i class="fas fa-map-marker-alt"></i> ${escapeHtml(storeInfo.diaChi)}`;
    }
    
    const storePhoneElement = document.querySelector('.store-basic-info p:nth-child(4)');
    if (storePhoneElement && storeInfo.soDienThoai) {
        storePhoneElement.innerHTML = `<i class="fas fa-phone"></i> ${escapeHtml(storeInfo.soDienThoai)}`;
    }
    
    // Cập nhật form fields để đồng bộ dữ liệu
    const storeNameField = document.getElementById('storeName');
    const storePhoneField = document.getElementById('storePhone');
    const storeEmailField = document.getElementById('storeEmail');
    const storeYearField = document.getElementById('storeYear');
    const storeAddressField = document.getElementById('storeAddress');
    const storeDescriptionField = document.getElementById('storeDescription');
    
    if (storeNameField) storeNameField.value = storeInfo.tenCuaHang || '';
    if (storePhoneField) storePhoneField.value = storeInfo.soDienThoai || '';
    if (storeEmailField) storeEmailField.value = storeInfo.email || '';
    if (storeYearField) storeYearField.value = storeInfo.namThanhLap || '';
    if (storeAddressField) storeAddressField.value = storeInfo.diaChi || '';
    if (storeDescriptionField) storeDescriptionField.value = storeInfo.moTa || '';
    
    console.log('Store info updated in UI without page reload');
}

// Hàm mới: Cập nhật thông tin vendor trong UI
function updateVendorInfoInUI(vendorData) {
    if (!vendorData) return;
    
    // Cập nhật sidebar info
    const vendorNameElement = document.querySelector('.vendor-info-sidebar h3');
    if (vendorNameElement && vendorData.tenNguoiDung) {
        vendorNameElement.textContent = vendorData.tenNguoiDung;
    }
    
    const vendorPhoneElement = document.querySelector('.vendor-info-sidebar p:nth-child(3)');
    if (vendorPhoneElement) {
        vendorPhoneElement.innerHTML = `<i class="fas fa-phone"></i> ${vendorData.sdt || 'Chưa cập nhật'}`;
    }
    
    const vendorAddressElement = document.querySelector('.vendor-info-sidebar p:nth-child(4)');
    if (vendorAddressElement && vendorData.diaChi) {
        vendorAddressElement.innerHTML = `<i class="fas fa-map-marker-alt"></i> ${vendorData.diaChi}`;
    }
    
    console.log('Vendor info updated in UI');
}

// Change vendor password
async function changeVendorPassword() {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
        showError('Phiên đăng nhập đã hết hạn');
        return;
    }

    if (!validateVendorPasswordForm()) {
        return;
    }

    try {
        const formData = {
            currentPassword: document.getElementById('currentPassword').value,
            newPassword: document.getElementById('newPassword').value
        };

        const response = await fetch('/vendor/api/account/change-password', {
            method: 'PUT',
            headers: {
                'Authorization': 'Bearer ' + token,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(formData)
        });

        const result = await response.json();

        if (result.success) {
            resetVendorPasswordForm();
            showToast('Thành công!', 'Đổi mật khẩu thành công!', 'success');
        } else {
            showToast('Lỗi!', 'Lỗi: ' + result.message, 'error');
        }

    } catch (error) {
        console.error('Change vendor password error:', error);
        showToast('Lỗi!', 'Lỗi khi đổi mật khẩu: ' + error.message, 'error');
    }
}

// Validate vendor profile form
function validateVendorProfileForm() {
    let isValid = true;
    clearFormErrors();

    const tenNguoiDung = document.getElementById('tenNguoiDung').value.trim();
    const sdt = document.getElementById('sdt').value.trim();

    if (!tenNguoiDung) {
        showFieldError('tenNguoiDung', 'Vui lòng nhập họ và tên');
        isValid = false;
    }

    if (sdt && !validatePhoneNumber(document.getElementById('sdt'))) {
        isValid = false;
    }

    return isValid;
}

// Validate store info form
function validateStoreInfoForm() {
    let isValid = true;
    clearFormErrors();

    const storeName = document.getElementById('storeName').value.trim();
    const storePhone = document.getElementById('storePhone').value.trim();
    const storeEmail = document.getElementById('storeEmail').value.trim();
    const storeAddress = document.getElementById('storeAddress').value.trim();

    if (!storeName) {
        showFieldError('storeName', 'Vui lòng nhập tên cửa hàng');
        isValid = false;
    }

    if (!storePhone) {
        showFieldError('storePhone', 'Vui lòng nhập số điện thoại cửa hàng');
        isValid = false;
    } else if (!validateStorePhoneNumber(document.getElementById('storePhone'))) {
        isValid = false;
    }

    if (!storeEmail) {
        showFieldError('storeEmail', 'Vui lòng nhập email cửa hàng');
        isValid = false;
    } else if (!isValidEmail(storeEmail)) {
        showFieldError('storeEmail', 'Email cửa hàng không hợp lệ');
        isValid = false;
    }

    if (!storeAddress) {
        showFieldError('storeAddress', 'Vui lòng nhập địa chỉ cửa hàng');
        isValid = false;
    }

    return isValid;
}

// Validate vendor password form
function validateVendorPasswordForm() {
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

// Validate email format
function isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
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

// Reset vendor password form
function resetVendorPasswordForm() {
    const passwordForm = document.getElementById('vendorPasswordForm');
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

// Handle vendor avatar upload
function handleVendorAvatarUpload(input) {
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
        
        uploadVendorAvatar(file);
    }
}

// Handle store image upload
function handleStoreImageUpload(input) {
    const file = input.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = function(e) {
            const preview = document.getElementById('storeImagePreview');
            if (preview) {
                preview.src = e.target.result;
            }
        };
        reader.readAsDataURL(file);
        
        uploadStoreImage(file);
    }
}

// Upload vendor avatar
async function uploadVendorAvatar(file) {
    console.log('=== VENDOR UPLOAD AVATAR START ===');
    console.log('File info:', {
        name: file.name,
        size: file.size,
        type: file.type
    });

    const token = localStorage.getItem('jwtToken');
    if (!token) {
        console.error('No JWT token found');
        showError('Phiên đăng nhập đã hết hạn');
        return;
    }

    try {
        const formData = new FormData();
        formData.append('avatar', file);
        
        console.log('Sending request to /vendor/api/account/upload-avatar');

        const response = await fetch('/vendor/api/account/upload-avatar', {
            method: 'POST',
            headers: {
                'Authorization': 'Bearer ' + token
            },
            body: formData
        });

        console.log('Response status:', response.status);

        const result = await response.json();
        console.log('Response body:', result);

        if (result.success) {
            console.log('Vendor avatar upload successful');
            showToast('Thành công!', 'Cập nhật ảnh đại diện thành công!', 'success');
            // Không reload trang, chỉ cập nhật ảnh
            console.log('Avatar updated without page reload');
        } else {
            console.error('Vendor avatar upload failed:', result.message);
            showToast('Lỗi!', 'Lỗi: ' + result.message, 'error');
        }

    } catch (error) {
        console.error('=== VENDOR UPLOAD AVATAR ERROR ===');
        console.error('Upload vendor avatar error:', error);
        showToast('Lỗi!', 'Lỗi khi tải ảnh lên: ' + error.message, 'error');
    }
}

// Upload store image
async function uploadStoreImage(file) {
    console.log('=== VENDOR UPLOAD STORE IMAGE START ===');

    const token = localStorage.getItem('jwtToken');
    if (!token) {
        showError('Phiên đăng nhập đã hết hạn');
        return;
    }

    try {
        const formData = new FormData();
        formData.append('storeImage', file);

        const response = await fetch('/vendor/api/account/upload-store-image', {
            method: 'POST',
            headers: {
                'Authorization': 'Bearer ' + token
            },
            body: formData
        });

        const result = await response.json();

        if (result.success) {
            showToast('Thành công!', 'Cập nhật ảnh cửa hàng thành công!', 'success');
            // Không reload trang
            console.log('Store image updated without page reload');
        } else {
            showToast('Lỗi!', 'Lỗi: ' + result.message, 'error');
        }

    } catch (error) {
        console.error('Upload store image error:', error);
        showToast('Lỗi!', 'Lỗi khi tải ảnh cửa hàng lên: ' + error.message, 'error');
    }
}

// Show toast notification - FIXED: Màu xanh lá cây
function showToast(title, message, type = 'success') {
    // Remove existing toast
    const existingToast = document.querySelector('.toast');
    if (existingToast) {
        existingToast.remove();
    }

    // Create toast element
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    
    const icon = type === 'success' ? 'fa-check-circle' : 
                 type === 'error' ? 'fa-exclamation-circle' : 'fa-exclamation-triangle';
    
    // Màu xanh lá cây cho success
    const successColor = '#10b981';
    const errorColor = '#ef4444';
    const warningColor = '#f59e0b';
    
    const borderColor = type === 'success' ? successColor : 
                       type === 'error' ? errorColor : warningColor;
    
    const iconColor = type === 'success' ? successColor : 
                     type === 'error' ? errorColor : warningColor;

    toast.innerHTML = `
        <i class="fas ${icon} toast-icon" style="color: ${iconColor};"></i>
        <div class="toast-content">
            <div class="toast-title">${title}</div>
            <div class="toast-message">${message}</div>
        </div>
        <button class="toast-close">
            <i class="fas fa-times"></i>
        </button>
    `;

    // Apply border color
    toast.style.borderLeftColor = borderColor;

    document.body.appendChild(toast);

    // Show toast with animation
    setTimeout(() => {
        toast.classList.add('show');
    }, 100);

    // Auto hide after 3 seconds
    const autoHide = setTimeout(() => {
        hideToast(toast);
    }, 3000);

    // Close button event
    const closeBtn = toast.querySelector('.toast-close');
    closeBtn.addEventListener('click', () => {
        clearTimeout(autoHide);
        hideToast(toast);
    });
}

// Hide toast with animation
function hideToast(toast) {
    toast.classList.remove('show');
    toast.classList.add('hide');
    
    setTimeout(() => {
        if (toast.parentNode) {
            toast.parentNode.removeChild(toast);
        }
    }, 300);
}

// Make functions available globally
window.loadVendorProfile = loadVendorProfile;
window.toggleVendorEditMode = toggleVendorEditMode;
window.toggleStoreEditMode = toggleStoreEditMode;
window.cancelVendorEdit = cancelVendorEdit;
window.cancelStoreEdit = cancelStoreEdit;
window.switchVendorTab = switchVendorTab;
window.resetVendorPasswordForm = resetVendorPasswordForm;