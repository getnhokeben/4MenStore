package com.example.sp.service.cuahang;

import com.example.sp.service.tienich.MoneyRoundingUtil;
import com.example.sp.dto.cuahang.ShopLookupDTO;
import com.example.sp.dto.cuahang.ShopProductDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class ShopChatbotAdvisor {

    private static final List<String> SIZE_ORDER = List.of("S", "M", "L", "XL", "XXL");
    private static final int[] SIZE_HEIGHTS = {150, 160, 165, 170, 175};
    private static final int[] SIZE_WEIGHTS = {50, 60, 65, 70, 76};
    private static final Pattern HEIGHT_M_CM = Pattern.compile("(?<!\\d)([12])\\s*m\\s*(\\d{1,2})(?!\\d)", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEIGHT_DECIMAL_M = Pattern.compile("(?<!\\d)(1(?:[.,]\\d{1,2}))\\s*m(?:et)?(?!\\w)", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEIGHT_CM = Pattern.compile("(?<!\\d)(1[4-9]\\d|20\\d|210)\\s*(?:cm|centimet)(?!\\w)", Pattern.CASE_INSENSITIVE);
    private static final Pattern WEIGHT_KG = Pattern.compile("(?<!\\d)(4\\d|[5-9]\\d|1[0-3]\\d|140)\\s*(?:kg|ki-?lo|kilogram)(?!\\w)", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXPLICIT_SIZE = Pattern.compile("\\b(?:size|co|mac)\\s*(xxl|2xl|xl|l|m|s)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRICE_K = Pattern.compile("(?<!\\d)(\\d{2,4})\\s*(?:k|nghin|ngan)(?!\\w)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRICE_MILLION = Pattern.compile("(?<!\\d)(\\d+(?:[.,]\\d+)?)\\s*(?:tr|trieu)(?!\\w)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRICE_VND = Pattern.compile("(?<!\\d)(\\d{1,3}(?:[.,]\\d{3})+|\\d{5,9})\\s*(?:d|dong|vnd)(?!\\w)", Pattern.CASE_INSENSITIVE);
    private static final Pattern RANGE_K = Pattern.compile("(?<!\\d)(\\d{2,4})\\s*(?:k|nghin|ngan)\\s*(?:-|den|toi)\\s*(\\d{2,4})\\s*(?:k|nghin|ngan)", Pattern.CASE_INSENSITIVE);
    private static final Pattern RANGE_MILLION = Pattern.compile("(?<!\\d)(\\d+(?:[.,]\\d+)?)\\s*(?:tr|trieu)\\s*(?:-|den|toi)\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:tr|trieu)", Pattern.CASE_INSENSITIVE);
    private static final Set<String> STOP_WORDS = Set.of(
            "minh", "toi", "em", "anh", "chi", "ban", "shop", "muon", "can", "tim", "cho", "voi",
            "mot", "nhung", "cac", "san", "pham", "ao", "mac", "hop", "phu", "giup", "duoc", "khong",
            "nhe", "nha", "nao", "co", "con", "hang", "gia", "khoang", "tam", "duoi", "tren", "mau",
            "size", "dang", "loai", "lay", "thich", "xem", "giai", "thieu"
    );

    // Thực hiện xử lý nghiệp vụ của hàm advise.
    public Advice advise(String latestMessage, String userContext, List<ShopProductDTO> catalog) {
        String latest = normalize(latestMessage);
        String context = normalize(userContext);
        CustomerProfile profile = extractProfile(latest, context, catalog);
        boolean productIntent = profile.hasShoppingCriteria() || containsAny(latest,
                "goi y", "tu van", "tim", "mua", "san pham", "polo", "so mi", "ao thun", "hoodie",
                "sweater", "khoac", "phoi do", "mau nao", "re hon", "re nhat", "so sanh", "con hang");
        boolean sizeIntent = containsAny(context, "size", "chieu cao", "can nang", "cao ", "nang ", "mac co");
        List<RankedProduct> rankedProducts = productIntent ? rankProducts(latest, profile, catalog) : List.of();
        boolean needsClarification = (sizeIntent && profile.explicitSizes().isEmpty()
                && (profile.heightCm() == null || profile.weightKg() == null))
                || (productIntent && rankedProducts.isEmpty());
        return new Advice(profile, rankedProducts, productIntent, needsClarification);
    }

    // Thực hiện xử lý nghiệp vụ của hàm recommend sizes.
    public List<String> recommendSizes(String text) {
        Integer height = extractHeight(text);
        Integer weight = extractWeight(text);
        if (height == null || weight == null) return List.of();

        int heightRank = chartRank(height, SIZE_HEIGHTS);
        int weightRank = chartRank(weight, SIZE_WEIGHTS);
        int from = Math.min(heightRank, weightRank);
        int to = Math.max(heightRank, weightRank);
        if (from == to) {
            LinkedHashSet<String> result = new LinkedHashSet<>();
            result.add(SIZE_ORDER.get(to));
            result.add(SIZE_ORDER.get(Math.min(SIZE_ORDER.size() - 1, to + 1)));
            return List.copyOf(result);
        }
        if (to - from == 1) return List.copyOf(SIZE_ORDER.subList(from, to + 1));
        return List.copyOf(SIZE_ORDER.subList(Math.max(0, to - 1), to + 1));
    }

    // Thực hiện xử lý nghiệp vụ của hàm summarize.
    public String summarize(CustomerProfile profile) {
        List<String> parts = new ArrayList<>();
        if (profile.heightCm() != null) parts.add("Cao " + profile.heightCm() + " cm");
        if (profile.weightKg() != null) parts.add(profile.weightKg() + " kg");
        if (!profile.sizes().isEmpty()) parts.add("Size " + String.join("–", profile.sizes()));
        if (!profile.categories().isEmpty()) parts.add(String.join(", ", profile.categories()));
        if (!profile.colors().isEmpty()) parts.add("Màu " + String.join(", ", profile.colors()));
        if (!profile.excludedColors().isEmpty()) parts.add("Không " + String.join(", ", profile.excludedColors()));
        if (!profile.materials().isEmpty()) parts.add("Chất liệu " + String.join(", ", profile.materials()));
        if (profile.minPrice() != null && profile.maxPrice() != null) {
            parts.add(formatMoney(profile.minPrice()) + "–" + formatMoney(profile.maxPrice()));
        } else if (profile.maxPrice() != null) {
            parts.add("Tối đa " + formatMoney(profile.maxPrice()));
        } else if (profile.minPrice() != null) {
            parts.add("Từ " + formatMoney(profile.minPrice()));
        }
        if (profile.fitPreference() != null) parts.add("Mặc " + profile.fitPreference());
        if (profile.occasion() != null) parts.add(profile.occasion());
        return String.join(" · ", parts);
    }

    // Thực hiện xử lý nghiệp vụ của hàm extract profile.
    private CustomerProfile extractProfile(String latest, String context, List<ShopProductDTO> catalog) {
        Integer height = extractHeight(context);
        Integer weight = extractWeight(context);
        List<String> latestExplicitSizes = extractExplicitSizes(latest);
        List<String> explicitSizes = latestExplicitSizes.isEmpty() ? extractExplicitSizes(context) : latestExplicitSizes;
        List<String> calculatedSizes = recommendSizes(context);
        List<String> sizes = explicitSizes.isEmpty() ? calculatedSizes : explicitSizes;

        List<String> availableColors = collectValues(catalog, this::productColors);
        List<String> latestExcludedColors = availableColors.stream().filter(value -> isExcluded(latest, value)).toList();
        List<String> excludedColors = latestExcludedColors.isEmpty()
                ? availableColors.stream().filter(value -> isExcluded(context, value)).toList()
                : latestExcludedColors;
        List<String> latestColors = mentionedValues(latest, availableColors);
        if (!latestColors.isEmpty() && latestExcludedColors.isEmpty()) {
            excludedColors = excludedColors.stream().filter(value -> !latestColors.contains(value)).toList();
        }
        List<String> activeExcludedColors = excludedColors;
        List<String> colors = (latestColors.isEmpty() ? mentionedValues(context, availableColors) : latestColors).stream()
                .filter(value -> !activeExcludedColors.contains(value))
                .toList();
        if (containsAny(latest, "mau nao cung duoc", "bo loc mau", "khong gioi han mau")) {
            colors = List.of();
            excludedColors = List.of();
        }
        List<String> categories = latestOrRememberedValues(latest, context, collectValues(catalog, this::productCategories));
        List<String> materials = latestOrRememberedValues(latest, context, collectValues(catalog, this::productMaterials));
        List<String> styles = latestOrRememberedValues(latest, context, collectValues(catalog, this::productStyles));
        List<String> fits = latestOrRememberedValues(latest, context, collectValues(catalog, this::productFits));
        PriceBounds latestPriceBounds = extractPriceBounds(latest);
        PriceBounds priceBounds = latestPriceBounds.hasValue() ? latestPriceBounds : extractPriceBounds(context);
        if (containsAny(latest, "bo gioi han gia", "khong gioi han gia", "gia nao cung duoc")) {
            priceBounds = new PriceBounds(null, null);
        }

        return new CustomerProfile(
                height,
                weight,
                sizes,
                explicitSizes,
                colors,
                excludedColors,
                categories,
                materials,
                styles,
                fits,
                priceBounds.min(),
                priceBounds.max(),
                firstNonNull(detectFitPreference(latest), detectFitPreference(context)),
                firstNonNull(detectOccasion(latest), detectOccasion(context)),
                detectSortPreference(latest, context)
        );
    }

    // Thực hiện xử lý nghiệp vụ của hàm latest or remembered values.
    private List<String> latestOrRememberedValues(String latest, String context, List<String> available) {
        List<String> latestValues = mentionedValues(latest, available);
        return latestValues.isEmpty() ? mentionedValues(context, available) : latestValues;
    }

    // Thực hiện xử lý nghiệp vụ của hàm first non null.
    private String firstNonNull(String preferred, String fallback) {
        return preferred == null ? fallback : preferred;
    }

    // Thực hiện xử lý nghiệp vụ của hàm rank products.
    private List<RankedProduct> rankProducts(String latest, CustomerProfile profile, List<ShopProductDTO> catalog) {
        if (catalog == null || catalog.isEmpty()) return List.of();
        List<String> terms = significantTerms(latest);
        List<RankedProduct> ranked = new ArrayList<>();

        for (ShopProductDTO product : catalog) {
            BigDecimal price = money(product.getGiaBanMin());
            if (profile.maxPrice() != null && price.compareTo(profile.maxPrice()) > 0) continue;
            if (profile.minPrice() != null && money(product.getGiaBanMax()).compareTo(profile.minPrice()) < 0) continue;

            List<String> productSizes = productSizes(product).stream().map(this::normalizeSize).toList();
            List<String> productColors = productColors(product);
            List<String> productCategories = productCategories(product);
            List<String> productMaterials = productMaterials(product);
            List<String> productStyles = productStyles(product);
            List<String> productFits = productFits(product);

            if (!profile.sizes().isEmpty() && productSizes.stream().noneMatch(profile.sizes()::contains)) continue;
            if (!profile.colors().isEmpty() && !overlaps(productColors, profile.colors())) continue;
            if (!profile.categories().isEmpty() && !overlaps(productCategories, profile.categories())) continue;
            if (!profile.materials().isEmpty() && !overlaps(productMaterials, profile.materials())) continue;
            if (!profile.styles().isEmpty() && !overlaps(productStyles, profile.styles())) continue;
            if (!profile.fits().isEmpty() && !overlaps(productFits, profile.fits())) continue;
            if (!profile.excludedColors().isEmpty() && !productColors.isEmpty()
                    && productColors.stream().allMatch(profile.excludedColors()::contains)) continue;

            int score = 30;
            List<String> reasons = new ArrayList<>();
            if (!profile.sizes().isEmpty()) {
                String matchedSize = profile.sizes().stream().filter(productSizes::contains).findFirst().orElse(profile.sizes().get(0));
                score += 22;
                reasons.add("Có size " + matchedSize);
            }
            if (!profile.colors().isEmpty()) {
                String color = profile.colors().stream().filter(productColors::contains).findFirst().orElse(profile.colors().get(0));
                score += 18;
                reasons.add("Có màu " + color);
            }
            if (!profile.categories().isEmpty()) {
                score += 18;
                reasons.add("Đúng loại " + profile.categories().get(0));
            }
            if (profile.maxPrice() != null || profile.minPrice() != null) {
                score += 14;
                reasons.add("Trong ngân sách");
            }
            if (!profile.materials().isEmpty()) {
                score += 12;
                reasons.add("Đúng chất liệu " + profile.materials().get(0));
            }
            if (!profile.styles().isEmpty() || !profile.fits().isEmpty()) {
                score += 10;
                reasons.add("Đúng phong cách đã chọn");
            }
            String haystack = productSearchText(product);
            if (profile.occasion() != null && occasionMatches(profile.occasion(), haystack)) {
                score += 10;
                reasons.add("Hợp " + profile.occasion().toLowerCase(Locale.ROOT));
            }
            if (profile.fitPreference() != null && fitMatches(profile.fitPreference(), haystack)) {
                score += 8;
                reasons.add("Form " + profile.fitPreference());
            }
            for (String term : terms) {
                if (haystack.contains(term)) score += term.length() >= 5 ? 4 : 2;
            }
            if (Boolean.TRUE.equals(product.getDangGiamGia())) {
                score += 4;
                reasons.add("Đang giảm giá");
            }
            score += Math.min(5, Math.max(0, product.getTongTon() == null ? 0 : product.getTongTon()) / 5);
            if (reasons.isEmpty()) reasons.add("Còn hàng và có nhiều lựa chọn");

            String preferredSize = choosePreferredSize(profile);
            String preferredColor = profile.colors().isEmpty() ? null : profile.colors().get(0);
            ranked.add(new RankedProduct(product, Math.min(99, score), reasons.stream().distinct().limit(3).toList(), preferredSize, preferredColor));
        }

        Comparator<RankedProduct> comparator = switch (profile.sortPreference()) {
            case "price_asc" -> Comparator.comparing(item -> money(item.product().getGiaBanMin()));
            case "price_desc" -> Comparator.comparing((RankedProduct item) -> money(item.product().getGiaBanMax())).reversed();
            case "newest" -> Comparator.comparing((RankedProduct item) -> item.product().getNgayTao(), Comparator.nullsLast(Comparator.reverseOrder()));
            case "discount" -> Comparator.comparing((RankedProduct item) -> Boolean.TRUE.equals(item.product().getDangGiamGia())).reversed()
                    .thenComparing(Comparator.comparingInt(RankedProduct::matchScore).reversed());
            default -> Comparator.comparingInt(RankedProduct::matchScore).reversed()
                    .thenComparing(item -> item.product().getNgayTao(), Comparator.nullsLast(Comparator.reverseOrder()));
        };
        return ranked.stream().sorted(comparator).limit(14).toList();
    }

    // Thực hiện xử lý nghiệp vụ của hàm choose preferred size.
    private String choosePreferredSize(CustomerProfile profile) {
        if (profile.sizes().isEmpty()) return null;
        if ("rộng".equals(profile.fitPreference())) return profile.sizes().get(profile.sizes().size() - 1);
        return profile.sizes().get(0);
    }

    // Thực hiện xử lý nghiệp vụ của hàm extract price bounds.
    private PriceBounds extractPriceBounds(String context) {
        Matcher rangeK = RANGE_K.matcher(context);
        if (rangeK.find()) return new PriceBounds(thousands(rangeK.group(1)), thousands(rangeK.group(2)));
        Matcher rangeMillion = RANGE_MILLION.matcher(context);
        if (rangeMillion.find()) return new PriceBounds(millions(rangeMillion.group(1)), millions(rangeMillion.group(2)));

        BigDecimal amount = firstPrice(context);
        if (amount == null) return new PriceBounds(null, null);
        if (containsAny(context, "tren", "tu ", "it nhat", "toi thieu")) return new PriceBounds(amount, null);
        if (containsAny(context, "duoi", "toi da", "khong qua", "tam", "khoang", "ngan sach", "gia")) {
            return new PriceBounds(null, amount);
        }
        return new PriceBounds(null, null);
    }

    // Thực hiện xử lý nghiệp vụ của hàm first price.
    private BigDecimal firstPrice(String text) {
        Matcher thousands = PRICE_K.matcher(text);
        if (thousands.find()) return thousands(thousands.group(1));
        Matcher million = PRICE_MILLION.matcher(text);
        if (million.find()) return millions(million.group(1));
        Matcher vnd = PRICE_VND.matcher(text);
        if (vnd.find()) return new BigDecimal(vnd.group(1).replace(".", "").replace(",", ""));
        return null;
    }

    // Thực hiện xử lý nghiệp vụ của hàm thousands.
    private BigDecimal thousands(String value) {
        return new BigDecimal(value).multiply(new BigDecimal("1000"));
    }

    // Thực hiện xử lý nghiệp vụ của hàm millions.
    private BigDecimal millions(String value) {
        return new BigDecimal(value.replace(',', '.')).multiply(new BigDecimal("1000000"));
    }

    // Thực hiện xử lý nghiệp vụ của hàm extract explicit sizes.
    private List<String> extractExplicitSizes(String context) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        Matcher matcher = EXPLICIT_SIZE.matcher(context);
        while (matcher.find()) values.add(normalizeSize(matcher.group(1)));
        return List.copyOf(values);
    }

    // Thực hiện xử lý nghiệp vụ của hàm detect fit preference.
    private String detectFitPreference(String context) {
        if (containsAny(context, "oversize", "rong", "thoai mai")) return "rộng";
        if (containsAny(context, "om", "body", "slim", "gon")) return "ôm";
        if (containsAny(context, "vua nguoi", "regular")) return "vừa người";
        return null;
    }

    // Thực hiện xử lý nghiệp vụ của hàm detect occasion.
    private String detectOccasion(String context) {
        if (containsAny(context, "di lam", "cong so", "hop hanh", "meeting")) return "Đi làm";
        if (containsAny(context, "du tiec", "tiec cuoi", "su kien")) return "Dự tiệc";
        if (containsAny(context, "the thao", "tap gym", "van dong")) return "Thể thao";
        if (containsAny(context, "hen ho", "di choi", "dao pho")) return "Đi chơi";
        return null;
    }

    // Thực hiện xử lý nghiệp vụ của hàm detect sort preference.
    private String detectSortPreference(String latest, String context) {
        String source = latest + " " + context;
        if (containsAny(latest, "re hon", "re nhat", "gia thap", "tiet kiem")) return "price_asc";
        if (containsAny(latest, "dat hon", "cao cap", "gia cao")) return "price_desc";
        if (containsAny(latest, "moi nhat", "mau moi")) return "newest";
        if (containsAny(latest, "giam gia", "khuyen mai", "sale")) return "discount";
        if (containsAny(source, "re nhat")) return "price_asc";
        return "relevance";
    }

    // Thực hiện xử lý nghiệp vụ của hàm occasion matches.
    private boolean occasionMatches(String occasion, String haystack) {
        return switch (occasion) {
            case "Đi làm" -> containsAny(haystack, "so mi", "cong so", "thanh lich", "polo");
            case "Dự tiệc" -> containsAny(haystack, "thanh lich", "sang trong", "so mi");
            case "Thể thao" -> containsAny(haystack, "the thao", "nang dong", "thun", "co gian");
            case "Đi chơi" -> containsAny(haystack, "casual", "nang dong", "street", "polo", "thun");
            default -> false;
        };
    }

    // Thực hiện xử lý nghiệp vụ của hàm fit matches.
    private boolean fitMatches(String preference, String haystack) {
        return switch (preference) {
            case "rộng" -> containsAny(haystack, "oversize", "rong", "relaxed");
            case "ôm" -> containsAny(haystack, "slim", "body", "om");
            case "vừa người" -> containsAny(haystack, "regular", "vua");
            default -> false;
        };
    }

    // Thực hiện xử lý nghiệp vụ của hàm collect values.
    private List<String> collectValues(List<ShopProductDTO> products, Function<ShopProductDTO, List<String>> extractor) {
        if (products == null) return List.of();
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (ShopProductDTO product : products) {
            extractor.apply(product).stream().filter(value -> value != null && !value.isBlank()).forEach(values::add);
        }
        return List.copyOf(values);
    }

    // Thực hiện xử lý nghiệp vụ của hàm mentioned values.
    private List<String> mentionedValues(String context, List<String> available) {
        return available.stream().filter(value -> mentionsValue(context, value)).toList();
    }

    // Thực hiện xử lý nghiệp vụ của hàm mentions value.
    private boolean mentionsValue(String context, String value) {
        String normalizedValue = normalize(value);
        if (normalizedValue.isBlank()) return false;
        if (context.contains(normalizedValue)) return true;
        return normalizedValue.startsWith("ao ") && context.contains(normalizedValue.substring(3));
    }

    // Kiểm tra điều kiện và tính hợp lệ cho is excluded.
    private boolean isExcluded(String context, String value) {
        String color = normalize(value);
        return containsAny(context,
                "khong mau " + color,
                "khong lay " + color,
                "khong lay mau " + color,
                "khong thich " + color,
                "khong thich mau " + color,
                "tru " + color,
                "bo " + color);
    }

    // Thực hiện xử lý nghiệp vụ của hàm overlaps.
    private boolean overlaps(List<String> left, List<String> right) {
        Set<String> normalizedRight = right.stream().map(this::normalize).collect(Collectors.toSet());
        return left.stream().map(this::normalize).anyMatch(normalizedRight::contains);
    }

    // Thực hiện xử lý nghiệp vụ của hàm product sizes.
    private List<String> productSizes(ShopProductDTO product) {
        return lookupNames(product.getKichCos());
    }

    // Thực hiện xử lý nghiệp vụ của hàm product colors.
    private List<String> productColors(ShopProductDTO product) {
        return lookupNames(product.getMauSacs());
    }

    // Thực hiện xử lý nghiệp vụ của hàm product categories.
    private List<String> productCategories(ShopProductDTO product) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (product.getLoaiAo() != null && !product.getLoaiAo().isBlank()) values.add(product.getLoaiAo());
        values.addAll(lookupNames(product.getLoaiAos()));
        return List.copyOf(values);
    }

    // Thực hiện xử lý nghiệp vụ của hàm product materials.
    private List<String> productMaterials(ShopProductDTO product) {
        return product.getChatLieu() == null || product.getChatLieu().getTen() == null
                ? List.of()
                : List.of(product.getChatLieu().getTen());
    }

    // Thực hiện xử lý nghiệp vụ của hàm product styles.
    private List<String> productStyles(ShopProductDTO product) {
        return lookupNames(product.getPhongCachMacs());
    }

    // Thực hiện xử lý nghiệp vụ của hàm product fits.
    private List<String> productFits(ShopProductDTO product) {
        return lookupNames(product.getKieuDangs());
    }

    // Thực hiện xử lý nghiệp vụ của hàm lookup names.
    private List<String> lookupNames(List<ShopLookupDTO> values) {
        if (values == null) return List.of();
        return values.stream().map(ShopLookupDTO::getTen).filter(value -> value != null && !value.isBlank()).distinct().toList();
    }

    // Thực hiện xử lý nghiệp vụ của hàm product search text.
    private String productSearchText(ShopProductDTO product) {
        List<String> values = new ArrayList<>();
        values.add(product.getTenSp());
        values.add(product.getMoTa());
        values.addAll(productCategories(product));
        values.addAll(productMaterials(product));
        values.addAll(productColors(product));
        values.addAll(productSizes(product));
        values.addAll(productStyles(product));
        values.addAll(productFits(product));
        return normalize(values.stream().filter(value -> value != null && !value.isBlank()).collect(Collectors.joining(" ")));
    }

    // Thực hiện xử lý nghiệp vụ của hàm significant terms.
    private List<String> significantTerms(String normalized) {
        return Pattern.compile("[^a-z0-9]+")
                .splitAsStream(normalized)
                .filter(term -> term.length() >= 3)
                .filter(term -> !STOP_WORDS.contains(term))
                .distinct()
                .limit(12)
                .toList();
    }

    // Thực hiện xử lý nghiệp vụ của hàm extract height.
    private Integer extractHeight(String text) {
        if (text == null) return null;
        Integer result = null;
        Matcher meters = HEIGHT_M_CM.matcher(text);
        while (meters.find()) result = Integer.parseInt(meters.group(1)) * 100 + Integer.parseInt(meters.group(2));
        Matcher decimal = HEIGHT_DECIMAL_M.matcher(text);
        while (decimal.find()) result = (int) Math.round(Double.parseDouble(decimal.group(1).replace(',', '.')) * 100);
        Matcher centimeters = HEIGHT_CM.matcher(text);
        while (centimeters.find()) result = Integer.parseInt(centimeters.group(1));
        return result != null && result >= 140 && result <= 210 ? result : null;
    }

    // Thực hiện xử lý nghiệp vụ của hàm extract weight.
    private Integer extractWeight(String text) {
        if (text == null) return null;
        Integer result = null;
        Matcher matcher = WEIGHT_KG.matcher(text);
        while (matcher.find()) result = Integer.parseInt(matcher.group(1));
        return result;
    }

    // Thực hiện xử lý nghiệp vụ của hàm chart rank.
    private int chartRank(int value, int[] thresholds) {
        int rank = 0;
        for (int index = 0; index < thresholds.length; index++) if (value >= thresholds[index]) rank = index;
        return Math.max(0, Math.min(SIZE_ORDER.size() - 1, rank));
    }

    // Thực hiện xử lý nghiệp vụ của hàm normalize size.
    private String normalizeSize(String value) {
        if (value == null) return "";
        String size = value.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
        return switch (size) {
            case "SMALL" -> "S";
            case "MEDIUM" -> "M";
            case "LARGE" -> "L";
            case "1XL", "EXTRALARGE" -> "XL";
            case "2XL" -> "XXL";
            default -> size;
        };
    }

    // Thực hiện xử lý nghiệp vụ của hàm format money.
    private String formatMoney(BigDecimal value) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        format.setMaximumFractionDigits(0);
        return format.format(MoneyRoundingUtil.roundNonNegative(value)) + "đ";
    }

    // Thực hiện xử lý nghiệp vụ của hàm money.
    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.max(BigDecimal.ZERO);
    }

    // Thực hiện xử lý nghiệp vụ của hàm contains any.
    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) if (value.contains(normalize(needle))) return true;
        return false;
    }

    // Thực hiện xử lý nghiệp vụ của hàm normalize.
    private String normalize(String value) {
        if (value == null) return "";
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD).replace('đ', 'd').replace('Đ', 'D');
        return decomposed.replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    // Thực hiện xử lý nghiệp vụ của hàm advice.
    public record Advice(
            CustomerProfile profile,
            List<RankedProduct> rankedProducts,
            boolean productIntent,
            boolean needsClarification
    ) {
    }

    // Thực hiện xử lý nghiệp vụ của hàm customer profile.
    public record CustomerProfile(
            Integer heightCm,
            Integer weightKg,
            List<String> sizes,
            List<String> explicitSizes,
            List<String> colors,
            List<String> excludedColors,
            List<String> categories,
            List<String> materials,
            List<String> styles,
            List<String> fits,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String fitPreference,
            String occasion,
            String sortPreference
    ) {
        // Kiểm tra điều kiện và tính hợp lệ cho has shopping criteria.
        public boolean hasShoppingCriteria() {
            return !sizes.isEmpty() || !colors.isEmpty() || !excludedColors.isEmpty() || !categories.isEmpty()
                    || !materials.isEmpty() || !styles.isEmpty() || !fits.isEmpty() || minPrice != null || maxPrice != null
                    || fitPreference != null || occasion != null;
        }
    }

    // Thực hiện xử lý nghiệp vụ của hàm ranked product.
    public record RankedProduct(
            ShopProductDTO product,
            int matchScore,
            List<String> reasons,
            String preferredSize,
            String preferredColor
    ) {
    }

    // Thực hiện xử lý nghiệp vụ của hàm price bounds.
    private record PriceBounds(BigDecimal min, BigDecimal max) {
        // Kiểm tra điều kiện và tính hợp lệ cho has value.
        private boolean hasValue() {
            return min != null || max != null;
        }
    }
}
