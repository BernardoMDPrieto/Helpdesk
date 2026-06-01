package com.bduarte.helpdeskserver.services;

import com.bduarte.helpdeskserver.models.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final SimpleMailMessage templateMessage;
    private final TemplateEngine templateEngine;

    public EmailService(JavaMailSender mailSender, SimpleMailMessage templateMessage, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateMessage = templateMessage;
        this.templateEngine = templateEngine;
    }

    public void sendMail(String messageTo) {
        SimpleMailMessage msg = new SimpleMailMessage(this.templateMessage);
        msg.setTo(messageTo);
        msg.setText("E-mail teste");

        try {
            this.mailSender.send(msg);
        } catch (MailException ex) {
            System.err.println(ex.getMessage());
        }
    }

    public void sendHtml(User user) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
        helper.setTo(user.getEmail());
        helper.setSubject("Cadastro de senha");

        String link = "http://localhost:8080";
        String htmlBody = BuildEmailTemplateNewUsers(user.getUserName(), link);
        helper.setText(htmlBody, true);
        mailSender.send(message);
    }

    private String BuildEmailTemplateNewUsers(String username, String passLink) {

        Context context = new Context();

        context.setVariable("username", username);
        context.setVariable("passLink", passLink);

        return templateEngine.process("emails/NewUserTemplate", context);
    }

}
