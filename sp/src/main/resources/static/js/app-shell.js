(function () {
    const attributeLinks = [
        ['xuat-xu', 'Xuất xứ'],
        ['chat-lieu', 'Chất liệu'],
        ['loai-ao', 'Loại áo'],
        ['kich-co', 'Kích cỡ'],
        ['mau-sac', 'Màu sắc'],
        ['phong-cach-mac', 'Phong cách mặc'],
        ['kieu-dang', 'Kiểu dáng']
    ];

    function injectShellStyle() {
        if (document.getElementById('appShellStyle')) return;
        const style = document.createElement('style');
        style.id = 'appShellStyle';
        style.textContent = `
            .sidebar{width:220px;background:#2b1d0e;position:fixed;inset:0 auto 0 0;display:flex;flex-direction:column;overflow-y:auto;z-index:100;scrollbar-width:none}
            .sidebar::-webkit-scrollbar{display:none}
            .sb-logo{display:flex;align-items:center;justify-content:center;padding:16px;border-bottom:1px solid rgba(255,255,255,.08)}
            .sb-logo img{width:66px;height:66px;border-radius:50%;object-fit:cover;border:2px solid rgba(200,164,116,.35);background:#fff}
            .sb-nav{padding:4px 0;flex:1}
            .ni{display:flex;align-items:center;gap:10px;padding:10px 16px;color:rgba(255,255,255,.65);cursor:pointer;font-size:13px;font-weight:500;text-decoration:none;border-left:3px solid transparent;transition:all .13s;user-select:none}
            .ni .ico{width:18px;text-align:center;font-size:15px;flex-shrink:0}
            .ni .arr{margin-left:auto;font-size:10px;opacity:.6;transition:transform .2s}
            .ni:hover{background:rgba(255,255,255,.06);color:#fff}
            .ni.open-parent{color:#c8a474}
            .ni.active-row{background:#7a5230!important;color:#fff!important;border-left-color:#c8a474!important}
            .submenu{overflow:hidden;max-height:0;transition:max-height .25s ease}
            .submenu.open{max-height:640px}
            .si{display:flex;align-items:center;gap:8px;padding:9px 16px 9px 42px;color:rgba(255,255,255,.5);font-size:12.5px;text-decoration:none;transition:all .12s;border-left:3px solid transparent}
            .si:hover{color:#fff;background:rgba(255,255,255,.05)}
            .si.active{color:#c8a474;font-weight:600;border-left-color:#c8a474;background:rgba(122,82,48,.2)}
            .main{margin-left:220px}
            @media (max-width:760px){.sidebar{position:static;width:100%;height:auto}.main{margin-left:0}.app{display:block}}
        `;
        document.head.appendChild(style);
    }

    function pathIs(path) {
        return window.location.pathname === path;
    }

    function pathStarts(path) {
        return window.location.pathname.startsWith(path);
    }

    function linkClass(path) {
        return `si ${pathIs(path) ? 'active' : ''}`.trim();
    }

    function renderSidebar() {
        const sidebar = document.querySelector('.sidebar');
        if (!sidebar) return;

        const isHome = pathIs('/san-pham/trang-chu');
        const isPos = pathIs('/ban-hang-tai-quay') || pathIs('/ban-hang-tai-quay.html');
        const isStats = pathIs('/thong-ke') || pathIs('/thong-ke.html');
        const isProduct = pathStarts('/san-pham/quan-ly') || pathStarts('/san-pham/bien-the');
        const isAttribute = pathStarts('/thuoc-tinh/');
        const isInvoice = pathIs('/hoa-don') || pathIs('/hoa-don.html') || pathIs('/hoa-don-chi-tiet.html');
        const isCustomer = pathIs('/khach-hang') || pathIs('/khach-hang.html') || pathStarts('/khachHang/');
        const isEmployee = pathIs('/nhan-vien') || pathIs('/nhan-vien.html') || pathStarts('/nhanVien/');
        const isVoucher = pathIs('/phieu-giam-gia') || pathIs('/phieu-giam-gia.html') || pathStarts('/voucher');
        const isPromotion = pathIs('/dot-giam-gia') || pathIs('/dot-giam-gia.html') || pathStarts('/promotion');

        sidebar.innerHTML = `
            <div class="sb-logo"><img src="/images/logo-4menstore.jpg" alt="4MenStore"></div>
            <nav class="sb-nav">
                <a href="/san-pham/trang-chu" class="ni ${isHome ? 'active-row' : ''}"><span class="ico">🏠</span> Trang chủ</a>
                <a href="/ban-hang-tai-quay" class="ni ${isPos ? 'active-row' : ''}"><span class="ico">🛒</span> Bán hàng tại quầy</a>
                <a href="/thong-ke" class="ni ${isStats ? 'active-row' : ''}"><span class="ico">📊</span> Thống kê</a>
                <a href="/hoa-don" class="ni ${isInvoice ? 'active-row' : ''}"><span class="ico">🧾</span> Quản lý hóa đơn</a>
                <div class="ni ${isProduct ? 'active-row open-parent' : ''}" onclick="window.toggleAppSub('sub-sp','arr-sp')">
                    <span class="ico">📦</span> Quản lý sản phẩm
                    <span class="arr" id="arr-sp" style="${isProduct ? 'transform:rotate(90deg)' : ''}">▶</span>
                </div>
                <div class="submenu ${isProduct ? 'open' : ''}" id="sub-sp">
                    <a href="/san-pham/quan-ly" class="${linkClass('/san-pham/quan-ly')}">Sản phẩm</a>
                    <a href="/san-pham/bien-the" class="${linkClass('/san-pham/bien-the')}">Biến thể sản phẩm</a>
                </div>
                <div class="ni ${isAttribute ? 'active-row open-parent' : ''}" onclick="window.toggleAppSub('sub-tt','arr-tt')">
                    <span class="ico">📋</span> Thuộc tính
                    <span class="arr" id="arr-tt" style="${isAttribute ? 'transform:rotate(90deg)' : ''}">▶</span>
                </div>
                <div class="submenu ${isAttribute ? 'open' : ''}" id="sub-tt">
                    ${attributeLinks.map(([slug, label]) => `<a href="/thuoc-tinh/${slug}" class="${linkClass(`/thuoc-tinh/${slug}`)}">${label}</a>`).join('')}
                </div>
                <a href="/phieu-giam-gia" class="ni ${isVoucher ? 'active-row' : ''}"><span class="ico">🏷️</span> Phiếu giảm giá</a>
                <a href="/dot-giam-gia" class="ni ${isPromotion ? 'active-row' : ''}"><span class="ico">🎟️</span> Đợt giảm giá</a>
                <a href="/khach-hang" class="ni ${isCustomer ? 'active-row' : ''}"><span class="ico">👥</span> Khách hàng</a>
                <a href="/nhan-vien" class="ni ${isEmployee ? 'active-row' : ''}"><span class="ico">👤</span> Nhân viên</a>
            </nav>
        `;
    }

    window.toggleAppSub = function (subId, arrId) {
        const sub = document.getElementById(subId);
        const arr = document.getElementById(arrId);
        if (!sub || !arr) return;
        const open = sub.classList.toggle('open');
        arr.style.transform = open ? 'rotate(90deg)' : 'rotate(0deg)';
    };

    document.addEventListener('DOMContentLoaded', () => {
        injectShellStyle();
        renderSidebar();
    });
})();
