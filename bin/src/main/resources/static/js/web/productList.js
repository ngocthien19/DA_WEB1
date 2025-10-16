// Function to remove individual filter
function removeFilter(element) {
    const filterType = element.getAttribute('data-type');
    const filterValue = element.getAttribute('data-value');
    
    // Get current URL parameters
    const urlParams = new URLSearchParams(window.location.search);
    
    // Remove the specific filter value
    if (urlParams.has(filterType)) {
        const currentValues = urlParams.getAll(filterType);
        const updatedValues = currentValues.filter(value => value !== filterValue);
        
        // Remove the parameter if no values left
        if (updatedValues.length === 0) {
            urlParams.delete(filterType);
        } else {
            // Update with remaining values
            urlParams.delete(filterType);
            updatedValues.forEach(value => urlParams.append(filterType, value));
        }
    }
    
    // Build new URL
    const newUrl = window.location.pathname + '?' + urlParams.toString();
    
    // Redirect to new URL
    window.location.href = newUrl;
}

// Function to clear all filters
function clearAllFilters() {
    // Keep only the search parameter if exists
    const urlParams = new URLSearchParams(window.location.search);
    const searchValue = urlParams.get('search');
    
    if (searchValue) {
        window.location.href = window.location.pathname + '?search=' + encodeURIComponent(searchValue);
    } else {
        window.location.href = window.location.pathname;
    }
}

// Toggle filter sections
function initFilterToggle() {
    document.querySelectorAll('.box-price').forEach(header => {
        header.addEventListener('click', function() {
            const priceRange = this.parentElement;
            priceRange.classList.toggle('active');
        });
    });
}

// Add to cart function
function addToCart(productId) {
    console.log('Add to cart:', productId);
    alert('Đã thêm sản phẩm vào giỏ hàng!');
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    // AOS Animation
    if (typeof AOS !== 'undefined') {
        AOS.init({
            duration: 800,
            once: true
        });
    }
    
    // Initialize filter toggles
    initFilterToggle();
});

// Export functions for global use
window.removeFilter = removeFilter;
window.clearAllFilters = clearAllFilters;
window.addToCart = addToCart;
window.initFilterToggle = initFilterToggle;