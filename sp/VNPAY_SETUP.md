# Cấu hình VNPay Sandbox

Dự án dùng VNPay API `2.1.0`, HMAC-SHA512 và profile Spring `vnpay-sandbox`. Secret Key không được lưu trong repository.

## Chạy trên máy cục bộ

Mở PowerShell tại thư mục dự án và chạy:

```powershell
.\scripts\run-vnpay-sandbox.ps1
```

Nhập Hash Secret được VNPay gửi qua email khi PowerShell yêu cầu. Nội dung nhập không hiển thị và chỉ tồn tại trong tiến trình Spring hiện tại.

Với localhost, trình duyệt có thể quay về Return URL, nhưng server VNPay không thể gọi IPN vào máy của bạn.

## Kiểm thử IPN đầy đủ

Dùng một URL HTTPS công khai trỏ tới ứng dụng, sau đó chạy:

```powershell
.\scripts\run-vnpay-sandbox.ps1 -PublicBaseUrl "https://your-public-domain.example"
```

Đăng ký với VNPay địa chỉ IPN:

```text
https://your-public-domain.example/api/shop/payments/vnpay/ipn
```

Return URL được ứng dụng tự đưa vào từng giao dịch:

```text
https://your-public-domain.example/api/shop/payments/vnpay/return
```

## Giao diện VNPay trong luồng chính

Mặc định dự án dùng giao diện checkout VNPay nội bộ tại
`/api/shop/payments/vnpay/checkout` để trình diễn đầy đủ luồng đặt hàng trên localhost.
Trang này kiểm tra chữ ký HMAC của giao dịch trước khi cập nhật trạng thái đơn hàng.

Để chuyển lại sang cổng VNPay Sandbox chính thức, đặt:

```text
VNPAY_LOCAL_CHECKOUT_ENABLED=false
```

Giao diện nội bộ chỉ phục vụ phát triển/trình diễn, không sử dụng cho thanh toán thật.

## Biến môi trường tương đương

- `SPRING_PROFILES_ACTIVE=vnpay-sandbox`
- `VNPAY_ENABLED=true`
- `VNPAY_TMN_CODE`: Terminal ID Sandbox
- `VNPAY_HASH_SECRET`: Secret Key Sandbox
- `VNPAY_LOCAL_CHECKOUT_ENABLED`: `true` để dùng checkout nội bộ, `false` để dùng VNPay chính thức
- `PAYMENT_PUBLIC_BASE_URL`: URL gốc mà VNPay có thể truy cập
- `VNPAY_PAYMENT_URL`: mặc định là URL thanh toán Sandbox chính thức

Không dùng Terminal ID hoặc Secret Key Sandbox cho thanh toán thật.
