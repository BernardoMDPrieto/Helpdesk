package com.bduarte.helpdeskserver.services;

import com.bduarte.helpdeskserver.models.ResetPasswordEvent;
import com.bduarte.helpdeskserver.models.UserRegisteredEvent;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class UserRegistrationListener {
    private static final Logger logger = LoggerFactory.getLogger(UserRegistrationListener.class);

    private final EmailService emailService;

    public UserRegistrationListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @EventListener
    @Async
    public void handleRegistered(UserRegisteredEvent event) throws MessagingException {
        try {
            logger.info("Processing user registration event for email: {}", event.email());
            emailService.sendMailNewUser(event.email(), event.userName(), event.token());
            logger.info("User registration email sent successfully for email: {}", event.email());
        } catch (MessagingException e) {
            logger.error("Error processing user registration event for email: {}", event.email(), e);
            throw e;
        }
    }

    @EventListener
    @Async
    public void handleResetPassword(ResetPasswordEvent event) throws MessagingException {
        try {
            logger.info("Processing password reset event for email: {}", event.email());
            emailService.sendResetPassword(event.email(), event.userName(), event.token());
            logger.info("Password reset email sent successfully for email: {}", event.email());
        } catch (MessagingException e) {
            logger.error("Error processing password reset event for email: {}", event.email(), e);
            throw e;
        }
    }

}
