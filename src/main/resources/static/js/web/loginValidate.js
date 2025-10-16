// Clear error khi người dùng nhập
document.addEventListener('DOMContentLoaded', function() {
    // Kiểm tra nếu đã đăng nhập thì chuyển hướng
    if (localStorage.getItem('jwtToken')) {
        // Kiểm tra role để chuyển hướng đúng
        const userData = localStorage.getItem('user');
        if (userData) {
            try {
				const user = JSON.parse(userData);
				// *** THAY ĐỔI START: Thêm logic VENDOR cho kiểm tra phiên đăng nhập ***
				const userRole = user.role || (user.vaiTro && user.vaiTro.maVaiTro);

				if (userRole === 'ADMIN') {
				    window.location.href = '/admin/dashboard';
				} else if (userRole === 'VENDOR') {
				    window.location.href = '/vendor/dashboard';
				} else if (userRole === 'SHIPPER') {
				    window.location.href = '/shipper/dashboard'; // Thêm dòng này
				} else {
				    window.location.href = '/';
				}
            } catch (e) {
                console.error('Error parsing user data:', e);
                // Xóa dữ liệu lỗi và reload
                localStorage.removeItem('user');
                localStorage.removeItem('jwtToken');
            }
        } else {
            window.location.href = '/';
        }
        return;
    }

    document.querySelectorAll('input').forEach(input => {
        input.addEventListener('input', function() {
            clearError(this.id);
        });
        
        input.addEventListener('focus', function() {
            this.parentElement.classList.add('focused');
        });
        
        input.addEventListener('blur', function() {
            if (!this.value) {
                this.parentElement.classList.remove('focused');
            }
        });
    });

	// Form submission handler
	    document.getElementById('loginForm').addEventListener('submit', function(e) {
	        e.preventDefault();
	        
	        if (!validateForm()) {
	            return;
	        }

	        const email = document.getElementById('email').value;
	        const matKhau = document.getElementById('matKhau').value;
	        const errorMessage = document.getElementById('errorMessage');
	        
	        // Reset error message
	        errorMessage.style.display = 'none';
	        errorMessage.textContent = '';
	        
	        // Hiệu ứng loading
	        const submitBtn = document.querySelector('.btn-login');
	        const originalText = submitBtn.innerHTML;
	        submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang đăng nhập...';
	        submitBtn.disabled = true;

	        fetch('/api/auth/login', {
	            method: 'POST',
	            headers: {
	                'Content-Type': 'application/json',
	            },
	            body: JSON.stringify({
	                email: email,
	                matKhau: matKhau
	            })
	        })
	        .then(response => response.json())
	        .then(data => {
	            if (data.success) {
	                // Lưu token vào localStorage
	                localStorage.setItem('jwtToken', data.data.token);
	                localStorage.setItem('user', JSON.stringify(data.data));
	                
	                // Lưu token vào cookie để server có thể truy cập
	                document.cookie = `jwtToken=${data.data.token}; path=/; max-age=86400; SameSite=Lax`;
	                
	                // Hiệu ứng thành công
	                submitBtn.innerHTML = '<i class="fas fa-check"></i> Đăng nhập thành công!';
	                
	                // Kiểm tra role để chuyển hướng đúng
	                setTimeout(() => {
	                    const userRole = data.data.role;
	                    console.log('User role after login:', userRole);
	                    
	                    // *** CẬP NHẬT LOGIC CHUYỂN HƯỚNG ***
						if (userRole === 'ADMIN') {
						    console.log('Redirecting to admin dashboard');
						    window.location.href = '/admin/dashboard';
						} else if (userRole === 'VENDOR') {
						    console.log('Redirecting to vendor dashboard');
						    window.location.href = '/vendor/dashboard';
						} else if (userRole === 'SHIPPER') {
						    console.log('Redirecting to shipper dashboard');
						    window.location.href = '/shipper/dashboard'; // Thêm dòng này
						} else {
						    console.log('Redirecting to home');
						    window.location.href = '/';
						}
	                    // *** THAY ĐỔI END ***
	                }, 1000);
	            } else {
	                errorMessage.textContent = data.message;
	                errorMessage.style.display = 'block';
	                submitBtn.innerHTML = originalText;
	                submitBtn.disabled = false;
	            }
	        })
	        .catch(error => {
	            errorMessage.textContent = 'Đã xảy ra lỗi khi đăng nhập. Vui lòng thử lại.';
	            errorMessage.style.display = 'block';
	            submitBtn.innerHTML = originalText;
	            submitBtn.disabled = false;
	            console.error('Error:', error);
	        });
	    });
});

function clearError(fieldId) {
    const input = document.getElementById(fieldId);
    const errorDiv = document.getElementById(fieldId + 'Error');
    
    if (input) {
        input.classList.remove('error');
        input.parentElement.classList.remove('error');
    }
    if (errorDiv) {
        errorDiv.textContent = '';
        errorDiv.style.display = 'none';
    }
}

function showError(fieldId, message) {
    const input = document.getElementById(fieldId);
    const errorDiv = document.getElementById(fieldId + 'Error');
    
    if (input) {
        input.classList.add('error');
        input.parentElement.classList.add('error');
    }
    if (errorDiv) {
        errorDiv.textContent = message;
        errorDiv.style.display = 'block';
    }
}

function validateForm() {
    let isValid = true;
    
    // Clear all errors
    document.querySelectorAll('.error-text').forEach(el => {
        el.style.display = 'none';
        el.textContent = '';
    });
    document.querySelectorAll('input').forEach(el => {
        el.classList.remove('error');
        el.parentElement.classList.remove('error');
    });
    
    // Validate Email
    const email = document.getElementById('email').value.trim();
    if (!email) {
        showError('email', 'Email không được để trống');
        isValid = false;
    } else {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) {
            showError('email', 'Email không đúng định dạng');
            isValid = false;
        }
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
    
    return isValid;
}

function togglePassword() {
    const passwordInput = document.getElementById('matKhau');
    const toggleIcon = document.querySelector('.toggle-password i');
    
    if (passwordInput.type === 'password') {
        passwordInput.type = 'text';
        toggleIcon.className = 'fas fa-eye-slash';
    } else {
        passwordInput.type = 'password';
        toggleIcon.className = 'fas fa-eye';
    }
}