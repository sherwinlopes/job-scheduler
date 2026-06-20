package com.scheduler.worker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduler.model.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;

public class HttpJobExecutor implements JobExecutor {

    private static final Logger log = LoggerFactory.getLogger(HttpJobExecutor.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String execute(Job job) throws Exception {
        JsonNode config = objectMapper.readTree(job.getPayload());

        String url = config.get("url").asText();
        String method = config.has("method") ? config.get("method").asText() : "GET";

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT);

        if (config.has("headers")) {
            JsonNode headers = config.get("headers");
            Iterator<Map.Entry<String, JsonNode>> fields = headers.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                requestBuilder.header(entry.getKey(), entry.getValue().asText());
            }
        }

        String body = config.has("body") ? config.get("body").asText() : "";

        switch (method.toUpperCase()) {
            case "POST" -> requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body));
            case "PUT" -> requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(body));
            case "DELETE" -> requestBuilder.DELETE();
            default -> requestBuilder.GET();
        }

        HttpResponse<String> response = httpClient.send(requestBuilder.build(),
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        }

        log.info("HTTP job completed: {} {} -> {}", method, url, response.statusCode());
        return response.body();
    }
}
