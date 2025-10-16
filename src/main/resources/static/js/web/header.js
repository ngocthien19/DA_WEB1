// header.js - Complete functionality for header

document.addEventListener('DOMContentLoaded', function() {
    
    // Variables
    let mobileMenu = false;
    let showCategory = false;
    let selectedIndex = -1;
    let searchTimeout;

    // DOM Elements
    const searchBox = document.querySelector('.search-box');
    const modalSearchElement = document.querySelector('.modal-search');
    const toggleButton = document.querySelector('.toggle');
    const navMenu = document.querySelector('.nav_link ul');
    const categoriesButton = document.querySelector('.categories.d_flex');
    const modalCategory = document.querySelector('.modal-category');
    const searchSection = document.querySelector('.search');
    const searchInput = document.getElementById('search-input');
    const searchForm = searchInput?.closest('form');
    const headSection = document.querySelector('.head');
    const headerSection = document.querySelector('.header');
    const registerStoreLink = document.getElementById('registerStoreLink');

    // Initialize
    checkAuthStatus();
    initSearch();
    initCategories();
    initMobileMenu();
    initActiveMenu();
    initFixedHeader();
    initCart();
    checkForToastMessage();

    // ========== FIXED HEADER FUNCTIONALITY ==========
    function initFixedHeader() {
        if (!searchSection || !headSection) return;

        const headHeight = headSection.offsetHeight;
        const headerHeight = headerSection ? headerSection.offsetHeight : 0;
        const totalHeight = headHeight + headerHeight;
        
        let lastScrollTop = 0;
        
        window.addEventListener('scroll', function() {
            const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
            
            if (scrollTop > headHeight) {
                searchSection.classList.add('fixed');
                document.body.classList.add('has-fixed-header');
            } else {
                searchSection.classList.remove('fixed');
                document.body.classList.remove('has-fixed-header');
            }
            
            lastScrollTop = scrollTop;
        });
    }

	// ========== TOAST STYLES & FUNCTIONS ==========
	if (!document.getElementById('toast-style')) {
	    const style = document.createElement('style');
	    style.id = 'toast-style';
	    style.textContent = `
	        #toastContainer {
	            position: fixed;
	            top: 80px;
	            right: 20px;
	            z-index: 9999;
	        }

	        .toast {
	            min-width: 300px;
	            background: white;
	            padding: 20px;
	            border-radius: 10px;
	            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
	            display: flex;
	            align-items: center;
	            gap: 15px;
	            margin-bottom: 15px;
	            animation: slideInRight 0.3s ease-out;
	            border-left: 4px solid;
	            opacity: 0;
	            transform: translateX(400px);
	        }

	        .toast.show {
	            opacity: 1;
	            transform: translateX(0);
	        }

	        .toast.hide {
	            animation: slideOutRight 0.3s ease-out forwards;
	        }

	        .toast.toast-success {
	            border-left-color: #28a745;
	        }

	        .toast.toast-error {
	            border-left-color: #e94560;
	        }

	        .toast-icon {
	            font-size: 24px;
	        }

	        .toast-success .toast-icon {
	            color: #28a745;
	        }

	        .toast-error .toast-icon {
	            color: #e94560;
	        }

	        .toast-content {
	            flex: 1;
	        }

	        .toast-title {
	            font-weight: 600;
	            margin-bottom: 5px;
	            color: #0f3460;
	        }

	        .toast-message {
	            color: #666;
	            font-size: 14px;
	        }

	        .toast-close {
	            background: none;
	            border: none;
	            font-size: 16px;
	            color: #999;
	            cursor: pointer;
	            padding: 5px;
	            transition: color 0.3s ease;
	        }

	        .toast-close:hover {
	            color: #666;
	        }

	        @keyframes slideInRight {
	            from {
	                opacity: 0;
	                transform: translateX(400px);
	            }
	            to {
	                opacity: 1;
	                transform: translateX(0);
	            }
	        }

	        @keyframes slideOutRight {
	            from {
	                opacity: 1;
	                transform: translateX(0);
	            }
	            to {
	                opacity: 0;
	                transform: translateX(400px);
	            }
	        }

	        /* Cart Notification Styles - Giữ lại cho tương thích */
	        .cart-notification {
	            position: fixed;
	            top: 100px;
	            right: 20px;
	            background: #4CAF50;
	            color: white;
	            padding: 15px 20px;
	            border-radius: 5px;
	            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
	            z-index: 10000;
	            animation: slideInRight 0.3s ease;
	            font-family: Arial, sans-serif;
	        }

	        .cart-notification .notification-content {
	            display: flex;
	            align-items: center;
	            gap: 10px;
	        }

	        .cart-notification i {
	            font-size: 18px;
	        }
	    `;
	    document.head.appendChild(style);
	}

	function showToast(type, title, message, duration = 1000) {
	    let toastContainer = document.getElementById('toastContainer');
	    if (!toastContainer) {
	        toastContainer = document.createElement('div');
	        toastContainer.id = 'toastContainer';
	        document.body.appendChild(toastContainer);
	    }
	    
	    const toast = document.createElement('div');
	    toast.className = `toast toast-${type}`;
	    
	    const icon = type === 'success' ? 
	        '<i class="fas fa-check-circle toast-icon"></i>' : 
	        '<i class="fas fa-exclamation-circle toast-icon"></i>';
	    
	    toast.innerHTML = `
	        ${icon}
	        <div class="toast-content">
	            <div class="toast-title">${title}</div>
	            <div class="toast-message">${message}</div>
	        </div>
	        <button class="toast-close" onclick="this.parentElement.remove()">
	            <i class="fas fa-times"></i>
	        </button>
	    `;
	    
	    toastContainer.appendChild(toast);
	    
	    setTimeout(() => {
	        toast.classList.add('show');
	    }, 10);
	    
	    if (duration > 0) {
	        setTimeout(() => {
	            hideToast(toast);
	        }, duration);
	    }
	    
	    return toast;
	}

	function hideToast(toast) {
	    toast.classList.remove('show');
	    toast.classList.add('hide');
	    
	    setTimeout(() => {
	        if (toast.parentElement) {
	            toast.parentElement.removeChild(toast);
	        }
	    }, 300);
	}

    function checkForToastMessage() {
        const urlParams = new URLSearchParams(window.location.search);
        const message = urlParams.get('toastMessage');
        
        if (message) {
            const decodedMessage = decodeURIComponent(message);
            showToast('success', 'Thành công!', decodedMessage, 1000);
            
            urlParams.delete('toastMessage');
            const newUrl = window.location.pathname + (urlParams.toString() ? '?' + urlParams.toString() : '');
            window.history.replaceState({}, document.title, newUrl);
        }
    }

    // ========== AUTHENTICATION FUNCTIONS ==========
    function checkAuthStatus() {
        const token = localStorage.getItem('jwtToken');
        const userData = localStorage.getItem('user');
        const authButtons = document.getElementById('authButtons');
        const userMenu = document.getElementById('userMenu');
        const userAccountLink = document.getElementById('userAccountLink');
        
        console.log('Auth check - Token:', !!token, 'UserData:', !!userData);
        
        if (token && userData) {
            // Đã đăng nhập
            if (authButtons) authButtons.style.display = 'none';
            if (userMenu) userMenu.style.display = 'flex';
            if (userAccountLink) userAccountLink.style.display = 'block';
            
            try {
                const user = JSON.parse(userData);
                const userNameElement = document.getElementById('userName');
                if (userNameElement) {
                    userNameElement.textContent = user.username || 'User';
                }
                
                // Kiểm tra và hiển thị menu ADMIN nếu có quyền
                if (user.role === 'ADMIN' || (user.vaiTro && user.vaiTro.maVaiTro === 'ADMIN')) {
                    showAdminMenu();
                }

                // Ẩn link Đăng ký cửa hàng nếu là VENDOR
                if (user.role === 'VENDOR' || (user.vaiTro && user.vaiTro.maVaiTro === 'VENDOR')) {
                    if (registerStoreLink) {
                        registerStoreLink.style.display = 'none';
                    }
                } else {
                    if (registerStoreLink) {
                        registerStoreLink.style.display = 'block';
                    }
                }
            } catch (e) {
                console.error('Error parsing user data:', e);
                localStorage.removeItem('user');
                localStorage.removeItem('jwtToken');
                location.reload();
            }
            
            // Load cart quantity
            loadCartQuantity();
        } else {
            // Chưa đăng nhập
            if (authButtons) authButtons.style.display = 'flex';
            if (userMenu) userMenu.style.display = 'none';
            if (userAccountLink) userAccountLink.style.display = 'none';
            hideAdminMenu();
            
            if (registerStoreLink) {
                registerStoreLink.style.display = 'block';
            }
            
            // Fallback: Nếu chưa đăng nhập, dùng localStorage
            updateCartQuantityFromLocalStorage();
        }
    }

    function showAdminMenu() {
        let adminMenu = document.querySelector('.admin-menu-item');
        
        if (!adminMenu) {
            adminMenu = document.createElement('li');
            adminMenu.className = 'admin-menu-item';
            adminMenu.innerHTML = `
                <a href="#" class="nav-link" data-menu="admin" onclick="goToAdminDashboard()">
                    <span class="nav_link_item">Quản trị</span>
                    <div class="nav-indicator"></div>
                </a>
            `;
            
            const navMenuUl = document.querySelector('.nav_link ul');
            const userMenuItem = document.querySelector('.nav_link ul li:nth-child(2)'); // Target "Tài khoản" link
            if (navMenuUl && userMenuItem) {
                navMenuUl.insertBefore(adminMenu, userMenuItem);
            } else if (navMenuUl) {
                navMenuUl.appendChild(adminMenu);
            }
            
            if (!document.getElementById('admin-menu-style')) {
                const style = document.createElement('style');
                style.id = 'admin-menu-style';
                style.textContent = `
                    .admin-menu-item .nav_link_item {
                        color: #ff6b6b !important;
                        font-weight: bold;
                    }
                    .admin-menu-item .nav_link_item:hover {
                        color: #ff5252 !important;
                    }
                    .admin-menu-item .nav_link_item.active {
                        color: #ff5252 !important;
                    }
                    .admin-menu-item .nav-indicator {
                        background-color: #ff6b6b !important;
                    }
                `;
                document.head.appendChild(style);
            }
        }
        
        adminMenu.style.display = 'block';
    }

    function hideAdminMenu() {
        const adminMenu = document.querySelector('.admin-menu-item');
        if (adminMenu) {
            adminMenu.style.display = 'none';
        }
    }

    // Cập nhật để lấy TỔNG SỐ LƯỢNG
    function loadCartQuantity() {
        const token = localStorage.getItem('jwtToken');
        if (!token) return;

        fetch('/api/cart/quantity', {
            headers: {
                'Authorization': 'Bearer ' + token
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
                const cartQtyElement = document.getElementById('cartQty');
                if (cartQtyElement) {
                    // data.data là TỔNG SỐ LƯỢNG ĐẶT
                    cartQtyElement.textContent = data.data || 0; 
                }
            }
        })
        .catch(error => {
            console.error('Error loading cart quantity:', error);
            // Fallback to localStorage cart if API fails
            updateCartQuantityFromLocalStorage();
        });
    }

	function initCart() {
	    // Remove all existing event listeners first
	    const cartButtons = document.querySelectorAll('.add-to-cart-btn, [onclick*="addToCart"], .cart-btn, .btn_action');
	    
	    cartButtons.forEach(button => {
	        const newButton = button.cloneNode(true);
	        button.parentNode.replaceChild(newButton, button);
	    });
	    
	    // Add new event listeners with prevention for duplicates
	    document.querySelectorAll('.add-to-cart-btn, [onclick*="addToCart"], .cart-btn, .btn_action').forEach(button => {
	        if (!button.hasAttribute('data-cart-listener')) {
	            button.setAttribute('data-cart-listener', 'true');
	            
	            button.addEventListener('click', function(e) {
	                e.preventDefault();
	                e.stopPropagation();
	                
	                // Xác định productId dựa trên loại nút
	                let productId;
	                
	                if (this.classList.contains('btn_action')) {
	                    // Nút trong trang chi tiết sản phẩm
	                    const productIdMeta = document.querySelector('meta[name="productId"]');
	                    if (productIdMeta) {
	                        productId = productIdMeta.content;
	                    } else {
	                        // Fallback: lấy từ URL hoặc các thuộc tính khác
	                        const currentPath = window.location.pathname;
	                        const pathParts = currentPath.split('/');
	                        productId = pathParts[pathParts.length - 1];
	                    }
	                } else {
	                    // Nút trong các product card thông thường
	                    const productCard = this.closest('.product-card');
	                    const productLink = productCard?.querySelector('a[href*="/view/"]');
	                    
	                    if (productLink) {
	                        const href = productLink.getAttribute('href');
	                        productId = href.split('/').pop();
	                    }
	                }
	                
	                if (productId) {
	                    // Disable button temporarily
	                    this.disabled = true;
	                    const originalHTML = this.innerHTML;
	                    this.innerHTML = '<i class="fa fa-spinner fa-spin"></i> Đang thêm...';
	                    
	                    // Add to cart
	                    addToCart(productId, 1);
	                    
	                    // Re-enable button after 1 second
	                    setTimeout(() => {
	                        this.disabled = false;
	                        this.innerHTML = originalHTML;
	                    }, 1000);
	                } else {
	                    console.error('Không tìm thấy productId');
	                    showCartNotification('Lỗi: Không tìm thấy thông tin sản phẩm!', true);
	                }
	            });
	        }
	    });

	    // Update cart quantity on load
	    updateCartQuantityFromLocalStorage();
	}
    
    function addToCart(productId, quantity = 1) {
        console.log('Adding to cart - Product ID:', productId, 'Quantity:', quantity);
        
        // Check if user is logged in
        const token = localStorage.getItem('jwtToken');
        
        if (token) {
            // User is logged in - use API
            addToCartAPI(productId, quantity);
        } else {
            // User is not logged in - use localStorage
            addToCartLocalStorage(productId, quantity);
        }
    }
    
    function addToCartAPI(productId, quantity) {
        const token = localStorage.getItem('jwtToken');
        
        fetch('/api/cart/add', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + token
            },
            body: JSON.stringify({
                maSanPham: productId,
                soLuong: quantity
            })
        })
        .then(response => {
            if (!response.ok) {
                // Xử lý lỗi từ server (ví dụ: hết hàng)
                return response.json().then(errorData => { 
                    showCartNotification(errorData.message || 'Lỗi khi thêm vào giỏ hàng!', true);
                    throw new Error(errorData.message); 
                });
            }
            return response.json();
        })
        .then(data => {
            if (data.success) {
                showCartNotification('Đã thêm vào giỏ hàng!');
                // Cập nhật số lượng từ data.data.totalQuantity
                const cartQtyElement = document.getElementById('cartQty');
                if (cartQtyElement) {
                    cartQtyElement.textContent = data.data.totalQuantity || 0;
                }
            } else {
                showCartNotification('Lỗi khi thêm vào giỏ hàng!', true);
            }
        })
        .catch(error => {
            console.error('Error adding to cart:', error);
            // Nếu lỗi không phải từ server (ví dụ: network), hiển thị thông báo chung
            if (!error.message.includes('Sản phẩm không đủ')) {
                showCartNotification('Lỗi kết nối hoặc hệ thống!', true);
            }
        });
    }
    
    function addToCartLocalStorage(productId, quantity) {
        let cartItems = JSON.parse(localStorage.getItem('cartItems') || '[]');
        
        // Check if product already exists in cart
        const existingItemIndex = cartItems.findIndex(item => item.productId === productId);
        
        if (existingItemIndex > -1) {
            // Update quantity if item exists
            cartItems[existingItemIndex].quantity += quantity;
        } else {
            // Add new item
            cartItems.push({
                productId: productId,
                quantity: quantity,
                addedAt: new Date().toISOString()
            });
        }
        
        // Save to localStorage
        localStorage.setItem('cartItems', JSON.stringify(cartItems));
        
        // Update cart quantity display
        updateCartQuantityFromLocalStorage();
        
        showCartNotification('Đã thêm vào giỏ hàng!');
    }
    
    // Cập nhật để tính TỔNG SỐ LƯỢNG từ localStorage
    function updateCartQuantityFromLocalStorage() {
        const cartQtyElement = document.getElementById('cartQty');
        if (cartQtyElement) {
            const cartItems = JSON.parse(localStorage.getItem('cartItems') || '[]');
            // Tính tổng số lượng (sum of quantity)
            const totalQty = cartItems.reduce((sum, item) => sum + (item.quantity || 1), 0);
            cartQtyElement.textContent = totalQty;
        }
    }
    
	function showCartNotification(message, isError = false) {
	    const type = isError ? 'error' : 'success';
	    const title = isError ? 'Thất bại' : 'Thành công';
	    showToast(type, title, message, 1000);
	}

    // ========== ACTIVE MENU MANAGEMENT ==========
    function initActiveMenu() {
        const currentPath = window.location.pathname;
        let activeMenu = 'home';
        
        if (currentPath === '/' || currentPath === '/home') {
            activeMenu = 'home';
        } else if (currentPath.includes('/profile') || currentPath.includes('/user')) {
            activeMenu = 'user';
        } else if (currentPath.includes('/track-order')) {
            activeMenu = 'track-order';
        } else if (currentPath.includes('/admin')) {
            activeMenu = 'admin';
        }
        
        setActiveMenu(activeMenu);
        
        const navLinks = document.querySelectorAll('.nav-link');
        navLinks.forEach(link => {
            link.addEventListener('click', function(e) {
                const menuType = this.getAttribute('data-menu');
                
                if (menuType === 'user') {
                    e.preventDefault();
                    goToProfile();
                } else if (menuType === 'admin') {
                    e.preventDefault();
                    goToAdminDashboard();
                } else {
                    setActiveMenu(menuType);
                }
            });
        });
    }

    function setActiveMenu(menuType) {
        const navItems = document.querySelectorAll('.nav_link_item');
        const navIndicators = document.querySelectorAll('.nav-indicator');
        
        navItems.forEach(item => {
            item.classList.remove('active');
        });
        
        navIndicators.forEach(indicator => {
            indicator.style.transform = 'scaleX(0)';
        });
        
        const activeLink = document.querySelector(`.nav-link[data-menu="${menuType}"]`);
        if (activeLink) {
            const navItem = activeLink.querySelector('.nav_link_item');
            const indicator = activeLink.querySelector('.nav-indicator');
            
            if (navItem) {
                navItem.classList.add('active');
            }
            if (indicator) {
                indicator.style.transform = 'scaleX(1)';
            }
        }
    }

    // ========== GLOBAL FUNCTIONS ==========
    window.logout = function() {
        localStorage.removeItem('jwtToken');
        localStorage.removeItem('user');
        localStorage.removeItem('cartItems');
        document.cookie = 'jwtToken=; path=/; expires=Thu, 01 Jan 1970 00:00:01 GMT;';
        window.location.href = '/';
    }

    window.goToProfile = function() {
        const token = localStorage.getItem('jwtToken');
        if (!token) {
            window.location.href = '/login';
            return;
        }
        window.location.href = '/profile';
    }

    window.goToAdminDashboard = function() {
        const token = localStorage.getItem('jwtToken');
        if (!token) {
            window.location.href = '/login';
            return;
        }
        
        const userData = localStorage.getItem('user');
        if (userData) {
            try {
                const user = JSON.parse(userData);
                if (user.role === 'ADMIN' || (user.vaiTro && user.vaiTro.maVaiTro === 'ADMIN')) {
                    window.location.href = '/admin/dashboard';
                } else {
                    alert('Bạn không có quyền truy cập trang quản trị');
                }
            } catch (e) {
                console.error('Error parsing user data:', e);
                localStorage.removeItem('user');
                localStorage.removeItem('jwtToken');
                window.location.href = '/login';
            }
        } else {
            window.location.href = '/login';
        }
    }

    // ========== USER DROPDOWN MENU ==========
    document.addEventListener('click', function(e) {
        const userDropdown = document.querySelector('.user-dropdown');
        if (userDropdown && !userDropdown.contains(e.target)) {
            const dropdownContent = userDropdown.querySelector('.dropdown-content');
            if (dropdownContent) {
                dropdownContent.style.display = 'none';
            }
        }
    });

    const userInfoElement = document.querySelector('.user-info');
    if (userInfoElement) {
        userInfoElement.addEventListener('click', function(e) {
            e.preventDefault();
            const dropdownContent = this.parentElement.querySelector('.dropdown-content');
            if (dropdownContent) {
                dropdownContent.style.display = dropdownContent.style.display === 'block' ? 'none' : 'block';
            }
        });
    }

    // ========== SEARCH FUNCTIONALITY ==========
    function initSearch() {
        if (!searchInput || !modalSearchElement) return;

        if (searchForm) {
            searchForm.addEventListener('submit', function(e) {
                const searchValue = searchInput.value.trim();
                if (!searchValue) {
                    e.preventDefault();
                }
            });
        }

        searchInput.addEventListener('input', function(e) {
            clearTimeout(searchTimeout);
            const searchValue = e.target.value.trim();
            
            if (searchValue.length >= 1) {
                searchTimeout = setTimeout(() => {
                    fetchSearchResults(searchValue);
                }, 300);
            } else {
                hideSearchModal();
            }
        });

        searchInput.addEventListener('keydown', function(e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                const searchValue = searchInput.value.trim();
                if (searchValue) {
                    hideSearchModal();
                    if (searchForm) {
                        searchForm.submit();
                    }
                }
            }

            if (e.key === 'ArrowDown') {
                e.preventDefault();
                navigateSearchResults('down');
            } else if (e.key === 'ArrowUp') {
                e.preventDefault();
                navigateSearchResults('up');
            }
        });

        document.addEventListener('click', function(e) {
            if (!searchSection.contains(e.target)) {
                hideSearchModal();
            }
        });
    }

    function fetchSearchResults(query) {
        const encodedQuery = encodeURIComponent(query);
        
        fetch(`/api/search?q=${encodedQuery}&limit=5`)
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            if (data.success && data.data) {
                displaySearchResults(data.data, query);
            }
        })
        .catch(error => {
            console.error('Search error:', error);
        });
    }

    function displaySearchResults(results, query) {
        if (!modalSearchElement) return;

        modalSearchElement.innerHTML = '';
        selectedIndex = -1;

        if (results.length === 0) {
            modalSearchElement.innerHTML = `
                <div class="no-results">
                    <p>Không tìm thấy sản phẩm nào cho "${query}"</p>
                </div>
            `;
        } else {
            results.forEach((product, index) => {
                const productElement = createSearchResultElement(product, index);
                modalSearchElement.appendChild(productElement);
            });
        }

        showSearchModal();
    }

    function createSearchResultElement(product, index) {
        const div = document.createElement('div');
        div.className = 'search-result-item';
        div.setAttribute('data-index', index);
        
        const imageUrl = product.hinhAnh ? `/static/images/products/${product.hinhAnh}` : '/static/images/default-product.jpg';
        const price = product.gia ? formatPrice(product.gia) : 'Liên hệ';
        
        div.innerHTML = `
            <div class="product-image">
                <img src="${imageUrl}" alt="${product.tenSanPham}" onerror="this.src='/static/images/default-product.jpg'">
            </div>
            <div class="product-info">
                <h4>${product.tenSanPham}</h4>
                <p class="price">${price}</p>
                <p class="store">${product.cuaHang ? product.cuaHang.tenCuaHang : 'Không rõ'}</p>
            </div>
        `;
        
        div.addEventListener('click', function() {
            window.location.href = `/view/${product.maSanPham}`;
        });
        
        return div;
    }

    function navigateSearchResults(direction) {
        const items = modalSearchElement.querySelectorAll('.search-result-item');
        if (items.length === 0) return;

        if (selectedIndex >= 0) {
            items[selectedIndex].classList.remove('selected');
        }

        if (direction === 'down') {
            selectedIndex = selectedIndex < items.length - 1 ? selectedIndex + 1 : 0;
        } else {
            selectedIndex = selectedIndex > 0 ? selectedIndex - 1 : items.length - 1;
        }

        items[selectedIndex].classList.add('selected');
        items[selectedIndex].scrollIntoView({ block: 'nearest' });
    }

    function showSearchModal() {
        if (modalSearchElement) {
            modalSearchElement.style.display = 'block';
        }
    }

    function hideSearchModal() {
        if (modalSearchElement) {
            modalSearchElement.style.display = 'none';
        }
        selectedIndex = -1;
    }

    function formatPrice(price) {
        return new Intl.NumberFormat('vi-VN', {
            style: 'currency',
            currency: 'VND'
        }).format(price);
    }

    // ========== CATEGORIES FUNCTIONALITY ==========
    function initCategories() {
        if (!categoriesButton || !modalCategory) return;

        categoriesButton.addEventListener('mouseenter', function(e) {
            e.preventDefault();
            showCategory = true;
            modalCategory.style.display = 'block';
            // loadCategories(); // Giữ nguyên logic Thymeleaf nếu đã load
        });

        categoriesButton.addEventListener('mouseleave', function(e) {
            setTimeout(() => {
                if (!isMouseOverModal()) {
                    modalCategory.style.display = 'none';
                    showCategory = false;
                }
            }, 200);
        });

        modalCategory.addEventListener('mouseenter', function(e) {
            showCategory = true;
            modalCategory.style.display = 'block';
        });

        modalCategory.addEventListener('mouseleave', function(e) {
            showCategory = false;
            modalCategory.style.display = 'none';
        });

        categoriesButton.addEventListener('click', function(e) {
            e.preventDefault();
            showCategory = !showCategory;
            
            if (showCategory) {
                modalCategory.style.display = 'block';
                // loadCategories(); // Giữ nguyên logic Thymeleaf nếu đã load
            } else {
                modalCategory.style.display = 'none';
            }
        });
    }

    function isMouseOverModal() {
        const modalRect = modalCategory.getBoundingClientRect();
        const relatedTarget = event.relatedTarget;
        
        if (!relatedTarget) return false;
        
        const relatedRect = relatedTarget.getBoundingClientRect();
        return (
            relatedRect.left >= modalRect.left &&
            relatedRect.right <= modalRect.right &&
            relatedRect.top >= modalRect.top &&
            relatedRect.bottom <= modalRect.bottom
        );
    }

    // Hàm loadCategories bị vô hiệu hóa vì dữ liệu đã có trong HTML Thymeleaf,
    // nhưng giữ lại để minh họa cách gọi API nếu cần.
    /*
    function loadCategories() {
        fetch('/api/categories')
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            if (data.success && data.data) {
                displayCategories(data.data);
            }
        })
        .catch(error => {
            console.error('Categories error:', error);
        });
    }

    function displayCategories(categories) {
        if (!modalCategory) return;

        // Xóa nội dung cũ nếu cần render lại từ API
        // modalCategory.innerHTML = '';
        
        // Logic render categories từ API...
    }
    */

    // ========== MOBILE MENU FUNCTIONALITY ==========
    function initMobileMenu() {
        if (!toggleButton || !navMenu) return;

        toggleButton.addEventListener('click', function() {
            mobileMenu = !mobileMenu;
            
            if (mobileMenu) {
                navMenu.classList.add('active');
                this.innerHTML = '<i class="fas fa-times"></i>';
            } else {
                navMenu.classList.remove('active');
                this.innerHTML = '<i class="fas fa-bars"></i>';
            }
        });
    }
});