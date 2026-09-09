package com.smartgrid.forecasteragent.llm;

import dev.langchain4j.model.output.structured.Description;

public record SeasonalAdjustment(
        @Description("multiplicative adjustment to apply to the statistical baseline based on seasonal/contextual signals, typically 0.5 to 2.0; 1.0 means no adjustment") double adjustmentFactor,
        @Description("confidence in this adjustment, 0.0 to 1.0") double confidence,
        @Description("summary explaining the seasonal reasoning behind the adjustment") String summary
) {
}
