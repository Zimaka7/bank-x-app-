// Bank X Application JavaScript

document.addEventListener('DOMContentLoaded', function() {
    // Auto-hide alerts after 5 seconds
    const alerts = document.querySelectorAll('.alert');
    alerts.forEach(alert => {
        setTimeout(() => {
            alert.style.opacity = '0';
            setTimeout(() => {
                alert.remove();
            }, 300);
        }, 5000);
    });

    // Form validation
    const forms = document.querySelectorAll('form');
    forms.forEach(form => {
        form.addEventListener('submit', function(e) {
            const requiredFields = form.querySelectorAll('[required]');
            let isValid = true;

            requiredFields.forEach(field => {
                if (!field.value.trim()) {
                    isValid = false;
                    field.style.borderColor = 'var(--absa-red)';
                } else {
                    field.style.borderColor = '#ddd';
                }
            });

            if (!isValid) {
                e.preventDefault();
                showAlert('Please fill in all required fields', 'error');
            }
        });
    });

    // Password confirmation validation
    const passwordField = document.getElementById('password');
    const confirmPasswordField = document.getElementById('confirmPassword');
    
    if (passwordField && confirmPasswordField) {
        confirmPasswordField.addEventListener('blur', function() {
            if (passwordField.value !== confirmPasswordField.value) {
                confirmPasswordField.style.borderColor = 'var(--absa-red)';
                showAlert('Passwords do not match', 'error');
            } else {
                confirmPasswordField.style.borderColor = 'var(--absa-green)';
            }
        });
    }

    // Transfer form validation
    const transferForm = document.getElementById('transferForm');
    if (transferForm) {
        transferForm.addEventListener('submit', function(e) {
            const fromAccount = document.getElementById('fromAccount').value;
            const toAccount = document.getElementById('toAccount').value;
            const amount = parseFloat(document.getElementById('amount').value);

            if (fromAccount === toAccount) {
                e.preventDefault();
                showAlert('Cannot transfer to the same account', 'error');
                return;
            }

            if (amount <= 0) {
                e.preventDefault();
                showAlert('Amount must be greater than zero', 'error');
                return;
            }

            // Show loading spinner
            const submitBtn = transferForm.querySelector('button[type="submit"]');
            submitBtn.innerHTML = '<div class="spinner"></div> Processing...';
            submitBtn.disabled = true;
        });
    }

    // Account balance formatting
    const balanceElements = document.querySelectorAll('.account-balance');
    balanceElements.forEach(element => {
        const balance = parseFloat(element.textContent.replace('R', ''));
        if (balance < 0) {
            element.style.color = 'var(--absa-red)';
        } else {
            element.style.color = 'var(--absa-green)';
        }
    });

    // Transaction type styling
    const transactionTypes = document.querySelectorAll('.transaction-type');
    transactionTypes.forEach(element => {
        const type = element.textContent.trim();
        if (type === 'CREDIT') {
            element.style.color = 'var(--absa-green)';
            element.innerHTML = '↗️ ' + type;
        } else if (type === 'DEBIT') {
            element.style.color = 'var(--absa-red)';
            element.innerHTML = '↙️ ' + type;
        } else if (type === 'TRANSFER') {
            element.style.color = 'var(--absa-blue)';
            element.innerHTML = '↔️ ' + type;
        }
    });

    // Real-time balance updates (for demo purposes)
    function updateBalances() {
        const balanceElements = document.querySelectorAll('.account-balance');
        balanceElements.forEach(element => {
            // Add subtle animation to show "live" updates
            element.style.transition = 'all 0.3s ease';
        });
    }

    // Call updateBalances every 30 seconds
    setInterval(updateBalances, 30000);

    // Mobile menu toggle
    const mobileMenuBtn = document.getElementById('mobileMenuBtn');
    const navMenu = document.querySelector('.nav-menu');
    
    if (mobileMenuBtn && navMenu) {
        mobileMenuBtn.addEventListener('click', function() {
            navMenu.classList.toggle('active');
        });
    }

    // Smooth scrolling for anchor links
    const anchorLinks = document.querySelectorAll('a[href^="#"]');
    anchorLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                target.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        });
    });

    // Copy account number functionality
    const accountNumbers = document.querySelectorAll('.account-number');
    accountNumbers.forEach(element => {
        element.style.cursor = 'pointer';
        element.title = 'Click to copy account number';
        
        element.addEventListener('click', function() {
            const accountNumber = this.textContent.trim();
            navigator.clipboard.writeText(accountNumber).then(() => {
                showAlert('Account number copied to clipboard!', 'success');
            });
        });
    });
});

// Utility function to show alerts
function showAlert(message, type = 'info') {
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type}`;
    alertDiv.textContent = message;
    
    const mainContent = document.querySelector('.main-content');
    if (mainContent) {
        mainContent.insertBefore(alertDiv, mainContent.firstChild);
        
        // Auto-hide after 5 seconds
        setTimeout(() => {
            alertDiv.style.opacity = '0';
            setTimeout(() => {
                alertDiv.remove();
            }, 300);
        }, 5000);
    }
}

// Format currency
function formatCurrency(amount) {
    return new Intl.NumberFormat('en-ZA', {
        style: 'currency',
        currency: 'ZAR'
    }).format(amount);
}

// Validate South African phone number
function validatePhoneNumber(phone) {
    const phoneRegex = /^(\+27|0)[0-9]{9}$/;
    return phoneRegex.test(phone);
}

// Validate email
function validateEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}