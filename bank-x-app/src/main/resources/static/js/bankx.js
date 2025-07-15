/**
 * Bank X - Enhanced UI Interactions and Notifications
 * ABSA-inspired banking application
 */

// Global notification system
class NotificationSystem {
    constructor() {
        this.container = document.getElementById('notificationContainer');
        if (!this.container) {
            this.createContainer();
        }
        this.shownNotifications = this.getShownNotifications();
    }
    
    getShownNotifications() {
        const stored = sessionStorage.getItem('bankx_shown_notifications');
        return stored ? JSON.parse(stored) : [];
    }
    
    markNotificationAsShown(notificationId) {
        this.shownNotifications.push(notificationId);
        sessionStorage.setItem('bankx_shown_notifications', JSON.stringify(this.shownNotifications));
    }
    
    hasNotificationBeenShown(notificationId) {
        return this.shownNotifications.includes(notificationId);
    }
    
    createContainer() {
        this.container = document.createElement('div');
        this.container.id = 'notificationContainer';
        this.container.className = 'notification-container';
        document.body.appendChild(this.container);
    }
    
    show(title, message, type = 'info', duration = 5000) {
        const notification = this.createNotification(title, message, type);
        this.container.appendChild(notification);
        
        // Animate in
        setTimeout(() => notification.classList.add('show'), 100);
        
        // Auto remove
        setTimeout(() => this.remove(notification), duration);
        
        return notification;
    }
    
    createNotification(title, message, type) {
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        
        const icons = {
            success: '✅',
            error: '❌',
            warning: '⚠️',
            info: 'ℹ️',
            money: '💰',
            security: '🔒',
            transfer: '💸'
        };
        
        notification.innerHTML = `
            <div class="notification-header">
                <span class="notification-title">${icons[type] || icons.info} ${title}</span>
                <button class="notification-close" onclick="bankX.notifications.remove(this.closest('.notification'))">×</button>
            </div>
            <div class="notification-body">${message}</div>
            <div class="notification-time">${this.getTimeString()}</div>
        `;
        
        return notification;
    }
    
    remove(notification) {
        if (notification && notification.parentElement) {
            notification.classList.add('removing');
            setTimeout(() => {
                if (notification.parentElement) {
                    notification.remove();
                }
            }, 300);
        }
    }
    
    getTimeString() {
        return new Date().toLocaleTimeString('en-ZA', { 
            hour: '2-digit', 
            minute: '2-digit' 
        });
    }
    
    // Predefined notification types with session tracking
    success(title, message, duration = 5000, notificationId = null) {
        if (notificationId && this.hasNotificationBeenShown(notificationId)) {
            return null;
        }
        if (notificationId) {
            this.markNotificationAsShown(notificationId);
        }
        return this.show(title, message, 'success', duration);
    }
    
    error(title, message, duration = 7000, notificationId = null) {
        if (notificationId && this.hasNotificationBeenShown(notificationId)) {
            return null;
        }
        if (notificationId) {
            this.markNotificationAsShown(notificationId);
        }
        return this.show(title, message, 'error', duration);
    }
    
    warning(title, message, duration = 6000, notificationId = null) {
        if (notificationId && this.hasNotificationBeenShown(notificationId)) {
            return null;
        }
        if (notificationId) {
            this.markNotificationAsShown(notificationId);
        }
        return this.show(title, message, 'warning', duration);
    }
    
    info(title, message, duration = 5000, notificationId = null) {
        if (notificationId && this.hasNotificationBeenShown(notificationId)) {
            return null;
        }
        if (notificationId) {
            this.markNotificationAsShown(notificationId);
        }
        return this.show(title, message, 'info', duration);
    }
    
    money(title, message, duration = 6000, notificationId = null) {
        if (notificationId && this.hasNotificationBeenShown(notificationId)) {
            return null;
        }
        if (notificationId) {
            this.markNotificationAsShown(notificationId);
        }
        return this.show(title, message, 'money', duration);
    }
    
