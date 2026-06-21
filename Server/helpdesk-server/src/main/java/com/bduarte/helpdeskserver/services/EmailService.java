package com.bduarte.helpdeskserver.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@Builder
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final SimpleMailMessage templateMessage;
    private final TemplateEngine templateEngine;


    public void sendMail(String messageTo) {
        logger.info("Sending simple email to: {}", messageTo);
        SimpleMailMessage msg = new SimpleMailMessage(this.templateMessage);
        msg.setTo(messageTo);
        msg.setText("E-mail teste");

        try {
            this.mailSender.send(msg);
            logger.info("Email sent successfully to: {}", messageTo);
        } catch (MailException ex) {
            logger.error("Error sending email to: {}", messageTo, ex);
        }
    }

    public void sendMailNewUser(String email, String userName, String token) throws MessagingException {
        logger.info("Sending new user registration email to: {}", email);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
        helper.setTo(email);
        helper.setSubject("Cadastro de senha");

        String link = "http://localhost:8080/new-user/" + token;
        String htmlBody = BuildEmailTemplateNewUsers(userName, link);
        helper.setText(htmlBody, true);
        mailSender.send(message);
        logger.info("New user registration email sent successfully to: {}", email);
    }

    public void sendResetPassword(String email, String userName, String token) throws MessagingException {
        logger.info("Sending password reset email to: {}", email);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
        helper.setTo(email);
        helper.setSubject("Cadastro de senha");

        String link = "http://localhost:8080/reset-password/" + token;
        String htmlBody = BuildEmailTemplateResetPassword(userName, link);
        helper.setText(htmlBody, true);
        mailSender.send(message);
        logger.info("Password reset email sent successfully to: {}", email);
    }

    private String BuildEmailTemplateResetPassword(String username, String passLink) {
        logger.debug("Building reset password email template for user: {}", username);
        Context context = new Context();

        context.setVariable("username", username);
        context.setVariable("passLink", passLink);

        return templateEngine.process("emails/ResetPasswordTemplate", context);
    }

    private String BuildEmailTemplateNewUsers(String username, String passLink) {
        logger.debug("Building new user registration email template for user: {}", username);
        Context context = new Context();

        context.setVariable("username", username);
        context.setVariable("passLink", passLink);

        return templateEngine.process("emails/NewUserTemplate", context);
    }

}
