// PAS - Project Approval System - Client-side JavaScript

document.addEventListener('DOMContentLoaded', function () {

    // ===== Flash message auto-dismiss =====
    const alerts = document.querySelectorAll('.alert');
    alerts.forEach(alert => {
        setTimeout(() => {
            alert.style.opacity = '0';
            alert.style.transform = 'translateY(-10px)';
            setTimeout(() => alert.remove(), 300);
        }, 5000);
    });

    // ===== Reject Modal Toggle =====
    window.openRejectModal = function (formId) {
        const modal = document.getElementById('rejectModal');
        if (modal) {
            modal.classList.add('active');
            // Set the target form
            modal.dataset.targetForm = formId;
        }
    };

    window.closeRejectModal = function () {
        const modal = document.getElementById('rejectModal');
        if (modal) {
            modal.classList.remove('active');
        }
    };

    window.submitRejectForm = function () {
        const modal = document.getElementById('rejectModal');
        const formId = modal.dataset.targetForm;
        const reason = document.getElementById('modalRejectionReason').value;
        const comments = document.getElementById('modalComments').value;

        if (!reason || reason.trim() === '') {
            // Using global modal instead of alert
            showGlobalAlert('Validation Error', 'Rejection reason is mandatory!');
            return;
        }

        const form = document.getElementById(formId);
        if (form) {
            // Set hidden fields
            const reasonField = form.querySelector('input[name="rejectionReason"]');
            const commentsField = form.querySelector('input[name="comments"]');
            if (reasonField) reasonField.value = reason;
            if (commentsField) commentsField.value = comments;
            form.submit();
        }

        closeRejectModal();
    };

    // Close modal on outside click
    const rejectModal = document.getElementById('rejectModal');
    if (rejectModal) {
        rejectModal.addEventListener('click', function (e) {
            if (e.target === rejectModal) {
                closeRejectModal();
            }
        });
    }

    // ===== Global Confirmation Modal =====
    let confirmActionCallback = null;
    const globalModalEl = document.getElementById('globalConfirmModal');
    let globalConfirmBsModal = null;
    if (globalModalEl && window.bootstrap) {
        globalConfirmBsModal = new bootstrap.Modal(globalModalEl);
    }

    window.showGlobalConfirm = function (title, message, callback) {
        if (!globalConfirmBsModal) {
            // Fallback if bootstrap JS is not loaded yet
            if (confirm(message)) { callback(); }
            return;
        }

        document.getElementById('globalConfirmModalLabel').textContent = title;
        document.getElementById('globalConfirmModalMessage').textContent = message;

        // Hide cancel button for alerts, show for confirm
        document.getElementById('globalConfirmModalCancelBtn').style.display = 'inline-block';

        confirmActionCallback = callback;
        globalConfirmBsModal.show();
    };

    window.showGlobalAlert = function (title, message) {
        if (!globalConfirmBsModal) {
            alert(message);
            return;
        }

        document.getElementById('globalConfirmModalLabel').textContent = title;
        document.getElementById('globalConfirmModalMessage').textContent = message;

        // Hide cancel button for alerts
        document.getElementById('globalConfirmModalCancelBtn').style.display = 'none';

        confirmActionCallback = null;
        globalConfirmBsModal.show();
    };

    const confirmBtn = document.getElementById('globalConfirmModalConfirmBtn');
    if (confirmBtn) {
        confirmBtn.addEventListener('click', function () {
            if (confirmActionCallback) {
                confirmActionCallback();
            }
            if (globalConfirmBsModal) {
                globalConfirmBsModal.hide();
            }
        });
    }

    // Bind data-confirm-action="submit" attributes
    document.querySelectorAll('[data-confirm-action="submit"]').forEach(btn => {
        btn.addEventListener('click', function (e) {
            e.preventDefault();
            const form = this.closest('form');
            if (!form) return;

            const title = this.getAttribute('data-confirm-title') || 'Confirm Action';
            const message = this.getAttribute('data-confirm-message') || 'Are you sure you want to proceed?';

            showGlobalConfirm(title, message, () => form.submit());
        });
    });

    // ===== Legacy Wrapper: Approve Confirmation =====
    window.confirmApprove = function (formId) {
        showGlobalConfirm('Approve Project', 'Are you sure you want to approve this project?', () => {
            const form = document.getElementById(formId);
            if (form) form.submit();
        });
    };

    // ===== Legacy Wrapper: Select topic confirmation =====
    window.confirmSelectTopic = function (formId) {
        showGlobalConfirm('Select Topic', 'Are you sure you want to select this topic? You can only have one active project at a time.', () => {
            const form = document.getElementById(formId);
            if (form) form.submit();
        });
    };

    // ===== File Upload Label =====
    const fileInputs = document.querySelectorAll('input[type="file"]');
    fileInputs.forEach(input => {
        input.addEventListener('change', function () {
            const label = this.closest('.file-upload');
            if (label) {
                const textEl = label.querySelector('.upload-text');
                if (textEl && this.files.length > 0) {
                    textEl.textContent = this.files[0].name;
                }
            }
        });
    });

    // ===== Sidebar active link =====
    const currentPath = window.location.pathname;
    document.querySelectorAll('.nav-link').forEach(link => {
        if (link.getAttribute('href') === currentPath) {
            link.classList.add('active');
        }
    });

    // ===== Mobile sidebar toggle =====
    const toggleBtn = document.getElementById('sidebarToggle');
    const sidebar = document.querySelector('.sidebar');
    if (toggleBtn && sidebar) {
        toggleBtn.addEventListener('click', () => {
            sidebar.classList.toggle('open');
        });
    }

    // ===== Form Validation =====
    const forms = document.querySelectorAll('form[data-validate]');
    forms.forEach(form => {
        form.addEventListener('submit', function (e) {
            const requiredFields = form.querySelectorAll('[required]');
            let isValid = true;

            requiredFields.forEach(field => {
                if (!field.value || field.value.trim() === '') {
                    isValid = false;
                    field.style.borderColor = '#ef4444';
                    field.style.boxShadow = '0 0 0 3px rgba(239, 68, 68, 0.15)';
                } else {
                    field.style.borderColor = '';
                    field.style.boxShadow = '';
                }
            });

            if (!isValid) {
                e.preventDefault();
                showGlobalAlert('Validation Error', 'Please fill in all required fields.');
            }
        });
    });
});
