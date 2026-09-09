package com.smartgrid.vendorevaluatoragent.llm;

import com.smartgrid.vendorevaluatoragent.config.VendorEvaluatorProperties;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LlmConfig {

    @Bean
    public ChatModel chatModel(VendorEvaluatorProperties properties) {
        return OpenAiChatModel.builder()
                .apiKey(properties.openai().apiKey())
                .baseUrl(properties.openai().baseUrl())
                .modelName(properties.openai().chatModel())
                .supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA)
                .strictJsonSchema(true)
                .build();
    }

    @Bean
    public VendorEvaluationAnalyzer vendorEvaluationAnalyzer(ChatModel chatModel) {
        return AiServices.create(VendorEvaluationAnalyzer.class, chatModel);
    }
}
