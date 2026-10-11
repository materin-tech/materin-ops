package com.materin.tech.ops.components.developer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.materin.tech.ops.common.api.PageResult;
import com.materin.tech.ops.common.exception.BusinessException;
import com.materin.tech.ops.common.exception.ErrorCode;
import com.materin.tech.ops.components.developer.entity.DevApi;
import com.materin.tech.ops.components.developer.entity.DevAppApi;
import com.materin.tech.ops.components.developer.mapper.DevApiMapper;
import com.materin.tech.ops.components.developer.mapper.DevAppApiMapper;
import com.materin.tech.ops.components.developer.vo.DevApiVo;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 开放接口清单服务：knife4j /v3/api-docs 同步 + 手工登记 + 开放状态管理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DevApiService {

    private static final QueryColumn C_ID = new QueryColumn("id");
    private static final QueryColumn C_METHOD = new QueryColumn("method");
    private static final QueryColumn C_PATH = new QueryColumn("path");
    private static final QueryColumn C_STATUS = new QueryColumn("status");
    private static final QueryColumn C_TAG = new QueryColumn("tag");
    private static final QueryColumn C_API_ID = new QueryColumn("api_id");

    private final DevApiMapper devApiMapper;
    private final DevAppApiMapper devAppApiMapper;
    private final ObjectMapper objectMapper;

    /** 分页查询接口清单 */
    public PageResult<DevApiVo> page(String keyword, Integer status, String method, int page, int pageSize) {
        QueryWrapper qw = QueryWrapper.create();
        if (keyword != null && !keyword.isBlank()) {
            qw.and(C_PATH.like(keyword).or(new QueryColumn("name").like(keyword)));
        }
        if (status != null) {
            qw.and(C_STATUS.eq(status));
        }
        if (method != null && !method.isBlank()) {
            qw.and(C_METHOD.eq(method.toUpperCase()));
        }
        qw.orderBy(C_TAG.asc(), C_PATH.asc(), C_METHOD.asc());
        Page<DevApi> p = devApiMapper.paginate(Page.of(page, pageSize), qw);
        List<DevApiVo> vos = p.getRecords().stream().map(this::toVo).toList();
        return PageResult.of(vos, p.getTotalRow(), page, pageSize);
    }

    /**
     * 从 knife4j（springdoc /v3/api-docs）同步接口清单：
     * 按 method+path upsert，已存在则回填名称与分组，不存在则新增（默认关闭状态）。
     */
    @Transactional
    public SyncResult sync() {
        String docs;
        try {
            docs = RestClient.create().get()
                    .uri("http://127.0.0.1:8080/v3/api-docs")
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "拉取 /v3/api-docs 失败：" + e.getMessage());
        }
        int created = 0;
        int updated = 0;
        try {
            JsonNode paths = objectMapper.readTree(docs).path("paths");
            for (Iterator<String> it = paths.fieldNames(); it.hasNext(); ) {
                String path = it.next();
                JsonNode methods = paths.path(path);
                for (String m : List.of("get", "post", "put", "delete", "patch")) {
                    if (!methods.has(m)) {
                        continue;
                    }
                    JsonNode op = methods.path(m);
                    String summary = op.path("summary").asText(null);
                    String tag = op.path("tags").isEmpty()
                            ? "default"
                            : op.path("tags").get(0).asText("default");
                    String method = m.toUpperCase();
                    DevApi exist = devApiMapper.selectOneByQuery(QueryWrapper.create()
                            .where(C_METHOD.eq(method)).and(C_PATH.eq(path)));
                    if (exist == null) {
                        DevApi api = new DevApi();
                        api.setMethod(method);
                        api.setPath(path);
                        api.setName(summary);
                        api.setTag(tag);
                        api.setStatus(0);
                        api.setSource("sync");
                        devApiMapper.insert(api);
                        created++;
                    } else {
                        DevApi upd = new DevApi();
                        upd.setId(exist.getId());
                        upd.setName(summary);
                        upd.setTag(tag);
                        devApiMapper.update(upd);
                        updated++;
                    }
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "解析 api-docs 失败：" + e.getMessage());
        }
        return new SyncResult(created, updated);
    }

    /** 手工登记接口（默认关闭状态） */
    @Transactional
    public void create(DevApi body) {
        String method = body.getMethod() == null ? null : body.getMethod().toUpperCase();
        Long count = devApiMapper.selectCountByQuery(QueryWrapper.create()
                .where(C_METHOD.eq(method)).and(C_PATH.eq(body.getPath())));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该 method + path 已存在");
        }
        DevApi api = new DevApi();
        api.setMethod(method);
        api.setPath(body.getPath());
        api.setName(body.getName());
        api.setTag(body.getTag() == null || body.getTag().isBlank() ? "manual" : body.getTag());
        api.setStatus(0);
        api.setSource("manual");
        devApiMapper.insert(api);
    }

    /** 编辑接口（名称 / 分组 / 开放状态） */
    @Transactional
    public void update(Long id, DevApi body) {
        DevApi api = devApiMapper.selectOneById(id);
        if (api == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "接口不存在");
        }
        DevApi upd = new DevApi();
        upd.setId(id);
        if (body.getName() != null) upd.setName(body.getName());
        if (body.getTag() != null) upd.setTag(body.getTag());
        if (body.getStatus() != null) upd.setStatus(body.getStatus());
        devApiMapper.update(upd);
    }

    /** 删除接口（逻辑删除）并物理删除相关授权 */
    @Transactional
    public void delete(Long id) {
        if (devApiMapper.selectOneById(id) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "接口不存在");
        }
        devApiMapper.deleteById(id);
        devAppApiMapper.deleteByQuery(QueryWrapper.create().where(C_API_ID.eq(id)));
    }

    private DevApiVo toVo(DevApi api) {
        DevApiVo vo = new DevApiVo();
        vo.setId(api.getId());
        vo.setMethod(api.getMethod());
        vo.setPath(api.getPath());
        vo.setName(api.getName());
        vo.setTag(api.getTag());
        vo.setStatus(api.getStatus());
        vo.setSource(api.getSource());
        return vo;
    }

    /** 同步结果统计 */
    public record SyncResult(int created, int updated) {
    }
}
