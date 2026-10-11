package com.materin.tech.ops.components.ticket.service;

import com.materin.tech.ops.common.api.PageResult;
import com.materin.tech.ops.common.exception.BusinessException;
import com.materin.tech.ops.common.exception.ErrorCode;
import com.materin.tech.ops.components.ticket.entity.SlaPolicy;
import com.materin.tech.ops.components.ticket.mapper.SlaPolicyMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * SLA 策略服务。
 */
@Service
@RequiredArgsConstructor
public class SlaPolicyService {

    private static final QueryColumn C_ID = new QueryColumn("id");
    private static final QueryColumn C_PRIORITY = new QueryColumn("priority");

    private final SlaPolicyMapper slaPolicyMapper;

    public List<SlaPolicy> listAll() {
        return slaPolicyMapper.selectListByQuery(QueryWrapper.create().orderBy(C_PRIORITY.asc()));
    }

    public PageResult<SlaPolicy> page(int pageNo, int pageSize) {
        Page<SlaPolicy> p = slaPolicyMapper.paginate(Page.of(pageNo, pageSize),
                QueryWrapper.create().orderBy(C_PRIORITY.asc()));
        return PageResult.of(p.getRecords(), p.getTotalRow(), pageNo, pageSize);
    }

    /** 编辑策略（时限/启用）；优先级唯一，不做变更 */
    public void update(Long id, SlaPolicy body) {
        SlaPolicy policy = slaPolicyMapper.selectOneById(id);
        if (policy == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "SLA 策略不存在");
        }
        SlaPolicy upd = new SlaPolicy();
        upd.setId(id);
        if (body.getName() != null) upd.setName(body.getName());
        if (body.getFirstResponseMinutes() != null) upd.setFirstResponseMinutes(body.getFirstResponseMinutes());
        if (body.getResolveMinutes() != null) upd.setResolveMinutes(body.getResolveMinutes());
        if (body.getEnabled() != null) upd.setEnabled(body.getEnabled());
        slaPolicyMapper.update(upd);
    }
}
