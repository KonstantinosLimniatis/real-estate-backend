package gr.hua.dit.ds.ds_2025.services;

import gr.hua.dit.ds.ds_2025.entities.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    public EmailService(JavaMailSender mailSender, SpringTemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    public void sendRegistrationSuccessEmail(User user) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return; // δεν έχουμε που να στείλουμε
        }

        String recipientEmail = user.getEmail();
        String subject = "Registration Successful";

        try {
            Context context = new Context();
            context.setVariable("username", user.getUsername());
            context.setVariable("email", user.getEmail());

            String body = templateEngine.process("email/registration-success.html", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(body, true);

            mailSender.send(message);
        } catch (MailException | MessagingException e) {
            // Δεν θέλουμε να αποτύχει η εγγραφή επειδή απέτυχε το email.
            System.err.println("Failed to send registration email to: " + recipientEmail);
            e.printStackTrace();
        }
    }
}