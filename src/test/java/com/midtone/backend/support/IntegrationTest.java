package com.midtone.backend.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@Import({ContainersConfig.class, TestSupportConfig.class})
public abstract class IntegrationTest {
}
