package com.materin.tech.ops.components.ops.requirement.service;

import com.materin.tech.ops.common.api.PageResult;
import com.materin.tech.ops.common.exception.BusinessException;
import com.materin.tech.ops.common.exception.ErrorCode;
import com.materin.tech.ops.components.ops.requirement.dto.StoryCreateBody;
import com.materin.tech.ops.components.ops.requirement.dto.StoryPageQuery;
import com.materin.tech.ops.components.ops.requirement.dto.StoryUpdateBody;
import com.materin.tech.ops.components.ops.requirement.entity.Product;
import com.materin.tech.ops.components.ops.requirement.entity.ProductModule;
import com.materin.tech.ops.components.ops.requirement.entity.Story;
import com.materin.tech.ops.components.ops.requirement.mapper.ProductMapper;
import com.materin.tech.ops.components.ops.requirement.mapper.ProductModuleMapper;
import com.materin.tech.ops.components.ops.requirement.mapper.StoryMapper;
import com.materin.tech.ops.components.ops.requirement.vo.StoryVo;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 需求服务：分页 / 详情 / CRUD / 状态流转。
 * <p>数据权限按产品过滤；状态流转合法性在服务端校验。</p>
 */
@Service
@RequiredArgsConstructor
public class StoryService {

    // ---------- 表列常量（不依赖 APT 生成的 TableDef） ----------
    private static final QueryColumn C_ID = new QueryColumn("id");
    private static final QueryColumn C_PRODUCT_ID = new QueryColumn("product_id");
    private static final QueryColumn C_MODULE_ID = new QueryColumn("module_id");
    private static final QueryColumn C_TITLE = new QueryColumn("title");
    private static final QueryColumn C_STATUS = new QueryColumn("status");
    private static final QueryColumn C_PRIORITY = new QueryColumn("priority");

    /** 需求状态合法迁移表：key=当前状态，value=允许迁移到的目标状态集合 */
    private static final Map<Integer, Set<Integer>> STATUS_TRANSITIONS = Map.of(
            0, Set.of(1),   // 草稿 → 评审中
            1, Set.of(0, 2),// 评审中 → 草稿（打回）/ 已确认
            2, Set.of(3)    // 已确认 → 已关闭
    );

    private final StoryMapper storyMapper;
    private final ProductMapper productMapper;
    private final ProductModuleMapper productModuleMapper;
    private final ProductDataScopeService dataScopeService;
    private final UserNicknameResolver nicknameResolver;

    /** 条件分页查询需求，按可见产品过滤 */
    public PageResult<StoryVo> page(StoryPageQuery query) {
        QueryWrapper qw = QueryWrapper.create();
        Set<Long> visible = dataScopeService.visibleProductIds(dataScopeService.currentUserId());
        if (visible != null) {
            if (visible.isEmpty()) {
                return PageResult.of(List.of(), 0, query.safePage(), query.safePageSize());
            }
            qw.and(C_PRODUCT_ID.in(visible));
        }
        if (query.getProductId() != null) {
            qw.and(C_PRODUCT_ID.eq(query.getProductId()));
        }
        if (query.getModuleId() != null) {
            qw.and(C_MODULE_ID.eq(query.getModuleId()));
        }
        if (query.getStatus() != null) {
            qw.and(C_STATUS.eq(query.getStatus()));
        }
        if (query.getPriority() != null) {
            qw.and(C_PRIORITY.eq(query.getPriority()));
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            qw.and(C_TITLE.like(query.getKeyword()));
        }
        qw.orderBy(new QueryColumn("sort").asc(), C_ID.desc());
        Page<Story> p = storyMapper.paginate(Page.of(query.safePage(), query.safePageSize()), qw);
        List<StoryVo> vos = p.getRecords().stream().map(this::toVo).toList();
        return PageResult.of(vos, p.getTotalRow(), query.safePage(), query.safePageSize());
    }

    /** 需求详情 */
    public StoryVo detail(Long id) {
        return toVo(requireVisibleStory(id));
    }

    /** 新增需求（初始状态草稿） */
    public Long create(StoryCreateBody body) {
        dataScopeService.assertProductVisible(dataScopeService.currentUserId(), body.getProductId());
        validateModule(body.getProductId(), body.getModuleId());
        Story story = new Story();
        story.setProductId(body.getProductId());
        story.setModuleId(body.getModuleId());
        story.setTitle(body.getTitle());
        story.setDescription(body.getDescription());
        story.setStatus(0);
        story.setPriority(body.getPriority() == null ? 2 : body.getPriority());
        story.setOwnerId(body.getOwnerId());
        story.setSort(body.getSort() == null ? 0 : body.getSort());
        storyMapper.insert(story);
        return story.getId();
    }

    /** 编辑需求（产品与状态不在此接口修改） */
    public void update(Long id, StoryUpdateBody body) {
        Story story = requireVisibleStory(id);
        if (body.getModuleId() != null) {
            validateModule(story.getProductId(), body.getModuleId());
            story.setModuleId(body.getModuleId());
        }
        if (body.getTitle() != null) story.setTitle(body.getTitle());
        if (body.getDescription() != null) story.setDescription(body.getDescription());
        if (body.getPriority() != null) story.setPriority(body.getPriority());
        if (body.getOwnerId() != null) story.setOwnerId(body.getOwnerId());
        if (body.getSort() != null) story.setSort(body.getSort());
        storyMapper.update(story);
    }

    /** 删除需求（逻辑删除） */
    public void delete(Long id) {
        requireVisibleStory(id);
        storyMapper.deleteById(id);
    }

    /** 状态流转：校验合法迁移 */
    public void changeStatus(Long id, Integer targetStatus) {
        Story story = requireVisibleStory(id);
        Set<Integer> allowed = STATUS_TRANSITIONS.getOrDefault(story.getStatus(), Set.of());
        if (!allowed.contains(targetStatus)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "不允许的状态变更：" + story.getStatus() + " → " + targetStatus);
        }
        Story update = new Story();
        update.setId(id);
        update.setStatus(targetStatus);
        storyMapper.update(update);
    }

    // ---------- 私有方法 ----------

    /** 取需求并断言可见性，不存在抛 404 */
    private Story requireVisibleStory(Long id) {
        Story story = storyMapper.selectOneById(id);
        if (story == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "需求不存在");
        }
        dataScopeService.assertProductVisible(dataScopeService.currentUserId(), story.getProductId());
        return story;
    }

    /** 校验模块属于指定产品 */
    private void validateModule(Long productId, Long moduleId) {
        if (moduleId == null) {
            return;
        }
        ProductModule module = productModuleMapper.selectOneById(moduleId);
        if (module == null || !module.getProductId().equals(productId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "模块不存在或不属于该产品");
        }
    }

    private StoryVo toVo(Story story) {
        StoryVo vo = new StoryVo();
        vo.setId(story.getId());
        vo.setProductId(story.getProductId());
        Product product = productMapper.selectOneById(story.getProductId());
        vo.setProductName(product == null ? null : product.getName());
        vo.setModuleId(story.getModuleId());
        if (story.getModuleId() != null) {
            ProductModule module = productModuleMapper.selectOneById(story.getModuleId());
            vo.setModuleTitle(module == null ? null : module.getName());
        }
        vo.setTitle(story.getTitle());
        vo.setDescription(story.getDescription());
        vo.setStatus(story.getStatus());
        vo.setPriority(story.getPriority());
        vo.setOwnerId(story.getOwnerId());
        vo.setOwnerName(nicknameResolver.resolveOne(story.getOwnerId()));
        vo.setSort(story.getSort());
        return vo;
    }
}
