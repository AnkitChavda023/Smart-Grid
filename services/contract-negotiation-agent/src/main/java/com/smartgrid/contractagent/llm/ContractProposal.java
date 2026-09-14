package com.smartgrid.contractagent.llm;

import dev.langchain4j.model.output.structured.Description;

public record ContractProposal(
        @Description("proposed revised contract terms text, grounded in the current terms, breach history, and market benchmarks given") String proposedTerms,
        @Description("summary of what changed and why, for a PLANNER/ADMIN reviewing this draft") String summary,
        @Description("confidence in this proposal, 0.0 to 1.0") double confidence
) {
}
