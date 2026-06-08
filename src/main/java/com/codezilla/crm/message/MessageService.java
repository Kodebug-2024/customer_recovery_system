package com.codezilla.crm.message;

import com.codezilla.crm.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MessageService {

    private final MessageRepository repo;

    public MessageService(MessageRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public Message record(UUID leadId, MessageDirection direction, String channel, String content) {
        Message m = new Message();
        m.setLeadId(leadId);
        m.setDirection(direction);
        m.setChannel(channel);
        m.setContent(content);
        return repo.save(m);
    }

    @Transactional(readOnly = true)
    public List<Message> conversation(UUID leadId) {
        return repo.findAllByTenantIdAndLeadIdOrderByCreatedAtAsc(TenantContext.require(), leadId);
    }
}
