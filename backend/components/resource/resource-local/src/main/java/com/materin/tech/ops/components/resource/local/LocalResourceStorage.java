package com.materin.tech.ops.components.resource.local;

import com.materin.tech.ops.common.resource.ResourceData;
import com.materin.tech.ops.common.resource.ResourceStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 本地文件存储：{base-path}/yyyy/MM/dd/{uuid}{ext}。
 * 当前系统默认启用（resource.storage.type=local）。
 */
@Component
@ConditionalOnProperty(name = "resource.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalResourceStorage implements ResourceStorage {

    private final Path basePath;

    public LocalResourceStorage(@Value("${resource.storage.local.base-path:./data/files}") String basePath) {
        this.basePath = Path.of(basePath);
    }

    @Override
    public String type() {
        return "local";
    }

    @Override
    public String upload(ResourceData data) {
        String ext = extOf(data.getOriginalName());
        LocalDate today = LocalDate.now();
        String key = "%04d/%02d/%02d/%s%s".formatted(
                today.getYear(), today.getMonthValue(), today.getDayOfMonth(),
                UUID.randomUUID().toString().replace("-", ""), ext);
        try {
            Path target = basePath.resolve(key);
            Files.createDirectories(target.getParent());
            Files.write(target, data.getBytes());
            return key;
        } catch (IOException e) {
            throw new IllegalStateException("本地存储写入失败：" + key, e);
        }
    }

    @Override
    public byte[] download(String key) {
        try {
            return Files.readAllBytes(basePath.resolve(key));
        } catch (IOException e) {
            throw new IllegalStateException("本地存储读取失败：" + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(basePath.resolve(key));
        } catch (IOException e) {
            throw new IllegalStateException("本地存储删除失败：" + key, e);
        }
    }

    private String extOf(String name) {
        if (name == null) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot);
    }
}
