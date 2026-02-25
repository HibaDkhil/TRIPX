package tn.esprit.services;

import tn.esprit.entities.Booking;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class EmailService {

    // Environment variables for email
    private static final String EMAIL_FROM = System.getenv("EMAIL_FROM");
    private static final String EMAIL_PASSWORD = System.getenv("EMAIL_PASSWORD");

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int SMTP_PORT = 587;

    private static EmailService instance;

    private EmailService() {}

    public static EmailService getInstance() {
        if (instance == null) {
            instance = new EmailService();
        }
        return instance;
    }

    public boolean sendBookingConfirmation(Booking booking) {
        String to = booking.getUserEmail();
        if (to == null || to.isEmpty()) return false;

        String subject = "✈️ TripX Booking Confirmation - " + booking.getBookingReference();

        String body = buildConfirmationHtml(booking);

        return sendEmail(to, subject, body);
    }

    private boolean sendEmail(String to, String subject, String htmlBody) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", String.valueOf(SMTP_PORT));

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_FROM));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setContent(htmlBody, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("Email sent successfully to: " + to);
            return true;
        } catch (MessagingException e) {
            System.err.println("Failed to send email to: " + to);
            e.printStackTrace();
            return false;
        }
    }

    private String buildConfirmationHtml(Booking booking) {
        String destName = booking.getDestinationName() != null ? booking.getDestinationName() : "N/A";
        String actName = booking.getActivityName() != null ? booking.getActivityName() : "None";
        String startDate = booking.getStartAt() != null ? booking.getStartAt().toLocalDateTime().toLocalDate().toString() : "N/A";
        String endDate = booking.getEndAt() != null ? booking.getEndAt().toLocalDateTime().toLocalDate().toString() : "N/A";

        return "<!DOCTYPE html>"
            + "<html><head><style>"
            + "body { font-family: 'Segoe UI', Arial, sans-serif; background: #f4f6f9; margin: 0; padding: 20px; }"
            + ".container { max-width: 600px; margin: 0 auto; background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 12px rgba(0,0,0,0.1); }"
            + ".header { background: linear-gradient(135deg, #3498db, #2ecc71); color: white; padding: 30px; text-align: center; }"
            + ".header h1 { margin: 0; font-size: 28px; }"
            + ".header p { margin: 5px 0 0; opacity: 0.9; }"
            + ".body { padding: 30px; }"
            + ".detail-row { display: flex; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid #eee; }"
            + ".detail-label { color: #7f8c8d; font-weight: bold; }"
            + ".detail-value { color: #2c3e50; font-weight: bold; }"
            + ".total { background: #e8f8f5; padding: 15px; border-radius: 8px; text-align: center; margin-top: 20px; font-size: 22px; color: #27ae60; font-weight: bold; }"
            + ".footer { background: #2c3e50; color: #bbb; text-align: center; padding: 15px; font-size: 12px; }"
            + "</style></head><body>"
            + "<div class='container'>"
            + "<div class='header'><h1>✈️ Booking Confirmed!</h1><p>Reference: " + booking.getBookingReference() + "</p></div>"
            + "<div class='body'>"
            + "<div class='detail-row'><span class='detail-label'>Destination</span><span class='detail-value'>" + destName + "</span></div>"
            + "<div class='detail-row'><span class='detail-label'>Activity</span><span class='detail-value'>" + actName + "</span></div>"
            + "<div class='detail-row'><span class='detail-label'>Dates</span><span class='detail-value'>" + startDate + " → " + endDate + "</span></div>"
            + "<div class='detail-row'><span class='detail-label'>Guests</span><span class='detail-value'>" + booking.getNumGuests() + "</span></div>"
            + "<div class='total'>Total: $" + String.format("%.2f", booking.getTotalAmount()) + " " + booking.getCurrency() + "</div>"
            + "</div>"
            + "<div class='footer'>Thank you for choosing TripX! 🌍</div>"
            + "</div></body></html>";
    }

    /**
     * Validate email format using regex.
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) return false;
        String regex = "^[\\w._%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$";
        return email.trim().matches(regex);
    }
}
