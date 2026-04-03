package com.agriscope.notification_service.service;

import com.agriscope.notification_service.model.Recommendation;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailTemplateService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private static final String FARM_ICON_URL = "https://cdn-icons-png.flaticon.com/512/4661/4661507.png";

    public String buildAlertEmailHtml(Recommendation rec, String fieldName) {
        String alertType = rec.getRecommendationType();
        String alertTitle = formatAlertTitle(alertType); // Includes icon now!
        String cropName = formatCropName(rec.getRecommendedSeed());
        String reasoning = rec.getReasoning() != null ? rec.getReasoning() : "No details provided.";
        String advice = rec.getAdvice() != null ? formatAdvice(rec.getAdvice()) : "";
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Farm Alert</title>
                <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                        margin: 0;
                        padding: 0;
                        background: linear-gradient(135deg, #f0fdf4 0%%, #dcfce7 100%%);
                        min-height: 100vh;
                    }
                    .email-wrapper {
                        width: 100%%;
                        padding: 40px 20px;
                    }
                    .email-container {
                        max-width: 650px;
                        margin: 0 auto;
                        background-color: #ffffff;
                        border-radius: 16px;
                        overflow: hidden;
                        box-shadow: 0 10px 40px rgba(0, 0, 0, 0.08);
                    }
                    .header {
                        background: linear-gradient(135deg, #16a34a 0%%, #15803d 100%%);
                        padding: 40px 30px;
                        text-align: center;
                        color: white;
                    }
                    .header .logo {
                        width: 64px;
                        height: 64px;
                        margin: 0 auto 16px auto;
                        display: block;
                    }
                    .header h1 {
                        margin: 0 0 12px 0;
                        font-size: 28px;
                        font-weight: 700;
                        letter-spacing: -0.5px;
                    }
                    .header p {
                        margin: 0;
                        font-size: 15px;
                        opacity: 0.95;
                        font-weight: 400;
                        line-height: 1.5;
                    }
                    .alert-section {
                        padding: 40px 35px;
                        background: linear-gradient(135deg, #f0fdf4 0%%, #ecfdf5 100%%);
                    }
                    .alert-header {
                        text-align: center;
                        margin-bottom: 30px;
                    }
                    .alert-title {
                        font-size: 28px;
                        font-weight: 700;
                        color: #1a1a1a;
                        margin: 0 0 15px 0;
                        line-height: 1.3;
                    }
                    .divider {
                        height: 1px;
                        background: linear-gradient(to right, rgba(0,0,0,0), rgba(22,163,74,0.2), rgba(0,0,0,0));
                        margin: 35px 0;
                    }
                    .info-section {
                        margin-bottom: 28px;
                        background: white;
                        padding: 24px;
                        border-radius: 12px;
                        border-left: 4px solid #16a34a;
                    }
                    .info-section h3 {
                        font-size: 17px;
                        font-weight: 600;
                        color: #16a34a;
                        margin: 0 0 14px 0;
                        display: flex;
                        align-items: center;
                        gap: 8px;
                    }
                    .info-section p {
                        font-size: 16px;
                        line-height: 1.8;
                        color: #333;
                        margin: 0;
                    }
                    .advice-section {
                        background: white;
                        border-left: 4px solid #15803d;
                        padding: 24px;
                        border-radius: 12px;
                        margin-top: 20px;
                    }
                    .advice-section h3 {
                        font-size: 17px;
                        font-weight: 600;
                        color: #15803d;
                        margin: 0 0 14px 0;
                        display: flex;
                        align-items: center;
                        gap: 8px;
                    }
                    .advice-section p {
                        font-size: 16px;
                        line-height: 1.8;
                        color: #333;
                        margin: 0;
                    }
                    .button-container {
                        text-align: center;
                        padding: 35px 30px;
                        background-color: #fafafa;
                        border-top: 1px solid #f0f0f0;
                    }
                    .button {
                        display: inline-block;
                        background: linear-gradient(135deg, #16a34a 0%%, #15803d 100%%);
                        color: white !important;
                        padding: 14px 35px;
                        text-decoration: none;
                        border-radius: 8px;
                        font-weight: 600;
                        font-size: 15px;
                        letter-spacing: 0.3px;
                        box-shadow: 0 4px 12px rgba(22, 163, 74, 0.3);
                    }
                    .footer {
                        background-color: #f9fafb;
                        padding: 28px 30px;
                        text-align: center;
                        border-top: 1px solid #f0f0f0;
                    }
                    .footer .timestamp {
                        font-size: 13px;
                        color: #999;
                        margin-bottom: 12px;
                    }
                    .footer p {
                        font-size: 13px;
                        color: #666;
                        margin: 5px 0;
                        line-height: 1.6;
                    }
                    .footer a {
                        color: #16a34a;
                        text-decoration: none;
                        font-weight: 500;
                    }
                    @media only screen and (max-width: 600px) {
                        .email-wrapper {
                            padding: 20px 10px;
                        }
                        .email-container {
                            border-radius: 12px;
                        }
                        .header {
                            padding: 30px 20px;
                        }
                        .header .logo {
                            width: 56px;
                            height: 56px;
                        }
                        .header h1 {
                            font-size: 24px;
                        }
                        .alert-section {
                            padding: 28px 20px;
                        }
                        .alert-title {
                            font-size: 22px;
                        }
                        .button-container {
                            padding: 24px 20px;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="email-wrapper">
                    <div class="email-container">
                        <!-- Header -->
                        <div class="header">
                            <img src="%s" alt="Farm Icon" class="logo">
                            <h1>Farm Alert Notification</h1>
                            <p>This is an automated alert from your Agriscope system regarding your <strong>%s</strong> field.</p>
                        </div>
                        
                        <!-- Alert Content -->
                        <div class="alert-section">
                            <div class="alert-header">
                                <h2 class="alert-title">%s for %s</h2>
                            </div>
                            
                            <div class="divider"></div>
                            
                            <!-- Alert Details -->
                            <div class="info-section">
                                <h3>📋 Details</h3>
                                <p>%s</p>
                            </div>
                            
                            <!-- Advice Section -->
                            %s
                        </div>
                        
                        <!-- Action Button -->
                        <div class="button-container">
                            <a href="http://localhost:4200/home" class="button">View in Dashboard</a>
                        </div>
                        
                        <!-- Footer -->
                        <div class="footer">
                            <div class="timestamp">📅 %s</div>
                            <p>This is an automated alert from Agriscope.</p>
                            <p>Questions? Contact <a href="mailto:aagriscope@gmail.com">aagriscope@gmail.com</a></p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """,
                FARM_ICON_URL,        // Farm icon
                cropName,            // Field name
                alertTitle,           // Alert title with icon (e.g., "❄️ Frost Alert")
                cropName,             // Crop name
                reasoning,            // Details
                buildAdviceSection(advice), // Advice section
                timestamp             // Timestamp
        );
    }

    private String buildAdviceSection(String advice) {
        if (advice == null || advice.trim().isEmpty()) {
            return "";
        }
        return String.format("""
            <div class="advice-section">
                <h3>💡 Recommended Action</h3>
                <p>%s</p>
            </div>
            """, advice);
    }

    private String formatAdvice(String advice) {
        if (advice == null || advice.trim().isEmpty()) {
            return "";
        }

        String[] words = advice.replace("_", " ").toLowerCase().split(" ");
        StringBuilder result = new StringBuilder();

        if (words.length > 0 && !words[0].isEmpty()) {
            result.append(Character.toUpperCase(words[0].charAt(0)))
                    .append(words[0].substring(1));
        }

        for (int i = 1; i < words.length; i++) {
            if (!words[i].isEmpty()) {
                result.append(" ").append(words[i]);
            }
        }

        return result.toString();
    }

    private String getAlertIcon(String alertType) {
        return switch (alertType) {
            case "FROST_ALERT" -> "❄️";
            case "HEAT_ALERT" -> "🌡️";
            case "STORM_ALERT" -> "⛈️";
            case "SAFETY_ALERT" -> "⚠️";
            case "IRRIGATE_NOW" -> "💧";
            case "DISEASE_PREVENTION", "DISEASE_RISK" -> "🦠";
            case "PEST_RISK" -> "🐛";
            case "READY_TO_HARVEST" -> "🌾";
            case "NUTRIENT_CHECK" -> "🌱";
            default -> "🔔";
        };
    }

    private String getAlertColor(String alertType) {
        return switch (alertType) {
            case "FROST_ALERT" -> "#3b82f6";
            case "HEAT_ALERT" -> "#ef4444";
            case "STORM_ALERT" -> "#8b5cf6";
            case "SAFETY_ALERT" -> "#dc2626";
            case "IRRIGATE_NOW" -> "#06b6d4";
            case "DISEASE_PREVENTION", "DISEASE_RISK" -> "#f59e0b";
            case "PEST_RISK" -> "#78716c";
            case "READY_TO_HARVEST" -> "#10b981";
            case "NUTRIENT_CHECK" -> "#84cc16";
            default -> "#6b7280";
        };
    }

    private String formatAlertTitle(String alertType) {
        String[] words = alertType.replace("_", " ").toLowerCase().split(" ");
        StringBuilder result = new StringBuilder();

        String icon = getAlertIcon(alertType);
        result.append(icon).append(" ");

        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }

        return result.toString().trim();
    }

    private String formatCropName(String cropName) {
        if (cropName == null) return "N/A";

        String[] words = cropName.replace("_", " ").toLowerCase().split(" ");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }

        return result.toString().trim();
    }

    public String buildWelcomeEmailHtml(String firstName) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);

        return String.format("""
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Welcome to Agriscope</title>
            <style>
                * {
                    margin: 0;
                    padding: 0;
                    box-sizing: border-box;
                }
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                    margin: 0;
                    padding: 0;
                    background: linear-gradient(135deg, #f0fdf4 0%%, #dcfce7 100%%);
                    min-height: 100vh;
                }
                .email-wrapper {
                    width: 100%%;
                    padding: 40px 20px;
                }
                .email-container {
                    max-width: 650px;
                    margin: 0 auto;
                    background-color: #ffffff;
                    border-radius: 16px;
                    overflow: hidden;
                    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.08);
                }
                .header {
                    background: linear-gradient(135deg, #16a34a 0%%, #15803d 100%%);
                    padding: 40px 30px;
                    text-align: center;
                    color: white;
                }
                .header .logo {
                    width: 64px;
                    height: 64px;
                    margin: 0 auto 16px auto;
                    display: block;
                }
                .header h1 {
                    margin: 0 0 8px 0;
                    font-size: 28px;
                    font-weight: 700;
                    letter-spacing: -0.5px;
                }
                .header p {
                    margin: 0;
                    font-size: 15px;
                    opacity: 0.95;
                    font-weight: 400;
                    line-height: 1.5;
                }
                .content-section {
                    padding: 40px 35px;
                    background: white;
                }
                .icon-container {
                    text-align: center;
                    margin-bottom: 25px;
                }
                .icon-container .icon {
                    font-size: 56px;
                }
                .content-section h2 {
                    font-size: 24px;
                    font-weight: 700;
                    color: #1a1a1a;
                    margin: 0 0 16px 0;
                    text-align: center;
                }
                .content-section p {
                    font-size: 16px;
                    line-height: 1.8;
                    color: #555;
                    margin: 0 0 16px 0;
                    text-align: center;
                }
                .features-box {
                    background: linear-gradient(135deg, #f0fdf4 0%%, #ecfdf5 100%%);
                    border-left: 4px solid #16a34a;
                    padding: 24px;
                    border-radius: 8px;
                    margin: 28px 0;
                }
                .features-box h3 {
                    font-size: 17px;
                    font-weight: 600;
                    color: #15803d;
                    margin: 0 0 16px 0;
                    text-align: center;
                }
                .features-list {
                    list-style: none;
                    padding: 0;
                    margin: 0;
                }
                .features-list li {
                    font-size: 15px;
                    line-height: 1.8;
                    color: #166534;
                    margin: 8px 0;
                    padding-left: 28px;
                    position: relative;
                }
                .features-list li:before {
                    content: "✓";
                    position: absolute;
                    left: 0;
                    color: #16a34a;
                    font-weight: bold;
                    font-size: 18px;
                }
                .button-container {
                    text-align: center;
                    padding: 35px 30px;
                    background-color: #fafafa;
                    border-top: 1px solid #f0f0f0;
                }
                .button {
                    display: inline-block;
                    background: linear-gradient(135deg, #16a34a 0%%, #15803d 100%%);
                    color: white !important;
                    padding: 14px 35px;
                    text-decoration: none;
                    border-radius: 8px;
                    font-weight: 600;
                    font-size: 15px;
                    letter-spacing: 0.3px;
                    box-shadow: 0 4px 12px rgba(22, 163, 74, 0.3);
                }
                .footer {
                    background-color: #f9fafb;
                    padding: 28px 30px;
                    text-align: center;
                    border-top: 1px solid #f0f0f0;
                }
                .footer .timestamp {
                    font-size: 13px;
                    color: #999;
                    margin-bottom: 12px;
                }
                .footer p {
                    font-size: 13px;
                    color: #666;
                    margin: 5px 0;
                    line-height: 1.6;
                }
                .footer a {
                    color: #16a34a;
                    text-decoration: none;
                    font-weight: 500;
                }
                @media only screen and (max-width: 600px) {
                    .email-wrapper {
                        padding: 20px 10px;
                    }
                    .email-container {
                        border-radius: 12px;
                    }
                    .header {
                        padding: 30px 20px;
                    }
                    .header .logo {
                        width: 56px;
                        height: 56px;
                    }
                    .header h1 {
                        font-size: 24px;
                    }
                    .content-section {
                        padding: 28px 20px;
                    }
                    .button-container {
                        padding: 24px 20px;
                    }
                }
            </style>
        </head>
        <body>
            <div class="email-wrapper">
                <div class="email-container">
                    <!-- Header -->
                    <div class="header">
                        <img src="%s" alt="Farm Icon" class="logo">
                        <h1>Welcome to Agriscope!</h1>
                        <p>Your Farm Management Journey Starts Here</p>
                    </div>
                    
                    <!-- Content -->
                    <div class="content-section">                        
                        <h2>Hi %s, Welcome Aboard!</h2>
                        
                        <p>Thank you for joining Agriscope. Your account has been successfully created, and you're now ready to transform the way you manage your farm.</p>
                                               
                        <p><strong>Ready to start monitoring your farm?</strong></p>
                    </div>
                    
                    <!-- Action Button -->
                    <div class="button-container">
                        <a href="http://localhost:4200/login" class="button">Go to Dashboard</a>
                    </div>
                    
                    <!-- Footer -->
                    <div class="footer">
                        <div class="timestamp">📅 %s</div>
                        <p>This is an automated email from Agriscope.</p>
                        <p>Questions? Contact <a href="mailto:aagriscope@gmail.com">aagriscope@gmail.com</a></p>
                        <p style="margin-top: 12px; font-size: 12px; color: #999;">You're receiving this email because you created an account at Agriscope.</p>
                    </div>
                </div>
            </div>
        </body>
        </html>
        """,
                FARM_ICON_URL,  // Farm icon
                firstName,      // User's first name
                timestamp       // Timestamp
        );
    }
}