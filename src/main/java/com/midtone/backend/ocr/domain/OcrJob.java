package com.midtone.backend.ocr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "ocr_jobs")
public class OcrJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OcrJobStatus status;

    @Lob
    @Column(columnDefinition = "MEDIUMBLOB")
    private byte[] image;

    @Column(name = "image_mime", length = 30)
    private String imageMime;

    @Column(name = "target_month", nullable = false, length = 7)
    private String targetMonth;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected OcrJob() {
    }

    public OcrJob(Long userId, byte[] image, String imageMime, String targetMonth) {
        this.userId = userId;
        this.image = image;
        this.imageMime = imageMime;
        this.targetMonth = targetMonth;
        this.status = OcrJobStatus.PENDING;
    }

    public void markProcessing() {
        this.status = OcrJobStatus.PROCESSING;
        this.errorMessage = null;
    }

    public void markCompleted() {
        this.status = OcrJobStatus.COMPLETED;
    }

    public void markFailed(String errorMessage) {
        this.status = OcrJobStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public void markConfirmed() {
        this.status = OcrJobStatus.CONFIRMED;
        this.image = null;
    }

    public boolean isOwnedBy(long userId) {
        return this.userId != null && this.userId == userId;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public OcrJobStatus getStatus() { return status; }
    public byte[] getImage() { return image; }
    public String getImageMime() { return imageMime; }
    public String getTargetMonth() { return targetMonth; }
    public String getErrorMessage() { return errorMessage; }
}
