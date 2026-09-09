package com.smartgrid.slabreachagent.llm;

import dev.langchain4j.model.output.structured.Description;

public record BreachRiskAnalysis(
        @Description("confidence in this risk assessment, 0.0 to 1.0") double confidence,
        @Description("summary of the vendor's breach risk: frequency pattern, contributing factors, and recommendation") String summary
) {
}
