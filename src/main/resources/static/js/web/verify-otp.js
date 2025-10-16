// Lấy email từ sessionStorage
const userEmail = sessionStorage.getItem('registerEmail');
if (!userEmail) {
    window.location.href = '/register';
}

document.getElementById('emailDisplay').textContent = userEmail;

// OTP Input handling
const otpInputs = document.querySelectorAll('.otp-input');

otpInputs.forEach((input, index) => {
    input.addEventListener('input', function(e) {
        // Chỉ cho phép nhập số
        this.value = this.value.replace(/[^0-9]/g, '');
        
        // Tự động chuyển sang ô tiếp theo
        if (this.value.length === 1 && index < otpInputs.length - 1) {
            otpInputs[index + 1].focus();
        }
        
        // Clear error state
        clearMessages();
        this.classList.remove('error');
    });

    input.addEventListener('keydown', function(e) {
        // Xử lý phím Backspace
        if (e.key === 'Backspace' && !this.value && index > 0) {
            otpInputs[index - 1].focus();
        }
        
        // Xử lý phím mũi tên
        if (e.key === 'ArrowLeft' && index > 0) {
            otpInputs[index - 1].focus();
        }
        if (e.key === 'ArrowRight' && index < otpInputs.length - 1) {
            otpInputs[index + 1].focus();
        }
    });

    input.addEventListener('paste', function(e) {
        e.preventDefault();
        const pastedData = e.clipboardData.getData('text').replace(/[^0-9]/g, '');
        
        for (let i = 0; i < pastedData.length && index + i < otpInputs.length; i++) {
            otpInputs[index + i].value = pastedData[i];
        }
        
        // Focus vào ô cuối cùng được điền
        const lastFilledIndex = Math.min(index + pastedData.length - 1, otpInputs.length - 1);
        otpInputs[lastFilledIndex].focus();
    });
});

// Focus vào ô đầu tiên khi load trang
otpInputs[0].focus();

// Countdown timer
let countdown = 60;
const countdownElement = document.getElementById('countdown');
const resendBtn = document.getElementById('resendBtn');
const resendText = document.getElementById('resendText');

function startCountdown() {
    countdown = 60;
    resendBtn.disabled = true;
    resendText.textContent = 'Gửi lại sau ';
    
    const timer = setInterval(() => {
        countdown--;
        countdownElement.textContent = countdown;
        
        if (countdown <= 0) {
            clearInterval(timer);
            resendBtn.disabled = false;
            resendText.textContent = 'Gửi lại mã';
            countdownElement.textContent = '';
        }
    }, 1000);
}

// Bắt đầu countdown khi load trang
startCountdown();

// Resend OTP
resendBtn.addEventListener('click', function() {
    if (this.disabled) return;
    
    const registrationData = sessionStorage.getItem('registrationData');
    if (!registrationData) {
        showError('Không tìm thấy thông tin đăng ký. Vui lòng đăng ký lại.');
        setTimeout(() => {
            window.location.href = '/register';
        }, 2000);
        return;
    }
    
    const originalText = this.innerHTML;
    this.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang gửi...';
    this.disabled = true;
    
    fetch('/api/auth/send-otp', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: registrationData
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showSuccess('Mã OTP mới đã được gửi đến email của bạn!');
            // Clear OTP inputs
            otpInputs.forEach(input => input.value = '');
            otpInputs[0].focus();
            startCountdown();
        } else {
            showError(data.message);
            this.disabled = false;
            this.innerHTML = originalText;
        }
    })
    .catch(error => {
        showError('Không thể gửi lại mã OTP. Vui lòng thử lại.');
        this.disabled = false;
        this.innerHTML = originalText;
        console.error('Error:', error);
    });
});

// Form submit
document.getElementById('verifyForm').addEventListener('submit', function(e) {
    e.preventDefault();
    
    // Lấy mã OTP
    let otpCode = '';
    otpInputs.forEach(input => {
        otpCode += input.value;
    });
    
    // Validate OTP
    if (otpCode.length !== 6) {
        showError('Vui lòng nhập đầy đủ 6 chữ số mã OTP');
        otpInputs.forEach(input => {
            if (!input.value) {
                input.classList.add('error');
            }
        });
        return;
    }
    
    const submitBtn = document.querySelector('.btn-verify');
    const originalText = submitBtn.innerHTML;
    submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang xác thực...';
    submitBtn.disabled = true;
    
    // Gửi request xác thực
    fetch('/api/auth/verify-otp', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            email: userEmail,
            otpCode: otpCode
        })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showSuccess(data.message + ' Đang chuyển hướng đến trang đăng nhập...');
            submitBtn.innerHTML = '<i class="fas fa-check-circle"></i> Thành công!';
            otpInputs.forEach(input => input.classList.add('success'));
            
            // Xóa dữ liệu tạm
            sessionStorage.removeItem('registerEmail');
            sessionStorage.removeItem('registrationData');
            
            setTimeout(() => {
                window.location.href = '/login';
            }, 2000);
        } else {
            showError(data.message);
            submitBtn.innerHTML = originalText;
            submitBtn.disabled = false;
            otpInputs.forEach(input => {
                input.classList.add('error');
                input.value = '';
            });
            otpInputs[0].focus();
        }
    })
    .catch(error => {
        showError('Có lỗi xảy ra trong quá trình xác thực. Vui lòng thử lại.');
        submitBtn.innerHTML = originalText;
        submitBtn.disabled = false;
        console.error('Error:', error);
    });
});

function showSuccess(message) {
    const successDiv = document.getElementById('successMessage');
    const errorDiv = document.getElementById('errorMessage');
    errorDiv.style.display = 'none';
    successDiv.textContent = message;
    successDiv.style.display = 'block';
}

function showError(message) {
    const errorDiv = document.getElementById('errorMessage');
    const successDiv = document.getElementById('successMessage');
    successDiv.style.display = 'none';
    errorDiv.textContent = message;
    errorDiv.style.display = 'block';
}

function clearMessages() {
    document.getElementById('successMessage').style.display = 'none';
    document.getElementById('errorMessage').style.display = 'none';
}

// Kiểm tra nếu đã đăng nhập thì chuyển hướng
if (localStorage.getItem('jwtToken')) {
    window.location.href = '/';
}