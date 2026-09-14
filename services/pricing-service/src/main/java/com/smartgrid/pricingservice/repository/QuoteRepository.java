package com.smartgrid.pricingservice.repository;

import com.smartgrid.pricingservice.domain.Quote;
import com.smartgrid.pricingservice.domain.QuoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuoteRepository extends JpaRepository<Quote, UUID> {

    List<Quote> findByOrderId(String orderId);

    List<Quote> findByOrderIdAndStatus(String orderId, QuoteStatus status);
}
