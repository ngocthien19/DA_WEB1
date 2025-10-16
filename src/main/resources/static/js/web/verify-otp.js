// Biến toàn cục
let wrongAttempts = 0;
const MAX_WRONG_ATTEMPTS = 5;
let countdownTimer = null; // Khai báo và khởi tạo là null
let countdownTime = 60; // Đổi tên để tránh trùng

// Lấy email từ sessionStorage
const userEmail = sessionStorage.getItem('registerEmail');
if (!userEmail) {
    window.location.href = '/register';
}

document.getElementById('emailDisplay').textContent = userEmail;

// Khôi phục số lần nhập sai từ sessionStorage
const savedAttempts = sessionStorage.getItem(`wrongAttempts_${userEmail}`);
if (savedAttempts) {
    wrongAttempts = parseInt(savedAttempts);
    updateAttemptsWarning();
}

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

// Countdown timer elements
const countdownElement = document.getElementById('countdown');
const resendBtn = document.getElementById('resendBtn');
const resendText = document.getElementById('resendText');

// Hàm dừng countdown
function stopCountdown() {
    if (countdownTimer) {
        clearInterval(countdownTimer);
        countdownTimer = null;
    }
}

// Hàm bắt đầu countdown
function startCountdown() {
    console.log('Starting countdown...'); // Debug log
    
    // Dừng countdown cũ nếu có
    stopCountdown();
    
    // Reset thời gian
    countdownTime = 60;
    
    // Cập nhật UI ngay lập tức
    countdownElement.textContent = countdownTime;
    resendBtn.disabled = true;
    resendText.textContent = 'Gửi lại sau ';
    
    // Bắt đầu countdown mới
    countdownTimer = setInterval(() => {
        countdownTime--;
        console.log('Countdown:', countdownTime); // Debug log
        countdownElement.textContent = countdownTime;
        
        if (countdownTime <= 0) {
            stopCountdown();
            resendBtn.disabled = false;
            resendText.textContent = 'Gửi lại mã';
            countdownElement.textContent = '';
            console.log('Countdown finished'); // Debug log
        }
    }, 1000);
}

// Bắt đầu countdown khi load trang
startCountdown();

// Cập nhật cảnh báo số lần nhập sai
function updateAttemptsWarning() {
    const attemptsWarning = document.getElementById('attemptsWarning');
    const attemptsText = document.getElementById('attemptsText');
    const verifyBtn = document.getElementById('verifyBtn');
    
    if (wrongAttempts > 0) {
        const remainingAttempts = MAX_WRONG_ATTEMPTS - wrongAttempts;
        attemptsText.textContent = `Bạn đã nhập sai ${wrongAttempts} lần. Còn ${remainingAttempts} lần thử.`;
        attemptsWarning.style.display = 'flex';
        
        if (wrongAttempts >= MAX_WRONG_ATTEMPTS) {
            attemptsText.textContent = 'Bạn đã nhập sai quá 5 lần. Vui lòng yêu cầu mã OTP mới.';
            verifyBtn.disabled = true;
            // Hiển thị nút gửi lại mã ngay lập tức
            resendBtn.disabled = false;
            resendText.textContent = 'Gửi lại mã';
            countdownElement.textContent = '';
            
            // Dừng countdown
            stopCountdown();
        }
    } else {
        attemptsWarning.style.display = 'none';
    }
}

// Xóa tất cả OTP input
function clearOtpInputs() {
    otpInputs.forEach(input => {
        input.value = '';
        input.classList.remove('error');
    });
    otpInputs[0].focus();
}

// Resend OTP
resendBtn.addEventListener('click', function() {
    console.log('Resend button clicked'); // Debug log
    
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
        console.log('Resend response:', data); // Debug log
        
        if (data.success) {
            showSuccess('Mã OTP mới đã được gửi đến email của bạn!');
            
            // Reset số lần nhập sai
            wrongAttempts = 0;
            sessionStorage.removeItem(`wrongAttempts_${userEmail}`);
            updateAttemptsWarning();
            
            // Clear OTP inputs
            clearOtpInputs();
            
            // Kích hoạt lại nút xác minh
            document.getElementById('verifyBtn').disabled = false;
            
            // QUAN TRỌNG: Bắt đầu lại countdown
            console.log('Calling startCountdown from resend...'); // Debug log
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
    
    // Kiểm tra nếu đã vượt quá số lần nhập sai
    if (wrongAttempts >= MAX_WRONG_ATTEMPTS) {
        showError('Bạn đã nhập sai quá 5 lần. Vui lòng yêu cầu mã OTP mới.');
        return;
    }
    
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
    
    const submitBtn = document.getElementById('verifyBtn');
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
            sessionStorage.removeItem(`wrongAttempts_${userEmail}`);
            
            // Dừng countdown
            stopCountdown();
            
            setTimeout(() => {
                window.location.href = '/login';
            }, 2000);
        } else {
            // Tăng số lần nhập sai
            wrongAttempts++;
            sessionStorage.setItem(`wrongAttempts_${userEmail}`, wrongAttempts.toString());
            
            // Xóa tất cả OTP input
            clearOtpInputs();
            
            // Cập nhật cảnh báo
            updateAttemptsWarning();
            
            showError(data.message);
            submitBtn.innerHTML = originalText;
            submitBtn.disabled = false;
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

// Debug: Kiểm tra xem hàm có được gọi không
console.log('Script loaded successfully');