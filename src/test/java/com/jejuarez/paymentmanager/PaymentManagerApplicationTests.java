package com.jejuarez.paymentmanager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("PaymentManagerApplication")
@SpringBootTest
@ActiveProfiles("test")
class PaymentManagerApplicationTests {

    @Test
    @DisplayName("Application context starts with the test profile")
    void contextLoads() {
    }

}
