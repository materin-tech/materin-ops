package com.materin.tech.ops.components.resource.core.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.materin.tech.ops.common.api.PageResult;
import com.materin.tech.ops.common.api.Result;
import com.materin.tech.ops.components.resource.core.entity.ResourceItem;
import com.materin.tech.ops.components.resource.core.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件资源接口（资源管理）。
 */
@RestController
@RequestMapping("/resource/files")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    /** 分页查询资源 */
    @GetMapping
    public Result<PageResult<ResourceItem>> page(@RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String storageType,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(resourceService.page(keyword, storageType, page, pageSize));
    }

    /** 上传文件 */
    @SaCheckPermission("resource:upload")
    @PostMapping("/upload")
    public Result<ResourceItem> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(resourceService.upload(file));
    }

    /** 下载文件 */
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        ResourceService.DownloadResult result = resourceService.download(id);
        String encoded = URLEncoder.encode(result.item().getOriginalName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(
                        result.item().getContentType() == null ? "application/octet-stream" : result.item().getContentType()))
                .body(result.bytes());
    }

    /** 删除资源 */
    @SaCheckPermission("resource:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        resourceService.delete(id);
        return Result.ok();
    }
}
