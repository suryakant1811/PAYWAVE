package com.paywave.web.outbox;

public interface OutboxDispatcher {
    void dispatch(OutboxEvent event);
}
