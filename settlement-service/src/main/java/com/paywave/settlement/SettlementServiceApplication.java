package com.paywave.settlement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;

@SpringBootApplication(scanBasePackages = {"com.paywave.settlement", "com.paywave.web"}, exclude = KafkaAutoConfiguration.class)
public class SettlementServiceApplication {
    public static void main(String[] args) { SpringApplication.run(SettlementServiceApplication.class, args); }
}
