package com.example.sp.service.tienich;

import java.util.List;

/**
 * A small, dependency-free template for transactional email.  It deliberately
 * uses tables and inline styles because those are the most consistently
 * supported layout primitives in email clients.
 */
public final class TransactionalEmailTemplate {

    private static final String BRAND = "4MenStore";

    // Thực hiện xử lý nghiệp vụ của hàm transactional email template.
    private TransactionalEmailTemplate() {
    }

    // Thực hiện xử lý nghiệp vụ của hàm compose.
    public static MailContent compose(
            String preheader,
            String label,
            String title,
            String greeting,
            String introduction,
            List<Section> sections,
            List<LineItem> items,
            String actionLabel,
            String actionUrl,
            String note
    ) {
        String safePreheader = value(preheader);
        String safeLabel = value(label);
        String safeTitle = value(title);
        String safeGreeting = value(greeting);
        String safeIntroduction = value(introduction);
        String safeNote = value(note);
        List<Section> safeSections = sections == null ? List.of() : sections;
        List<LineItem> safeItems = items == null ? List.of() : items;

        StringBuilder html = new StringBuilder(4_000);
        html.append("<!doctype html><html lang=\"vi\"><head><meta charset=\"UTF-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
                .append("</head><body style=\"margin:0;padding:0;background:#f4f5f7;color:#172033;font-family:Arial,Helvetica,sans-serif;\">")
                .append("<div style=\"display:none;max-height:0;overflow:hidden;opacity:0;color:transparent;\">")
                .append(escape(safePreheader)).append("</div>")
                .append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"background:#f4f5f7;\"><tr><td align=\"center\" style=\"padding:32px 16px;\">")
                .append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"max-width:620px;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 8px 24px rgba(15,23,42,.08);\">")
                .append("<tr><td style=\"padding:25px 36px;background:#101827;color:#ffffff;\">")
                .append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\"><tr>")
                .append("<td style=\"font-size:22px;line-height:26px;font-weight:700;letter-spacing:.2px;\">").append(BRAND).append("</td>")
                .append("<td align=\"right\" style=\"font-size:11px;line-height:16px;letter-spacing:1.4px;text-transform:uppercase;color:#d9b77a;\">THÔNG BÁO DỊCH VỤ</td>")
                .append("</tr></table></td></tr>")
                .append("<tr><td style=\"padding:36px 36px 12px;\">");

        if (!safeLabel.isBlank()) {
            html.append("<p style=\"margin:0 0 10px;color:#99713d;font-size:12px;font-weight:700;letter-spacing:1.1px;text-transform:uppercase;\">")
                    .append(escape(safeLabel)).append("</p>");
        }
        html.append("<h1 style=\"margin:0 0 16px;color:#101827;font-size:28px;line-height:36px;font-weight:700;\">")
                .append(escape(safeTitle)).append("</h1>")
                .append("<p style=\"margin:0 0 10px;color:#27344a;font-size:16px;line-height:24px;\">")
                .append(escape(safeGreeting)).append("</p>")
                .append("<p style=\"margin:0 0 24px;color:#526077;font-size:15px;line-height:23px;\">")
                .append(escape(safeIntroduction)).append("</p>");

        for (Section section : safeSections) {
            appendSectionHtml(html, section);
        }
        if (!safeItems.isEmpty()) {
            appendItemsHtml(html, safeItems);
        }
        if (isHttpUrl(actionUrl) && !value(actionLabel).isBlank()) {
            html.append("<table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"margin:26px 0 24px;\"><tr><td style=\"border-radius:8px;background:#101827;\">")
                    .append("<a href=\"").append(escapeAttribute(actionUrl)).append("\" style=\"display:inline-block;padding:13px 22px;color:#ffffff;font-size:14px;font-weight:700;line-height:20px;text-decoration:none;\">")
                    .append(escape(actionLabel)).append("</a></td></tr></table>");
        }
        if (!safeNote.isBlank()) {
            html.append("<p style=\"margin:0 0 24px;padding:14px 16px;border-left:3px solid #d9b77a;background:#fbf8f2;color:#5f523e;font-size:13px;line-height:20px;\">")
                    .append(escape(safeNote)).append("</p>");
        }
        html.append("</td></tr>")
                .append("<tr><td style=\"padding:22px 36px;background:#f8fafc;border-top:1px solid #e7ebf0;color:#718096;font-size:12px;line-height:19px;\">")
                .append("Email này được gửi liên quan đến tài khoản hoặc đơn hàng của bạn tại ").append(BRAND).append(".<br>")
                .append("Nếu cần hỗ trợ, bạn có thể phản hồi trực tiếp email này.")
                .append("</td></tr></table></td></tr></table></body></html>");

        return new MailContent(html.toString(), text(
                safeGreeting,
                safeIntroduction,
                safeSections,
                safeItems,
                actionLabel,
                actionUrl,
                safeNote
        ));
    }

    // Thực hiện xử lý nghiệp vụ của hàm append section html.
    private static void appendSectionHtml(StringBuilder html, Section section) {
        if (section == null || section.details() == null || section.details().isEmpty()) {
            return;
        }
        html.append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"margin:0 0 18px;border:1px solid #e6eaf0;border-radius:10px;\">");
        if (!value(section.title()).isBlank()) {
            html.append("<tr><td colspan=\"2\" style=\"padding:13px 16px 8px;color:#344158;font-size:12px;font-weight:700;letter-spacing:.6px;text-transform:uppercase;\">")
                    .append(escape(section.title())).append("</td></tr>");
        }
        for (Detail detail : section.details()) {
            if (detail == null) {
                continue;
            }
            String valueStyle = detail.emphasized()
                    ? "color:#101827;font-size:15px;font-weight:700;line-height:21px;"
                    : "color:#27344a;font-size:14px;font-weight:600;line-height:20px;";
            html.append("<tr><td valign=\"top\" style=\"width:38%;padding:8px 16px;color:#718096;font-size:13px;line-height:20px;\">")
                    .append(escape(detail.label())).append("</td><td valign=\"top\" style=\"padding:8px 16px 8px 4px;")
                    .append(valueStyle).append("\">").append(escape(detail.value())).append("</td></tr>");
        }
        html.append("<tr><td colspan=\"2\" style=\"height:6px;line-height:6px;\">&nbsp;</td></tr></table>");
    }

    // Thực hiện xử lý nghiệp vụ của hàm append items html.
    private static void appendItemsHtml(StringBuilder html, List<LineItem> items) {
        html.append("<p style=\"margin:0 0 9px;color:#344158;font-size:12px;font-weight:700;letter-spacing:.6px;text-transform:uppercase;\">Sản phẩm</p>")
                .append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"margin:0 0 18px;border-top:1px solid #e6eaf0;\">");
        for (LineItem item : items) {
            if (item == null) {
                continue;
            }
            String description = value(item.description());
            html.append("<tr><td valign=\"top\" style=\"padding:14px 0;border-bottom:1px solid #e6eaf0;\">")
                    .append("<p style=\"margin:0 0 4px;color:#172033;font-size:14px;font-weight:700;line-height:20px;\">").append(escape(item.name())).append("</p>")
                    .append("<p style=\"margin:0;color:#718096;font-size:12px;line-height:18px;\">")
                    .append(description.isBlank() ? "" : escape(description) + " · ")
                    .append(escape(item.quantity())).append("</p>")
                    .append("</td><td valign=\"top\" align=\"right\" style=\"padding:14px 0 14px 12px;border-bottom:1px solid #e6eaf0;color:#172033;font-size:14px;font-weight:700;line-height:20px;white-space:nowrap;\">")
                    .append(escape(item.amount())).append("</td></tr>");
        }
        html.append("</table>");
    }

    // Thực hiện xử lý nghiệp vụ của hàm text.
    private static String text(
            String greeting,
            String introduction,
            List<Section> sections,
            List<LineItem> items,
            String actionLabel,
            String actionUrl,
            String note
    ) {
        StringBuilder text = new StringBuilder();
        text.append(BRAND).append('\n').append('\n')
                .append(greeting).append('\n').append('\n')
                .append(introduction).append('\n');
        for (Section section : sections) {
            if (section == null || section.details() == null || section.details().isEmpty()) {
                continue;
            }
            text.append('\n').append(value(section.title())).append('\n');
            for (Detail detail : section.details()) {
                if (detail != null) {
                    text.append(detail.label()).append(": ").append(detail.value()).append('\n');
                }
            }
        }
        if (!items.isEmpty()) {
            text.append("\nSản phẩm\n");
            for (LineItem item : items) {
                if (item != null) {
                    text.append("- ").append(item.name()).append(" | ")
                            .append(item.description()).append(" | ").append(item.quantity())
                            .append(" | ").append(item.amount()).append('\n');
                }
            }
        }
        if (isHttpUrl(actionUrl) && !value(actionLabel).isBlank()) {
            text.append('\n').append(actionLabel).append(": ").append(actionUrl).append('\n');
        }
        if (!note.isBlank()) {
            text.append('\n').append(note).append('\n');
        }
        text.append("\nEmail này được gửi liên quan đến tài khoản hoặc đơn hàng của bạn tại ")
                .append(BRAND).append(".\nNếu cần hỗ trợ, bạn có thể phản hồi trực tiếp email này.");
        return text.toString();
    }

    // Kiểm tra điều kiện và tính hợp lệ cho is http url.
    private static boolean isHttpUrl(String url) {
        String value = value(url).toLowerCase();
        return value.startsWith("https://") || value.startsWith("http://");
    }

    // Thực hiện xử lý nghiệp vụ của hàm value.
    private static String value(String input) {
        return input == null ? "" : input.trim();
    }

    // Thực hiện xử lý nghiệp vụ của hàm escape.
    private static String escape(String input) {
        return value(input)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    // Thực hiện xử lý nghiệp vụ của hàm escape attribute.
    private static String escapeAttribute(String input) {
        return escape(input);
    }

    // Thực hiện xử lý nghiệp vụ của hàm detail.
    public record Detail(String label, String value, boolean emphasized) {
        // Thực hiện xử lý nghiệp vụ của hàm detail.
        public Detail(String label, String value) {
            this(label, value, false);
        }
    }

    // Thực hiện xử lý nghiệp vụ của hàm section.
    public record Section(String title, List<Detail> details) {
    }

    // Thực hiện xử lý nghiệp vụ của hàm line item.
    public record LineItem(String name, String description, String quantity, String amount) {
    }

    // Thực hiện xử lý nghiệp vụ của hàm mail content.
    public record MailContent(String html, String text) {
    }
}
