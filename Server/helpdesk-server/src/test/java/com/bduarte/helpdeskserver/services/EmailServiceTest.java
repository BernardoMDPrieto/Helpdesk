package com.bduarte.helpdeskserver.services;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    private SimpleMailMessage templateMessage;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        templateMessage = new SimpleMailMessage();
        templateMessage.setFrom("helpdesk@helpdesk.com");
        templateMessage.setSubject("Teste");
        emailService = EmailService.builder()
                .mailSender(mailSender)
                .templateMessage(templateMessage)
                .templateEngine(templateEngine)
                .build();
    }

    @Test
    void sendMailUsesTemplateMessage() {
        emailService.sendMail("ana@helpdesk.com");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getFrom()).isEqualTo("helpdesk@helpdesk.com");
        assertThat(sent.getSubject()).isEqualTo("Teste");
        assertThat(sent.getTo()).containsExactly("ana@helpdesk.com");
        assertThat(sent.getText()).isEqualTo("E-mail teste");
        // The shared template bean must not be modified
        assertThat(templateMessage.getTo()).isNull();
    }

    @Test
    void sendMailSwallowsMailErrors() {
        doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> emailService.sendMail("ana@helpdesk.com")).doesNotThrowAnyException();
    }

    @Test
    void sendMailNewUserRendersTemplateWithRegistrationLink() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        when(templateEngine.process(eq("emails/NewUserTemplate"), any(IContext.class))).thenReturn("<p>Olá Ana</p>");

        emailService.sendMailNewUser("ana@helpdesk.com", "Ana", "token-123");

        Context context = captureContext("emails/NewUserTemplate");
        assertThat(context.getVariable("username")).isEqualTo("Ana");
        assertThat(context.getVariable("passLink")).isEqualTo("http://localhost:8080/new-user/token-123");
        assertSentHtmlMessage(message);
    }

    @Test
    void sendResetPasswordRendersTemplateWithResetLink() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        when(templateEngine.process(eq("emails/ResetPasswordTemplate"), any(IContext.class))).thenReturn("<p>Olá Ana</p>");

        emailService.sendResetPassword("ana@helpdesk.com", "Ana", "token-123");

        Context context = captureContext("emails/ResetPasswordTemplate");
        assertThat(context.getVariable("username")).isEqualTo("Ana");
        assertThat(context.getVariable("passLink")).isEqualTo("http://localhost:8080/reset-password/token-123");
        assertSentHtmlMessage(message);
    }

    private Context captureContext(String template) {
        ArgumentCaptor<IContext> captor = ArgumentCaptor.forClass(IContext.class);
        verify(templateEngine).process(eq(template), captor.capture());
        return (Context) captor.getValue();
    }

    private void assertSentHtmlMessage(MimeMessage message) throws Exception {
        verify(mailSender).send(message);
        assertThat(message.getRecipients(Message.RecipientType.TO))
                .extracting(Object::toString)
                .containsExactly("ana@helpdesk.com");
        assertThat(message.getSubject()).isEqualTo("Cadastro de senha");
        assertThat(message.getContent()).isEqualTo("<p>Olá Ana</p>");
        // The real sender finalises headers before sending; the mock does not
        message.saveChanges();
        assertThat(message.getContentType()).startsWith("text/html");
    }
}
