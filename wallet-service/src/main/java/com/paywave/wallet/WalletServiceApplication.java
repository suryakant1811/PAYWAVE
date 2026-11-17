package com.paywave.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;

@SpringBootApplication(
        scanBasePackages = {"com.paywave.wallet", "com.paywave.ledger", "com.paywave.web"},
        exclude = KafkaAutoConfiguration.class)
public class WalletServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(WalletServiceApplication.class, args);
    }
}
