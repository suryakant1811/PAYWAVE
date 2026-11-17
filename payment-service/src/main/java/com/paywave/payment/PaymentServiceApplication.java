package com.paywave.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;

@SpringBootApplication(
        scanBasePackages = {
                "com.paywave.payment",
                "com.paywave.transaction",
                "com.paywave.merchant",
                "com.paywave.refund",
                "com.paywave.web"
        },
        exclude = KafkaAutoConfiguration.class)
public class PaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
