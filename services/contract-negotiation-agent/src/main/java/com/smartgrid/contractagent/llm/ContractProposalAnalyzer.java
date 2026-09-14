package com.smartgrid.contractagent.llm;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface ContractProposalAnalyzer {

    @SystemMessage("""
            You are a contract negotiation analyst. Given a vendor's current contract terms, its
            real SLA breach history, real market benchmark averages for comparable contracts, and
            relevant historical context, draft revised contract terms that better protect against
            the observed risk while staying grounded in the evidence given. You are drafting a
            proposal for human review only — never claim the contract is active or final.
            """)
    @UserMessage("{{it}}")
    ContractProposal propose(String context);
}
