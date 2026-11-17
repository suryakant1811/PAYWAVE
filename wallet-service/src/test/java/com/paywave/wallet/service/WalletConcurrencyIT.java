package com.paywave.wallet.service;

import com.paywave.wallet.domain.OwnerType;
import com.paywave.wallet.domain.Wallet;
import com.paywave.wallet.repo.WalletRepository;
import com.paywave.wallet.web.WalletMutationRequest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@Testcontainers
@SpringBootTest
class WalletConcurrencyIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("paywave.security.jwt-secret", () -> "dev-only-paywave-jwt-secret-change-me-32b");
        registry.add("paywave.security.internal-api-key", () -> "test-internal-key");
        registry.add("paywave.messaging.mode", () -> "log");
    }

    @Autowired WalletRepository wallets;
    @Autowired WalletService walletService;

    @Test
    void concurrentDebitsCannotOverdraw() throws Exception {
        Wallet wallet = wallets.save(Wallet.open(UUID.randomUUID(), OwnerType.CUSTOMER, "INR",
                new BigDecimal("100000.00"), new BigDecimal("500000.00"), new BigDecimal("50000.00")));
        wallet.setBalance(new BigDecimal("100.00"));
        wallets.save(wallet);
        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        for (int i = 0; i < threads; i++) {
            int n = i;
            pool.submit(() -> {
                try {
                    start.await();
                    walletService.debit(new WalletMutationRequest(wallet.getOwnerId(), "CUSTOMER", new BigDecimal("10.00"),
                            "debit-" + n, "TRANSFER", UUID.randomUUID(), "INR", wallet.getOwnerId()));
                    success.incrementAndGet();
                } catch (RuntimeException ex) {
                    rejected.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        done.await();
        pool.shutdown();
        Wallet reloaded = wallets.findByOwnerId(wallet.getOwnerId()).orElseThrow();
        assertThat(success.get()).isEqualTo(10);
        assertThat(rejected.get()).isZero();
        assertThat(reloaded.getBalance()).isEqualByComparingTo("0.00");
    }
}
