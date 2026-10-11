package com.materin.tech.ops.components.resource.minio;

import com.materin.tech.ops.common.resource.ResourceData;
import com.materin.tech.ops.common.resource.ResourceStorage;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.UUID;

/**
 * MinIO 存储：resource.storage.type=minio 时启用。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "resource.storage.type", havingValue = "minio")
public class MinioResourceStorage implements ResourceStorage {

    private final MinioClient client;
    private final String bucket;

    public MinioResourceStorage(@Value("${resource.storage.minio.end-point}") String endPoint,
                                @Value("${resource.storage.minio.ak}") String ak,
                                @Value("${resource.storage.minio.sk}") String sk,
                                @Value("${resource.storage.minio.bucket}") String bucket) throws Exception {
        this.client = MinioClient.builder().endpoint(endPoint).credentials(ak, sk).build();
        this.bucket = bucket;
        if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
        log.info("MinIO 存储已启用，bucket={}", bucket);
    }

    @Override
    public String type() {
        return "minio";
    }

    @Override
    public String upload(ResourceData data) {
        LocalDate today = LocalDate.now();
        String key = "%04d/%02d/%02d/%s-%s".formatted(
                today.getYear(), today.getMonthValue(), today.getDayOfMonth(),
                UUID.randomUUID().toString().replace("-", ""), safeName(data.getOriginalName()));
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket).object(key)
                    .stream(new ByteArrayInputStream(data.getBytes()), data.getBytes().length, -1)
                    .contentType(data.getContentType() == null ? "application/octet-stream" : data.getContentType())
                    .build());
            return key;
        } catch (Exception e) {
            throw new IllegalStateException("MinIO 上传失败：" + key, e);
        }
    }

    @Override
    public byte[] download(String key) {
        try (var is = client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build())) {
            return is.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("MinIO 读取失败：" + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception e) {
            throw new IllegalStateException("MinIO 删除失败：" + key, e);
        }
    }

    private String safeName(String name) {
        return name == null ? "file" : name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
