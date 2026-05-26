package za.co.tuks.amrdashboard.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmbeddingService {

    @Value("${embedding.api-url:https://api.openai.com/v1/embeddings}")
    private String apiUrl;

    @Value("${embedding.api-key:}")
    private String apiKey;

    @Value("${embedding.model:text-embedding-3-small}")
    private String model;

    @Value("${embedding.dimensions:1024}")
    private int dimensions;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @PostConstruct
    void init() {
        if (isOllama()) {
            ensureOllamaModel();
        }
    }

    private boolean isOllama() {
        return apiUrl != null && apiUrl.contains("11434");
    }

    private void ensureOllamaModel() {
        try {
            String ollamaBase = apiUrl.replace("/v1/embeddings", "");
            HttpRequest checkReq = HttpRequest.newBuilder(URI.create(ollamaBase + "/api/tags"))
                    .timeout(Duration.ofSeconds(5))
                    .GET().build();
            HttpResponse<String> checkRes = http.send(checkReq, HttpResponse.BodyHandlers.ofString());

            if (checkRes.body().contains(model)) {
                log.info("[Embedding] Ollama model '{}' already available", model);
                return;
            }

            log.info("[Embedding] Pulling Ollama model '{}' — this may take a few minutes on first run...", model);
            String pullJson = mapper.writeValueAsString(Map.of("name", model, "stream", false));
            HttpRequest pullReq = HttpRequest.newBuilder(URI.create(ollamaBase + "/api/pull"))
                    .timeout(Duration.ofMinutes(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(pullJson))
                    .build();
            HttpResponse<String> pullRes = http.send(pullReq, HttpResponse.BodyHandlers.ofString());

            if (pullRes.statusCode() < 400) {
                log.info("[Embedding] Ollama model '{}' pulled successfully", model);
            } else {
                log.error("[Embedding] Failed to pull Ollama model: {}", pullRes.body());
            }
        } catch (Exception e) {
            log.warn("[Embedding] Could not auto-pull Ollama model (is Ollama running?): {}", e.getMessage());
        }
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    private Map<String, Object> buildRequestBody(Object input) {
        var body = new java.util.HashMap<String, Object>();
        body.put("input", input);
        body.put("model", model);
        if (!isOllama()) {
            body.put("dimensions", dimensions);
        }
        return body;
    }

    public int getDimensions() {
        return dimensions;
    }

    public float[] embed(String text) {
        if (!isConfigured()) {
            log.warn("[Embedding] API key not configured — skipping embedding");
            return null;
        }

        try {
            Map<String, Object> body = buildRequestBody(text);
            String json = mapper.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 400) {
                log.error("[Embedding] API error {}: {}", res.statusCode(), res.body());
                return null;
            }

            JsonNode root = mapper.readTree(res.body());
            JsonNode data = root.path("data");
            if (data.isArray() && !data.isEmpty()) {
                JsonNode embeddingNode = data.get(0).path("embedding");
                float[] embedding = new float[embeddingNode.size()];
                for (int i = 0; i < embeddingNode.size(); i++) {
                    embedding[i] = embeddingNode.get(i).floatValue();
                }
                return embedding;
            }
            return null;
        } catch (Exception e) {
            log.error("[Embedding] Call failed: {}", e.getMessage(), e);
            return null;
        }
    }

    public List<float[]> embedBatch(List<String> texts) {
        if (!isConfigured() || texts.isEmpty()) return List.of();

        List<float[]> results = new ArrayList<>();
        int batchSize = 20;

        for (int i = 0; i < texts.size(); i += batchSize) {
            List<String> batch = texts.subList(i, Math.min(i + batchSize, texts.size()));
            try {
                Map<String, Object> body = buildRequestBody(batch);
                String json = mapper.writeValueAsString(body);
                HttpRequest req = HttpRequest.newBuilder(URI.create(apiUrl))
                        .timeout(Duration.ofSeconds(60))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() >= 400) {
                    log.error("[Embedding] Batch API error {}: {}", res.statusCode(), res.body());
                    for (int j = 0; j < batch.size(); j++) results.add(null);
                    continue;
                }

                JsonNode root = mapper.readTree(res.body());
                JsonNode data = root.path("data");
                for (int j = 0; j < data.size(); j++) {
                    JsonNode embeddingNode = data.get(j).path("embedding");
                    float[] embedding = new float[embeddingNode.size()];
                    for (int k = 0; k < embeddingNode.size(); k++) {
                        embedding[k] = embeddingNode.get(k).floatValue();
                    }
                    results.add(embedding);
                }
            } catch (Exception e) {
                log.error("[Embedding] Batch call failed: {}", e.getMessage());
                for (int j = 0; j < batch.size(); j++) results.add(null);
            }
        }
        return results;
    }
}
