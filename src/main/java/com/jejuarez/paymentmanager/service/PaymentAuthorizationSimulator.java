package com.jejuarez.paymentmanager.service;

import com.jejuarez.paymentmanager.dto.request.SimulationResult;
import com.jejuarez.paymentmanager.persistence.entity.PaymentStatus;
import org.springframework.stereotype.Component;

@Component
public class PaymentAuthorizationSimulator {

    public PaymentStatus authorize(SimulationResult simulationResult) {
        return switch (simulationResult) {
            case APPROVED -> PaymentStatus.APPROVED;
            case REJECTED -> PaymentStatus.REJECTED;
        };
    }
}
