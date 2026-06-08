package com.codezilla.crm.message;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-process pub/sub for new messages on a lead. Each lead gets its own
 * multicast sink lazily; subscribers receive every event published after they join.
 *
 * Single-instance only — fine for a monolith. If we ever scale horizontally,
 * replace with Redis pub/sub.
 */
@Component
public class MessageEventBus {

    private final ConcurrentHashMap<UUID, Sinks.Many<Object>> sinks = new ConcurrentHashMap<>();

    public void publish(UUID leadId, Object payload) {
        sink(leadId).tryEmitNext(payload);
    }

    public Flux<Object> subscribe(UUID leadId) {
        return sink(leadId).asFlux();
    }

    private Sinks.Many<Object> sink(UUID leadId) {
        return sinks.computeIfAbsent(leadId,
                k -> Sinks.many().multicast().directBestEffort());
    }
}
