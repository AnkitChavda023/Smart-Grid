package com.smartgrid.disruptionagent.llm;

import dev.langchain4j.model.output.structured.Description;

public record DisruptionAnalysis(
        @Description("confidence this is a genuine supply disruption, 0.0 to 1.0") double confidence,
        @Description("step-by-step reasoning explaining the confidence score, grounded in the signals and historical context given") String reasoningTrace
) {
}
