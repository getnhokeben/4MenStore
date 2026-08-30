IF COL_LENGTH('chi_tiet_san_pham', 'so_luong_giu') IS NULL ALTER TABLE chi_tiet_san_pham ADD so_luong_giu int NOT NULL DEFAULT 0 WITH VALUES;
IF COL_LENGTH('hoa_don', 'da_giu_ton') IS NULL ALTER TABLE hoa_don ADD da_giu_ton bit NOT NULL DEFAULT 0 WITH VALUES;
IF COL_LENGTH('hoa_don', 'da_tru_ton') IS NULL ALTER TABLE hoa_don ADD da_tru_ton bit NULL;
IF COL_LENGTH('hoa_don', 'da_hoan_ton') IS NULL ALTER TABLE hoa_don ADD da_hoan_ton bit NOT NULL DEFAULT 0 WITH VALUES;
IF COL_LENGTH('hoa_don', 'ly_do_hoan_hang') IS NULL ALTER TABLE hoa_don ADD ly_do_hoan_hang nvarchar(500) NULL;
IF COL_LENGTH('hoa_don', 'ghi_chu_hoan_hang') IS NULL ALTER TABLE hoa_don ADD ghi_chu_hoan_hang nvarchar(1000) NULL;
IF COL_LENGTH('hoa_don', 'ngay_yeu_cau_hoan') IS NULL ALTER TABLE hoa_don ADD ngay_yeu_cau_hoan datetime2 NULL;
IF COL_LENGTH('hoa_don', 'ngay_nhan_hang_hoan') IS NULL ALTER TABLE hoa_don ADD ngay_nhan_hang_hoan datetime2 NULL;
IF COL_LENGTH('hoa_don_chi_tiet', 'so_luong_hoan_kho') IS NULL ALTER TABLE hoa_don_chi_tiet ADD so_luong_hoan_kho int NOT NULL DEFAULT 0 WITH VALUES;
IF COL_LENGTH('hoa_don', 'hinh_thuc_nhan_hang') IS NULL ALTER TABLE hoa_don ADD hinh_thuc_nhan_hang nvarchar(30) NULL;
IF COL_LENGTH('hoa_don', 'pgg_snapshot_ma') IS NULL ALTER TABLE hoa_don ADD pgg_snapshot_ma nvarchar(100) NULL;
IF COL_LENGTH('hoa_don', 'pgg_snapshot_ten') IS NULL ALTER TABLE hoa_don ADD pgg_snapshot_ten nvarchar(255) NULL;
IF COL_LENGTH('hoa_don', 'pgg_snapshot_loai') IS NULL ALTER TABLE hoa_don ADD pgg_snapshot_loai nvarchar(50) NULL;
IF COL_LENGTH('hoa_don', 'pgg_snapshot_gia_tri') IS NULL ALTER TABLE hoa_don ADD pgg_snapshot_gia_tri decimal(18,2) NULL;
IF COL_LENGTH('hoa_don', 'pgg_snapshot_gia_tri_toi_da') IS NULL ALTER TABLE hoa_don ADD pgg_snapshot_gia_tri_toi_da decimal(18,2) NULL;
IF COL_LENGTH('hoa_don', 'pgg_snapshot_dieu_kien') IS NULL ALTER TABLE hoa_don ADD pgg_snapshot_dieu_kien decimal(18,2) NULL;
IF COL_LENGTH('hoa_don', 'pgg_snapshot_so_tien_giam') IS NULL ALTER TABLE hoa_don ADD pgg_snapshot_so_tien_giam decimal(18,2) NULL;
UPDATE hoa_don SET hinh_thuc_nhan_hang = CASE WHEN loai_don IN (N'Giao hàng', N'Trực tuyến') THEN N'Giao hàng' ELSE N'Tại quầy' END WHERE hinh_thuc_nhan_hang IS NULL;
UPDATE hoa_don SET loai_don = N'Tại quầy' WHERE loai_don = N'Giao hàng';
IF OBJECT_ID('shop_chat_session', 'U') IS NULL
CREATE TABLE shop_chat_session (
    id_session int IDENTITY(1,1) NOT NULL PRIMARY KEY,
    session_key varchar(120) NOT NULL UNIQUE,
    id_kh int NULL,
    ten_khach_hang nvarchar(255) NULL,
    email nvarchar(255) NULL,
    trang_thai varchar(30) NOT NULL DEFAULT 'AI',
    id_nhan_vien int NULL,
    ten_nhan_vien nvarchar(255) NULL,
    ngay_tao datetime2 NOT NULL DEFAULT SYSDATETIME(),
    ngay_cap_nhat datetime2 NOT NULL DEFAULT SYSDATETIME(),
    ngay_dong datetime2 NULL,
    CONSTRAINT FK_shop_chat_session_khach_hang FOREIGN KEY (id_kh) REFERENCES khach_hang(id_kh),
    CONSTRAINT FK_shop_chat_session_nhan_vien FOREIGN KEY (id_nhan_vien) REFERENCES nhan_vien(id_nv)
);

