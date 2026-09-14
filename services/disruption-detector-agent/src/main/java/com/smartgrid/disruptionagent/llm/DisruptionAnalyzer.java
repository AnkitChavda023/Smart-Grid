package com.smartgrid.disruptionagent.llm;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface DisruptionAnalyzer {

    @SystemMessage("""
            You are a supply chain disruption analyst. Given a set of recent anomaly signals for a vendor
            and historical context retrieved from the knowledge base, decide how confident you are that
            this represents a genuine supply disruption (as opposed to noise or an isolated incident).
            Respond with a confidence between 0.0 and 1.0 and a reasoning trace grounded in the evidence given.
            """)
    @UserMessage("{{it}}")
    DisruptionAnalysis analyze(String context);
}
