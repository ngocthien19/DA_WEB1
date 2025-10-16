// ===== PROMOTION FORM JAVASCRIPT =====

class PromotionFormManager {
    constructor() {
        this.isEdit = document.getElementById('maKhuyenMai') !== null;
        this.init();
    }

    init() {
        this.bindEvents();
        this.initValidation();
        this.setupDateValidation();
    }

    bindEvents() {
        const submitBtn = document.getElementById('submitBtn');
        if (submitBtn) {
            submitBtn.addEventListener('click', (e) => this.handleSubmit(e));
        }

        // Real-time validation
        document.querySelectorAll('#promotionForm input, #promotionForm select').forEach(element => {
            element.addEventListener('blur', (e) => this.validateField(e.target));
            element.addEventListener('input', (e) => this.clearFieldError(e.target));
        });
    }

    initValidation() {
        // Custom validation rules
        this.validationRules = {
            maGiamGia: {
                required: true,
                minLength: 3,
                maxLength: 50,
                pattern: /^[A-Z0-9_-]+$/,
                message: 'Mã giảm giá chỉ được chứa chữ hoa, số, gạch dưới và gạch ngang'
            },
            discount: {
                required: true,
                min: 1,
                max: 100,
                message: 'Giảm giá phải từ 1% đến 100%'
            },
            ngayBatDau: {
                required: true,
                pattern: /^\d{4}-\d{2}-\d{2}$/,
                message: 'Ngày bắt đầu phải có định dạng YYYY-MM-DD'
            },
            ngayKetThuc: {
                required: true,
                pattern: /^\d{4}-\d{2}-\d{2}$/,
                message: 'Ngày kết thúc phải có định dạng YYYY-MM-DD'
            },
            soLuongMaGiamGia: {
                required: true,
                min: 1,
                message: 'Số lượng phải lớn hơn 0'
            }
        };
    }

    setupDateValidation() {
        const startDateInput = document.getElementById('ngayBatDau');
        const endDateInput = document.getElementById('ngayKetThuc');

        if (startDateInput && endDateInput) {
            // Set min date to today
            const today = new Date();
            const todayFormatted = today.toISOString().split('T')[0];
            startDateInput.min = todayFormatted;

            startDateInput.addEventListener('change', () => {
                if (startDateInput.value) {
                    // Validate date format
                    if (!this.isValidDate(startDateInput.value)) {
                        this.showFieldError('ngayBatDau', 'Ngày không hợp lệ');
                        return;
                    }
                    
                    endDateInput.min = startDateInput.value;
                    this.validateDateRange();
                }
            });

            endDateInput.addEventListener('change', () => {
                if (endDateInput.value && !this.isValidDate(endDateInput.value)) {
                    this.showFieldError('ngayKetThuc', 'Ngày không hợp lệ');
                    return;
                }
                this.validateDateRange();
            });

            // Add input event listeners to prevent invalid input
            startDateInput.addEventListener('input', (e) => {
                this.sanitizeDateInput(e.target);
            });

            endDateInput.addEventListener('input', (e) => {
                this.sanitizeDateInput(e.target);
            });
        }
    }

    isValidDate(dateString) {
        const regex = /^\d{4}-\d{2}-\d{2}$/;
        if (!regex.test(dateString)) return false;
        
        const date = new Date(dateString);
        return date instanceof Date && !isNaN(date);
    }

    sanitizeDateInput(input) {
        // Remove any non-digit characters except hyphens
        let value = input.value.replace(/[^\d-]/g, '');
        
        // Auto-format as yyyy-mm-dd
        if (value.length > 4 && value.charAt(4) !== '-') {
            value = value.substring(0, 4) + '-' + value.substring(4);
        }
        if (value.length > 7 && value.charAt(7) !== '-') {
            value = value.substring(0, 7) + '-' + value.substring(7);
        }
        
        // Limit year to 4 digits
        if (value.length > 4 && value.indexOf('-') === 4) {
            const year = value.substring(0, 4);
            if (year.length > 4) {
                value = year.substring(0, 4) + value.substring(4);
            }
        }
        
        input.value = value;
    }

