package com.materin.tech.ops.components.ops.requirement.service;

import com.materin.tech.ops.common.api.PageResult;
import com.materin.tech.ops.common.exception.BusinessException;
import com.materin.tech.ops.common.exception.ErrorCode;
import com.materin.tech.ops.components.ops.requirement.dto.IterationCreateBody;
import com.materin.tech.ops.components.ops.requirement.dto.IterationPageQuery;
import com.materin.tech.ops.components.ops.requirement.dto.IterationUpdateBody;
import com.materin.tech.ops.components.ops.requirement.entity.Iteration;
import com.materin.tech.ops.components.ops.requirement.entity.Project;
import com.materin.tech.ops.components.ops.requirement.entity.Task;
import com.materin.tech.ops.components.ops.requirement.mapper.IterationMapper;
import com.materin.tech.ops.components.ops.requirement.mapper.ProjectMapper;
import com.materin.tech.ops.components.ops.requirement.mapper.TaskMapper;
import com.materin.tech.ops.components.ops.requirement.vo.IterationVo;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 迭代服务：分页 / CRUD / 状态流转。
 * <p>数据权限按项目过滤；开始迭代回填实际开始日期，结束迭代前校验无未完成任务。</p>
 */
@Service
@RequiredArgsConstructor
public class IterationService {

    // ---------- 表列常量（不依赖 APT 生成的 TableDef） ----------
    private static final QueryColumn C_ID = new QueryColumn("id");
    private static final QueryColumn C_PROJECT_ID = new QueryColumn("project_id");
    private static final QueryColumn C_NAME = new QueryColumn("name");
    private static final QueryColumn C_STATUS = new QueryColumn("status");

    /** 迭代状态合法迁移表 */
    private static final Map<Integer, Set<Integer>> STATUS_TRANSITIONS = Map.of(
            0, Set.of(1),
            1, Set.of(2)
    );

    private final IterationMapper iterationMapper;
    private final ProjectMapper projectMapper;
    private final TaskMapper taskMapper;
    private final ProductDataScopeService dataScopeService;

    /** 条件分页查询迭代，按可见项目过滤 */
    public PageResult<IterationVo> page(IterationPageQuery query) {
        QueryWrapper qw = QueryWrapper.create();
        Set<Long> visible = dataScopeService.visibleProjectIds(dataScopeService.currentUserId());
        if (visible != null) {
            if (visible.isEmpty()) {
                return PageResult.of(List.of(), 0, query.safePage(), query.safePageSize());
            }
            qw.and(C_PROJECT_ID.in(visible));
        }
        if (query.getProjectId() != null) {
            qw.and(C_PROJECT_ID.eq(query.getProjectId()));
        }
        if (query.getStatus() != null) {
            qw.and(C_STATUS.eq(query.getStatus()));
        }
        qw.orderBy(C_ID.desc());
        Page<Iteration> p = iterationMapper.paginate(Page.of(query.safePage(), query.safePageSize()), qw);
        List<IterationVo> vos = p.getRecords().stream().map(this::toVo).toList();
        return PageResult.of(vos, p.getTotalRow(), query.safePage(), query.safePageSize());
    }

    /** 迭代详情 */
    public IterationVo detail(Long id) {
        return toVo(requireVisibleIteration(id));
    }

    /** 新增迭代（初始状态未开始） */
    public Long create(IterationCreateBody body) {
        dataScopeService.assertProjectVisible(dataScopeService.currentUserId(), body.getProjectId());
        Iteration iteration = new Iteration();
        iteration.setProjectId(body.getProjectId());
        iteration.setName(body.getName());
        iteration.setStatus(0);
        iteration.setPlanStart(body.getPlanStart());
        iteration.setPlanEnd(body.getPlanEnd());
        iterationMapper.insert(iteration);
        return iteration.getId();
    }

    /** 编辑迭代（项目与状态不在此接口修改） */
    public void update(Long id, IterationUpdateBody body) {
        Iteration iteration = requireVisibleIteration(id);
        if (body.getName() != null) iteration.setName(body.getName());
        if (body.getPlanStart() != null) iteration.setPlanStart(body.getPlanStart());
        if (body.getPlanEnd() != null) iteration.setPlanEnd(body.getPlanEnd());
        iterationMapper.update(iteration);
    }

    /** 删除迭代（逻辑删除）：迭代下仍有任务时禁止 */
    public void delete(Long id) {
        requireVisibleIteration(id);
        Long taskCount = taskMapper.selectCountByQuery(
                QueryWrapper.create().where(new QueryColumn("iteration_id").eq(id)));
        if (taskCount != null && taskCount > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "迭代下仍有任务，无法删除");
        }
        iterationMapper.deleteById(id);
    }

    /** 状态流转：开始回填实际开始日期；结束前校验无未完成任务 */
    public void changeStatus(Long id, Integer targetStatus) {
        Iteration iteration = requireVisibleIteration(id);
        Set<Integer> allowed = STATUS_TRANSITIONS.getOrDefault(iteration.getStatus(), Set.of());
        if (!allowed.contains(targetStatus)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "不允许的状态变更：" + iteration.getStatus() + " → " + targetStatus);
        }
        Iteration update = new Iteration();
        update.setId(id);
        update.setStatus(targetStatus);
        if (targetStatus == 1) {
            update.setActualStart(LocalDate.now());
        }
        if (targetStatus == 2) {
            Long unfinished = taskMapper.selectCountByQuery(
                    QueryWrapper.create()
                            .where(new QueryColumn("iteration_id").eq(id))
                            .and(new QueryColumn("status").in(0, 1)));
            if (unfinished != null && unfinished > 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "迭代下仍有未完成任务，无法结束");
            }
            update.setActualEnd(LocalDate.now());
        }
        iterationMapper.update(update);
    }

    // ---------- 私有方法 ----------

    /** 取迭代并断言可见性，不存在抛 404 */
    private Iteration requireVisibleIteration(Long id) {
        Iteration iteration = iterationMapper.selectOneById(id);
        if (iteration == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "迭代不存在");
        }
        dataScopeService.assertProjectVisible(dataScopeService.currentUserId(), iteration.getProjectId());
        return iteration;
    }

    private IterationVo toVo(Iteration iteration) {
        IterationVo vo = new IterationVo();
        vo.setId(iteration.getId());
        vo.setProjectId(iteration.getProjectId());
        Project project = projectMapper.selectOneById(iteration.getProjectId());
        vo.setProjectName(project == null ? null : project.getName());
        vo.setName(iteration.getName());
        vo.setStatus(iteration.getStatus());
        vo.setPlanStart(iteration.getPlanStart());
        vo.setPlanEnd(iteration.getPlanEnd());
        vo.setActualStart(iteration.getActualStart());
        vo.setActualEnd(iteration.getActualEnd());
        return vo;
    }
}
