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
    
    // Determine current page based on URL
    let activePage = 'dashboard'; // default
    
    if (currentPath.includes('/admin/users')) {
        activePage = 'users';
    } else if (currentPath.includes('/admin/stores')) {
        activePage = 'stores';
    } else if (currentPath.includes('/admin/products')) {
        activePage = 'products';
    } else if (currentPath.includes('/admin/categories')) {
        activePage = 'categories';
    } else if (currentPath.includes('/admin/orders')) {
        activePage = 'orders';
    } else if (currentPath.includes('/admin/dashboard')) {
        activePage = 'dashboard';
    }
    
    // Try to get from localStorage first (for persistence during navigation)
    const storedPage = localStorage.getItem('activeSidebarPage');
    if (storedPage) {
        activePage = storedPage;
    }
    
    // Set active class
    sidebarLinks.forEach(link => {
        if (link.getAttribute('data-page') === activePage) {
            link.classList.add('active');
        } else {
            link.classList.remove('active');
        }
    });
}

// Clear sidebar state on logout
function clearAuthData() {
    localStorage.removeItem('activeSidebarPage');
    // Add other cleanup as needed
}
// Function để clear tất cả auth data
function clearAuthData() {
	console.log('Clearing auth data...');

	// Clear localStorage
	localStorage.removeItem('jwtToken');
	localStorage.removeItem('user');

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