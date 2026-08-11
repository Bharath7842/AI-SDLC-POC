package com.portpoc.recipient.infrastructure.storage.minio;

import com.portpoc.recipient.domain.ports.ReceiptStorage;
import java.io.InputStream;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
public class MinioReceiptStorage implements ReceiptStorage {
    private static final Logger logger = LoggerFactory.getLogger(MinioReceiptStorage.class);

    private final S3Client s3Client;
    private final String bucketName;

    public MinioReceiptStorage(MinioClientConfig config) {
        this.bucketName = config.getBucket();
        this.s3Client = createS3Client(config);
        ensureBucketExists();
    }

    private S3Client createS3Client(MinioClientConfig config) {
        String endpoint = "http://" + config.getHost() + ":" + config.getPort();

        S3ClientBuilder builder = S3Client.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(config.getAccessKey(), config.getSecretKey())
            ))
            .endpointOverride(java.net.URI.create(endpoint))
            .forcePathStyle(true);

        return builder.build();
    }

    private void ensureBucketExists() {
        try {
            HeadBucketRequest headRequest = HeadBucketRequest.builder()
                .bucket(bucketName)
                .build();
            s3Client.headBucket(headRequest);
            logger.info("Bucket {} exists", bucketName);
        } catch (NoSuchBucketException e) {
            logger.warn("Bucket {} does not exist; will be created on first upload", bucketName);
        }
    }

    @Override
    public String uploadReceipt(String key, InputStream content) {
        try {
            byte[] bytes = content.readAllBytes();
            PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentLength((long) bytes.length)
                .build();

            s3Client.putObject(putRequest, software.amazon.awssdk.core.sync.RequestBody.fromBytes(bytes));

            String uri = String.format("s3://%s/%s", bucketName, key);
            logger.info("Uploaded receipt to {}", uri);
            return uri;
        } catch (Exception e) {
            logger.error("Failed to upload receipt {}", key, e);
            throw new RuntimeException("Failed to upload receipt", e);
        }
    }

    @Override
    public Optional<InputStream> downloadReceipt(String key) {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

            InputStream stream = s3Client.getObject(getRequest, ResponseTransformer.toInputStream());
            logger.info("Downloaded receipt from {}/{}", bucketName, key);
            return Optional.of(stream);
        } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
            logger.warn("Receipt not found: {}/{}", bucketName, key);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Failed to download receipt {}", key, e);
            throw new RuntimeException("Failed to download receipt", e);
        }
    }
}