IF OBJECT_ID('shop_chat_message', 'U') IS NULL
CREATE TABLE shop_chat_message (
    id_message int IDENTITY(1,1) NOT NULL PRIMARY KEY,
    id_session int NOT NULL,
    sender_type varchar(20) NOT NULL,
    sender_name nvarchar(255) NULL,
    noi_dung nvarchar(1200) NOT NULL,
    ai_generated bit NOT NULL DEFAULT 0,
    ngay_tao datetime2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_shop_chat_message_session FOREIGN KEY (id_session) REFERENCES shop_chat_session(id_session)
);

-- Bổ sung khóa ngoại cho database đã được tạo từ phiên bản schema cũ.
-- Database Diagram chỉ tự sinh dây nối khi những constraint này tồn tại.
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_shop_chat_session_khach_hang' AND parent_object_id = OBJECT_ID('shop_chat_session'))
ALTER TABLE shop_chat_session WITH CHECK ADD CONSTRAINT FK_shop_chat_session_khach_hang
    FOREIGN KEY (id_kh) REFERENCES khach_hang(id_kh);

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_shop_chat_session_nhan_vien' AND parent_object_id = OBJECT_ID('shop_chat_session'))
ALTER TABLE shop_chat_session WITH CHECK ADD CONSTRAINT FK_shop_chat_session_nhan_vien
    FOREIGN KEY (id_nhan_vien) REFERENCES nhan_vien(id_nv);

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_shop_chat_message_session' AND parent_object_id = OBJECT_ID('shop_chat_message'))
ALTER TABLE shop_chat_message WITH CHECK ADD CONSTRAINT FK_shop_chat_message_session
    FOREIGN KEY (id_session) REFERENCES shop_chat_session(id_session);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_shop_chat_message_session' AND object_id = OBJECT_ID('shop_chat_message'))
CREATE INDEX IX_shop_chat_message_session ON shop_chat_message(id_session, ngay_tao, id_message);

IF OBJECT_ID('phan_hoi_san_pham', 'U') IS NULL
CREATE TABLE phan_hoi_san_pham (
    id_phan_hoi int IDENTITY(1,1) NOT NULL PRIMARY KEY,
    id_sp int NOT NULL,
    id_kh int NOT NULL,
    diem_danh_gia int NOT NULL,
    noi_dung nvarchar(1000) NULL,
    phan_hoi_quan_tri nvarchar(1000) NULL,
    trang_thai bit NOT NULL DEFAULT 1,
    ngay_tao datetime2 NOT NULL DEFAULT SYSDATETIME(),
    ngay_cap_nhat datetime2 NOT NULL DEFAULT SYSDATETIME(),
    ngay_phan_hoi datetime2 NULL,
    CONSTRAINT CK_phan_hoi_san_pham_diem CHECK (diem_danh_gia BETWEEN 1 AND 5),
    CONSTRAINT UQ_phan_hoi_san_pham_khach_hang UNIQUE (id_sp, id_kh),
    CONSTRAINT FK_phan_hoi_san_pham_san_pham FOREIGN KEY (id_sp) REFERENCES san_pham(id_sp),
    CONSTRAINT FK_phan_hoi_san_pham_khach_hang FOREIGN KEY (id_kh) REFERENCES khach_hang(id_kh)
);

IF COL_LENGTH('phan_hoi_san_pham', 'phan_hoi_quan_tri') IS NULL
ALTER TABLE phan_hoi_san_pham ADD phan_hoi_quan_tri nvarchar(1000) NULL;

IF COL_LENGTH('phan_hoi_san_pham', 'ngay_phan_hoi') IS NULL
ALTER TABLE phan_hoi_san_pham ADD ngay_phan_hoi datetime2 NULL;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_phan_hoi_san_pham_hien_thi' AND object_id = OBJECT_ID('phan_hoi_san_pham'))
CREATE INDEX IX_phan_hoi_san_pham_hien_thi ON phan_hoi_san_pham(id_sp, trang_thai, ngay_tao DESC);
