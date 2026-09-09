package com.smartgrid.forecasteragent.llm;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface SeasonalAdjustmentAnalyzer {

    @SystemMessage("""
            You are a demand forecasting analyst. You are given a statistical exponential-smoothing
            baseline for a SKU's daily demand, computed from real order history, plus retrieved
            contextual signals. Propose a multiplicative adjustment factor to account for seasonality
            or context the statistical baseline can't see, grounded only in the evidence given. If
            there is no clear seasonal signal, return an adjustment factor of 1.0.
            """)
    @UserMessage("{{it}}")
    SeasonalAdjustment analyze(String context);
}
