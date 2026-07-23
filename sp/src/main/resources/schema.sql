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
    ngay_dong datetime2 NULL
);

IF OBJECT_ID('shop_chat_message', 'U') IS NULL
CREATE TABLE shop_chat_message (
    id_message int IDENTITY(1,1) NOT NULL PRIMARY KEY,
    id_session int NOT NULL,
    sender_type varchar(20) NOT NULL,
    sender_name nvarchar(255) NULL,
    noi_dung nvarchar(1200) NOT NULL,
    ai_generated bit NOT NULL DEFAULT 0,
    ngay_tao datetime2 NOT NULL DEFAULT SYSDATETIME()
);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_shop_chat_message_session' AND object_id = OBJECT_ID('shop_chat_message'))
CREATE INDEX IX_shop_chat_message_session ON shop_chat_message(id_session, ngay_tao, id_message);
