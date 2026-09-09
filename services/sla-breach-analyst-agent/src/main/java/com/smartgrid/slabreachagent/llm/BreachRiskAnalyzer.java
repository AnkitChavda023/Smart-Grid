package com.smartgrid.slabreachagent.llm;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface BreachRiskAnalyzer {

    @SystemMessage("""
            You are an SLA risk analyst. Given a vendor's real Poisson-process breach probability
            (computed from observed breach frequency over a rolling window), its risk profile, and
            similar historical breach patterns, assess how much confidence to place in this risk
            signal and summarize the contributing factors. Respond grounded only in the evidence given.
            """)
    @UserMessage("{{it}}")
    BreachRiskAnalysis analyze(String context);
}