    security(title, message, duration = 8000, notificationId = null) {
        if (notificationId && this.hasNotificationBeenShown(notificationId)) {
            return null;
        }
        if (notificationId) {
            this.markNotificationAsShown(notificationId);
        }
        return this.show(title, message, 'security', duration);
    }
    
    transfer(title, message, duration = 6000, notificationId = null) {
        if (notificationId && this.hasNotificationBeenShown(notificationId)) {
            return null;
        }
        if (notificationId) {
            this.markNotificationAsShown(notificationId);
        }
        return this.show(title, message, 'transfer', duration);
    }
}

// Enhanced form handling
class FormHandler {
    constructor() {
        this.initializeForms();
    }
    
    initializeForms() {
        // Add loading states to all forms
        document.querySelectorAll('form').forEach(form => {
            form.addEventListener('submit', this.handleFormSubmit.bind(this));
        });
        
        // Enhanced input validation
        document.querySelectorAll('.form-control').forEach(input => {
            input.addEventListener('blur', this.validateInput.bind(this));
            input.addEventListener('input', this.clearValidation.bind(this));
        });
        
        // Amount formatting for money inputs
        document.querySelectorAll('input[type="number"][step="0.01"]').forEach(input => {
            input.addEventListener('input', this.formatAmount.bind(this));
        });
    }
    
    handleFormSubmit(event) {
        const form = event.target;
        const submitBtn = form.querySelector('button[type="submit"], input[type="submit"]');
        
        if (submitBtn && !submitBtn.classList.contains('loading')) {
            submitBtn.classList.add('loading');
            submitBtn.disabled = true;
            
            // Re-enable after 5 seconds as fallback
            setTimeout(() => {
                submitBtn.classList.remove('loading');
                submitBtn.disabled = false;
            }, 5000);
        }
    }
    
    validateInput(event) {
        const input = event.target;
        const value = input.value.trim();
        
        // Remove existing validation classes
        input.classList.remove('is-valid', 'is-invalid');
        
        // Basic validation
        if (input.required && !value) {
            this.showInputError(input, 'This field is required');
            return false;
        }
        
        // Email validation
        if (input.type === 'email' && value) {
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(value)) {
                this.showInputError(input, 'Please enter a valid email address');
                return false;
            }
        }
        
        // Amount validation
        if (input.type === 'number' && input.min && parseFloat(value) < parseFloat(input.min)) {
            this.showInputError(input, `Minimum amount is ${input.min}`);
            return false;
        }
        
        // Success state
        if (value) {
            input.classList.add('is-valid');
        }
        
        return true;
    }
    
    clearValidation(event) {
        const input = event.target;
        input.classList.remove('is-invalid');
        const errorMsg = input.parentElement.querySelector('.error-message');
        if (errorMsg) {
            errorMsg.remove();
        }
    }
    
    showInputError(input, message) {
        input.classList.add('is-invalid');
        
        // Remove existing error message
        const existingError = input.parentElement.querySelector('.error-message');
        if (existingError) {
            existingError.remove();
        }
        
        // Add new error message
        const errorDiv = document.createElement('div');
        errorDiv.className = 'error-message';
        errorDiv.textContent = message;
        errorDiv.style.color = 'var(--absa-red)';
        errorDiv.style.fontSize = '0.8rem';
        errorDiv.style.marginTop = '0.25rem';
        
        input.parentElement.appendChild(errorDiv);
    }
    
    formatAmount(event) {
        const input = event.target;
        let value = input.value;
        
        // Remove non-numeric characters except decimal point
        value = value.replace(/[^0-9.]/g, '');
        
        // Ensure only one decimal point
        const parts = value.split('.');
        if (parts.length > 2) {
            value = parts[0] + '.' + parts.slice(1).join('');
        }
        
        // Limit decimal places to 2
        if (parts[1] && parts[1].length > 2) {
            value = parts[0] + '.' + parts[1].substring(0, 2);
        }
        
        input.value = value;
    }
}

// Enhanced UI interactions
class UIEnhancements {
    constructor() {
        this.initializeEnhancements();
    }
    
