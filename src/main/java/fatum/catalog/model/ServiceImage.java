package fatum.catalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "SERVICE_PORTAFOLIO")
public class ServiceImage {

    @Column(name = "IMAGE_KEY", nullable = false, length = 512)
    private String imageKey;

    @Column(name = "ORIGINAL_FILENAME", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "CONTENT_TYPE", nullable = false, length = 100)
    private String contentType;

    @Column(name = "FILE_SIZE", nullable = false)
    private long fileSize;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", length = 36, nullable = false, updatable = false)
    private String id;

    protected ServiceImage() {
    }

    public ServiceImage(String imageKey, String originalFilename, String contentType, long fileSize) {
        this.imageKey = imageKey;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
    }

    public ServiceImage(String imageKey, String originalFilename, String contentType) {
        this(imageKey, originalFilename, contentType, 0L);
    }

    public String getImageKey() {
        return imageKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getId() {
        return id;
    }

    protected void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ServiceImage)) {
            return false;
        }
        if (this == other) {
            return true;
        }
        ServiceImage serviceImage = (ServiceImage) other;
        return Objects.equals(this.id, serviceImage.id);
    }

    @Override
    public int hashCode() {
        return this.id == null ? 0 : this.id.hashCode();
    }
}
