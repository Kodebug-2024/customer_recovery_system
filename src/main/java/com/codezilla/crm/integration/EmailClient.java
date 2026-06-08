package com.codezilla.crm.integration;

public interface EmailClient {
    void send(String to, String subject, String body);
}