    initializeEnhancements() {
        this.addButtonEffects();
        this.addCardHoverEffects();
        this.addProgressIndicators();
        this.addKeyboardShortcuts();
    }
    
    addButtonEffects() {
        document.querySelectorAll('.btn').forEach(btn => {
            btn.addEventListener('click', function(e) {
                // Ripple effect
                const ripple = document.createElement('span');
                ripple.className = 'ripple';
                
                const rect = this.getBoundingClientRect();
                const size = Math.max(rect.width, rect.height);
                const x = e.clientX - rect.left - size / 2;
                const y = e.clientY - rect.top - size / 2;
                
                ripple.style.width = ripple.style.height = size + 'px';
                ripple.style.left = x + 'px';
                ripple.style.top = y + 'px';
                
                this.appendChild(ripple);
                
                setTimeout(() => ripple.remove(), 600);
            });
        });
    }
    
    addCardHoverEffects() {
        document.querySelectorAll('.card, .stat-card, .account-card').forEach(card => {
            card.addEventListener('mouseenter', function() {
                this.style.transform = 'translateY(-5px)';
            });
            
            card.addEventListener('mouseleave', function() {
                this.style.transform = 'translateY(0)';
            });
        });
    }
    
    addProgressIndicators() {
        // Add progress bars for loading states
        const style = document.createElement('style');
        style.textContent = `
            .loading-progress {
                position: relative;
                overflow: hidden;
            }
            
            .loading-progress::after {
                content: '';
                position: absolute;
                top: 0;
                left: -100%;
                width: 100%;
                height: 100%;
                background: linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent);
                animation: loading-sweep 1.5s infinite;
            }
            
            @keyframes loading-sweep {
                0% { left: -100%; }
                100% { left: 100%; }
            }
        `;
        document.head.appendChild(style);
    }
    
    addKeyboardShortcuts() {
        document.addEventListener('keydown', function(e) {
            // Alt + D for Dashboard
            if (e.altKey && e.key === 'd') {
                e.preventDefault();
                const dashboardLink = document.querySelector('a[href*="dashboard"]');
                if (dashboardLink) dashboardLink.click();
            }
            
            // Alt + T for Transfers
            if (e.altKey && e.key === 't') {
                e.preventDefault();
                const transferLink = document.querySelector('a[href*="transfer"]');
                if (transferLink) transferLink.click();
            }
            
            // Alt + A for Accounts
            if (e.altKey && e.key === 'a') {
                e.preventDefault();
                const accountsLink = document.querySelector('a[href*="accounts"]');
                if (accountsLink) accountsLink.click();
            }
            
            // Escape to close notifications
            if (e.key === 'Escape') {
                const notifications = document.querySelectorAll('.notification');
                notifications.forEach(n => bankX.notifications.remove(n));
            }
        });
    }
}

// Banking-specific features
class BankingFeatures {
    constructor() {
        this.initializeBankingFeatures();
    }
    
    initializeBankingFeatures() {
        this.addBalanceAnimations();
        this.addTransactionEffects();
        this.addSecurityIndicators();
        this.addRealTimeUpdates();
    }
    
    addBalanceAnimations() {
        document.querySelectorAll('.account-balance, .stat-number').forEach(element => {
            const finalValue = element.textContent;
            if (finalValue.includes('R') || !isNaN(parseFloat(finalValue))) {
                this.animateNumber(element, finalValue);
            }
        });
    }
    
    animateNumber(element, finalValue) {
        const isMonetary = finalValue.includes('R');
        const numericValue = parseFloat(finalValue.replace(/[^0-9.-]/g, ''));
        
        if (isNaN(numericValue)) return;
        
        let currentValue = 0;
        const increment = numericValue / 50;
        const duration = 1500;
        const stepTime = duration / 50;
        
        const timer = setInterval(() => {
            currentValue += increment;
            if (currentValue >= numericValue) {
                currentValue = numericValue;
                clearInterval(timer);
            }
            
            if (isMonetary) {
                element.textContent = 'R' + currentValue.toFixed(2);
            } else {
                element.textContent = Math.floor(currentValue).toString();
            }
        }, stepTime);
    }
    
