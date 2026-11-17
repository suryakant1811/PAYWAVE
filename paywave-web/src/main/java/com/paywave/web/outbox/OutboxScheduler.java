package com.paywave.web.outbox;

import org.springframework.scheduling.annotation.Scheduled;

public class OutboxScheduler {

    private final OutboxRelay relay;

    public OutboxScheduler(OutboxRelay relay) {
        this.relay = relay;
    }

    @Scheduled(fixedDelayString = "${paywave.messaging.relay-delay-ms:2000}")
    public void publish() {
        relay.publishPending();
    }
}
