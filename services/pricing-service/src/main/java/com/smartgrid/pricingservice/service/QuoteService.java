package com.smartgrid.pricingservice.service;

import com.smartgrid.commons.exception.ConflictException;
import com.smartgrid.commons.exception.ResourceNotFoundException;
import com.smartgrid.pricingservice.domain.Quote;
import com.smartgrid.pricingservice.domain.QuoteLineItem;
import com.smartgrid.pricingservice.domain.QuoteStatus;
import com.smartgrid.pricingservice.dto.CreateQuoteRequest;
import com.smartgrid.pricingservice.dto.QuoteItemRequest;
import com.smartgrid.pricingservice.messaging.QuoteEventPublisher;
import com.smartgrid.pricingservice.repository.QuoteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class QuoteService {

    private final QuoteRepository quoteRepository;
    private final PriceLookupService priceLookupService;
    private final QuoteRedisService quoteRedisService;
    private final QuoteEventPublisher publisher;
    private final long quoteTtlSeconds;

    public QuoteService(
            QuoteRepository quoteRepository,
            PriceLookupService priceLookupService,
            QuoteRedisService quoteRedisService,
            QuoteEventPublisher publisher,
            @Value("${smartgrid.pricing.quote-ttl-seconds:1800}") long quoteTtlSeconds
    ) {
        this.quoteRepository = quoteRepository;
        this.priceLookupService = priceLookupService;
        this.quoteRedisService = quoteRedisService;
        this.publisher = publisher;
        this.quoteTtlSeconds = quoteTtlSeconds;
    }

    @Transactional
    public Quote createQuote(CreateQuoteRequest request) {
        Duration ttl = Duration.ofSeconds(quoteTtlSeconds);
        Quote quote = new Quote(request.orderId(), request.vendorId(), Instant.now().plus(ttl));

        for (QuoteItemRequest item : request.items()) {
            double unitPrice = priceLookupService.currentPrice(item.skuId(), LocalDate.now());
            quote.addItem(new QuoteLineItem(item.skuId(), item.quantity(), unitPrice));
        }

        Quote saved = quoteRepository.save(quote);
        quoteRedisService.markActive(saved.getId(), ttl);
        publisher.publishQuoteCreated(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public Quote getQuote(UUID id) {
        return quoteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quote", id.toString()));
    }

    @Transactional
    public void acceptQuote(UUID id) {
        if (!quoteRedisService.isActive(id)) {
            throw new ConflictException("Quote " + id + " is no longer active");
        }
        if (!quoteRedisService.tryAcceptLock(id)) {
            throw new ConflictException("Quote " + id + " has already been accepted");
        }

        Quote quote = getQuote(id);
        quote.setStatus(QuoteStatus.ACCEPTED);
        quoteRepository.save(quote);
        publisher.publishQuoteAccepted(quote);
    }

    @Transactional(readOnly = true)
    public List<Quote> listQuotes(String orderId, QuoteStatus status) {
        return status == null ? quoteRepository.findByOrderId(orderId) : quoteRepository.findByOrderIdAndStatus(orderId, status);
    }

    @Transactional
    public void handleExpiry(UUID quoteId) {
        quoteRepository.findById(quoteId).ifPresent(quote -> {
            if (quote.getStatus() == QuoteStatus.ACTIVE) {
                quote.setStatus(QuoteStatus.EXPIRED);
                quoteRepository.save(quote);
                publisher.publishQuoteExpired(quote);
            }
        });
    }
}
