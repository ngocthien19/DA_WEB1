	// Vendor Dashboard JavaScript
	document.addEventListener('DOMContentLoaded', function() {
	    console.log('Vendor Dashboard loaded');
	    
	    // Initialize all components
	    initVendorDashboard();
	});
	
	// Initialize all dashboard components
	function initVendorDashboard() {
	    initVendorSidebarActiveState();
	    initSidebarNavigation();
	    initQuickActionLinks();
	    initHeaderLogout();
	    animateVendorCards();
	    initVendorCharts();
	    handleStoreLockedAlertOnLoad();
	}
	
	// Initialize sidebar navigation
	function initSidebarNavigation() {
	    const sidebarLinks = document.querySelectorAll('.sidebar-link');
	    
	    sidebarLinks.forEach(link => {
	        link.addEventListener('click', function(e) {
	            const page = this.getAttribute('data-page');
	            const href = this.getAttribute('href');
	            
	            console.log('Sidebar link clicked:', page, href);
	            
	            // Handle logout separately
	            if (page === 'logout') {
	                e.preventDefault();
	                handleLogout();
	                return;
	            }
	            
	            // Update UI for other links
	            updateSidebarActiveState(page);
	            localStorage.setItem('activeVendorPage', page);
	            
	            // Allow natural navigation for other links
	        });
	    });
	}
	
	// Initialize header logout functionality
	function initHeaderLogout() {
	    const headerLogoutLinks = document.querySelectorAll('.dropdown-item[href*="logout"], .header-logout-link');
	    
	    headerLogoutLinks.forEach(link => {
	        link.addEventListener('click', function(e) {
	            e.preventDefault();
	            console.log('Header logout clicked');
	            handleLogout();
	        });
	    });
	    
	    // Also handle onclick for backward compatibility
	    const onclickLogoutLinks = document.querySelectorAll('[onclick*="logout"], [onclick*="clearAuthData"]');
	    onclickLogoutLinks.forEach(link => {
	        const originalOnclick = link.getAttribute('onclick');
	        link.removeAttribute('onclick');
	        link.addEventListener('click', function(e) {
	            e.preventDefault();
	            handleLogout();
	        });
	    });
	}
	
	// Main logout handler
	function handleLogout() {
	    console.log('Logout process started');
	    
	    // Clear authentication data
	    clearVendorAuthData();
	    
	    // Redirect to login page after a short delay
	    setTimeout(() => {
	        window.location.href = '/login?logout=true';
	    }, 100);
	}
	
	// Clear vendor authentication data
	function clearVendorAuthData() {
	    console.log('Clearing vendor auth data...');
	
	    try {
	        // Set flag for fresh login next time
	        localStorage.setItem('freshVendorLogin', 'true');
	        
	        // Clear vendor-specific localStorage items
	        const itemsToRemove = [
	            'jwtToken', 'user', 'activeVendorPage', 'vendorStoreData',
	            'vendorSession', 'userData', 'authToken'
	        ];
	        
	        itemsToRemove.forEach(item => {
	            localStorage.removeItem(item);
	        });
	
	        // Clear sessionStorage
	        sessionStorage.clear();
	
	        // Clear cookies
	        document.cookie.split(";").forEach(function(c) {
	            document.cookie = c.replace(/^ +/, "").replace(/=.*/, "=;expires=" + new Date().toUTCString() + ";path=/");
	        });
	
	        console.log('Vendor auth data cleared successfully');
	    } catch (error) {
	        console.error('Error clearing auth data:', error);
	    }
	}
	
	// Store locked status functions
	function checkStoreLockedStatus() {
	    const lockAlert = document.getElementById('storeLockAlert');
	    const storeStatusBadge = document.querySelector('.store-status-badge, .header-status-badge');
	    
	    if (lockAlert && lockAlert.getAttribute('data-is-locked') === 'true') {
	        return true;
	    }
	    
	    if (storeStatusBadge && storeStatusBadge.textContent.includes('Ngừng hoạt động')) {
	        return true;
	    }
	    
	    return false;
	}
	
	function showStoreLockedAlert() {
	    const lockAlert = document.getElementById('storeLockAlert');
	    if (lockAlert) {
	        toggleLockAlert(true);
	    }
	}
	
	function handleStoreLockedAlertOnLoad() {
	    const currentPath = window.location.pathname;
	    
	    if (currentPath.includes('/vendor/dashboard')) {
	        const isLocked = checkStoreLockedStatus();
	        const isFreshLogin = localStorage.getItem('freshVendorLogin') === 'true';
	
	        console.log('Page load - Locked:', isLocked, 'Fresh login:', isFreshLogin);
	
	        if (isLocked && isFreshLogin) {
	            console.log('Showing locked alert on fresh login');
	            toggleLockAlert(true);
	        }
	        
	        localStorage.setItem('freshVendorLogin', 'false');
	    }
	}
	
	function toggleLockAlert(show) {
	    const lockAlert = document.getElementById('storeLockAlert');
	    const isLocked = checkStoreLockedStatus();
	
	    let backdrop = document.getElementById('lockBackdrop');
	
	    if (show && isLocked) {
	        console.log('Showing lock alert and backdrop');
	        
	        if (lockAlert) {
	            lockAlert.classList.remove('d-none');
	            lockAlert.classList.add('d-block', 'show');
	        }
	
	        if (!backdrop) {
	            backdrop = document.createElement('div');
	            backdrop.id = 'lockBackdrop';
	            backdrop.className = 'alert-lock-backdrop';
	            document.body.appendChild(backdrop);
	            
	            backdrop.addEventListener('click', () => {
	                console.log('Backdrop clicked, closing alert');
	                if (lockAlert && lockAlert.classList.contains('d-block')) {
	                    lockAlert.classList.remove('show');
	                    setTimeout(() => {
	                        lockAlert.classList.remove('d-block');
	                        lockAlert.classList.add('d-none');
	                        toggleLockAlert(false);
	                    }, 300);
	                }
	            });
	        }
	    } else {
	        console.log('Hiding lock alert and backdrop');
	        if (lockAlert) {
	            lockAlert.classList.remove('d-block', 'show');
	            lockAlert.classList.add('d-none');
	        }
	        if (backdrop) {
	            backdrop.remove();
	        }
	    }
	}
	
	// Quick action links
	function initQuickActionLinks() {
	    const quickActionLinks = document.querySelectorAll('.list-group-item-action[data-target-page]');
	    
	    quickActionLinks.forEach(link => {
	        link.addEventListener('click', function(e) {
	            const targetPage = this.getAttribute('data-target-page');
	            console.log('Quick action link clicked, target page:', targetPage);
	            
	            if (targetPage) {
	                localStorage.setItem('activeVendorPage', targetPage);
	                updateSidebarActiveState(targetPage);
	            }
	        });
	    });
	}
	
	// Sidebar active state management
	function updateSidebarActiveState(activePage) {
	    const sidebarLinks = document.querySelectorAll('.sidebar-link');
	    sidebarLinks.forEach(sidebarLink => {
	        const sidebarPage = sidebarLink.getAttribute('data-page');
	        if (sidebarPage === activePage) {
	            sidebarLink.classList.add('active');
	        } else {
	            sidebarLink.classList.remove('active');
	        }
	    });
	}
	
	function initVendorSidebarActiveState() {
	    const currentPath = window.location.pathname;
	    const sidebarLinks = document.querySelectorAll('.sidebar-link');
	    
	    console.log('Current path:', currentPath);
	    
	    let activePage = 'dashboard';
	    
		if (currentPath.includes('/vendor/products')) {
			activePage = 'products';
		} else if (currentPath.includes('/vendor/promotions')) {
			activePage = 'promotions';
		} else if (currentPath.includes('/vendor/orders')) {
			activePage = 'orders';
		} else if (currentPath.includes('/vendor/delivery')) {
			activePage = 'delivery';
		} else if (currentPath.includes('/vendor/reviews')) {
			activePage = 'reviews'; 
		} else if (currentPath.includes('/vendor/sales-history')) {
			activePage = 'sales-history';
		} else if (currentPath.includes('/vendor/account')) {
			activePage = 'account';
		} else if (currentPath.includes('/vendor/dashboard')) {
			activePage = 'dashboard';
		} else if (currentPath.includes('/vendor/messages')) {
			activePage = 'messages';
		}
	    
	    const storedPage = localStorage.getItem('activeVendorPage');
	    if (storedPage && !currentPath.includes('/vendor/dashboard')) {
	        activePage = storedPage;
	    }
	    
	    console.log('Vendor active page:', activePage);
	    
	    sidebarLinks.forEach(link => {
	        const linkPage = link.getAttribute('data-page');
	        if (linkPage === activePage) {
	            link.classList.add('active');
	            console.log('Activated vendor link:', linkPage);
	        } else {
	            link.classList.remove('active');
	        }
	    });
	}
	
	// Animations
	function animateVendorCards() {
	    const cards = document.querySelectorAll('.stat-card, .section-card');
	    cards.forEach((card, index) => {
	        card.style.animationDelay = `${index * 0.1}s`;
	        card.classList.add('fade-in');
	    });
	}
	
	// Revenue Chart Functions
	function initVendorCharts() {
	    initRevenueChart();
	}
	
	async function initRevenueChart() {
	    const ctx = document.getElementById('revenueChart');
	    if (!ctx) {
	        console.log('Revenue chart element not found');
	        return;
	    }
	    
	    try {
	        console.log('Loading actual revenue data...');
	        const revenueData = await getActualRevenueData();
	        renderRevenueChart(ctx, revenueData);
	    } catch (error) {
	        console.error('Error loading revenue chart:', error);
	        const sampleData = generateSampleRevenueData();
	        renderRevenueChart(ctx, sampleData);
	    }
	}
	
	async function getActualRevenueData() {
	    try {
	        const response = await fetch('/vendor/api/revenue-data?days=7');
	        if (response.ok) {
	            return await response.json();
	        } else {
	            throw new Error('Failed to fetch revenue data');
	        }
	    } catch (error) {
	        console.error('Error fetching revenue data:', error);
	        throw error;
	    }
	}
	
	function generateSampleRevenueData() {
	    const days = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];
	    const data = [];
	    const orderCounts = [];
	    const productCounts = [];
	    
	    let baseRevenue = 5000000;
	    
	    for (let i = 0; i < 7; i++) {
	        const variation = 0.8 + (Math.random() * 0.5);
	        const dailyRevenue = Math.round(baseRevenue * variation);
	        data.push(dailyRevenue);
	        orderCounts.push(Math.floor(Math.random() * 13) + 3);
	        productCounts.push(Math.floor(Math.random() * 36) + 5);
	        baseRevenue *= 1.02;
	    }
	    
	    const totalRevenue = data.reduce((sum, value) => sum + value, 0);
	    const totalOrders = orderCounts.reduce((sum, value) => sum + value, 0);
	    const totalProducts = productCounts.reduce((sum, value) => sum + value, 0);
	    
	    return {
	        labels: days,
	        data: data,
	        orderCounts: orderCounts,
	        productCounts: productCounts,
	        totalRevenue: totalRevenue,
	        totalOrders: totalOrders,
	        totalProducts: totalProducts,
	        averageRevenue: totalRevenue / 7,
	        storeName: 'Cửa hàng của bạn',
	        period: 'Tuần này'
	    };
	}
	
	function renderRevenueChart(ctx, revenueData) {
	    if (ctx.chart) {
	        ctx.chart.destroy();
	    }
	    
	    ctx.chart = new Chart(ctx, {
	        type: 'line',
	        data: {
	            labels: revenueData.labels,
	            datasets: [{
	                label: 'Doanh thu (VND)',
	                data: revenueData.data,
	                borderColor: '#4e73df',
	                backgroundColor: 'rgba(78, 115, 223, 0.1)',
	                borderWidth: 2,
	                fill: true,
	                tension: 0.4,
	                pointBackgroundColor: '#4e73df',
	                pointBorderColor: '#ffffff',
	                pointBorderWidth: 2,
	                pointRadius: 4,
	                pointHoverRadius: 6
	            }]
	        },
	        options: {
	            responsive: true,
	            maintainAspectRatio: false,
	            plugins: {
	                legend: {
	                    display: true,
	                    position: 'top',
	                    labels: {
	                        usePointStyle: true,
	                        padding: 15,
	                        font: { size: 12 }
	                    }
	                },
	                tooltip: {
	                    mode: 'index',
	                    intersect: false,
	                    backgroundColor: 'rgba(0, 0, 0, 0.8)',
	                    padding: 12,
	                    cornerRadius: 8,
	                    callbacks: {
	                        label: function(context) {
	                            let label = context.dataset.label || '';
	                            if (label) label += ': ';
	                            if (context.parsed.y !== null) {
	                                label += formatCurrency(context.parsed.y);
	                            }
	                            return label;
	                        },
	                        afterLabel: function(context) {
	                            const index = context.dataIndex;
	                            const orderCount = revenueData.orderCounts ? revenueData.orderCounts[index] : 0;
	                            const productCount = revenueData.productCounts ? revenueData.productCounts[index] : 0;
	                            return [`Số đơn: ${orderCount}`, `Số SP: ${productCount}`];
	                        }
	                    }
	                }
	            },
	            scales: {
	                x: {
	                    grid: { display: false, drawBorder: false },
	                    ticks: { maxRotation: 0, font: { size: 11 } }
	                },
	                y: {
	                    beginAtZero: true,
	                    grid: { borderDash: [2], drawBorder: false },
	                    ticks: {
	                        callback: function(value) {
	                            if (value >= 1000000) return (value / 1000000).toFixed(1) + 'tr';
	                            if (value >= 1000) return (value / 1000).toFixed(0) + 'k';
	                            return value;
	                        },
	                        font: { size: 11 }
	                    }
	                }
	            },
	            interaction: { intersect: false, mode: 'nearest' },
	            elements: { line: { tension: 0.4 } }
	        }
	    });
	    
	    displayRevenueSummary(revenueData);
	}
	
	function displayRevenueSummary(revenueData) {
	    const summaryContainer = document.getElementById('revenueSummary');
	    if (!summaryContainer) return;
	    
	    const summaryHtml = `
	        <div class="row text-center mt-3">
	            <div class="col-md-4 mb-3">
	                <div class="card border-0 bg-primary text-white">
	                    <div class="card-body py-3">
	                        <h5 class="card-title mb-1">${formatCurrency(revenueData.totalRevenue)}</h5>
	                        <small class="card-text">Tổng doanh thu</small>
	                    </div>
	                </div>
	            </div>
	            <div class="col-md-4 mb-3">
	                <div class="card border-0 bg-success text-white">
	                    <div class="card-body py-3">
	                        <h5 class="card-title mb-1">${revenueData.totalOrders || 0}</h5>
	                        <small class="card-text">Tổng đơn hàng</small>
	                    </div>
	                </div>
	            </div>
	            <div class="col-md-4 mb-3">
	                <div class="card border-0 bg-info text-white">
	                    <div class="card-body py-3">
	                        <h5 class="card-title mb-1">${revenueData.totalProducts || 0}</h5>
	                        <small class="card-text">Sản phẩm đã bán</small>
	                    </div>
	                </div>
	            </div>
	        </div>
	        ${revenueData.period ? `
	        <div class="text-center mt-2">
	            <small class="text-muted">
	                <i class="fas fa-calendar me-1"></i>${revenueData.period}
	            </small>
	        </div>
	        ` : ''}
	    `;
	    
	    summaryContainer.innerHTML = summaryHtml;
	}
	
	// Utility functions
	function formatCurrency(amount) {
	    return new Intl.NumberFormat('vi-VN', {
	        style: 'currency',
	        currency: 'VND'
	    }).format(amount);
	}
	
	// Make functions globally available
	window.clearAuthData = clearVendorAuthData;
	window.handleLogout = handleLogout;