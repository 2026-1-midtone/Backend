package com.midtone.backend.chat.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ChatReferenceCatalogTest {

    private final ChatReferenceCatalog catalog = new ChatReferenceCatalog();

    @Test
    void 수면_질문은_SLEEP_도메인으로_라우팅한다() {
        ChatReference reference = catalog.match("나 어제 얼마나 잤어?");

        assertEquals(ChatDomain.SLEEP, reference.domain());
        assertTrue(reference.excerpt().contains("recentSleepLogs"));
    }

    @Test
    void 낮잠_질문은_여전히_NAP으로_라우팅한다() {
        assertEquals(ChatDomain.NAP, catalog.match("낮잠 자도 돼?").domain());
    }

    @Test
    void 분류되지_않으면_NUTRITION이_기본값이다() {
        assertEquals(ChatDomain.NUTRITION, catalog.match("안녕하세요").domain());
    }
}
