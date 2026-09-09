package com.smartgrid.vendorevaluatoragent.llm;

import dev.langchain4j.model.output.structured.Description;

public record VendorEvaluationAnalysis(
        @Description("confidence in this health assessment, 0.0 to 1.0") double confidence,
        @Description("summary of the vendor's health: trend, breach history, and overall recommendation") String summary
) {
}
