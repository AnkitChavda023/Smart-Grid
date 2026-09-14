package com.smartgrid.rerouteagent.domain;

public record DagTraceNode(int stepIndex, String toolName, String input, String output, long latencyMs, String llmReasoning) {
}
