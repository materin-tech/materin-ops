package com.materin.tech.ops.components.developer.service;

import com.materin.tech.ops.common.api.PageResult;
import com.materin.tech.ops.common.exception.BusinessException;
import com.materin.tech.ops.common.exception.ErrorCode;
import com.materin.tech.ops.components.developer.entity.DevApp;
import com.materin.tech.ops.components.developer.entity.DevAppApi;
import com.materin.tech.ops.components.developer.mapper.DevAppApiMapper;
import com.materin.tech.ops.components.developer.mapper.DevAppMapper;
import com.materin.tech.ops.components.developer.vo.DevAppVo;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;

/**
 * 开发者应用服务：CRUD / 密钥生成与重置。
 */
@Service
@RequiredArgsConstructor
public class DevAppService {

    private static final QueryColumn C_ID = new QueryColumn("id");
    private static final QueryColumn C_NAME = new QueryColumn("name");
    private static final QueryColumn C_APP_KEY = new QueryColumn("app_key");
    private static final QueryColumn C_STATUS = new QueryColumn("status");
    private static final QueryColumn C_APP_ID = new QueryColumn("app_id");

    private final DevAppMapper devAppMapper;
    private final DevAppApiMapper devAppApiMapper;

    /** 分页查询应用 */
    public PageResult<DevAppVo> page(String keyword, Integer status, int page, int pageSize) {
        QueryWrapper qw = QueryWrapper.create();
        if (keyword != null && !keyword.isBlank()) {
            qw.and(C_NAME.like(keyword).or(C_APP_KEY.like(keyword)));
        }
        if (status != null) {
            qw.and(C_STATUS.eq(status));
        }
        qw.orderBy(C_ID.desc());
        Page<DevApp> p = devAppMapper.paginate(Page.of(page, pageSize), qw);
        List<DevAppVo> vos = p.getRecords().stream().map(this::toVo).toList();
        return PageResult.of(vos, p.getTotalRow(), page, pageSize);
    }

    /** 应用详情 */
    public DevAppVo detail(Long id) {
        return toVo(requireApp(id));
    }

    /** 新增应用：生成 AppKey / AppSecret */
    @Transactional
    public DevAppVo create(DevApp body) {
        DevApp app = new DevApp();
        app.setName(body.getName());
        app.setDescription(body.getDescription());
        app.setStatus(body.getStatus() == null ? 1 : body.getStatus());
        app.setAppKey(generateAppKey());
        app.setAppSecret(generateSecret());
        devAppMapper.insert(app);
        return toVo(app);
    }

    /** 编辑应用（名称 / 描述 / 状态，密钥走重置接口） */
    @Transactional
    public void update(Long id, DevApp body) {
        DevApp app = requireApp(id);
        if (body.getName() != null) app.setName(body.getName());
        if (body.getDescription() != null) app.setDescription(body.getDescription());
        if (body.getStatus() != null) app.setStatus(body.getStatus());
        devAppMapper.update(app);
    }

    /** 重置 AppSecret（旧密钥立即失效） */
    @Transactional
    public DevAppVo resetSecret(Long id) {
        DevApp app = requireApp(id);
        DevApp update = new DevApp();
        update.setId(id);
        update.setAppSecret(generateSecret());
        devAppMapper.update(update);
        app.setAppSecret(update.getAppSecret());
        return toVo(app);
    }

    /** 删除应用（逻辑删除）并物理删除其接口授权 */
    @Transactional
    public void delete(Long id) {
        requireApp(id);
        devAppMapper.deleteById(id);
        devAppApiMapper.deleteByQuery(QueryWrapper.create().where(C_APP_ID.eq(id)));
    }

    private DevApp requireApp(Long id) {
        DevApp app = devAppMapper.selectOneById(id);
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "应用不存在");
        }
        return app;
    }

    private String generateAppKey() {
        return "ak_" + randomHex(12);
    }

    private String generateSecret() {
        return "sk_" + randomHex(24);
    }

    private String randomHex(int bytes) {
        byte[] buf = new byte[bytes];
        new SecureRandom().nextBytes(buf);
        return HexFormat.of().formatHex(buf);
    }

    private DevAppVo toVo(DevApp app) {
        DevAppVo vo = new DevAppVo();
        vo.setId(app.getId());
        vo.setName(app.getName());
        vo.setAppKey(app.getAppKey());
        vo.setAppSecret(app.getAppSecret());
        vo.setDescription(app.getDescription());
        vo.setStatus(app.getStatus());
        return vo;
    }
}
