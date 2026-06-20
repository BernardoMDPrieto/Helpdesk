package com.bduarte.helpdeskserver.services;

import com.bduarte.helpdeskserver.models.ResetPasswordEvent;
import com.bduarte.helpdeskserver.models.UserRegisteredEvent;
import jakarta.mail.MessagingException;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class UserRegistrationListener {

    private final EmailService emailService;

    public UserRegistrationListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @EventListener
    public void handleRegistered(UserRegisteredEvent event) throws MessagingException {
        try {
            emailService.sendMailNewUser(event.email(), event.userName(), event.token());
        } catch (MessagingException e) {
            throw e;
        }
    }

    @EventListener
    public void handleResetPassword(ResetPasswordEvent event) throws MessagingException {

        try {
            emailService.sendResetPassword(event.email(), event.userName(), event.token());
        } catch (MessagingException e) {
            throw e;
        }
    }

}
