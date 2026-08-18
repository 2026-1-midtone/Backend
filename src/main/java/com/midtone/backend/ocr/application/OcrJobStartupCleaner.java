package com.midtone.backend.ocr.application;

import com.midtone.backend.ocr.domain.OcrJob;
import com.midtone.backend.ocr.domain.OcrJobRepository;
import com.midtone.backend.ocr.domain.OcrJobStatus;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OcrJobStartupCleaner implements ApplicationRunner {

    private static final String INTERRUPTED_MESSAGE = "서버 재시작으로 분석이 중단되었습니다. 다시 시도해 주세요.";

    private final OcrJobRepository ocrJobRepository;

    public OcrJobStartupCleaner(OcrJobRepository ocrJobRepository) {
        this.ocrJobRepository = ocrJobRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<OcrJob> stuckJobs = ocrJobRepository.findByStatus(OcrJobStatus.PROCESSING);
        stuckJobs.forEach(job -> job.markFailed(INTERRUPTED_MESSAGE));
        ocrJobRepository.saveAll(stuckJobs);
    }
}
