package dezsys.mail;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

public class EmailSender {
    private JavaMailSender mailSender;

    public EmailSender(String mailServer, int port, String username, String password, boolean tls) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);

    
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        if(tls) {
            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.debug", "true");
        }
    }

    public void sendEmail(String senderName, String senderEmail, String recipientEmail, String subject, String content) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        helper.setFrom(senderEmail, senderName);
        helper.setTo(recipientEmail);

        helper.setSubject(subject);
        helper.setText(content, true);

        mailSender.send(message);
    }
}