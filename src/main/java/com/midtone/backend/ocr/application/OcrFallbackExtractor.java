package com.midtone.backend.ocr.application;

import java.time.YearMonth;
import java.util.List;

public interface OcrFallbackExtractor {

    List<OcrDraftParser.ParsedDraft> extract(byte[] image, String mimeType, YearMonth targetMonth);
}
