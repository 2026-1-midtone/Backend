package com.midtone.backend.ocr.documentai;

import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import org.springframework.stereotype.Component;

@Component
public class AdcAccessTokenProvider implements DocumentAiAccessTokenProvider {

    private static final String CLOUD_PLATFORM_SCOPE = "https://www.googleapis.com/auth/cloud-platform";

    @Override
    public String getAccessToken() {
        try {
            GoogleCredentials credentials =
                    GoogleCredentials.getApplicationDefault().createScoped(CLOUD_PLATFORM_SCOPE);
            credentials.refreshIfExpired();
            return credentials.getAccessToken().getTokenValue();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "ADC 자격증명을 찾을 수 없습니다. gcloud auth application-default login을 실행하세요.", e);
        }
    }
}
