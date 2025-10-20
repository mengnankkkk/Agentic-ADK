package com.alibaba.agentic.core.models;

import com.alibaba.agentic.core.engine.delegation.domain.LlmRequest;
import com.alibaba.agentic.core.engine.delegation.domain.LlmResponse;
import com.alibaba.agentic.core.executor.InvokeMode;
import com.alibaba.agentic.core.executor.SystemContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Minimal Cerebras Cloud LLM adapter.
 * - Non-streaming chat completion is supported.
 * - Streaming (SSE) is not implemented to avoid extra deps; will fall back to non-stream.
 *
 * Use by setting LlmRequest.model to "cerebras" and modelName to a concrete model (e.g., "llama3.1-8b").
 */
@Slf4j
@Component
public class CerebrasLlm implements BasicLlm {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String model() {
        return "cerebras";
    }

    @Override
    public Flowable<LlmResponse> invoke(LlmRequest llmRequest, SystemContext systemContext) {
        // Fallback: even if SSE requested, use non-stream for now to avoid new deps
        return Flowable.fromCallable(() -> callChatOnce(llmRequest));
    }

    private LlmResponse callChatOnce(LlmRequest req) throws Exception {
        String apiKey = firstNonEmpty(
                System.getProperty("ali.agentic.adk.flownode.cerebras.apiKey"),
                System.getenv("CEREBRAS_API_KEY")
        );
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("CEREBRAS_API_KEY is required for CerebrasLlm");
        }
        String baseUrl = firstNonEmpty(
                System.getProperty("CEREBRAS_BASE_URL"),
                System.getenv("CEREBRAS_BASE_URL"),
                "https://api.cerebras.ai"
        );
        String modelName = req.getModelName() != null ? req.getModelName() : req.getModel();
        if (modelName == null || modelName.isEmpty()) {
            modelName = "llama3.1-8b";
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", modelName);
        body.put("messages", toMessages(req));
        // optional knobs
        if (req.getMaxTokens() != null) body.put("max_completion_tokens", req.getMaxTokens());
        if (req.getTemperature() != null) body.put("temperature", req.getTemperature());
        if (req.getTopP() != null) body.put("top_p", req.getTopP());
        if (req.getStop() != null) body.put("stop", req.getStop());
        if (req.getUser() != null) body.put("user", req.getUser());

        String json = MAPPER.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(normalizeUrl(baseUrl) + "/v1/chat/completions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() / 100 != 2) {
            log.warn("Cerebras call failed: {} - {}", resp.statusCode(), resp.body());
            LlmResponse.ErrorInfo error = new LlmResponse.ErrorInfo();
            error.setCode(String.valueOf(resp.statusCode()));
            error.setType("cerebras_api_error");
            error.setMessage(resp.body());
            return new LlmResponse().setError(error);
        }
        return parseChatCompletion(resp.body());
    }

    private static List<Map<String, Object>> toMessages(LlmRequest llmRequest) {
        if (llmRequest.getMessages() == null) return List.of();
        return llmRequest.getMessages().stream().map(m -> {
            Map<String, Object> mm = new LinkedHashMap<>();
            mm.put("role", m.getRole() == null ? "user" : m.getRole());
            mm.put("content", m.getContent());
            return mm;
        }).collect(Collectors.toList());
    }

    private LlmResponse parseChatCompletion(String body) throws Exception {
        JsonNode root = MAPPER.readTree(body);
        LlmResponse response = new LlmResponse();
        if (root.hasNonNull("id")) response.setId(root.get("id").asText());

        // usage
        if (root.has("usage") && root.get("usage").isObject()) {
            JsonNode u = root.get("usage");
            LlmResponse.Usage usage = new LlmResponse.Usage();
            if (u.hasNonNull("prompt_tokens")) usage.setPromptTokens(u.get("prompt_tokens").asInt());
            if (u.hasNonNull("completion_tokens")) usage.setCompletionTokens(u.get("completion_tokens").asInt());
            if (u.hasNonNull("total_tokens")) usage.setTotalTokens(u.get("total_tokens").asInt());
            response.setUsage(usage);
        }

        // choices
        if (root.has("choices") && root.get("choices").isArray() && root.get("choices").size() > 0) {
            JsonNode c0 = root.get("choices").get(0);
            LlmResponse.Choice c = new LlmResponse.Choice();
            if (c0.hasNonNull("finish_reason")) c.setFinishReason(c0.get("finish_reason").asText());
            if (c0.hasNonNull("index")) c.setIndex(c0.get("index").asInt());
            JsonNode message = c0.get("message");
            if (message != null && message.hasNonNull("content")) {
                String content = message.get("content").asText();
                c.setText(content);
                LlmResponse.Message m = new LlmResponse.Message();
                m.setRole(message.hasNonNull("role") ? message.get("role").asText() : "assistant");
                m.setContent(content);
                c.setMessage(m);
            }
            response.setChoices(List.of(c));
        }
        return response;
    }

    private static String normalizeUrl(String base) {
        if (base.endsWith("/")) return base.substring(0, base.length() - 1);
        return base;
    }

    @SafeVarargs
    private static <T> T firstNonEmpty(T... vals) {
        for (T v : vals) {
            if (v instanceof String) {
                if (v != null && !((String) v).isEmpty()) return v;
            } else if (v != null) return v;
        }
        return null;
    }
}

