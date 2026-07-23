param(
    [string]$PublicBaseUrl = "http://localhost:8081",
    [string]$TmnCode = "L5HQXLHO"
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$secretValue = $null
$secretPointer = [IntPtr]::Zero

if ($PublicBaseUrl -notmatch '^https?://') {
    throw "PublicBaseUrl phải bắt đầu bằng http:// hoặc https://"
}

if ($PublicBaseUrl -match '^http://(localhost|127\.0\.0\.1)') {
    Write-Warning "Return URL chạy được trên máy này, nhưng VNPay không thể gọi IPN localhost. Hãy dùng URL HTTPS công khai để kiểm thử IPN."
}

$secureSecret = Read-Host "Nhập VNPay Hash Secret (nội dung sẽ không hiển thị)" -AsSecureString

try {
    $secretPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureSecret)
    $secretValue = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($secretPointer)
    if ([string]::IsNullOrWhiteSpace($secretValue)) {
        throw "VNPay Hash Secret không được để trống"
    }

    $env:SPRING_PROFILES_ACTIVE = "vnpay-sandbox"
    $env:VNPAY_ENABLED = "true"
    $env:VNPAY_TMN_CODE = $TmnCode
    $env:VNPAY_HASH_SECRET = $secretValue
    $env:PAYMENT_PUBLIC_BASE_URL = $PublicBaseUrl.TrimEnd('/')
    $env:VNPAY_RECONCILE_ON_RETURN = if ($PublicBaseUrl -match '^http://(localhost|127\.0\.0\.1)') { "true" } else { "false" }

    Write-Host "VNPay Sandbox đã bật."
    Write-Host "Return URL: $($env:PAYMENT_PUBLIC_BASE_URL)/api/shop/payments/vnpay/return"
    Write-Host "IPN URL:    $($env:PAYMENT_PUBLIC_BASE_URL)/api/shop/payments/vnpay/ipn"

    Push-Location $projectRoot
    try {
        & ".\mvnw.cmd" spring-boot:run
    } finally {
        Pop-Location
    }
} finally {
    $env:VNPAY_HASH_SECRET = $null
    $env:VNPAY_RECONCILE_ON_RETURN = $null
    $secretValue = $null
    if ($secretPointer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($secretPointer)
    }
}
