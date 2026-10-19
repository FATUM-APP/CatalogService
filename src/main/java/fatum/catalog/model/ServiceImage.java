package fatum.catalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Table(name = "SERVICE_PORTAFOLIO")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
public class ServiceImage {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "IMAGE_KEY", nullable = false, length = 512)
    private String imageKey;

    @Column(name = "ORIGINAL_FILENAME", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "CONTENT_TYPE", nullable = false, length = 100)
    private String contentType;

    @Column(name = "FILE_SIZE", nullable = false)
    private long fileSize;

    public ServiceImage(String imageKey, String originalFilename, String contentType, long fileSize) {
        this.imageKey = imageKey;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
    }


}
