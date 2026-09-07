package com.smartgrid.pricingservice.config;

import com.smartgrid.pricingservice.service.QuoteRedisService;
import com.smartgrid.pricingservice.service.QuoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class QuoteExpiryListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(QuoteExpiryListener.class);

    private final QuoteService quoteService;

    public QuoteExpiryListener(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = new String(message.getBody(), StandardCharsets.UTF_8);
        if (!QuoteRedisService.isActiveKey(expiredKey)) {
            return;
        }
        try {
            UUID quoteId = QuoteRedisService.quoteIdFromActiveKey(expiredKey);
            quoteService.handleExpiry(quoteId);
        } catch (IllegalArgumentException e) {
            log.warn("Ignoring malformed expired quote key: {}", expiredKey);
        }
    }
}
