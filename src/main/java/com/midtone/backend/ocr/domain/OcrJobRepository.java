package com.midtone.backend.ocr.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OcrJobRepository extends JpaRepository<OcrJob, Long> {

    List<OcrJob> findByStatus(OcrJobStatus status);
}
