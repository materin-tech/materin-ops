package com.materin.tech.ops.components.resource.obs;

import com.materin.tech.ops.common.resource.ResourceData;
import com.materin.tech.ops.common.resource.ResourceStorage;
import com.obs.services.ObsClient;
import com.obs.services.model.PutObjectResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 华为云 OBS 存储：resource.storage.type=obs 时启用。
 * 需要 obs.end-point / ak / sk / bucket 配置。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "resource.storage.type", havingValue = "obs")
public class ObsResourceStorage implements ResourceStorage {

    private final ObsClient obsClient;
    private final String bucket;

    public ObsResourceStorage(@Value("${resource.storage.obs.end-point}") String endPoint,
                              @Value("${resource.storage.obs.ak}") String ak,
                              @Value("${resource.storage.obs.sk}") String sk,
                              @Value("${resource.storage.obs.bucket}") String bucket) {
        this.obsClient = new ObsClient(ak, sk, endPoint);
        this.bucket = bucket;
        log.info("华为云 OBS 存储已启用，bucket={}", bucket);
    }

    @Override
    public String type() {
        return "obs";
    }

    @Override
    public String upload(ResourceData data) {
        LocalDate today = LocalDate.now();
        String key = "%04d/%02d/%02d/%s-%s".formatted(
                today.getYear(), today.getMonthValue(), today.getDayOfMonth(),
                UUID.randomUUID().toString().replace("-", ""), safeName(data.getOriginalName()));
        PutObjectResult result = obsClient.putObject(bucket, key, new ByteArrayInputStream(data.getBytes()));
        if (result.getStatusCode() != 200) {
            throw new IllegalStateException("OBS 上传失败，status=" + result.getStatusCode());
        }
        return key;
    }

    @Override
    public byte[] download(String key) {
        try (var is = obsClient.getObject(bucket, key).getObjectContent()) {
            return is.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("OBS 读取失败：" + key, e);
        }
    }

    @Override
    public void delete(String key) {
        obsClient.deleteObject(bucket, key);
    }

    private String safeName(String name) {
        return name == null ? "file" : name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
