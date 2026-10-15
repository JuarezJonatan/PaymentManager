package com.jejuarez.paymentmanager.service;

import com.jejuarez.paymentmanager.dto.request.CreatePaymentRequest;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class PaymentRequestHasher {

    private static final String ALGORITHM = "SHA-256";
    private static final int AMOUNT_SCALE = 2;

    public String hash(CreatePaymentRequest request) {
        String canonicalRequest = String.join("\n",
                request.merchantId(),
                request.amount().setScale(AMOUNT_SCALE, RoundingMode.UNNECESSARY).toPlainString(),
                request.currency().name(),
                request.simulationResult().name());
        return HexFormat.of().formatHex(sha256(canonicalRequest));
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance(ALGORITHM).digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(ALGORITHM + " is not available", exception);
        }
    }
}
