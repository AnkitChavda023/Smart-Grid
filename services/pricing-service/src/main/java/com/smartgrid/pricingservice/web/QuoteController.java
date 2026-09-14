package com.smartgrid.pricingservice.web;

import com.smartgrid.pricingservice.domain.QuoteStatus;
import com.smartgrid.pricingservice.dto.CreateQuoteRequest;
import com.smartgrid.pricingservice.dto.QuoteResponse;
import com.smartgrid.pricingservice.service.QuoteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/quotes")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @PostMapping
    public ResponseEntity<QuoteResponse> createQuote(@Valid @RequestBody CreateQuoteRequest request) {
        var quote = quoteService.createQuote(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(QuoteResponse.from(quote));
    }

    @GetMapping("/{id}")
    public QuoteResponse getQuote(@PathVariable UUID id) {
        return QuoteResponse.from(quoteService.getQuote(id));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<Void> acceptQuote(@PathVariable UUID id) {
        quoteService.acceptQuote(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<QuoteResponse> listQuotes(@RequestParam String orderId, @RequestParam(required = false) QuoteStatus status) {
        return quoteService.listQuotes(orderId, status).stream().map(QuoteResponse::from).toList();
    }
}
