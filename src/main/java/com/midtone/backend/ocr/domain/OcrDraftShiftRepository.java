package com.midtone.backend.ocr.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OcrDraftShiftRepository extends JpaRepository<OcrDraftShift, Long> {

    List<OcrDraftShift> findByJobIdOrderByWorkDateAsc(Long jobId);

    void deleteByJobId(Long jobId);
}
