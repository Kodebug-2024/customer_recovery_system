package com.codezilla.crm.integration;

public interface TelegramClient {
    void sendMessage(String chatId, String text);
}
