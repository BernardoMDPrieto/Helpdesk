package com.bduarte.helpdeskserver.services;

import com.bduarte.helpdeskserver.models.ResetPasswordEvent;
import com.bduarte.helpdeskserver.models.UserRegisteredEvent;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserRegistrationListenerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserRegistrationListener listener;

    @Test
    void registeredEventSendsNewUserMail() throws Exception {
        listener.handleRegistered(new UserRegisteredEvent("ana@helpdesk.com", "Ana", "token-123"));

        verify(emailService).sendMailNewUser("ana@helpdesk.com", "Ana", "token-123");
    }

    @Test
    void registeredEventRethrowsMailFailure() throws Exception {
        MessagingException failure = new MessagingException("SMTP down");
        doThrow(failure).when(emailService).sendMailNewUser("ana@helpdesk.com", "Ana", "token-123");

        assertThatThrownBy(() -> listener.handleRegistered(new UserRegisteredEvent("ana@helpdesk.com", "Ana", "token-123")))
                .isSameAs(failure);
    }

    @Test
    void resetPasswordEventSendsResetMail() throws Exception {
        listener.handleResetPassword(new ResetPasswordEvent("ana@helpdesk.com", "Ana", "token-123"));

        verify(emailService).sendResetPassword("ana@helpdesk.com", "Ana", "token-123");
    }

    @Test
    void resetPasswordEventRethrowsMailFailure() throws Exception {
        MessagingException failure = new MessagingException("SMTP down");
        doThrow(failure).when(emailService).sendResetPassword("ana@helpdesk.com", "Ana", "token-123");

        assertThatThrownBy(() -> listener.handleResetPassword(new ResetPasswordEvent("ana@helpdesk.com", "Ana", "token-123")))
                .isSameAs(failure);
    }
}
