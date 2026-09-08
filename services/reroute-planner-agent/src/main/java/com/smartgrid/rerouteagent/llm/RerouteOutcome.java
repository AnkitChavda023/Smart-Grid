package com.smartgrid.rerouteagent.llm;

import dev.langchain4j.model.output.structured.Description;

public record RerouteOutcome(
        @Description("vendor id selected for the reroute — must be a real vendorId returned by searchVendors, or null if none could be confidently selected") String selectedVendorId,
        @Description("quote id returned by acceptQuote, or null if no quote was accepted") String quoteId,
        @Description("confidence in this reroute decision, 0.0 to 1.0") double confidence,
        @Description("summary of the reasoning behind this decision") String summary
) {
}
