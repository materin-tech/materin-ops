package com.materin.tech.ops.components.ops.requirement.service;

import com.materin.tech.ops.common.api.PageResult;
import com.materin.tech.ops.common.exception.BusinessException;
import com.materin.tech.ops.common.exception.ErrorCode;
import com.materin.tech.ops.components.ops.requirement.dto.TaskAssignBody;
import com.materin.tech.ops.components.ops.requirement.dto.TaskCreateBody;
import com.materin.tech.ops.components.ops.requirement.dto.TaskPageQuery;
import com.materin.tech.ops.components.ops.requirement.dto.TaskUpdateBody;
import com.materin.tech.ops.components.ops.requirement.entity.Iteration;
import com.materin.tech.ops.components.ops.requirement.entity.Story;
import com.materin.tech.ops.components.ops.requirement.entity.Task;
import com.materin.tech.ops.components.ops.requirement.mapper.IterationMapper;
import com.materin.tech.ops.components.ops.requirement.mapper.ProjectMapper;
import com.materin.tech.ops.components.ops.requirement.mapper.StoryMapper;
import com.materin.tech.ops.components.ops.requirement.mapper.TaskMapper;
import com.materin.tech.ops.components.ops.requirement.vo.TaskVo;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 任务服务：分页 / 按需求查询 / CRUD / 状态流转 / 指派。
 * <p>数据权限走冗余 project_id → project_product → product 链路。</p>
 */
@Service
@RequiredArgsConstructor
public class TaskService {

    // ---------- 表列常量（不依赖 APT 生成的 TableDef） ----------
    private static final QueryColumn C_ID = new QueryColumn("id");
    private static final QueryColumn C_PROJECT_ID = new QueryColumn("project_id");
    private static final QueryColumn C_ITERATION_ID = new QueryColumn("iteration_id");
    private static final QueryColumn C_STORY_ID = new QueryColumn("story_id");
    private static final QueryColumn C_TITLE = new QueryColumn("title");
    private static final QueryColumn C_STATUS = new QueryColumn("status");
    private static final QueryColumn C_TYPE = new QueryColumn("type");
    private static final QueryColumn C_ASSIGNEE_ID = new QueryColumn("assignee_id");

    /** 任务状态合法迁移表 */
    private static final Map<Integer, Set<Integer>> STATUS_TRANSITIONS = Map.of(
            0, Set.of(1, 3),   // 待处理 → 进行中 / 直接关闭
            1, Set.of(0, 2, 3),// 进行中 → 待处理（退回）/ 已完成 / 已关闭
            2, Set.of(3)       // 已完成 → 已关闭
    );

    private final TaskMapper taskMapper;
    private final ProjectMapper projectMapper;
    private final IterationMapper iterationMapper;
    private final StoryMapper storyMapper;
    private final ProductDataScopeService dataScopeService;
    private final UserNicknameResolver nicknameResolver;

    /** 条件分页查询任务，按可见项目过滤 */
    public PageResult<TaskVo> page(TaskPageQuery query) {
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
        if (query.getIterationId() != null) {
            qw.and(C_ITERATION_ID.eq(query.getIterationId()));
        }
        if (query.getStatus() != null) {
            qw.and(C_STATUS.eq(query.getStatus()));
        }
        if (query.getType() != null) {
            qw.and(C_TYPE.eq(query.getType()));
        }
        if (query.getAssigneeId() != null) {
            qw.and(C_ASSIGNEE_ID.eq(query.getAssigneeId()));
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            qw.and(C_TITLE.like(query.getKeyword()));
        }
        qw.orderBy(C_ID.desc());
        Page<Task> p = taskMapper.paginate(Page.of(query.safePage(), query.safePageSize()), qw);
        List<TaskVo> vos = p.getRecords().stream().map(this::toVo).toList();
        return PageResult.of(vos, p.getTotalRow(), query.safePage(), query.safePageSize());
    }

