document.addEventListener('DOMContentLoaded', function() {
    const selectAllCheckbox = document.getElementById('selectAll');
    const customerCheckboxes = document.querySelectorAll('.customer-checkbox');
    const selectedCountElement = document.getElementById('selectedCount');
    const deleteSelectedBtn = document.getElementById('deleteSelectedBtn');
    const exportExcelBtn = document.getElementById('exportExcelBtn');
    const confirmDeleteSelectedBtn = document.getElementById('confirmDeleteSelectedBtn');
    const deselectAllBtn = document.getElementById('deselectAllBtn');
    const deleteSelectedModal = new bootstrap.Modal(document.getElementById('deleteSelectedModal'));

    // Select All functionality
    selectAllCheckbox.addEventListener('change', function() {
        const isChecked = this.checked;
        customerCheckboxes.forEach(checkbox => {
            checkbox.checked = isChecked;
        });
        updateSelectedCount();
        updateDeselectAllButton();
    });

    // Individual checkbox change
    customerCheckboxes.forEach(checkbox => {
        checkbox.addEventListener('change', function() {
            updateSelectedCount();
            updateSelectAllCheckbox();
            updateDeselectAllButton();
        });
    });

    // Bỏ chọn tất cả
    deselectAllBtn.addEventListener('click', function() {
        selectAllCheckbox.checked = false;
        selectAllCheckbox.indeterminate = false;
        customerCheckboxes.forEach(checkbox => {
            checkbox.checked = false;
        });
        updateSelectedCount();
        updateDeselectAllButton();
    });

	// Hàm cập nhật số lượng khách hàng đã chọn và nút "Xóa đã chọn"
	function updateSelectedCount() {
	    const selectedCount = document.querySelectorAll('.customer-checkbox:checked').length;
	    selectedCountElement.textContent = `Đã chọn: ${selectedCount} khách hàng`;

	    // Enable/disable buttons based on selection
	    const hasSelection = selectedCount > 0;
	    deleteSelectedBtn.disabled = !hasSelection;
	    exportExcelBtn.disabled = !hasSelection;

	    // Hiển thị nút "Xóa đã chọn" khi có khách hàng được chọn
	    if (hasSelection) {
	        deleteSelectedBtn.style.display = 'inline-block';  // Hiển thị nút
	    } else {
	        deleteSelectedBtn.style.display = 'none';  // Ẩn nút
	    }
	}

    // Update select all checkbox state
    function updateSelectAllCheckbox() {
        const totalCheckboxes = customerCheckboxes.length;
        const checkedCheckboxes = document.querySelectorAll('.customer-checkbox:checked').length;
        selectAllCheckbox.checked = totalCheckboxes > 0 && checkedCheckboxes === totalCheckboxes;
        selectAllCheckbox.indeterminate = checkedCheckboxes > 0 && checkedCheckboxes < totalCheckboxes;
    }

    // Update deselect all button visibility
    function updateDeselectAllButton() {
        const selectedCount = document.querySelectorAll('.customer-checkbox:checked').length;
        deselectAllBtn.style.display = selectedCount > 0 ? 'inline-block' : 'none';
    }

    // Delete selected customers
    deleteSelectedBtn.addEventListener('click', function() {
        const selectedCustomers = getSelectedCustomers();
        if (selectedCustomers.length === 0) return;

        document.getElementById('selectedCustomersCount').textContent = selectedCustomers.length + ' khách hàng';
        deleteSelectedModal.show();
    });

    confirmDeleteSelectedBtn.addEventListener('click', function() {
        const selectedCustomers = getSelectedCustomers();
        deleteSelectedCustomers(selectedCustomers);
        deleteSelectedModal.hide();
    });

    // Export to Excel
    exportExcelBtn.addEventListener('click', function() {
        const selectedCustomers = getSelectedCustomers();
        exportToExcel(selectedCustomers);
    });

    // Get selected customers data
    function getSelectedCustomers() {
        const selected = [];
        document.querySelectorAll('.customer-checkbox:checked').forEach(checkbox => {
            selected.push({
                id: checkbox.getAttribute('data-customer-id'),
                name: checkbox.getAttribute('data-customer-name')
            });
        });
        return selected;
    }

    // Delete selected customers function
    function deleteSelectedCustomers(customers) {
        const customerIds = customers.map(customer => customer.id);
        
        deleteSelectedBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i>Đang xóa...';
        deleteSelectedBtn.disabled = true;

        fetch('/admin/customers/delete-selected', {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ customerIds: customerIds })
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showToast('success', 'Thành công', data.message);
                setTimeout(() => {
                    window.location.reload();
                }, 1000);
            } else {
                showToast('error', 'Lỗi', data.message);
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showToast('error', 'Lỗi', 'Có lỗi xảy ra khi xóa khách hàng');
        })
        .finally(() => {
            deleteSelectedBtn.innerHTML = '<i class="fas fa-trash me-1"></i>Xóa đã chọn';
            updateSelectedCount();
        });
    }

    // Export to Excel function
    function exportToExcel(customers) {
        exportExcelBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Đang xuất...';
        exportExcelBtn.disabled = true;

        // Chuyển đổi ID sang số nguyên
        const customerIds = customers.map(c => {
            const id = c.id;
            return typeof id === 'string' ? parseInt(id) : id;
        });

        fetch('/admin/customers/export-excel', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ 
                customerIds: customerIds,
                keyword: document.querySelector('input[name="keyword"]').value
            })
        })
        .then(response => {
            if (response.ok) {
                return response.blob();
            } else {
                return response.json().then(errorData => {
                    throw new Error(errorData.message || 'Export failed');
                });
            }
        })
        .then(blob => {
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.style.display = 'none';
            a.href = url;
            a.download = `customers_${new Date().toISOString().split('T')[0]}.xlsx`;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            
            showToast('success', 'Thành công', 'Xuất Excel thành công');
        })
        .catch(error => {
            console.error('Error:', error);
            showToast('error', 'Lỗi', error.message || 'Có lỗi xảy ra khi xuất Excel');
        })
        .finally(() => {
            exportExcelBtn.innerHTML = '<i class="fas fa-file-excel me-2"></i>Xuất Excel';
            updateSelectedCount();
        });
    }

    // Toast notification function với style mới
    function showToast(type, title, message) {
        // Tạo toast container nếu chưa có
        if (!document.getElementById('toastContainer')) {
            const toastContainer = document.createElement('div');
            toastContainer.id = 'toastContainer';
            document.body.appendChild(toastContainer);
        }

        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        
        // Icon tương ứng với loại toast
        let icon = '';
        if (type === 'success') {
            icon = '<i class="fas fa-check-circle toast-icon"></i>';
        } else if (type === 'error') {
            icon = '<i class="fas fa-exclamation-circle toast-icon"></i>';
        } else if (type === 'warning') {
            icon = '<i class="fas fa-exclamation-triangle toast-icon"></i>';
        } else {
            icon = '<i class="fas fa-info-circle toast-icon"></i>';
        }

        toast.innerHTML = `
            ${icon}
            <div class="toast-content">
                <div class="toast-title">${title}</div>
                <div class="toast-message">${message}</div>
            </div>
            <button class="toast-close">
                <i class="fas fa-times"></i>
            </button>
        `;

        document.getElementById('toastContainer').appendChild(toast);

        // Hiệu ứng hiển thị
        setTimeout(() => {
            toast.classList.add('show');
        }, 10);

        // Tự động ẩn sau 5 giây
        const autoHide = setTimeout(() => {
            hideToast(toast);
        }, 5000);

        // Sự kiện click nút đóng
        const closeBtn = toast.querySelector('.toast-close');
        closeBtn.addEventListener('click', () => {
            clearTimeout(autoHide);
            hideToast(toast);
        });

        // Tự động xóa sau khi ẩn
        toast.addEventListener('animationend', function(e) {
            if (e.animationName === 'slideOutRight' || e.animationName === 'slideOutRight') {
                toast.remove();
            }
        });
    }

    // Hàm ẩn toast
    function hideToast(toast) {
        toast.classList.remove('show');
        toast.classList.add('hide');
    }

    // Initialize
    updateSelectedCount();
    updateDeselectAllButton();
});