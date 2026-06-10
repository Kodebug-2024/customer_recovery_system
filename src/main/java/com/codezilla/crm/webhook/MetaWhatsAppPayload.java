package com.codezilla.crm.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Subset of the Meta WhatsApp Cloud API webhook payload we need.
 * Shape: { object, entry: [{ id, changes: [{ field, value: { messages, contacts, ... } }] }] }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MetaWhatsAppPayload(String object, List<Entry> entry) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entry(String id, List<Change> changes) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Change(String field, Value value) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Value(Metadata metadata, List<Contact> contacts, List<Message> messages) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Metadata(
            @com.fasterxml.jackson.annotation.JsonProperty("display_phone_number") String displayPhoneNumber,
            @com.fasterxml.jackson.annotation.JsonProperty("phone_number_id") String phoneNumberId) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Contact(String wa_id, Profile profile) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Profile(String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(String id, String from, String timestamp, String type, Text text) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Text(String body) {}
}