    /** 按需求查询任务列表（需求详情侧边场景） */
    public List<TaskVo> listByStory(Long storyId) {
        Story story = storyMapper.selectOneById(storyId);
        if (story == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "需求不存在");
        }
        dataScopeService.assertProductVisible(dataScopeService.currentUserId(), story.getProductId());
        return taskMapper.selectListByQuery(
                        QueryWrapper.create().where(C_STORY_ID.eq(storyId)).orderBy(C_ID.desc()))
                .stream().map(this::toVo).toList();
    }

    /** 任务详情 */
    public TaskVo detail(Long id) {
        return toVo(requireVisibleTask(id));
    }

    /** 新增任务（初始状态待处理）：校验项目可见、迭代归属项目、需求可见 */
    public Long create(TaskCreateBody body) {
        dataScopeService.assertProjectVisible(dataScopeService.currentUserId(), body.getProjectId());
        validateIteration(body.getProjectId(), body.getIterationId());
        if (body.getStoryId() != null) {
            Story story = storyMapper.selectOneById(body.getStoryId());
            if (story == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "关联需求不存在");
            }
            dataScopeService.assertProductVisible(dataScopeService.currentUserId(), story.getProductId());
        }
        Task task = new Task();
        task.setProjectId(body.getProjectId());
        task.setIterationId(body.getIterationId());
        task.setStoryId(body.getStoryId());
        task.setTitle(body.getTitle());
        task.setDescription(body.getDescription());
        task.setType(body.getType() == null ? 0 : body.getType());
        task.setStatus(0);
        task.setPriority(body.getPriority() == null ? 2 : body.getPriority());
        task.setAssigneeId(body.getAssigneeId());
        task.setPlanHours(body.getPlanHours());
        task.setActualHours(body.getActualHours());
        task.setPlanStart(body.getPlanStart());
        task.setPlanEnd(body.getPlanEnd());
        taskMapper.insert(task);
        return task.getId();
    }

    /** 编辑任务（项目与状态不在此接口修改） */
    public void update(Long id, TaskUpdateBody body) {
        Task task = requireVisibleTask(id);
        if (body.getIterationId() != null) {
            validateIteration(task.getProjectId(), body.getIterationId());
            task.setIterationId(body.getIterationId());
        }
        if (body.getStoryId() != null) {
            Story story = storyMapper.selectOneById(body.getStoryId());
            if (story == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "关联需求不存在");
            }
            dataScopeService.assertProductVisible(dataScopeService.currentUserId(), story.getProductId());
            task.setStoryId(body.getStoryId());
        }
        if (body.getTitle() != null) task.setTitle(body.getTitle());
        if (body.getDescription() != null) task.setDescription(body.getDescription());
        if (body.getType() != null) task.setType(body.getType());
        if (body.getPriority() != null) task.setPriority(body.getPriority());
        if (body.getPlanHours() != null) task.setPlanHours(body.getPlanHours());
        if (body.getActualHours() != null) task.setActualHours(body.getActualHours());
        if (body.getPlanStart() != null) task.setPlanStart(body.getPlanStart());
        if (body.getPlanEnd() != null) task.setPlanEnd(body.getPlanEnd());
        taskMapper.update(task);
    }

    /** 删除任务（逻辑删除） */
    public void delete(Long id) {
        requireVisibleTask(id);
        taskMapper.deleteById(id);
    }

    /** 状态流转：校验合法迁移 */
    public void changeStatus(Long id, Integer targetStatus) {
        Task task = requireVisibleTask(id);
        Set<Integer> allowed = STATUS_TRANSITIONS.getOrDefault(task.getStatus(), Set.of());
        if (!allowed.contains(targetStatus)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "不允许的状态变更：" + task.getStatus() + " → " + targetStatus);
        }
        Task update = new Task();
        update.setId(id);
        update.setStatus(targetStatus);
        taskMapper.update(update);
    }

    /** 指派任务 */
    public void assign(Long id, Long userId) {
        requireVisibleTask(id);
        Task update = new Task();
        update.setId(id);
        update.setAssigneeId(userId);
        taskMapper.update(update);
    }

    // ---------- 私有方法 ----------

    /** 校验迭代归属指定项目 */
    private void validateIteration(Long projectId, Long iterationId) {
        if (iterationId == null) {
            return;
        }
        Iteration iteration = iterationMapper.selectOneById(iterationId);
        if (iteration == null || !iteration.getProjectId().equals(projectId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "迭代不存在或不属于该项目");
        }
    }

    /** 取任务并断言可见性，不存在抛 404 */
    private Task requireVisibleTask(Long id) {
        Task task = taskMapper.selectOneById(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
        }
        dataScopeService.assertProjectVisible(dataScopeService.currentUserId(), task.getProjectId());
        return task;
    }

    private TaskVo toVo(Task task) {
        TaskVo vo = new TaskVo();
        vo.setId(task.getId());
        vo.setProjectId(task.getProjectId());
        com.materin.tech.ops.components.ops.requirement.entity.Project project =
                projectMapper.selectOneById(task.getProjectId());
        vo.setProjectName(project == null ? null : project.getName());
        vo.setIterationId(task.getIterationId());
        if (task.getIterationId() != null) {
            Iteration iteration = iterationMapper.selectOneById(task.getIterationId());
            vo.setIterationName(iteration == null ? null : iteration.getName());
        }
        vo.setStoryId(task.getStoryId());
        if (task.getStoryId() != null) {
            Story story = storyMapper.selectOneById(task.getStoryId());
            vo.setStoryTitle(story == null ? null : story.getTitle());
        }
        vo.setTitle(task.getTitle());
        vo.setDescription(task.getDescription());
        vo.setType(task.getType());
        vo.setStatus(task.getStatus());
        vo.setPriority(task.getPriority());
        vo.setAssigneeId(task.getAssigneeId());
        vo.setAssigneeName(nicknameResolver.resolveOne(task.getAssigneeId()));
        vo.setPlanHours(task.getPlanHours());
        vo.setActualHours(task.getActualHours());
        vo.setPlanStart(task.getPlanStart());
        vo.setPlanEnd(task.getPlanEnd());
        return vo;
    }
}
