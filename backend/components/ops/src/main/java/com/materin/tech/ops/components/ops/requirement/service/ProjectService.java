package com.materin.tech.ops.components.ops.requirement.service;

import com.materin.tech.ops.common.api.PageResult;
import com.materin.tech.ops.common.exception.BusinessException;
import com.materin.tech.ops.common.exception.ErrorCode;
import com.materin.tech.ops.components.ops.requirement.dto.ProjectCreateBody;
import com.materin.tech.ops.components.ops.requirement.dto.ProjectPageQuery;
import com.materin.tech.ops.components.ops.requirement.dto.ProjectUpdateBody;
import com.materin.tech.ops.components.ops.requirement.entity.Product;
import com.materin.tech.ops.components.ops.requirement.entity.Project;
import com.materin.tech.ops.components.ops.requirement.entity.ProjectProduct;
import com.materin.tech.ops.components.ops.requirement.mapper.IterationMapper;
import com.materin.tech.ops.components.ops.requirement.mapper.ProductMapper;
import com.materin.tech.ops.components.ops.requirement.mapper.ProjectMapper;
import com.materin.tech.ops.components.ops.requirement.mapper.ProjectProductMapper;
import com.materin.tech.ops.components.ops.requirement.vo.ProjectVo;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 项目服务：分页 / 选项 / CRUD / 关联产品维护。
 * <p>数据权限经 project_product 由可见产品推导可见项目。</p>
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    // ---------- 表列常量（不依赖 APT 生成的 TableDef） ----------
    private static final QueryColumn C_ID = new QueryColumn("id");
    private static final QueryColumn C_NAME = new QueryColumn("name");
    private static final QueryColumn C_CODE = new QueryColumn("code");
    private static final QueryColumn C_STATUS = new QueryColumn("status");
    private static final QueryColumn C_PROJECT_ID = new QueryColumn("project_id");
    private static final QueryColumn C_PRODUCT_ID = new QueryColumn("product_id");

    private final ProjectMapper projectMapper;
    private final ProjectProductMapper projectProductMapper;
    private final ProductMapper productMapper;
    private final IterationMapper iterationMapper;
    private final ProductDataScopeService dataScopeService;

    /** 条件分页查询项目，按可见项目过滤 */
    public PageResult<ProjectVo> page(ProjectPageQuery query) {
        QueryWrapper qw = QueryWrapper.create();
        Set<Long> visible = dataScopeService.visibleProjectIds(dataScopeService.currentUserId());
        if (visible != null) {
            if (visible.isEmpty()) {
                return PageResult.of(List.of(), 0, query.safePage(), query.safePageSize());
            }
            qw.and(C_ID.in(visible));
        }
        if (query.getStatus() != null) {
            qw.and(C_STATUS.eq(query.getStatus()));
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            qw.and(C_NAME.like(query.getKeyword()).or(C_CODE.like(query.getKeyword())));
        }
        qw.orderBy(C_ID.desc());
        Page<Project> p = projectMapper.paginate(Page.of(query.safePage(), query.safePageSize()), qw);
        List<ProjectVo> vos = p.getRecords().stream().map(this::toVo).toList();
        return PageResult.of(vos, p.getTotalRow(), query.safePage(), query.safePageSize());
    }

    /** 下拉选项：进行中的可见项目 */
    public List<ProjectVo> options() {
        QueryWrapper qw = QueryWrapper.create();
        Set<Long> visible = dataScopeService.visibleProjectIds(dataScopeService.currentUserId());
        if (visible != null) {
            if (visible.isEmpty()) {
                return List.of();
            }
            qw.and(C_ID.in(visible));
        }
        qw.and(C_STATUS.eq(1)).orderBy(C_ID.desc());
        return projectMapper.selectListByQuery(qw).stream().map(this::toVo).toList();
    }

    /** 项目详情 */
    public ProjectVo detail(Long id) {
        return toVo(requireVisibleProject(id));
    }

    /** 新增项目：code 唯一校验 + 绑定关联产品 */
    @Transactional
    public void create(ProjectCreateBody body) {
        assertCodeUnique(body.getCode(), null);
        Project project = new Project();
        project.setName(body.getName());
        project.setCode(body.getCode());
        project.setDescription(body.getDescription());
        project.setStatus(body.getStatus() == null ? 1 : body.getStatus());
        project.setStartDate(body.getStartDate());
        project.setEndDate(body.getEndDate());
        projectMapper.insert(project);
        bindProducts(project.getId(), body.getProductIds());
    }

    /** 编辑项目（编码不可改）+ 全量覆盖关联产品 */
    @Transactional
    public void update(Long id, ProjectUpdateBody body) {
        Project project = requireVisibleProject(id);
        if (body.getName() != null) project.setName(body.getName());
        if (body.getDescription() != null) project.setDescription(body.getDescription());
        if (body.getStatus() != null) project.setStatus(body.getStatus());
        if (body.getStartDate() != null) project.setStartDate(body.getStartDate());
        if (body.getEndDate() != null) project.setEndDate(body.getEndDate());
        projectMapper.update(project);
        if (body.getProductIds() != null) {
            bindProducts(id, body.getProductIds());
        }
    }

    /** 删除项目（逻辑删除）：项目下仍有迭代时禁止；物理删除产品关联 */
    @Transactional
    public void delete(Long id) {
        requireVisibleProject(id);
        Long iterationCount = iterationMapper.selectCountByQuery(
                QueryWrapper.create().where(C_PROJECT_ID.eq(id)));
        if (iterationCount != null && iterationCount > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "项目下仍有迭代，无法删除");
        }
        projectMapper.deleteById(id);
        projectProductMapper.deleteByQuery(QueryWrapper.create().where(C_PROJECT_ID.eq(id)));
    }

    /** 项目关联的产品 ID 集合 */
    public List<Long> productIds(Long projectId) {
        requireVisibleProject(projectId);
        return projectProductMapper.selectListByQuery(
                        QueryWrapper.create().where(C_PROJECT_ID.eq(projectId)))
                .stream().map(ProjectProduct::getProductId).toList();
    }

    /** 全量覆盖项目关联产品 */
    @Transactional
    public void bindProducts(Long projectId, List<Long> productIds) {
        requireVisibleProject(projectId);
        projectProductMapper.deleteByQuery(QueryWrapper.create().where(C_PROJECT_ID.eq(projectId)));
        if (productIds == null || productIds.isEmpty()) {
            return;
        }
        List<ProjectProduct> list = productIds.stream().distinct()
                .map(pid -> {
                    ProjectProduct pp = new ProjectProduct();
                    pp.setProjectId(projectId);
                    pp.setProductId(pid);
                    return pp;
                })
                .toList();
        projectProductMapper.insertBatch(list);
    }

    // ---------- 私有方法 ----------

    /** 取项目并断言可见性，不存在抛 404 */
    private Project requireVisibleProject(Long id) {
        Project project = projectMapper.selectOneById(id);
        if (project == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "项目不存在");
        }
        dataScopeService.assertProjectVisible(dataScopeService.currentUserId(), id);
        return project;
    }

    /** 项目编码唯一校验（excludeId 用于更新场景排除自身） */
    private void assertCodeUnique(String code, Long excludeId) {
        QueryWrapper qw = QueryWrapper.create().where(C_CODE.eq(code));
        if (excludeId != null) {
            qw.and(C_ID.ne(excludeId));
        }
        Long count = projectMapper.selectCountByQuery(qw);
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "项目编码已存在");
        }
    }

    private ProjectVo toVo(Project project) {
        ProjectVo vo = new ProjectVo();
        vo.setId(project.getId());
        vo.setName(project.getName());
        vo.setCode(project.getCode());
        vo.setDescription(project.getDescription());
        vo.setStatus(project.getStatus());
        vo.setStartDate(project.getStartDate());
        vo.setEndDate(project.getEndDate());
        List<ProjectProduct> links = projectProductMapper.selectListByQuery(
                QueryWrapper.create().where(C_PROJECT_ID.eq(project.getId())));
        List<Long> productIds = links.stream().map(ProjectProduct::getProductId).toList();
        vo.setProductIds(productIds);
        if (productIds.isEmpty()) {
            vo.setProductNames(List.of());
        } else {
            vo.setProductNames(productMapper.selectListByQuery(
                            QueryWrapper.create().where(new QueryColumn("id").in(productIds)))
                    .stream().map(Product::getName).toList());
        }
        return vo;
    }
}
