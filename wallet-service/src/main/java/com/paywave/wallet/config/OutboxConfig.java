package com.paywave.wallet.config;

import com.paywave.web.http.InternalRestClientFactory;
import com.paywave.web.outbox.OutboxDispatcher;
import com.paywave.web.outbox.OutboxEventRepository;
import com.paywave.web.outbox.OutboxRelay;
import com.paywave.web.outbox.OutboxScheduler;
import com.paywave.web.outbox.OutboxWriter;
import com.paywave.web.outbox.RoutingOutboxDispatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableScheduling
@EntityScan(basePackages = {"com.paywave.wallet", "com.paywave.ledger", "com.paywave.web.outbox"})
@EnableJpaRepositories(basePackages = {"com.paywave.wallet", "com.paywave.ledger", "com.paywave.web.outbox"})
public class OutboxConfig {

    @Bean
    OutboxWriter outboxWriter(OutboxEventRepository repository, ObjectMapper objectMapper) {
        return new OutboxWriter(repository, objectMapper);
    }

    @Bean
    OutboxDispatcher outboxDispatcher(@Value("${paywave.messaging.mode:log}") String mode,
                                      @Value("${paywave.clients.notification-service-url:http://localhost:8081}") String notificationUrl,
                                      InternalRestClientFactory factory,
                                      ObjectMapper objectMapper,
                                      ObjectProvider<KafkaTemplate<String, String>> kafkaTemplate) {
        return RoutingOutboxDispatcher.create(mode, notificationUrl, factory, objectMapper, kafkaTemplate);
    }

    @Bean
    OutboxRelay outboxRelay(OutboxEventRepository repository,
                            OutboxDispatcher dispatcher,
                            PlatformTransactionManager transactionManager) {
        return new OutboxRelay(repository, dispatcher, transactionManager);
    }

    @Bean
    OutboxScheduler outboxScheduler(OutboxRelay relay) {
        return new OutboxScheduler(relay);
    }
}
