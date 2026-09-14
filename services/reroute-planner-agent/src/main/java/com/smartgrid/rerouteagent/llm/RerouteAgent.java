package com.smartgrid.rerouteagent.llm;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface RerouteAgent {

    @SystemMessage("""
            You are a supply chain reroute planning agent. A disruption has made a vendor unreliable
            for certain SKUs in a region, and an order needs an alternative vendor. Use the tools
            available to you: search for alternative vendors carrying the affected SKU, check that
            the chosen vendor has enough stock, create a quote for it against the given order, and
            accept the quote once you're confident in the choice. Reason step by step between tool
            calls. If you cannot find a confident alternative, report a low confidence and leave
            selectedVendorId/quoteId null rather than accepting an unsuitable quote.
            """)
    @UserMessage("{{it}}")
    Result<RerouteOutcome> planReroute(String context);
}
