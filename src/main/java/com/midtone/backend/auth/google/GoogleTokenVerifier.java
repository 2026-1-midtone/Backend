package com.midtone.backend.auth.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.midtone.backend.auth.AuthException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GoogleTokenVerifier {

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifier(@Value("${app.google.client-id}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    public GoogleUserInfo verify(String idTokenString) {
        GoogleIdToken idToken = parse(idTokenString);
        return toUserInfo(idToken);
    }

    private GoogleIdToken parse(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new AuthException(AuthException.ErrorCode.INVALID_GOOGLE_TOKEN);
            }
            return idToken;
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            throw new AuthException(AuthException.ErrorCode.GOOGLE_TOKEN_VERIFICATION_FAILED, e);
        }
    }

    private GoogleUserInfo toUserInfo(GoogleIdToken idToken) {
        GoogleIdToken.Payload payload = idToken.getPayload();

        return new GoogleUserInfo(
                payload.getSubject(),
                payload.getEmail(),
                (String) payload.get("name"),
                (String) payload.get("picture")
        );
    }
}
