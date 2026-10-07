package com.ga.RentalSystem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends plain-text emails, such as account verification and password recovery messages.
 * The mail server is set in the application properties (a Mailtrap sandbox inbox is used
 * for testing).
 */
@Service
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    /**
     * Sends a plain-text email from {@code noreply@rentalsystem.com}.
     *
     * @param to      the recipient's email address
     * @param subject the subject line
     * @param body    the message text
     * @throws org.springframework.mail.MailException if the message cannot be sent
     */
    public void sendEmail(String to,
                          String subject,
                          String body){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom("noreply@rentalsystem.com");
        message.setSubject(subject);
        message.setText(body);
        javaMailSender.send(message);
    }
}