package com.midtone.backend.ocr.documentai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import tools.jackson.databind.JsonNode;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class DocumentAiClientTest {

    private static final String EXPECTED_URL =
            "https://us-documentai.googleapis.com/v1/projects/437332095325/locations/us/processors/cbb47f2525db9dc0:process";

    private MockRestServiceServer server;
    private DocumentAiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new DocumentAiClient(
                builder, () -> "test-token",
                "https://us-documentai.googleapis.com", "437332095325", "us", "cbb47f2525db9dc0",
                "shiftmate-504210");
    }

    @Test
    void 프로세서에_이미지를_전송하고_document_노드를_반환한다() {
        byte[] image = new byte[] {1, 2, 3};
        server.expect(requestTo(EXPECTED_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andExpect(header("x-goog-user-project", "shiftmate-504210"))
                .andExpect(jsonPath("$.rawDocument.content").value(Base64.getEncoder().encodeToString(image)))
                .andExpect(jsonPath("$.rawDocument.mimeType").value("image/png"))
                .andRespond(withSuccess("{\"document\":{\"text\":\"hello\"}}", MediaType.APPLICATION_JSON));

        JsonNode document = client.process(image, "image/png");

        assertEquals("hello", document.path("text").asText());
        server.verify();
    }

    @Test
    void 서버_오류면_DocumentAiCallException을_던진다() {
        server.expect(requestTo(EXPECTED_URL)).andRespond(withServerError());
        assertThrows(DocumentAiClient.DocumentAiCallException.class,
                () -> client.process(new byte[] {1}, "image/png"));
    }
}
