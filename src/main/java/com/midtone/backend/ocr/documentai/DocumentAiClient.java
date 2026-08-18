package com.midtone.backend.ocr.documentai;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.MissingNode;
import java.util.Base64;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class DocumentAiClient {

    public static class DocumentAiCallException extends RuntimeException {
        public DocumentAiCallException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private final RestClient restClient;
    private final DocumentAiAccessTokenProvider tokenProvider;
    private final String processUrl;
    private final String quotaProject;

    public DocumentAiClient(
            RestClient.Builder restClientBuilder,
            DocumentAiAccessTokenProvider tokenProvider,
            @Value("${app.documentai.endpoint}") String endpoint,
            @Value("${app.documentai.project-number}") String projectNumber,
            @Value("${app.documentai.location}") String location,
            @Value("${app.documentai.processor-id}") String processorId,
            @Value("${app.documentai.quota-project}") String quotaProject) {
        this.restClient = restClientBuilder.build();
        this.tokenProvider = tokenProvider;
        this.processUrl = "%s/v1/projects/%s/locations/%s/processors/%s:process"
                .formatted(endpoint, projectNumber, location, processorId);
        this.quotaProject = quotaProject;
    }

    public JsonNode process(byte[] image, String mimeType) {
        try {
            JsonNode response = restClient.post()
                    .uri(processUrl)
                    .header("Authorization", "Bearer " + tokenProvider.getAccessToken())
                    .header("x-goog-user-project", quotaProject)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("rawDocument", Map.of(
                            "content", Base64.getEncoder().encodeToString(image),
                            "mimeType", mimeType)))
                    .retrieve()
                    .body(JsonNode.class);
            return response == null ? MissingNode.getInstance() : response.path("document");
        } catch (RestClientException e) {
            throw new DocumentAiCallException("Document AI 호출에 실패했습니다.", e);
        }
    }
}
