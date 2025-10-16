// Header JavaScript File

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

    // Initialize
    checkAuthStatus();
    initSearch();
    initCategories();
    initMobileMenu();
    initActiveMenu();

    // Authentication Functions
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
            } catch (e) {
                console.error('Error parsing user data:', e);
                // Xóa dữ liệu lỗi
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
        }
    }

    function showAdminMenu() {
        // Kiểm tra xem đã có menu admin chưa
        let adminMenu = document.querySelector('.admin-menu-item');
        
        if (!adminMenu) {
            // Tạo menu item cho admin
            adminMenu = document.createElement('li');
            adminMenu.className = 'admin-menu-item';
            adminMenu.innerHTML = `
                <a href="#" class="nav-link" data-menu="admin" onclick="goToAdminDashboard()">
                    <span class="nav_link_item">Quản trị</span>
                    <div class="nav-indicator"></div>
                </a>
            `;
            
            // Thêm vào trước menu user
            const navMenuUl = document.querySelector('.nav_link ul');
            const userMenuItem = document.querySelector('.user-menu-item');
            if (navMenuUl && userMenuItem) {
                navMenuUl.insertBefore(adminMenu, userMenuItem);
            } else if (navMenuUl) {
                navMenuUl.appendChild(adminMenu);
            }
            
            // Thêm style cho menu admin
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
                    cartQtyElement.textContent = data.data || 0;
                }
            }
        })
        .catch(error => {
            console.error('Error loading cart quantity:', error);
        });
    }

    // Active Menu Management
    function initActiveMenu() {
        // Get current page or set default
        const currentPath = window.location.pathname;
        let activeMenu = 'home'; // default
        
        // Determine active menu based on current path
        if (currentPath === '/' || currentPath === '/home') {
            activeMenu = 'home';
        } else if (currentPath.includes('/profile') || currentPath.includes('/user')) {
            activeMenu = 'user';
        } else if (currentPath.includes('/track-order')) {
            activeMenu = 'track-order';
        } else if (currentPath.includes('/admin')) {
            activeMenu = 'admin';
        }
        
        // Set active menu
        setActiveMenu(activeMenu);
        
        // Add click handlers for menu items
        const navLinks = document.querySelectorAll('.nav-link');
        navLinks.forEach(link => {
            link.addEventListener('click', function(e) {
                const menuType = this.getAttribute('data-menu');
                
                // For user account link, prevent default and use our function
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
        // Remove active class from all menu items
        const navItems = document.querySelectorAll('.nav_link_item');
        const navIndicators = document.querySelectorAll('.nav-indicator');
        
        navItems.forEach(item => {
            item.classList.remove('active');
        });
        
        navIndicators.forEach(indicator => {
            indicator.style.transform = 'scaleX(0)';
        });
        
        // Add active class to current menu item
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

    // Global functions for onclick events
    window.logout = function() {
        localStorage.removeItem('jwtToken');
        localStorage.removeItem('user');
        // Xóa cookie JWT token
        document.cookie = 'jwtToken=; path=/; expires=Thu, 01 Jan 1970 00:00:01 GMT;';
        window.location.href = '/';
    }

    window.goToProfile = function() {
        const token = localStorage.getItem('jwtToken');
        if (!token) {
            // Chưa đăng nhập, chuyển đến trang đăng nhập
            window.location.href = '/login';
            return;
        }

        // Đã đăng nhập, chuyển đến trang hồ sơ
        window.location.href = '/profile';
    }

    window.goToAdminDashboard = function() {
        const token = localStorage.getItem('jwtToken');
        if (!token) {
            window.location.href = '/login';
            return;
        }
        
        // Kiểm tra quyền ADMIN trước khi chuyển hướng
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

    // Dropdown user menu
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

    // Search functionality
    function initSearch() {
        if (!searchInput || !modalSearchElement) return;

        // Prevent form submission when empty
        if (searchForm) {
            searchForm.addEventListener('submit', function(e) {
                const searchValue = searchInput.value.trim();
                if (!searchValue) {
                    e.preventDefault();
                }
            });
        }

        // Real-time search on input
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

        // Handle Enter key
        searchInput.addEventListener('keydown', function(e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                const searchValue = searchInput.value.trim();
                if (searchValue) {
                    hideSearchModal();
                    // Submit form or redirect
                    if (searchForm) {
                        searchForm.submit();
                    }
                }
            }

            // Keyboard navigation in search results
            if (e.key === 'ArrowDown') {
                e.preventDefault();
                navigateSearchResults('down');
            } else if (e.key === 'ArrowUp') {
                e.preventDefault();
                navigateSearchResults('up');
            }
        });

        // Hide search modal when clicking outside
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

        // Remove current selection
        if (selectedIndex >= 0) {
            items[selectedIndex].classList.remove('selected');
        }

        // Update selection
        if (direction === 'down') {
            selectedIndex = selectedIndex < items.length - 1 ? selectedIndex + 1 : 0;
        } else {
            selectedIndex = selectedIndex > 0 ? selectedIndex - 1 : items.length - 1;
        }

        // Add new selection
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

    // Categories functionality
    function initCategories() {
        if (!categoriesButton || !modalCategory) return;

        categoriesButton.addEventListener('click', function(e) {
            e.preventDefault();
            showCategory = !showCategory;
            
            if (showCategory) {
                modalCategory.style.display = 'block';
                loadCategories();
            } else {
                modalCategory.style.display = 'none';
            }
        });

        // Hide categories when clicking outside
        document.addEventListener('click', function(e) {
            if (!categoriesButton.contains(e.target) && !modalCategory.contains(e.target)) {
                modalCategory.style.display = 'none';
                showCategory = false;
            }
        });
    }

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

        modalCategory.innerHTML = '';
        
        categories.forEach(category => {
            const categoryElement = document.createElement('div');
            categoryElement.className = 'category-item';
            categoryElement.innerHTML = `
                <a href="/products?category=${category.maDanhMuc}">
                    <i class="fas fa-tag"></i>
                    ${category.tenDanhMuc}
                </a>
            `;
            modalCategory.appendChild(categoryElement);
        });
    }

    // Mobile menu functionality
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