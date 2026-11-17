package com.paywave.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(
        scanBasePackages = {
                "com.paywave.account",
                "com.paywave.auth",
                "com.paywave.user",
                "com.paywave.notification",
                "com.paywave.web"
        },
        exclude = KafkaAutoConfiguration.class)
@EntityScan(basePackages = {"com.paywave.auth", "com.paywave.user", "com.paywave.notification"})
@EnableJpaRepositories(basePackages = {"com.paywave.auth", "com.paywave.user", "com.paywave.notification"})
public class AccountServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }
}