    addTransactionEffects() {
        document.querySelectorAll('.transaction-item, .transaction-row').forEach(item => {
            item.addEventListener('click', function() {
                this.style.background = 'rgba(227, 30, 36, 0.1)';
                setTimeout(() => {
                    this.style.background = '';
                }, 300);
            });
        });
    }
    
    addSecurityIndicators() {
        // Add security status indicator
        const securityIndicator = document.createElement('div');
        securityIndicator.innerHTML = '🔒 Secure Connection';
        securityIndicator.style.cssText = `
            position: fixed;
            bottom: 20px;
            right: 20px;
            background: var(--absa-green);
            color: white;
            padding: 0.5rem 1rem;
            border-radius: 20px;
            font-size: 0.8rem;
            z-index: 1000;
            opacity: 0.8;
        `;
        document.body.appendChild(securityIndicator);
    }
    
    addRealTimeUpdates() {
        // Simulate real-time balance updates
        setInterval(() => {
            const balances = document.querySelectorAll('.account-balance');
            balances.forEach(balance => {
                // Add subtle pulse effect to indicate live data
                balance.style.animation = 'pulse 0.5s ease-in-out';
                setTimeout(() => {
                    balance.style.animation = '';
                }, 500);
            });
        }, 30000); // Every 30 seconds
    }
}

// Main Bank X application object
const bankX = {
    notifications: null,
    forms: null,
    ui: null,
    banking: null,
    
    init() {
        // Initialize all systems
        this.notifications = new NotificationSystem();
        this.forms = new FormHandler();
        this.ui = new UIEnhancements();
        this.banking = new BankingFeatures();
        
        // Add global styles
        this.addGlobalStyles();
        
        // Show welcome message
        this.showWelcomeMessage();
        
        console.log('🏦 Bank X Enhanced UI System Initialized');
    },
    
    addGlobalStyles() {
        const style = document.createElement('style');
        style.textContent = `
            .notification.show {
                transform: translateX(0);
                opacity: 1;
            }
            
            .notification.removing {
                transform: translateX(100%);
                opacity: 0;
            }
            
            .form-control.is-valid {
                border-color: var(--absa-green);
                box-shadow: 0 0 0 3px rgba(40, 167, 69, 0.1);
            }
            
            .form-control.is-invalid {
                border-color: var(--absa-red);
                box-shadow: 0 0 0 3px rgba(227, 30, 36, 0.1);
            }
            
            .btn {
                position: relative;
                overflow: hidden;
            }
            
            .ripple {
                position: absolute;
                border-radius: 50%;
                background: rgba(255, 255, 255, 0.3);
                transform: scale(0);
                animation: ripple-animation 0.6s linear;
                pointer-events: none;
            }
            
            @keyframes ripple-animation {
                to {
                    transform: scale(4);
                    opacity: 0;
                }
            }
            
            @keyframes pulse {
                0%, 100% { transform: scale(1); }
                50% { transform: scale(1.05); }
            }
        `;
        document.head.appendChild(style);
    },
    
    showWelcomeMessage() {
        setTimeout(() => {
            const isAdmin = document.querySelector('.admin-nav a[href*="admin"]');
            const isCustomer = document.querySelector('.admin-nav a[href*="customer"]');
            
            if (isAdmin) {
                this.notifications.success(
                    'Admin Portal Ready',
                    'Welcome to Bank X Admin Dashboard. All systems operational.',
                    6000,
                    'admin-welcome'
                );
            } else if (isCustomer) {
                this.notifications.success(
                    'Welcome to Bank X',
                    'Your secure banking dashboard is ready. Manage your finances with confidence.',
                    6000,
                    'customer-welcome'
                );
            }
        }, 1500);
    }
};

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    bankX.init();
});

// Export for global access
window.bankX = bankX;