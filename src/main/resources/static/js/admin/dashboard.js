// Sidebar active state management
document.addEventListener('DOMContentLoaded', function() {
    // Initialize sidebar active state
    initSidebarActiveState();
    
    // Add click event listeners to sidebar links
    const sidebarLinks = document.querySelectorAll('.sidebar-link');
    
    sidebarLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            // Don't prevent default for logout link
            if (this.getAttribute('data-page') === 'logout') {
                return;
            }
            
            e.preventDefault();
            
            // Remove active class from all links
            sidebarLinks.forEach(l => l.classList.remove('active'));
            
            // Add active class to clicked link
            this.classList.add('active');
            
            // Store active page in localStorage
            localStorage.setItem('activeSidebarPage', this.getAttribute('data-page'));
            
            // Navigate to the link
            window.location.href = this.getAttribute('href');
        });
    });
});

// Function to initialize sidebar active state
function initSidebarActiveState() {
    const currentPath = window.location.pathname;
    const sidebarLinks = document.querySelectorAll('.sidebar-link');
    
    // Reset fresh login flag when accessing dashboard
    if (currentPath.includes('/admin/dashboard')) {
        localStorage.setItem('freshLogin', 'false');
    }
    
    // Check if this is a fresh login
    const isFreshLogin = localStorage.getItem('freshLogin') === 'true';
    
    // Determine current page based on URL
    let activePage = 'dashboard'; // default
    
    // If this is a fresh login, always default to dashboard
    if (isFreshLogin) {
        activePage = 'dashboard';
        localStorage.setItem('freshLogin', 'false');
    } else {
        // Normal navigation logic
		if (currentPath.includes('/admin/users')) {
		    activePage = 'users';
		} else if (currentPath.includes('/admin/customers')) {
		    activePage = 'customers';
		} else if (currentPath.includes('/admin/stores')) {
		    activePage = 'stores';
		} else if (currentPath.includes('/admin/products')) {
		    activePage = 'products';
		} else if (currentPath.includes('/admin/categories')) {
		    activePage = 'categories';
		} else if (currentPath.includes('/admin/orders')) {
		    activePage = 'orders';
		} else if (currentPath.includes('/admin/sales-history')) {
		    activePage = 'sales-history';
		} else if (currentPath.includes('/admin/reviews')) {
			activePage = 'reviews';
		}
		else if (currentPath.includes('/admin/dashboard')) {
		    activePage = 'dashboard';
		}
        
        // Try to get from localStorage (for persistence during navigation)
        const storedPage = localStorage.getItem('activeSidebarPage');
        if (storedPage) {
            activePage = storedPage;
        }
    }
    
    console.log('Active page determined:', activePage);
    console.log('Current path:', currentPath);
    console.log('Fresh login:', isFreshLogin);
    
    // Set active class
    sidebarLinks.forEach(link => {
        const linkPage = link.getAttribute('data-page');
        if (linkPage === activePage) {
            link.classList.add('active');
            console.log('Activated link:', linkPage);
        } else {
            link.classList.remove('active');
        }
    });
}

// Clear sidebar state on logout
function clearAuthData() {
    console.log('Clearing auth data...');

    // Set flag for fresh login for next time
    localStorage.setItem('freshLogin', 'true');
    
    // Clear localStorage
    localStorage.removeItem('jwtToken');
    localStorage.removeItem('user');
    localStorage.removeItem('activeSidebarPage');

    // Clear sessionStorage
    sessionStorage.clear();

    // Xóa cookies
    document.cookie = "jwtToken=; path=/; expires=Thu, 01 Jan 1970 00:00:01 GMT;";
    document.cookie = "JSESSIONID=; path=/; expires=Thu, 01 Jan 1970 00:00:01 GMT;";

    console.log('Auth data cleared successfully');
}

// Add loading animation
document.addEventListener('DOMContentLoaded', function() {
    console.log('Dashboard loaded - logout handlers attached');

    // Add fade-in animation to all cards
    const cards = document.querySelectorAll('.stat-card, .section-card');
    cards.forEach((card, index) => {
        card.style.animationDelay = `${index * 0.1}s`;
    });
});

// Set fresh login flag when arriving at login page
function setFreshLoginFlag() {
    localStorage.setItem('freshLogin', 'true');
    console.log('Fresh login flag set to true');
}