    validateDateRange() {
        const startDate = document.getElementById('ngayBatDau').value;
        const endDate = document.getElementById('ngayKetThuc').value;
        const errorElement = document.getElementById('ngayKetThucError');

        if (startDate && endDate) {
            const start = new Date(startDate);
            const end = new Date(endDate);

            if (end <= start) {
                this.showFieldError('ngayKetThuc', 'Ngày kết thúc phải sau ngày bắt đầu');
                return false;
            }
        }

        this.clearFieldError('ngayKetThuc');
        return true;
    }

    validateField(field) {
        const fieldName = field.id;
        const value = field.value.trim();
        const rules = this.validationRules[fieldName];

        if (!rules) return true;

        // Required validation
        if (rules.required && !value) {
            this.showFieldError(fieldName, 'Trường này là bắt buộc');
            return false;
        }

        // Pattern validation (thêm trước các validation khác)
        if (rules.pattern && !rules.pattern.test(value)) {
            this.showFieldError(fieldName, rules.message);
            return false;
        }

        // Min length validation
        if (rules.minLength && value.length < rules.minLength) {
            this.showFieldError(fieldName, `Tối thiểu ${rules.minLength} ký tự`);
            return false;
        }

        // Max length validation
        if (rules.maxLength && value.length > rules.maxLength) {
            this.showFieldError(fieldName, `Tối đa ${rules.maxLength} ký tự`);
            return false;
        }

        // Min value validation
        if (rules.min !== undefined && Number(value) < rules.min) {
            this.showFieldError(fieldName, `Giá trị tối thiểu là ${rules.min}`);
            return false;
        }

        // Max value validation
        if (rules.max !== undefined && Number(value) > rules.max) {
            this.showFieldError(fieldName, `Giá trị tối đa là ${rules.max}`);
            return false;
        }

        this.clearFieldError(fieldName);
        return true;
    }

    showFieldError(fieldName, message) {
        const field = document.getElementById(fieldName);
        const errorElement = document.getElementById(fieldName + 'Error');

        field.classList.add('is-invalid');
        field.classList.remove('is-valid');
        
        if (errorElement) {
            errorElement.textContent = message;
            errorElement.style.display = 'block';
        }
    }

    clearFieldError(fieldName) {
        const field = document.getElementById(fieldName);
        const errorElement = document.getElementById(fieldName + 'Error');

        field.classList.remove('is-invalid');
        
        if (errorElement) {
            errorElement.textContent = '';
            errorElement.style.display = 'none';
        }
    }

    validateForm() {
        let isValid = true;
        const fields = ['maGiamGia', 'discount', 'ngayBatDau', 'ngayKetThuc', 'soLuongMaGiamGia'];

        fields.forEach(fieldName => {
            const field = document.getElementById(fieldName);
            if (field) {
                if (!this.validateField(field)) {
                    isValid = false;
                }
            }
        });

        // Additional date range validation
        if (!this.validateDateRange()) {
            isValid = false;
        }

        return isValid;
    }

    getFormData() {
        const formData = {
            maGiamGia: document.getElementById('maGiamGia').value.trim(),
            discount: parseFloat(document.getElementById('discount').value),
            ngayBatDau: document.getElementById('ngayBatDau').value,
            ngayKetThuc: document.getElementById('ngayKetThuc').value,
            soLuongMaGiamGia: parseInt(document.getElementById('soLuongMaGiamGia').value),
            trangThai: document.getElementById('trangThai').value === 'true'
        };

        // Debug dates to console
        console.log('Form Data:', formData);
        console.log('Start Date:', formData.ngayBatDau);
        console.log('End Date:', formData.ngayKetThuc);

        if (this.isEdit) {
            formData.maKhuyenMai = parseInt(document.getElementById('maKhuyenMai').value);
        }

        return formData;
    }

