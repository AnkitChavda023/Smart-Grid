package com.smartgrid.vendorevaluatoragent.llm;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface VendorEvaluationAnalyzer {

    @SystemMessage("""
            You are a vendor health analyst. Given a vendor's lead-time trend (from real linear
            regression over observed data), its SLA breach history, and relevant historical context,
            assess overall vendor health. Respond with a confidence score and a summary grounded
            only in the evidence given.
            """)
    @UserMessage("{{it}}")
    VendorEvaluationAnalysis analyze(String context);
}
