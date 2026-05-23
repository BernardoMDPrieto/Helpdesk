package com.bduarte.helpdeskserver.services;

import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final MailSender mailSender;
    private final SimpleMailMessage templateMessage;

    public EmailService(MailSender mailSender, SimpleMailMessage templateMessage) {
        this.mailSender = mailSender;
        this.templateMessage = templateMessage;
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
}