    async handleSubmit(e) {
        e.preventDefault();

        if (!this.validateForm()) {
            this.showToast('error', 'Lỗi', 'Vui lòng kiểm tra lại thông tin trong form');
            return;
        }

        const submitBtn = document.getElementById('submitBtn');
        const submitText = document.getElementById('submitText');
        const loadingSpinner = document.getElementById('loadingSpinner');

        // Show loading state
        submitBtn.disabled = true;
        submitText.style.display = 'none';
        loadingSpinner.style.display = 'inline-block';

        const formData = this.getFormData();

        try {
            const url = this.isEdit 
                ? `/vendor/promotions/edit/${formData.maKhuyenMai}`
                : '/vendor/promotions/add';

            console.log('Sending data to:', url);
            console.log('Data:', formData);

            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: JSON.stringify(formData)
            });

            const result = await response.json();
            console.log('Server response:', result);

            if (result.success) {
                this.showToast('success', 'Thành công', result.message);
                
                // Redirect after success
                setTimeout(() => {
                    window.location.href = '/vendor/promotions';
                }, 1000);
            } else {
                this.showToast('error', 'Lỗi', result.message);
                
                // Highlight specific field errors if provided
                if (result.fieldErrors) {
                    Object.keys(result.fieldErrors).forEach(fieldName => {
                        this.showFieldError(fieldName, result.fieldErrors[fieldName]);
                    });
                }
            }
        } catch (error) {
            console.error('Form submission error:', error);
            this.showToast('error', 'Lỗi', 'Lỗi kết nối. Vui lòng thử lại.');
        } finally {
            // Restore button state
            submitBtn.disabled = false;
            submitText.style.display = 'inline-block';
            loadingSpinner.style.display = 'none';
        }
    }

    showToast(type, title, message) {
        const toastContainer = document.getElementById('toastContainer');
        if (!toastContainer) {
            console.error('Toast container not found');
            return;
        }

        const toastId = 'toast-' + Date.now();
        
        const iconClass = type === 'success' ? 'fa-check-circle' : 
                         type === 'error' ? 'fa-exclamation-circle' : 
                         'fa-info-circle';
        
        const toastHTML = `
            <div id="${toastId}" class="toast toast-${type}">
                <div class="toast-icon">
                    <i class="fas ${iconClass}"></i>
                </div>
                <div class="toast-content">
                    <div class="toast-title">${title}</div>
                    <div class="toast-message">${message}</div>
                </div>
                <button class="toast-close" onclick="this.closest('.toast').remove()">
                    <i class="fas fa-times"></i>
                </button>
            </div>
        `;
        
        toastContainer.insertAdjacentHTML('beforeend', toastHTML);
        
        const toastElement = document.getElementById(toastId);
        
        // Show toast with animation
        setTimeout(() => {
            toastElement.classList.add('show');
        }, 100);
        
        // Auto remove after 5 seconds
        setTimeout(() => {
            this.closeToast(toastId);
        }, 5000);
    }

    closeToast(toastId) {
        const toast = document.getElementById(toastId);
        if (toast) {
            toast.classList.remove('show');
            toast.classList.add('hide');
            setTimeout(() => {
                if (toast.parentNode) {
                    toast.remove();
                }
            }, 300);
        }
    }
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    window.promotionFormManager = new PromotionFormManager();
});

// Global function to close toast (for onclick events)
window.closeToast = function(toastId) {
    if (window.promotionFormManager) {
        window.promotionFormManager.closeToast(toastId);
    } else {
        const toast = document.getElementById(toastId);
        if (toast) {
            toast.classList.remove('show');
            toast.classList.add('hide');
            setTimeout(() => {
                if (toast.parentNode) {
                    toast.remove();
                }
            }, 300);
        }
    }
};