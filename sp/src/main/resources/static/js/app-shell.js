
(() => {
    const ROLE_MANAGER = 'Quản lý';
    const ROLE_STAFF = 'Nhân viên';
    const AUTH_API = '/api/nhanvien/auth';

    const MENU = [
        {
            label: 'Trang chủ',
            icon: 'fa-home',
            href: '/san-pham/trang-chu',
            paths: ['/san-pham/trang-chu', '/trang-chu'],
            roles: [ROLE_MANAGER, ROLE_STAFF]
        },
        {
            label: 'Bán hàng tại quầy',
            icon: 'fa-store',
            href: '/ban-hang-tai-quay',
            paths: ['/ban-hang', '/ban-hang-tai-quay', '/admin/pos'],
            roles: [ROLE_MANAGER, ROLE_STAFF]
        },
        {
            label: 'Thống kê',
            icon: 'fa-chart-line',
            href: '/thong-ke',
            roles: [ROLE_MANAGER]
        },
        {
            label: 'Quản lý hóa đơn',
            icon: 'fa-file-invoice',
            href: '/hoa-don',
            roles: [ROLE_MANAGER, ROLE_STAFF]
        },
        {
            label: 'Chat hỗ trợ',
            icon: 'fa-comments',
            href: '/chat-ho-tro',
            roles: [ROLE_MANAGER, ROLE_STAFF]
        },
        {
            label: 'Quản lý sản phẩm',
            icon: 'fa-box',
            roles: [ROLE_MANAGER],
            children: [
                {
                    label: 'Sản phẩm',
                    href: '/san-pham/quan-ly',
                    paths: ['/san-pham/quan-ly', '/san-pham']
                },
                {
                    label: 'Biến thể sản phẩm',
                    href: '/san-pham/bien-the'
                }
            ]
        },
        {
            label: 'Quản lý thuộc tính',
            icon: 'fa-list',
            roles: [ROLE_MANAGER],
            children: [
                { label: 'Loại áo', href: '/thuoc-tinh/loai-ao' },
                { label: 'Kích cỡ', href: '/thuoc-tinh/kich-co' },
                { label: 'Màu sắc', href: '/thuoc-tinh/mau-sac' },
                { label: 'Phong cách mặc', href: '/thuoc-tinh/phong-cach-mac' },
                { label: 'Kiểu dáng', href: '/thuoc-tinh/kieu-dang' },
                { label: 'Xuất xứ', href: '/thuoc-tinh/xuat-xu' },
                { label: 'Chất liệu', href: '/thuoc-tinh/chat-lieu' }
            ]
        },
        {
            label: 'Quản lý giảm giá',
            icon: 'fa-tags',
            roles: [ROLE_MANAGER],
            children: [
                { label: 'Đợt giảm giá', href: '/dot-giam-gia' },
                { label: 'Phiếu giảm giá', href: '/phieu-giam-gia' }
            ]
        },
        {
            label: 'Khách hàng',
            icon: 'fa-users',
            href: '/khach-hang',
            roles: [ROLE_MANAGER, ROLE_STAFF]
        },
        {
            label: 'Nhân viên',
            icon: 'fa-user-tie',
            href: '/nhan-vien',
            roles: [ROLE_MANAGER]
        }
    ];

    document.addEventListener('DOMContentLoaded', initShell);

    async function initShell() {
        ensureFontAwesome();
        injectStyle();

        const sidebar = document.querySelector('.sidebar');
        if (!sidebar) return;

        const isLoginPage = window.location.pathname.includes('/dang-nhap');
        if (isLoginPage) return;

        const user = await getCurrentUser();

        if (!user) {
            window.location.href = '/dang-nhap';
            return;
        }

        const role = normalizeRole(user.vaiTro);

        renderSidebar(user, role);
        renderChangePasswordModal();
        bindShellEvents(role);
        protectManagerPages(role);
    }

    function ensureFontAwesome() {
        const existed = [...document.styleSheets].some(sheet => {
            try {
                return sheet.href && sheet.href.includes('font-awesome');
            } catch {
                return false;
            }
        });

        if (existed || document.getElementById('app-shell-fa')) {
            return;
        }

        const link = document.createElement('link');
        link.id = 'app-shell-fa';
        link.rel = 'stylesheet';
        link.href = 'https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css';
        document.head.appendChild(link);
    }

    async function getCurrentUser() {
        try {
            const response = await fetch(`${AUTH_API}/me`, {
credentials: 'same-origin'
});

if (!response.ok) return null;
return await response.json();
} catch {
    return null;
}
}

function normalizeRole(role) {
    const value = String(role || '')
        .trim()
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .toLowerCase();

    if (!value) {
        return ROLE_STAFF;
    }

    if (
        value.includes('nhan vien') ||
        value.includes('nhanvien') ||
        value.includes('staff') ||
        value.includes('employee')
    ) {
        return ROLE_STAFF;
    }

    return ROLE_MANAGER;
}

function renderSidebar(user, role) {
    const sidebar = document.querySelector('.sidebar');
    if (!sidebar) return;

    const currentPath = window.location.pathname.toLowerCase();

    sidebar.className = 'sidebar app-sidebar';

    const menuHtml = MENU
        .filter(item => item.roles.includes(role))
        .map((item, index) => buildMenuItem(item, currentPath, index, role))
        .join('');

    sidebar.innerHTML = `
            <div class="shell-logo">
                <div class="shell-logo-circle">
                    <img src="/images/logo-4menstore.jpg" alt="4MenStore">
                </div>
                <div class="shell-logo-name">4MenStore</div>
            </div>

            <nav class="shell-nav">
                ${menuHtml}
            </nav>

            <div class="shell-account">
                <button type="button"
                        class="shell-account-button"
                        id="shellAccountButton"
                        aria-expanded="false">
                    <div class="shell-account-avatar">
                        ${escapeHtml(initials(user.hoTen))}
                    </div>

                    <div class="shell-account-info">
                        <strong>${escapeHtml(user.hoTen || 'Nhân viên')}</strong>
                        <span>${escapeHtml(role)}</span>
                    </div>

                    <i class="fas fa-chevron-up shell-account-arrow"></i>
                </button>

                <div class="shell-account-menu" id="shellAccountMenu">
                    <div class="shell-account-menu-head">
                        <strong>${escapeHtml(user.hoTen || 'Nhân viên')}</strong>
                        <span>${escapeHtml(user.email || '')}</span>
                    </div>

                    <button type="button"
                            class="shell-account-item"
                            id="shellChangePasswordButton">
                        <i class="fas fa-key"></i>
                        Đổi mật khẩu
                    </button>

                    <button type="button"
                            class="shell-account-item shell-account-logout"
                            id="shellLogoutButton">
                        <i class="fas fa-sign-out-alt"></i>
                        Đăng xuất
                    </button>
                </div>
            </div>
        `;
}

function buildMenuItem(item, currentPath, index, role) {
    if (!item.children) {
        const active = isActive(item, currentPath);

        return `
                <a class="shell-menu-link ${active ? 'active' : ''}"
                   href="${item.href}">
                    <span class="shell-menu-left">
                        <i class="fas ${item.icon}"></i>
                        <span>${item.label}</span>
                    </span>
                </a>
            `;
    }

    const active = item.children.some(child => isActive(child, currentPath));
    const open = active;
    const submenuId = `shell-submenu-${index}`;

    return `
            <button type="button"
                    class="shell-menu-group ${active ? 'active' : ''}"
                    data-submenu="${submenuId}">
                <span class="shell-menu-left">
                    <i class="fas ${item.icon}"></i>
                    <span>${item.label}</span>
                </span>
                <i class="fas fa-chevron-down shell-chevron ${open ? 'rotate' : ''}"></i>
            </button>

            <div id="${submenuId}" class="shell-submenu ${open ? 'open' : ''}">
                ${item.children.map(child => `
                    <a class="shell-submenu-link ${isActive(child, currentPath) ? 'active' : ''}"
                       href="${child.href}">
                        ${child.label}
                    </a>
                `).join('')}
            </div>
        `;
}

function isActive(item, path) {
    const paths = item.paths || [item.href];

    return paths.some(value => {
        const target = String(value || '').toLowerCase();
        return target && (path === target || path.startsWith(`${target}/`));
    });
}

function bindShellEvents(role) {
    document.querySelectorAll('.shell-menu-group').forEach(button => {
        button.addEventListener('click', () => {
            const submenu = document.getElementById(button.dataset.submenu);
            const chevron = button.querySelector('.shell-chevron');

            submenu?.classList.add('open');
            chevron?.classList.add('rotate');
        });
    });

    const accountButton = document.getElementById('shellAccountButton');
    const accountMenu = document.getElementById('shellAccountMenu');

    accountButton?.addEventListener('click', event => {
        event.stopPropagation();
        const opened = accountMenu?.classList.toggle('open');
        accountButton.setAttribute('aria-expanded', String(Boolean(opened)));
    });

    document.addEventListener('click', event => {
        const account = document.querySelector('.shell-account');

        if (account && !account.contains(event.target)) {
            accountMenu?.classList.remove('open');
            accountButton?.setAttribute('aria-expanded', 'false');
        }
    });

    document.getElementById('shellChangePasswordButton')
        ?.addEventListener('click', () => {
            accountMenu?.classList.remove('open');
            accountButton?.setAttribute('aria-expanded', 'false');
            openChangePasswordModal();
        });

    document.getElementById('shellLogoutButton')
        ?.addEventListener('click', logout);

    document.getElementById('shellClosePasswordModal')
        ?.addEventListener('click', closeChangePasswordModal);

    document.getElementById('shellCancelPasswordButton')
        ?.addEventListener('click', closeChangePasswordModal);

    document.getElementById('shellSavePasswordButton')
        ?.addEventListener('click', submitChangePassword);

    document.getElementById('shellPasswordModal')
        ?.addEventListener('click', event => {
            if (event.target.id === 'shellPasswordModal') {
                closeChangePasswordModal();
            }
        });
}

function renderChangePasswordModal() {
    if (document.getElementById('shellPasswordModal')) return;

    const modal = document.createElement('div');
    modal.id = 'shellPasswordModal';
    modal.className = 'shell-password-overlay';

    modal.innerHTML = `
            <div class="shell-password-modal">
                <div class="shell-password-head">
                    <div>
                        <h3>Đổi mật khẩu</h3>
                        <p>Cập nhật mật khẩu đăng nhập tài khoản của bạn.</p>
                    </div>
                    <button class="shell-password-close"
                            id="shellClosePasswordModal"
                            type="button">×</button>
                </div>

                <div class="shell-password-body">
                    <div class="shell-password-field">
                        <label>Mật khẩu hiện tại</label>
                        <input id="shellOldPassword"
                               type="password"
                               placeholder="Nhập mật khẩu hiện tại">
                        <small id="shellOldPasswordError"></small>
                    </div>

                    <div class="shell-password-field">
                        <label>Mật khẩu mới</label>
                        <input id="shellNewPassword"
                               type="password"
                               placeholder="Tối thiểu 6 ký tự">
                        <small id="shellNewPasswordError"></small>
                    </div>

                    <div class="shell-password-field">
                        <label>Xác nhận mật khẩu mới</label>
                        <input id="shellConfirmPassword"
                               type="password"
                               placeholder="Nhập lại mật khẩu mới">
                        <small id="shellConfirmPasswordError"></small>
                    </div>
                </div>

                <div class="shell-password-foot">
                    <button class="shell-password-btn"
                            id="shellCancelPasswordButton"
                            type="button">Hủy</button>
                    <button class="shell-password-btn primary"
                            id="shellSavePasswordButton"
                            type="button">Cập nhật</button>
                </div>
            </div>
        `;

    document.body.appendChild(modal);
}

function openChangePasswordModal() {
    clearPasswordErrors();

    document.getElementById('shellOldPassword').value = '';
    document.getElementById('shellNewPassword').value = '';
    document.getElementById('shellConfirmPassword').value = '';

    const modal = document.getElementById('shellPasswordModal');
    if (modal) {
        modal.classList.add('show');
    }

    setTimeout(() => {
        document.getElementById('shellOldPassword')?.focus();
    }, 50);
}

function closeChangePasswordModal() {
    const modal = document.getElementById('shellPasswordModal');
    if (modal) {
        modal.classList.remove('show');
    }
}

async function submitChangePassword() {
    clearPasswordErrors();

    const oldPassword = document.getElementById('shellOldPassword')?.value || '';
    const newPassword = document.getElementById('shellNewPassword')?.value || '';
    const confirmPassword = document.getElementById('shellConfirmPassword')?.value || '';

    let valid = true;

    if (!oldPassword.trim()) {
        setPasswordError('shellOldPasswordError', 'Vui lòng nhập mật khẩu hiện tại');
        valid = false;
    }

    if (!newPassword.trim()) {
        setPasswordError('shellNewPasswordError', 'Vui lòng nhập mật khẩu mới');
        valid = false;
    } else if (newPassword.length < 6) {
        setPasswordError('shellNewPasswordError', 'Mật khẩu mới phải có ít nhất 6 ký tự');
        valid = false;
    }

    if (!confirmPassword.trim()) {
        setPasswordError('shellConfirmPasswordError', 'Vui lòng xác nhận mật khẩu mới');
        valid = false;
    } else if (newPassword !== confirmPassword) {
        setPasswordError('shellConfirmPasswordError', 'Xác nhận mật khẩu không khớp');
        valid = false;
    }

    if (!valid) return;

    const button = document.getElementById('shellSavePasswordButton');
    button.disabled = true;
    button.textContent = 'Đang cập nhật...';

    try {
        const response = await fetch(`${AUTH_API}/change-password`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'same-origin',
            body: JSON.stringify({
                matKhauCu: oldPassword,
                matKhauMoi: newPassword
            })
        });

        const message = await readMessage(response);

        if (!response.ok) {
            throw new Error(message || 'Không thể đổi mật khẩu');
        }

        closeChangePasswordModal();
        showToast(message || 'Đổi mật khẩu thành công');
    } catch (error) {
        showToast(error.message || 'Không thể đổi mật khẩu', true);
    } finally {
        button.disabled = false;
        button.textContent = 'Cập nhật';
    }
}

async function logout() {
    try {
        await fetch(`${AUTH_API}/logout`, {
            method: 'POST',
            credentials: 'same-origin'
        });
    } finally {
        window.location.href = '/dang-nhap';
    }
}

function protectManagerPages(role) {
    if (role !== ROLE_STAFF) return;

    const path = window.location.pathname.toLowerCase();
    const managerOnlyPaths = [
        '/thong-ke',
        '/san-pham/quan-ly',
        '/san-pham/bien-the',
        '/thuoc-tinh',
        '/nhan-vien',
        '/dot-giam-gia',
        '/phieu-giam-gia'
    ];

    if (managerOnlyPaths.some(item => path.startsWith(item))) {
        window.location.href = '/san-pham/trang-chu';
    }
}

function setPasswordError(id, message) {
    const element = document.getElementById(id);
    if (element) {
        element.textContent = message;
    }
}

function clearPasswordErrors() {
    [
        'shellOldPasswordError',
        'shellNewPasswordError',
        'shellConfirmPasswordError'
    ].forEach(id => {
        const element = document.getElementById(id);
        if (element) {
            element.textContent = '';
        }
    });
}

async function readMessage(response) {
    const text = await response.text();

    try {
        const json = JSON.parse(text);
        return json.message || text;
    } catch {
        return text;
    }
}

function initials(name) {
    return String(name || 'NV')
        .trim()
        .split(/\s+/)
        .filter(Boolean)
        .slice(-2)
        .map(item => item[0])
        .join('')
        .toUpperCase() || 'NV';
}

function showToast(message, error = false) {
    let wrap = document.getElementById('shellToastWrap');

    if (!wrap) {
        wrap = document.createElement('div');
        wrap.id = 'shellToastWrap';
        wrap.className = 'shell-toast-wrap';
        document.body.appendChild(wrap);
    }

    const item = document.createElement('div');
    item.className = `shell-toast ${error ? 'error' : ''}`;
    item.textContent = message;

    wrap.appendChild(item);
    setTimeout(() => item.remove(), 3500);
}

function escapeHtml(value) {
    return String(value ?? '').replace(/[&<>"']/g, character => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    }[character]));
}

function injectStyle() {
    if (document.getElementById('fourMenShellStyle')) return;

    const style = document.createElement('style');
    style.id = 'fourMenShellStyle';
    style.textContent = `
            :root {
                --shell-width: 230px;
                --shell-bg: #ffffff;
                --shell-line: #e6ebf2;
                --shell-hover: #f5f8fc;
                --shell-active: #eef4fb;
                --shell-text: #334155;
                --shell-muted: #7c8ca1;
                --shell-primary: #102a56;
                --shell-gold: #c8a474;
            }

            .sidebar.app-sidebar {
                position: fixed !important;
                inset: 0 auto 0 0 !important;
                z-index: 1000 !important;
                width: var(--shell-width) !important;
                height: 100vh !important;
                padding: 0 !important;
                overflow-y: auto !important;
                display: flex !important;
                flex-direction: column !important;
                background: var(--shell-bg) !important;
                color: var(--shell-text) !important;
                border-right: 1px solid var(--shell-line) !important;
            }

            .main {
                min-width: 0 !important;
                min-height: 100vh !important;
                margin-left: var(--shell-width) !important;
                background: #f3f4f6 !important;
            }

            .shell-logo {
                padding: 16px 12px 14px;
                border-bottom: 1px solid var(--shell-line);
                text-align: center;
            }

            .shell-logo-circle {
                display: flex;
                align-items: center;
                justify-content: center;
                width: 74px;
                height: 74px;
                margin: 0 auto 8px;
                border-radius: 50%;
                background: #fff;
                overflow: hidden;
                border: 1px solid #e6ebf2;
            }

            .shell-logo-circle img {
                width: 100%;
                height: 100%;
                object-fit: cover;
                display: block;
            }

            .shell-logo-name {
                color: var(--shell-primary);
                font-size: 15px;
                font-weight: 800;
            }

            .shell-nav {
                flex: 1;
                padding: 8px 0 16px;
            }

            .shell-menu-link,
            .shell-menu-group {
                display: flex;
                align-items: center;
                justify-content: space-between;
                width: 100%;
                min-height: 40px;
                padding: 0 15px;
                border: 0;
                border-left: 3px solid transparent;
                background: transparent;
                color: var(--shell-text);
                font: 600 13px Arial, sans-serif;
                text-align: left;
                text-decoration: none;
                cursor: pointer;
            }

            .shell-menu-left {
                display: flex;
                align-items: center;
                gap: 10px;
            }

            .shell-menu-link i,
            .shell-menu-group i {
                width: 16px;
                text-align: center;
                color: #5d718b;
            }

            .shell-menu-link:hover,
            .shell-menu-group:hover {
                background: var(--shell-hover);
            }

            .shell-menu-link.active,
            .shell-menu-group.active {
                border-left-color: var(--shell-primary);
                background: var(--shell-active);
                color: var(--shell-primary);
            }

            .shell-menu-link.active i,
            .shell-menu-group.active i {
                color: var(--shell-primary);
            }

            .shell-chevron {
                font-size: 10px;
                color: var(--shell-muted);
                transition: transform .2s ease;
            }

            .shell-chevron.rotate {
                transform: rotate(180deg);
            }

            .shell-submenu {
                display: none;
                background: #fafcff;
            }

            .shell-submenu.open {
                display: block;
            }

            .shell-submenu-link {
                display: block;
                padding: 9px 14px 9px 44px;
                border-left: 3px solid transparent;
                color: #66788f;
                font: 12.5px Arial, sans-serif;
                text-decoration: none;
            }

            .shell-submenu-link:hover {
                background: #f4f8fd;
                color: var(--shell-primary);
            }

            .shell-submenu-link.active {
                border-left-color: var(--shell-primary);
                background: var(--shell-active);
                color: var(--shell-primary);
                font-weight: 700;
            }

            .shell-account {
                position: sticky;
                bottom: 0;
                margin-top: auto;
                border-top: 1px solid var(--shell-line);
                background: #fff;
            }

            .shell-account-button {
                display: flex;
                align-items: center;
                width: 100%;
                gap: 10px;
                padding: 12px;
                border: 0;
                background: transparent;
                color: var(--shell-text);
                cursor: pointer;
                text-align: left;
            }

            .shell-account-button:hover {
                background: var(--shell-hover);
            }

            .shell-account-avatar {
                display: flex;
                align-items: center;
                justify-content: center;
                width: 38px;
                height: 38px;
                flex: 0 0 38px;
                border-radius: 50%;
                background: var(--shell-primary);
                color: #fff;
                font-size: 13px;
                font-weight: 800;
            }

            .shell-account-info {
                min-width: 0;
                flex: 1;
            }

            .shell-account-info strong,
            .shell-account-info span {
                display: block;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
            }

            .shell-account-info strong {
                font-size: 12.5px;
                color: #223a61;
            }

            .shell-account-info span {
                margin-top: 3px;
                color: var(--shell-muted);
                font-size: 11px;
            }

            .shell-account-arrow {
                color: var(--shell-muted);
                font-size: 10px;
            }

            .shell-account-menu {
                position: absolute;
                right: 10px;
                bottom: calc(100% + 8px);
                left: 10px;
                display: none;
                overflow: hidden;
                border: 1px solid var(--shell-line);
                border-radius: 10px;
                background: #fff;
                box-shadow: 0 16px 34px rgba(0,0,0,.12);
            }

            .shell-account-menu.open {
                display: block;
            }

            .shell-account-menu-head {
                padding: 11px 12px;
                border-bottom: 1px solid #edf0f4;
                background: #f8fafc;
            }

            .shell-account-menu-head strong,
            .shell-account-menu-head span {
                display: block;
                overflow: hidden;
                color: #334155;
                text-overflow: ellipsis;
                white-space: nowrap;
            }

            .shell-account-menu-head strong {
                font-size: 12px;
            }

            .shell-account-menu-head span {
                margin-top: 3px;
                color: #718096;
                font-size: 10px;
            }

            .shell-account-item {
                display: flex;
                align-items: center;
                width: 100%;
                gap: 8px;
                padding: 11px 12px;
                border: 0;
                background: #fff;
                color: #35465e;
                font: 700 12px Arial, sans-serif;
                cursor: pointer;
                text-align: left;
            }

            .shell-account-item:hover {
                background: #f7f9fc;
            }

            .shell-account-logout {
                color: #c13d49;
            }

            .shell-password-overlay {
                position: fixed;
                inset: 0;
                z-index: 5000;
                display: none;
                align-items: center;
                justify-content: center;
                padding: 18px;
                background: rgba(15,23,42,.48);
            }

            .shell-password-overlay.show {
                display: flex;
            }

            .shell-password-modal {
                width: min(440px, 100%);
                overflow: hidden;
                border-radius: 13px;
                background: #fff;
                box-shadow: 0 25px 70px rgba(0,0,0,.28);
            }

            .shell-password-head {
                display: flex;
                justify-content: space-between;
                gap: 12px;
                padding: 16px 18px;
                border-bottom: 1px solid #e9edf3;
            }

            .shell-password-head h3 {
                margin: 0;
                color: #253b5c;
                font-size: 16px;
            }

            .shell-password-head p {
                margin: 4px 0 0;
                color: #7b899c;
                font-size: 11px;
            }

            .shell-password-close {
                width: 32px;
                height: 32px;
                border: 0;
                border-radius: 7px;
                background: #f1f4f7;
                color: #68788f;
                font-size: 21px;
                cursor: pointer;
            }

            .shell-password-body {
                padding: 18px;
            }

            .shell-password-field {
                margin-bottom: 12px;
            }

            .shell-password-field label {
                display: block;
                margin-bottom: 6px;
                color: #455a75;
                font-size: 12px;
                font-weight: 700;
            }

            .shell-password-field input {
                width: 100%;
                height: 39px;
                padding: 0 11px;
                border: 1px solid #dce4ee;
                border-radius: 7px;
                outline: none;
                font: 13px Inter, Arial, sans-serif;
            }

            .shell-password-field input:focus {
                border-color: #9ab0d1;
                box-shadow: 0 0 0 3px rgba(71,112,173,.12);
            }

            .shell-password-field small {
                display: block;
                min-height: 15px;
                margin-top: 4px;
                color: #d54753;
                font-size: 10px;
            }

            .shell-password-foot {
                display: flex;
                justify-content: flex-end;
                gap: 8px;
                padding: 13px 18px;
                border-top: 1px solid #e9edf3;
                background: #fafbfd;
            }

            .shell-password-btn {
                height: 35px;
                padding: 0 13px;
                border: 1px solid #dce4ee;
                border-radius: 7px;
                background: #fff;
                color: #42566f;
                font: 700 12px Inter, Arial, sans-serif;
                cursor: pointer;
            }

            .shell-password-btn.primary {
                border-color: var(--shell-primary);
                background: var(--shell-primary);
                color: #fff;
            }

            .shell-toast-wrap {
                position: fixed;
                right: 20px;
                bottom: 20px;
                z-index: 8000;
                display: grid;
                gap: 8px;
            }

            .shell-toast {
                max-width: 380px;
                padding: 12px 14px;
                border-radius: 9px;
                background: #16815e;
                color: #fff;
                font: 700 12px Arial, sans-serif;
                box-shadow: 0 13px 28px rgba(0,0,0,.18);
            }

            .shell-toast.error {
                background: #c73d4a;
            }

            /* Pagination shared across all admin list screens. */
            .pagination .page-btn,
            .pager .page-btn,
            .pg-nav .pg-btn,
            .pages .pg,
            #pgArea .pg-btn {
                border-color: #d9e2ee !important;
                background: #fff !important;
                color: #425b7c !important;
                font-weight: 700 !important;
            }

            .pagination .page-btn:hover:not(:disabled),
            .pager .page-btn:hover:not(:disabled),
            .pg-nav .pg-btn:hover:not(:disabled),
            .pages .pg:hover:not(:disabled),
            #pgArea .pg-btn:hover:not(:disabled) {
                border-color: #102a56 !important;
                background: #eef4fb !important;
                color: #102a56 !important;
            }

            .pagination .page-btn.active,
            .pagination .page-btn.current,
            .pager .page-btn.active,
            .pager .page-btn.current,
            .pg-nav .pg-btn.active,
            .pages .pg.active,
            #pgArea .pg-btn.active {
                border-color: #102a56 !important;
                background: #102a56 !important;
                color: #fff !important;
            }

            .pagination .page-btn:disabled,
            .pager .page-btn:disabled,
            .pg-nav .pg-btn:disabled,
            .pages .pg:disabled,
            #pgArea .pg-btn:disabled {
                opacity: .42;
                cursor: not-allowed;
            }

            @media (max-width: 900px) {
                .sidebar.app-sidebar {
                    width: 0 !important;
                    overflow: hidden !important;
                }

                .main {
                    margin-left: 0 !important;
                }
            }
        `;

    document.head.appendChild(style);
}
})();
