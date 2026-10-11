package com.materin.tech.ops.components.resource.core.service;

import cn.dev33.satoken.stp.StpUtil;
import com.materin.tech.ops.common.api.PageResult;
import com.materin.tech.ops.common.exception.BusinessException;
import com.materin.tech.ops.common.exception.ErrorCode;
import com.materin.tech.ops.common.resource.ResourceData;
import com.materin.tech.ops.common.resource.ResourceStorage;
import com.materin.tech.ops.components.resource.core.entity.ResourceItem;
import com.materin.tech.ops.components.resource.core.mapper.ResourceItemMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 资源服务：上传（按配置路由存储实现）→ 元数据落库；查询 / 下载 / 删除。
 */
@Slf4j
@Service
public class ResourceService {

    private static final QueryColumn C_ID = new QueryColumn("id");
    private static final QueryColumn C_ORIGINAL_NAME = new QueryColumn("original_name");
    private static final QueryColumn C_STORAGE_TYPE = new QueryColumn("storage_type");
    private static final QueryColumn C_UPLOADER_ID = new QueryColumn("uploader_id");

    private final ResourceItemMapper resourceItemMapper;
    private final Map<String, ResourceStorage> storageMap;
    private final String activeType;

    public ResourceService(ResourceItemMapper resourceItemMapper,
                           List<ResourceStorage> storages,
                           @Value("${resource.storage.type:local}") String activeType) {
        this.resourceItemMapper = resourceItemMapper;
        this.storageMap = storages.stream()
                .collect(Collectors.toMap(ResourceStorage::type, Function.identity()));
        this.activeType = activeType;
        log.info("资源存储实现：{}，当前启用：{}", storageMap.keySet(), activeType);
    }

    private ResourceStorage storage() {
        ResourceStorage storage = storageMap.get(activeType);
        if (storage == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "存储实现未加载：" + activeType);
        }
        return storage;
    }

    /** 上传文件 */
    public ResourceItem upload(MultipartFile file) {
        ResourceData data;
        try {
            data = ResourceData.builder()
                    .originalName(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .bytes(file.getBytes())
                    .build();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "读取上传文件失败");
        }
        String key = storage().upload(data);
        ResourceItem item = new ResourceItem();
        item.setOriginalName(data.getOriginalName());
        item.setContentType(data.getContentType());
        item.setSize((long) data.getBytes().length);
        item.setStorageType(activeType);
        item.setStorageKey(key);
        item.setUploaderId(currentUserIdOrNull());
        resourceItemMapper.insert(item);
        return item;
    }

    /** 分页查询资源 */
    public PageResult<ResourceItem> page(String keyword, String storageType, int pageNo, int pageSize) {
        QueryWrapper qw = QueryWrapper.create();
        if (keyword != null && !keyword.isBlank()) {
            qw.and(C_ORIGINAL_NAME.like(keyword));
        }
        if (storageType != null && !storageType.isBlank()) {
            qw.and(C_STORAGE_TYPE.eq(storageType));
        }
        qw.orderBy(C_ID.desc());
        Page<ResourceItem> p = resourceItemMapper.paginate(Page.of(pageNo, pageSize), qw);
        return PageResult.of(p.getRecords(), p.getTotalRow(), pageNo, pageSize);
    }

    /** 下载：返回元数据 + 字节 */
    public DownloadResult download(Long id) {
        ResourceItem item = requireItem(id);
        byte[] bytes = storageMap.getOrDefault(item.getStorageType(), storage()).download(item.getStorageKey());
        return new DownloadResult(item, bytes);
    }

    /** 删除：先删存储对象，再逻辑删除元数据 */
    public void delete(Long id) {
        ResourceItem item = requireItem(id);
        try {
            storageMap.getOrDefault(item.getStorageType(), storage()).delete(item.getStorageKey());
        } catch (Exception e) {
            log.warn("存储对象删除失败（继续逻辑删除元数据）：{}", item.getStorageKey(), e);
        }
        resourceItemMapper.deleteById(id);
    }

    private ResourceItem requireItem(Long id) {
        ResourceItem item = resourceItemMapper.selectOneById(id);
        if (item == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "资源不存在");
        }
        return item;
    }

    private Long currentUserIdOrNull() {
        try {
            String id = StpUtil.getLoginId().toString();
            return id.startsWith("app:") ? null : Long.valueOf(id);
        } catch (Exception e) {
            return null;
        }
    }

    /** 下载结果 */
    public record DownloadResult(ResourceItem item, byte[] bytes) {
    }
}
