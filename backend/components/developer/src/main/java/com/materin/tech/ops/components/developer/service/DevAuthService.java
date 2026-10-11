package com.materin.tech.ops.components.developer.service;

import com.materin.tech.ops.common.exception.BusinessException;
import com.materin.tech.ops.common.exception.ErrorCode;
import com.materin.tech.ops.components.developer.entity.DevApp;
import com.materin.tech.ops.components.developer.entity.DevAppApi;
import com.materin.tech.ops.components.developer.mapper.DevAppApiMapper;
import com.materin.tech.ops.components.developer.mapper.DevAppMapper;
import com.materin.tech.ops.components.developer.vo.DevAuthVo;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用-接口授权服务：查询应用授权清单 / 全量覆盖授权。
 */
@Service
@RequiredArgsConstructor
public class DevAuthService {

    private static final QueryColumn C_APP_ID = new QueryColumn("app_id");
    private static final QueryColumn C_API_ID = new QueryColumn("api_id");

    private final DevAppMapper devAppMapper;
    private final DevAppApiMapper devAppApiMapper;
    private final DevApiService devApiService;

    /** 应用授权详情：全部接口 + 该应用已授权 apiId 集合 */
    public DevAuthVo detail(Long appId) {
        requireApp(appId);
        List<Long> authorized = devAppApiMapper.selectListByQuery(
                        QueryWrapper.create().select(C_API_ID).where(C_APP_ID.eq(appId)))
                .stream().map(DevAppApi::getApiId).toList();
        DevAuthVo vo = new DevAuthVo();
        vo.setAppId(appId);
        vo.setAuthorizedApiIds(authorized);
        vo.setApis(devApiService.page(null, null, null, 1, 500).getList());
        return vo;
    }

    /** 全量覆盖应用接口授权 */
    @Transactional
    public void saveGrant(Long appId, List<Long> apiIds) {
        requireApp(appId);
        devAppApiMapper.deleteByQuery(QueryWrapper.create().where(C_APP_ID.eq(appId)));
        if (apiIds == null || apiIds.isEmpty()) {
            return;
        }
        List<DevAppApi> list = apiIds.stream().distinct()
                .map(apiId -> {
                    DevAppApi link = new DevAppApi();
                    link.setAppId(appId);
                    link.setApiId(apiId);
                    return link;
                })
                .toList();
        devAppApiMapper.insertBatch(list);
    }

    private void requireApp(Long appId) {
        if (devAppMapper.selectOneById(appId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "应用不存在");
        }
    }
}
