package com.paywave.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paywave.common.messaging.EventTypes;
import com.paywave.notification.repo.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock NotificationRepository repository;

    @Test
    void createsEmailSmsAndInAppForEachUser() throws Exception {
        NotificationService service = new NotificationService(repository);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode payload = mapper.createObjectNode();
        payload.put("amount", "25.00");
        ArrayNode users = payload.putArray("userIds");
        users.add(UUID.randomUUID().toString());
        when(repository.findByDedupKey(any())).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        int created = service.accept(UUID.randomUUID(), EventTypes.PAYMENT_COMPLETED, payload);
        assertThat(created).isEqualTo(3);
        verify(repository, times(3)).saveAndFlush(any());
    }
}
