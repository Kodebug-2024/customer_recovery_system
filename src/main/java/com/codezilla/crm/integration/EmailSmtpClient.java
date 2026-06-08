package com.codezilla.crm.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "integrations.email.mode", havingValue = "real")
public class EmailSmtpClient implements EmailClient {
    private static final Logger log = LoggerFactory.getLogger(EmailSmtpClient.class);

    private final JavaMailSender sender;
    private final String from;

    public EmailSmtpClient(JavaMailSender sender,
                           @Value("${integrations.email.from}") String from) {
        this.sender = sender;
        this.from = from;
    }

    @Override
    public void send(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            sender.send(msg);
            log.info("Email sent to={} subject={}", to, subject);
        } catch (Exception e) {
            log.error("Email send failed to={} subject={}", to, subject, e);
        }
    }
}
