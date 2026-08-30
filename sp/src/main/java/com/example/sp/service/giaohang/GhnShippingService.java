package com.example.sp.service.giaohang;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GhnShippingService {

    private static final BigDecimal DEFAULT_FALLBACK_FEE = new BigDecimal("35000");

    private static final Pattern TOTAL_PATTERN =
            Pattern.compile("\"total\"\\s*:\\s*(\\d+)");

    private static final Pattern MESSAGE_PATTERN =
            Pattern.compile("\"message\"\\s*:\\s*\"([^\"]*)\"");

    private volatile HttpClient httpClient;

    @Value("${ghn.api.base-url}")
    private String baseUrl;

    @Value("${ghn.api.token}")
    private String token;

    @Value("${ghn.api.shop-id:0}")
    private int shopId;

    @Value("${ghn.api.from-district-id:0}")
    private int fromDistrictId;

    @Value("${ghn.api.service-type-id:2}")
    private int serviceTypeId;

    // Thực hiện xử lý nghiệp vụ của hàm provinces.
    public String provinces() {
        return get("/master-data/province");
    }

    // Thực hiện xử lý nghiệp vụ của hàm districts.
    public String districts(Integer provinceId) {
        if (provinceId == null || provinceId <= 0) {
            throw new IllegalArgumentException("Vui lòng chọn tỉnh/thành phố");
        }

        return get("/master-data/district?province_id=" + provinceId);
    }

    // Thực hiện xử lý nghiệp vụ của hàm wards.
    public String wards(Integer districtId) {
        if (districtId == null || districtId <= 0) {
            throw new IllegalArgumentException("Vui lòng chọn quận/huyện");
        }

        return get("/master-data/ward?district_id=" + districtId);
    }

    // Thực hiện xử lý nghiệp vụ của hàm calculate fee.
    public BigDecimal calculateFee(
            Integer toDistrictId,
            String toWardCode,
            BigDecimal insuranceValue
    ) {
        requireConfigured();

        if (toDistrictId == null || toDistrictId <= 0) {
            throw new IllegalArgumentException("Vui lòng chọn quận/huyện");
        }

        if (toWardCode == null || toWardCode.isBlank()) {
            throw new IllegalArgumentException("Vui lòng chọn xã/phường");
        }

        int insurance = insuranceValue == null
                ? 0
                : insuranceValue.max(BigDecimal.ZERO).intValue();

        String body = """
                {
                  "service_type_id": %d,
                  "from_district_id": %d,
                  "to_district_id": %d,
                  "to_ward_code": "%s",
                  "height": 10,
                  "length": 20,
                  "weight": 500,
                  "width": 20,
                  "insurance_value": %d
                }
                """.formatted(
                serviceTypeId,
                fromDistrictId,
                toDistrictId,
                escapeJson(toWardCode),
                insurance
        );

        String response = post("/v2/shipping-order/fee", body);

        Matcher matcher = TOTAL_PATTERN.matcher(response);
        if (!matcher.find()) {
            throw new IllegalStateException(
                    "GHN không trả về phí vận chuyển. Response: " + response
            );
        }

        return BigDecimal.valueOf(Long.parseLong(matcher.group(1)));
    }

    /**
     * The carrier quote is preferred, but a carrier outage must never block
     * checkout or invoice creation.
     */
    // Thực hiện xử lý nghiệp vụ của hàm calculate fee with fallback.
    public BigDecimal calculateFeeWithFallback(
            Integer toDistrictId,
            String toWardCode,
            BigDecimal insuranceValue,
            String provinceName,
            String wardName
    ) {
        try {
            return calculateFee(toDistrictId, toWardCode, insuranceValue);
        } catch (RuntimeException ignored) {
            return calculateFallbackFee(provinceName, wardName, insuranceValue);
        }
    }

    /**
     * Tính cước cho địa chỉ hành chính 2 cấp sau sáp nhập.
     * GHN vẫn yêu cầu mã huyện ở API fee cũ, vì vậy luồng mới dùng bảng cước theo tỉnh
     * cho tới khi nhà vận chuyển cung cấp endpoint tính phí 2 cấp ổn định.
     */
    // Thực hiện xử lý nghiệp vụ của hàm calculate two tier fee.
    public BigDecimal calculateTwoTierFee(
            String provinceName,
            String wardName,
            BigDecimal orderValue
    ) {
        return calculateFallbackFee(provinceName, wardName, orderValue);
    }

    /**
     * Local address-based fee table. It has no network or GHN configuration
     * dependency and is therefore safe as the last checkout fallback.
     */
    // Thực hiện xử lý nghiệp vụ của hàm calculate fallback fee.
    public BigDecimal calculateFallbackFee(
            String provinceName,
            String wardName,
            BigDecimal orderValue
    ) {

        BigDecimal value = orderValue == null ? BigDecimal.ZERO : orderValue.max(BigDecimal.ZERO);
        if (value.compareTo(new BigDecimal("500000")) >= 0) {
            return BigDecimal.ZERO;
        }

        String province = normalizeLocation(provinceName);
        if (province.contains("ha noi") || province.contains("ho chi minh") || province.contains("da nang")) {
            return new BigDecimal("25000");
        }
        if (province.contains("hai phong") || province.contains("can tho")
                || province.contains("binh duong") || province.contains("dong nai")) {
            return new BigDecimal("30000");
        }
        return DEFAULT_FALLBACK_FEE;
    }

    // Thực hiện xử lý nghiệp vụ của hàm normalize location.
    private String normalizeLocation(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
    }

    // Tải hoặc truy xuất dữ liệu cho get.
    private String get(String path) {
        requireToken();

        HttpRequest request = baseRequest(path)
                .GET()
                .build();

        return send(request);
    }

    // Thực hiện xử lý nghiệp vụ của hàm post.
    private String post(String path, String body) {
        requireToken();

        HttpRequest request = baseRequest(path)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        body,
                        StandardCharsets.UTF_8
                ))
                .build();

        return send(request);
    }

    // Thực hiện xử lý nghiệp vụ của hàm base request.
    private HttpRequest.Builder baseRequest(String path) {
        String cleanBaseUrl = baseUrl == null
                ? ""
                : baseUrl.trim().replaceAll("/+$", "");

        String cleanPath = path.startsWith("/")
                ? path
                : "/" + path;

        return HttpRequest.newBuilder(
                        URI.create(cleanBaseUrl + cleanPath)
                )
                .timeout(Duration.ofSeconds(30))
                .header("Token", token.trim())
                .header("Accept", "application/json")
                .header("User-Agent", "4MenStore-POS/1.0");
    }

    // Xử lý tương tác người dùng cho send.
    private String send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient().send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            String body = response.body() == null ? "" : response.body();

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalArgumentException(
                        extractMessage(
                                body,
                                "GHN trả về lỗi HTTP " + response.statusCode()
                        )
                );
            }

            if (body.contains("\"code\":401")
                    || body.contains("\"code\": 401")
                    || body.contains("\"code\":400")
                    || body.contains("\"code\": 400")) {

                throw new IllegalArgumentException(
                        extractMessage(body, "GHN trả về lỗi")
                );
            }

            return body;

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Không kết nối được tới máy chủ GHN",
                    e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Yêu cầu tới GHN bị gián đoạn",
                    e
            );
        }
    }

    // Thực hiện xử lý nghiệp vụ của hàm http client.
    private HttpClient httpClient() {
        HttpClient client = httpClient;
        if (client == null) {
            synchronized (this) {
                client = httpClient;
                if (client == null) {
                    try {
                        client = HttpClient.newBuilder()
                                .connectTimeout(Duration.ofSeconds(15))
                                .build();
                    } catch (RuntimeException e) {
                        throw new IllegalStateException(
                                "Không khởi tạo được kết nối tới GHN",
                                e
                        );
                    }
                    httpClient = client;
                }
            }
        }
        return client;
    }

    // Thực hiện xử lý nghiệp vụ của hàm require token.
    private void requireToken() {
        if (token == null
                || token.isBlank()
                || token.equals("DAN_TOKEN_GHN_VAO_DAY")) {

            throw new IllegalStateException(
                    "Chưa cấu hình ghn.api.token trong application.properties"
            );
        }
    }

    // Thực hiện xử lý nghiệp vụ của hàm require configured.
    private void requireConfigured() {
        requireToken();

        if (shopId <= 0) {
            throw new IllegalStateException(
                    "Chưa cấu hình ghn.api.shop-id"
            );
        }

        if (fromDistrictId <= 0) {
            throw new IllegalStateException(
                    "Chưa cấu hình ghn.api.from-district-id"
            );
        }
    }

    // Thực hiện xử lý nghiệp vụ của hàm extract message.
    private String extractMessage(String body, String fallback) {
        Matcher matcher = MESSAGE_PATTERN.matcher(
                body == null ? "" : body
        );

        return matcher.find()
                ? matcher.group(1)
                : fallback;
    }

    // Thực hiện xử lý nghiệp vụ của hàm escape json.
    private String escapeJson(String value) {
        return String.valueOf(value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
