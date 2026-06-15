package online.dbarenholz.omiyage.service;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageStorageService {

    private final online.dbarenholz.omiyage.config.MinioProperties minioProperties;

    private volatile boolean bucketEnsured = false;

    public String uploadWishImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is required");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image uploads are supported");
        }

        String extension = extensionFor(contentType);
        String objectName = "wishes/" + UUID.randomUUID() + extension;

        try {
            ensureBucket();
            try (InputStream input = file.getInputStream()) {
                minioClient().putObject(
                        PutObjectArgs.builder()
                                .bucket(minioProperties.bucket())
                                .object(objectName)
                                .stream(input, file.getSize(), -1)
                                .contentType(contentType)
                                .build());
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Failed to store image", e);
        }

        return normalizedPublicBaseUrl() + "/" + minioProperties.bucket() + "/" + objectName;
    }

    private void ensureBucket() {
        if (bucketEnsured)
            return;
        synchronized (this) {
            if (bucketEnsured)
                return;
            try {
                boolean exists = minioClient().bucketExists(
                        BucketExistsArgs.builder().bucket(minioProperties.bucket()).build());
                if (!exists) {
                    minioClient().makeBucket(
                            MakeBucketArgs.builder().bucket(minioProperties.bucket()).build());
                }

                // Public read access for uploaded wish images.
                String policy = """
                        {
                            "Version": "2012-10-17",
                            "Statement": [
                                {
                                    "Effect": "Allow",
                                    "Principal": {"AWS": ["*"]},
                                    "Action": ["s3:GetObject"],
                                    "Resource": ["arn:aws:s3:::%s/*"]
                                }
                            ]
                        }
                        """.formatted(minioProperties.bucket());

                minioClient().setBucketPolicy(
                        SetBucketPolicyArgs.builder().bucket(minioProperties.bucket()).config(policy).build());
                bucketEnsured = true;
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Failed to initialize image storage", e);
            }
        }
    }

    private MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioProperties.endpoint())
                .credentials(minioProperties.accessKey(), minioProperties.secretKey())
                .build();
    }

    private static String extensionFor(String contentType) {
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "image/svg+xml" -> ".svg";
            default -> "";
        };
    }

    private String normalizedPublicBaseUrl() {
        String publicUrl = minioProperties.publicUrl();
        return publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
    }
}
