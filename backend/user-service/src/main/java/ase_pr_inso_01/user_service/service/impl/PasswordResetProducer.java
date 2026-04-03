package ase_pr_inso_01.user_service.service.impl;

import ase_pr_inso_01.user_service.config.RabbitMQConfig;
import ase_pr_inso_01.user_service.model.EmailRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordResetProducer {

  private final RabbitTemplate rabbitTemplate;

  public void sendResetEmail(String userEmail, String token) {
    String frontendUrl = System.getenv("FRONTEND_URL");
    if (frontendUrl == null) {
      frontendUrl = "http://localhost:4200";
    }
    String resetLink = frontendUrl + "/reset-password?token=" + token;

    // Build HTML email body
    String htmlBody = buildPasswordResetEmailHtml(resetLink, userEmail);

    EmailRequest request = new EmailRequest(
            userEmail,
            "Password Reset Request - Agriscope",
            htmlBody
    );

    rabbitTemplate.convertAndSend(
            RabbitMQConfig.EMAIL_EXCHANGE,
            RabbitMQConfig.EMAIL_ROUTING_KEY,
            request
    );
  }

  private String buildPasswordResetEmailHtml(String resetLink, String userEmail) {
    String timestamp = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));

    return String.format("""
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Password Reset</title>
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
                    margin: 0 0 24px 0;
                    text-align: center;
                }
                .info-box {
                    background: linear-gradient(135deg, #f0fdf4 0%%, #ecfdf5 100%%);
                    border-left: 4px solid #16a34a;
                    padding: 20px;
                    border-radius: 8px;
                    margin: 24px 0;
                }
                .info-box p {
                    font-size: 15px;
                    line-height: 1.7;
                    color: #166534;
                    margin: 0;
                    text-align: left;
                }
                .info-box strong {
                    color: #15803d;
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
                .link-section {
                    margin-top: 20px;
                    padding: 16px;
                    background: #f9fafb;
                    border-radius: 8px;
                    border: 1px solid #e5e7eb;
                }
                .link-section p {
                    font-size: 13px;
                    color: #666;
                    margin-bottom: 8px;
                    text-align: left;
                }
                .link-section a {
                    font-size: 13px;
                    color: #16a34a;
                    word-break: break-all;
                    text-align: left;
                    display: block;
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
                .warning {
                    margin-top: 20px;
                    padding: 16px;
                    background: #fef2f2;
                    border-left: 4px solid #dc2626;
                    border-radius: 8px;
                }
                .warning p {
                    font-size: 14px;
                    color: #991b1b;
                    margin: 0;
                    text-align: left;
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
                        <img src="https://cdn-icons-png.flaticon.com/512/4661/4661507.png" alt="Farm Icon" class="logo">
                        <h1>Password Reset Request</h1>
                        <p>Agriscope Farm Management System</p>
                    </div>
                    
                    <!-- Content -->
                    <div class="content-section">
                        <h2>🔒 Reset Your Password</h2>
                        
                        <p>We received a request to reset the password for your Agriscope account associated with <strong>%s</strong>.</p>
                        
                        <p>Click the button below to reset your password. This link will expire in 15 minutes for security reasons.</p>
                        
                    </div>
                    
                    <!-- Action Button -->
                    <div class="button-container">
                        <a href="%s" class="button">Reset Password</a>
                        
                        <div class="link-section">
                            <p>Or copy and paste this link into your browser:</p>
                            <a href="%s">%s</a>
                        </div>
                    </div>
                    
                    <!-- Warning -->
                    <div class="content-section">
                        <div class="warning">
                            <p><strong>️🛡️ Important:</strong> If you didn't request this password reset, please ignore this email. Your password will remain unchanged.</p>
                        </div>
                    </div>
                    
                    <!-- Footer -->
                    <div class="footer">
                        <div class="timestamp">📅 %s</div>
                        <p>This is an automated email from Agriscope.</p>
                        <p>Questions? Contact <a href="mailto:aagriscope@gmail.com">aagriscope@gmail.com</a></p>
                    </div>
                </div>
            </div>
        </body>
        </html>
        """,
            userEmail,   // User email
            resetLink,   // Reset link (for button)
            resetLink,   // Reset link (for copy-paste)
            resetLink,   // Reset link (display)
            timestamp    // Timestamp
    );
  }
}